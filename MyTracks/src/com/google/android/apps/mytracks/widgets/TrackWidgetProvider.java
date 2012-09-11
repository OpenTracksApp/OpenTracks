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

import static com.google.android.apps.mytracks.Constants.SETTINGS_NAME;

import com.google.android.apps.mytracks.TrackDetailActivity;
import com.google.android.apps.mytracks.TrackListActivity;
import com.google.android.apps.mytracks.content.MyTracksProviderUtils;
import com.google.android.apps.mytracks.content.Track;
import com.google.android.apps.mytracks.content.TracksColumns;
import com.google.android.apps.mytracks.services.ControlRecordingService;
import com.google.android.apps.mytracks.stats.TripStatistics;
import com.google.android.apps.mytracks.util.IntentUtils;
import com.google.android.apps.mytracks.util.PreferencesUtils;
import com.google.android.apps.mytracks.util.StringUtils;
import com.google.android.maps.mytracks.R;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.ContentObserver;
import android.os.Handler;
import android.support.v4.app.TaskStackBuilder;
import android.view.View;
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

  /**
   * Observer for track content.
   * 
   * @author Jimmy Shih
   */
  private class TrackObserver extends ContentObserver {

    public TrackObserver() {
      super(handler);
    }

    @Override
    public void onChange(boolean selfChange) {
      update();
    }
  }

  // Set in constructor
  private final Handler handler;

  // Set when initializing
  private Context context;
  private TrackObserver trackObserver;
  private MyTracksProviderUtils myTracksProviderUtils;
  private RemoteViews remoteViews;
  private String unknown;
  private SharedPreferences sharedPreferences;

  // Set if there is a track
  private TripStatistics tripStatistics;

  // Preferences
  private long selectedTrackId;
  private long recordingTrackId;
  private boolean recordingtrackPaused;
  private boolean metricUnits;
  private boolean reportSpeed;
  private boolean showMovingTime;

  public TrackWidgetProvider() {
    super();
    handler = new Handler();
  }

  private final OnSharedPreferenceChangeListener
      sharedPreferenceChangeListener = new OnSharedPreferenceChangeListener() {
          @Override
        public void onSharedPreferenceChanged(SharedPreferences preferences, String key) {
          if (key == null
              || key.equals(PreferencesUtils.getKey(context, R.string.selected_track_id_key))) {
            selectedTrackId = PreferencesUtils.getLong(context, R.string.selected_track_id_key);
          }
          if (key == null
              || key.equals(PreferencesUtils.getKey(context, R.string.recording_track_id_key))) {
            long oldValue = recordingTrackId;
            recordingTrackId = PreferencesUtils.getLong(context, R.string.recording_track_id_key);
            if (oldValue == PreferencesUtils.RECORDING_TRACK_ID_DEFAULT
                && recordingTrackId != PreferencesUtils.RECORDING_TRACK_ID_DEFAULT) {
              recordingtrackPaused = false;
            }
          }
          if (key == null || key.equals(
              PreferencesUtils.getKey(context, R.string.recording_track_paused_key))) {
            recordingtrackPaused = PreferencesUtils.getBoolean(context,
                R.string.recording_track_paused_key,
                PreferencesUtils.RECORDING_TRACK_PAUSED_DEFAULT);
          }
          if (key == null
              || key.equals(PreferencesUtils.getKey(context, R.string.metric_units_key))) {
            metricUnits = PreferencesUtils.getBoolean(
                context, R.string.metric_units_key, PreferencesUtils.METRIC_UNITS_DEFAULT);
          }
          if (key == null
              || key.equals(PreferencesUtils.getKey(context, R.string.report_speed_key))) {
            reportSpeed = PreferencesUtils.getBoolean(
                context, R.string.report_speed_key, PreferencesUtils.REPORT_SPEED_DEFAULT);
          }
          if (key == null || key.equals(
              PreferencesUtils.getKey(context, R.string.stats_show_moving_time_key))) {
            showMovingTime = PreferencesUtils.getBoolean(context,
                R.string.stats_show_moving_time_key,
                PreferencesUtils.STATS_SHOW_MOVING_TIME_DEFAULT);
          }
          if (key != null) {
            update();
          }
        }
      };

  @Override
  public void onReceive(Context aContext, Intent intent) {
    super.onReceive(aContext, intent);
    if (context == null) {
      // Not initialized
      context = aContext;
      trackObserver = new TrackObserver();
      myTracksProviderUtils = MyTracksProviderUtils.Factory.get(context);
      remoteViews = new RemoteViews(context.getPackageName(), R.layout.track_widget);

      unknown = context.getString(R.string.value_unknown);

      sharedPreferences = context.getSharedPreferences(SETTINGS_NAME, Context.MODE_PRIVATE);
      sharedPreferences.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
      sharedPreferenceChangeListener.onSharedPreferenceChanged(sharedPreferences, null);
      context.getContentResolver()
          .registerContentObserver(TracksColumns.CONTENT_URI, true, trackObserver);
    }
    String action = intent.getAction();
    if (AppWidgetManager.ACTION_APPWIDGET_ENABLED.equals(action)
        || AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(action)) {
      update();
    }
  }

  @Override
  public void onDisabled(Context aContext) {
    if (sharedPreferences != null) {
      sharedPreferences.unregisterOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
    }
    if (trackObserver != null) {
      aContext.getContentResolver().unregisterContentObserver(trackObserver);
    }
  }

  /**
   * Updates the widget.
   */
  private void update() {
    boolean isRecording = recordingTrackId != PreferencesUtils.RECORDING_TRACK_ID_DEFAULT;
    long trackId = isRecording ? recordingTrackId : selectedTrackId;
    Track track = trackId != -1L ? myTracksProviderUtils.getTrack(trackId)
        : myTracksProviderUtils.getLastTrack();
    tripStatistics = track == null ? null : track.getTripStatistics();

    updateStatisticsContainer(track);
    updateTotalDistance();
    updateTotalTime(isRecording);
    updateAverageSpeed();
    updateMovingTime();
    updateRecordButton(isRecording);
    updateRecordStatus(isRecording);
    updateStopButton(isRecording);

    AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
    int[] appWidgetIds = appWidgetManager.getAppWidgetIds(
        new ComponentName(context, TrackWidgetProvider.class));
    for (int appWidgetId : appWidgetIds) {
      appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
    }
  }

  /**
   * Updates the statistics container.
   * 
   * @param track the track.
   */
  private void updateStatisticsContainer(Track track) {
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
   * Updates total distance.
   */
  private void updateTotalDistance() {
    String totalDistanceValue = tripStatistics == null ? unknown
        : StringUtils.formatDistance(context, tripStatistics.getTotalDistance(), metricUnits);
    remoteViews.setTextViewText(R.id.track_widget_total_distance_value, totalDistanceValue);
  }

  /**
   * Updates total time.
   * 
   * @param isRecording true if recording
   */
  private void updateTotalTime(boolean isRecording) {
    String totalTimeValue;
    if (tripStatistics == null) {
      totalTimeValue = unknown;
    } else {
      long totalTime = tripStatistics.getTotalTime();
      if (isRecording && !recordingtrackPaused) {
        totalTime += System.currentTimeMillis() - tripStatistics.getStopTime();
      }
      totalTimeValue = StringUtils.formatElapsedTime(totalTime);
    }
    remoteViews.setTextViewText(R.id.track_widget_total_time_value, totalTimeValue);
  }

  /**
   * Updates average speed.
   */
  private void updateAverageSpeed() {
    String averageSpeedLabel = context.getString(
        reportSpeed ? R.string.stats_average_speed : R.string.stats_average_pace);
    remoteViews.setTextViewText(R.id.track_widget_average_speed_label, averageSpeedLabel);
    String averageSpeedValue = tripStatistics == null ? unknown
        : StringUtils.formatSpeed(
            context, tripStatistics.getAverageSpeed(), metricUnits, reportSpeed);
    remoteViews.setTextViewText(R.id.track_widget_average_speed_value, averageSpeedValue);
  }

  /**
   * Updates moving time.
   */
  private void updateMovingTime() {
    remoteViews.setViewVisibility(
        R.id.track_widget_moving_time_container, showMovingTime ? View.VISIBLE : View.GONE);
    if (showMovingTime) {
      String movingTimeValue = tripStatistics == null ? unknown
          : StringUtils.formatElapsedTime(tripStatistics.getMovingTime());
      remoteViews.setTextViewText(R.id.track_widget_moving_time_value, movingTimeValue);
    }
  }

  /**
   * Updates the record button.
   * 
   * @param isRecording true if recording
   */
  private void updateRecordButton(boolean isRecording) {
    remoteViews.setImageViewResource(R.id.track_widget_record_button,
        isRecording && !recordingtrackPaused ? R.drawable.btn_pause : R.drawable.btn_record);
    int recordActionId;
    if (isRecording) {
      recordActionId = recordingtrackPaused ? R.string.track_action_resume
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
   * Updates recording status.
   * 
   * @param isRecording true if recording
   */
  private void updateRecordStatus(boolean isRecording) {
    String status;
    int colorId;
    if (isRecording) {
      status = context.getString(
          recordingtrackPaused ? R.string.generic_paused : R.string.generic_recording);
      colorId = recordingtrackPaused ? android.R.color.white : R.color.red;
    } else {
      status = "";
      colorId = android.R.color.white;
    }
    remoteViews.setTextColor(
        R.id.track_widget_record_status, context.getResources().getColor(colorId));
    remoteViews.setTextViewText(R.id.track_widget_record_status, status);
  }

  /**
   * Updates the stop button.
   * 
   * @param isRecording true if recording
   */
  private void updateStopButton(boolean isRecording) {
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
}
