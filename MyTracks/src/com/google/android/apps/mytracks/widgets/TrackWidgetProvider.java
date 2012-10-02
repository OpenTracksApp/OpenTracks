/*
 * Copyright 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.android.apps.mytracks.widgets;

import com.google.android.apps.mytracks.TrackDetailActivity;
import com.google.android.apps.mytracks.TrackListActivity;
import com.google.android.apps.mytracks.content.MyTracksProviderUtils;
import com.google.android.apps.mytracks.content.Track;
import com.google.android.apps.mytracks.services.ControlRecordingService;
import com.google.android.apps.mytracks.stats.TripStatistics;
import com.google.android.apps.mytracks.util.IntentUtils;
import com.google.android.apps.mytracks.util.PreferencesUtils;
import com.google.android.apps.mytracks.util.StringUtils;
import com.google.android.maps.mytracks.R;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.TaskStackBuilder;
import android.util.SparseBooleanArray;
import android.widget.RemoteViews;

/**
 * A track widget to start/stop/pause/resume recording, launch My Tracks, and
 * display track statistics (total distance, total time, average speed, and
 * moving time) for the recording track, the selected track or the last track.
 * 
 * @author Sandor Dornbush
 * @author Paul R. Saxman
 */
public class TrackWidgetProvider extends AppWidgetProvider {

  private static final int LARGE_HEIGHT = 110;
  // Array of appwidget id to use large size
  private static final SparseBooleanArray useLargeSize = new SparseBooleanArray();

  @Override
  public void onReceive(Context context, Intent intent) {
    super.onReceive(context, intent);
    String action = intent.getAction();
    if (AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(action)
        || context.getString(R.string.track_paused_broadcast_action).equals(action)
        || context.getString(R.string.track_resumed_broadcast_action).equals(action)
        || context.getString(R.string.track_started_broadcast_action).equals(action)
        || context.getString(R.string.track_stopped_broadcast_action).equals(action)
        || context.getString(R.string.track_update_broadcast_action).equals(action)) {
      long trackId = intent.getLongExtra(context.getString(R.string.track_id_broadcast_extra), -1L);

      AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
      int[] appWidgetIds = appWidgetManager.getAppWidgetIds(
          new ComponentName(context, TrackWidgetProvider.class));
      for (int appWidgetId : appWidgetIds) {
        RemoteViews remoteViews = getRemoteViews(context, trackId, useLargeSize.get(appWidgetId));
        appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
      }
    }
  }

  @TargetApi(16)
  @Override
  public void onAppWidgetOptionsChanged(
      Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {
    boolean large = newOptions == null
        || newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT) >= LARGE_HEIGHT;
    useLargeSize.put(appWidgetId, large);
    RemoteViews remoteViews = getRemoteViews(context, -1L, large);
    appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
  }

  /**
   * Updates the widget.
   * 
   * @param context the context
   * @param trackId the track id
   * @param large true to use the large layout
   */
  private RemoteViews getRemoteViews(Context context, long trackId, boolean large) {
    RemoteViews remoteViews = new RemoteViews(context.getPackageName(),
        large ? R.layout.track_widget_large : R.layout.track_widget_small);

    // Get the preferences
    long recordingTrackId = PreferencesUtils.getLong(context, R.string.recording_track_id_key);
    boolean isRecording = recordingTrackId != PreferencesUtils.RECORDING_TRACK_ID_DEFAULT;
    boolean recordingTrackPaused = PreferencesUtils.getBoolean(context,
        R.string.recording_track_paused_key, PreferencesUtils.RECORDING_TRACK_PAUSED_DEFAULT);
    boolean metricUnits = PreferencesUtils.getBoolean(
        context, R.string.metric_units_key, PreferencesUtils.METRIC_UNITS_DEFAULT);

    // Get track and trip statistics
    MyTracksProviderUtils myTracksProviderUtils = MyTracksProviderUtils.Factory.get(context);
    if (trackId == -1L) {
      trackId = recordingTrackId;
    }
    Track track = trackId != -1L ? myTracksProviderUtils.getTrack(trackId)
        : myTracksProviderUtils.getLastTrack();
    TripStatistics tripStatistics = track == null ? null : track.getTripStatistics();

    updateStatisticsContainer(context, remoteViews, track);
    updateMovingTime(context, remoteViews, tripStatistics);
    updateTotalDistance(context, remoteViews, tripStatistics, metricUnits);
    updateRecordButton(context, remoteViews, isRecording, recordingTrackPaused);
    updateStopButton(context, remoteViews, isRecording);
    if (large) {
      boolean reportSpeed = PreferencesUtils.getBoolean(
          context, R.string.report_speed_key, PreferencesUtils.REPORT_SPEED_DEFAULT);
      updateAverageMovingSpeed(context, remoteViews, tripStatistics, metricUnits, reportSpeed);
      updateAverageSpeed(context, remoteViews, tripStatistics, metricUnits, reportSpeed);
      updateRecordStatus(context, remoteViews, isRecording, recordingTrackPaused);
    }
    return remoteViews;
  }

  /**
   * Updates the statistics container.
   * 
   * @param context the context
   * @param remoteViews the remote views
   * @param track the track
   */
  private void updateStatisticsContainer(Context context, RemoteViews remoteViews, Track track) {
    Intent intent;
    if (track != null) {
      intent = IntentUtils.newIntent(context, TrackDetailActivity.class)
          .putExtra(TrackDetailActivity.EXTRA_TRACK_ID, track.getId());
    } else {
      intent = IntentUtils.newIntent(context, TrackListActivity.class);
    }
    TaskStackBuilder taskStackBuilder = TaskStackBuilder.from(context);
    taskStackBuilder.addNextIntent(intent);
    PendingIntent pendingIntent = taskStackBuilder.getPendingIntent(0, 0);
    remoteViews.setOnClickPendingIntent(R.id.track_widget_statistics, pendingIntent);
  }

  /**
   * Updates moving time.
   * 
   * @param context the context
   * @param remoteViews the remote views
   * @param tripStatistics the trip statistics
   */
  private void updateMovingTime(
      Context context, RemoteViews remoteViews, TripStatistics tripStatistics) {
    String movingTimeValue = tripStatistics == null ? context.getString(R.string.value_unknown)
        : StringUtils.formatElapsedTime(tripStatistics.getMovingTime());
    remoteViews.setTextViewText(R.id.track_widget_moving_time_value, movingTimeValue);
  }

  /**
   * Updates total distance.
   * 
   * @param context the context
   * @param remoteViews the remote views
   * @param tripStatistics the trip statistics
   * @param metricUnits true to use metric units
   */
  private void updateTotalDistance(Context context, RemoteViews remoteViews,
      TripStatistics tripStatistics, boolean metricUnits) {
    double distance = tripStatistics == null ? Double.NaN : tripStatistics.getTotalDistance();
    String[] distanceParts = StringUtils.getDistanceParts(context, distance, metricUnits);
    remoteViews.setTextViewText(R.id.track_widget_total_distance_value, distanceParts[0]);
    remoteViews.setTextViewText(R.id.track_widget_total_distance_unit, distanceParts[1]);
  }

  /**
   * Updates the record button.
   * 
   * @param context the context
   * @param remoteViews the remote views
   * @param isRecording true if recording
   * @param recordingTrackPaused true if recording track is paused
   */
  private void updateRecordButton(
      Context context, RemoteViews remoteViews, boolean isRecording, boolean recordingTrackPaused) {
    remoteViews.setImageViewResource(R.id.track_widget_record_button,
        isRecording && !recordingTrackPaused ? R.drawable.btn_pause : R.drawable.btn_record);
    int recordActionId;
    if (isRecording) {
      recordActionId = recordingTrackPaused ? R.string.track_action_resume
          : R.string.track_action_pause;
    } else {
      recordActionId = R.string.track_action_start;
    }
    Intent intent = new Intent(context, ControlRecordingService.class).setAction(
        context.getString(recordActionId));
    PendingIntent pendingIntent = PendingIntent.getService(
        context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    remoteViews.setOnClickPendingIntent(R.id.track_widget_record_button, pendingIntent);
  }

  /**
   * Updates the stop button.
   * 
   * @param context the context
   * @param remoteViews the remote views
   * @param isRecording true if recording
   */
  private void updateStopButton(Context context, RemoteViews remoteViews, boolean isRecording) {
    remoteViews.setImageViewResource(
        R.id.track_widget_stop_button, isRecording ? R.drawable.btn_stop_1 : R.drawable.btn_stop_0);
    remoteViews.setBoolean(R.id.track_widget_stop_button, "setEnabled", isRecording);
    if (isRecording) {
      Intent intent = new Intent(context, ControlRecordingService.class).setAction(
          context.getString(R.string.track_action_end));
      PendingIntent pendingIntent = PendingIntent.getService(
          context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
      remoteViews.setOnClickPendingIntent(R.id.track_widget_stop_button, pendingIntent);
    }
  }

  /**
   * Updates average moving speed.
   * 
   * @param context the context
   * @param remoteViews the remote views
   * @param tripStatistics the trip statistics
   * @param metricUnits true to use metric units
   * @param reportSpeed true to report speed
   */
  private void updateAverageMovingSpeed(Context context, RemoteViews remoteViews,
      TripStatistics tripStatistics, boolean metricUnits, boolean reportSpeed) {
    String averageMovingSpeedLabel = context.getString(
        reportSpeed ? R.string.stats_average_moving_speed : R.string.stats_average_moving_pace);
    remoteViews.setTextViewText(
        R.id.track_widget_average_moving_speed_label, averageMovingSpeedLabel);

    Double speed = tripStatistics == null ? Double.NaN : tripStatistics.getAverageMovingSpeed();
    String[] speedParts = StringUtils.getSpeedParts(context, speed, metricUnits, reportSpeed);
    remoteViews.setTextViewText(R.id.track_widget_average_moving_speed_value, speedParts[0]);
    remoteViews.setTextViewText(R.id.track_widget_average_moving_speed_unit, speedParts[1]);
  }

  /**
   * Updates average speed.
   * 
   * @param context the context
   * @param remoteViews the remote views
   * @param tripStatistics the trip statistics
   * @param metricUnits true to use metric units
   * @param reportSpeed true to report speed
   */
  private void updateAverageSpeed(Context context, RemoteViews remoteViews,
      TripStatistics tripStatistics, boolean metricUnits, boolean reportSpeed) {
    String averageSpeedLabel = context.getString(
        reportSpeed ? R.string.stats_average_speed : R.string.stats_average_pace);
    remoteViews.setTextViewText(R.id.track_widget_average_speed_label, averageSpeedLabel);

    Double speed = tripStatistics == null ? Double.NaN : tripStatistics.getAverageSpeed();
    String[] speedParts = StringUtils.getSpeedParts(context, speed, metricUnits, reportSpeed);
    remoteViews.setTextViewText(R.id.track_widget_average_speed_value, speedParts[0]);
    remoteViews.setTextViewText(R.id.track_widget_average_speed_unit, speedParts[1]);
  }

  /**
   * Updates recording status.
   * 
   * @param context the context
   * @param remoteViews the remote views
   * @param isRecording true if recording
   * @param recordingTrackPaused true if recording track is paused
   */
  private void updateRecordStatus(
      Context context, RemoteViews remoteViews, boolean isRecording, boolean recordingTrackPaused) {
    String status;
    int colorId;
    if (isRecording) {
      status = context.getString(
          recordingTrackPaused ? R.string.generic_paused : R.string.generic_recording);
      colorId = recordingTrackPaused ? android.R.color.white : R.color.red;
    } else {
      status = "";
      colorId = android.R.color.white;
    }
    remoteViews.setTextColor(
        R.id.track_widget_record_status, context.getResources().getColor(colorId));
    remoteViews.setTextViewText(R.id.track_widget_record_status, status);
  }
}
