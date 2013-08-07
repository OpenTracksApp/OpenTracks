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
import com.google.android.apps.mytracks.util.ApiAdapterFactory;
import com.google.android.apps.mytracks.util.IntentUtils;
import com.google.android.apps.mytracks.util.PreferencesUtils;
import com.google.android.apps.mytracks.util.StringUtils;
import com.google.android.maps.mytracks.R;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
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

  public static final int KEYGUARD_DEFAULT_SIZE = 1;
  public static final int HOME_SCREEN_DEFAULT_SIZE = 2;

  private static final int TWO_CELLS = 110;
  private static final int THREE_CELLS = 180;
  private static final int FOUR_CELLS = 250;

  private static final int[] ITEM1_IDS = { R.id.track_widget_item1_label,
      R.id.track_widget_item1_value, R.id.track_widget_item1_unit,
      R.id.track_widget_item1_chronometer };
  private static final int[] ITEM2_IDS = { R.id.track_widget_item2_label,
      R.id.track_widget_item2_value, R.id.track_widget_item2_unit,
      R.id.track_widget_item2_chronometer };
  private static final int[] ITEM3_IDS = { R.id.track_widget_item3_label,
      R.id.track_widget_item3_value, R.id.track_widget_item3_unit,
      R.id.track_widget_item3_chronometer };
  private static final int[] ITEM4_IDS = { R.id.track_widget_item4_label,
      R.id.track_widget_item4_value, R.id.track_widget_item4_unit,
      R.id.track_widget_item4_chronometer };

  @Override
  public void onReceive(Context context, Intent intent) {
    super.onReceive(context, intent);
    String action = intent.getAction();
    if (context.getString(R.string.track_paused_broadcast_action).equals(action)
        || context.getString(R.string.track_resumed_broadcast_action).equals(action)
        || context.getString(R.string.track_started_broadcast_action).equals(action)
        || context.getString(R.string.track_stopped_broadcast_action).equals(action)
        || context.getString(R.string.track_update_broadcast_action).equals(action)) {
      long trackId = intent.getLongExtra(context.getString(R.string.track_id_broadcast_extra), -1L);
      updateAllAppWidgets(context, trackId);
    }
  }

  @Override
  public void onEnabled(Context context) {
    super.onEnabled(context);
    // Need to update all app widgets after phone reboot
    updateAllAppWidgets(context, -1L);
  }

  @Override
  public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
    super.onUpdate(context, appWidgetManager, appWidgetIds);
    // Need to update all app widgets after software update
    updateAllAppWidgets(context, -1L);
  }

  @TargetApi(16)
  @Override
  public void onAppWidgetOptionsChanged(
      Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {
    super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);
    if (newOptions != null) {
      int newSize;
      if (newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY, -1)
          == AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD) {
        newSize = 1;
      } else {
        int height = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT);
        if (height == 0) {
          newSize = 2;
        } else if (height >= FOUR_CELLS) {
          newSize = 4;
        } else if (height >= THREE_CELLS) {
          newSize = 3;
        } else if (height >= TWO_CELLS) {
          newSize = 2;
        } else {
          newSize = 1;
        }
      }
      int size = ApiAdapterFactory.getApiAdapter().getAppWidgetSize(appWidgetManager, appWidgetId);
      if (size != newSize) {
        ApiAdapterFactory.getApiAdapter().setAppWidgetSize(appWidgetManager, appWidgetId, newSize);
        updateAppWidget(context, appWidgetManager, appWidgetId, -1L);
      }
    }
  }

  /**
   * Updates an app widget.
   * 
   * @param context the context
   * @param appWidgetManager the app widget manager
   * @param appWidgetId the app widget id
   * @param trackId the track id. -1L to not specify one
   */
  public static void updateAppWidget(
      Context context, AppWidgetManager appWidgetManager, int appWidgetId, long trackId) {
    int size = ApiAdapterFactory.getApiAdapter().getAppWidgetSize(appWidgetManager, appWidgetId);
    RemoteViews remoteViews = getRemoteViews(context, trackId, size);
    appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
  }
  
  /**
   * Updates all app widgets.
   * 
   * @param context the context
   * @param trackId track id
   */
  private static void updateAllAppWidgets(Context context, long trackId) {
    AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
    int[] appWidgetIds = appWidgetManager.getAppWidgetIds(
        new ComponentName(context, TrackWidgetProvider.class));
    for (int appWidgetId : appWidgetIds) {
      updateAppWidget(context, appWidgetManager, appWidgetId, trackId);
    }
  }

  /**
   * Gets the remote views.
   * 
   * @param context the context
   * @param trackId the track id
   * @param heightSize the layout height size
   */
  private static RemoteViews getRemoteViews(Context context, long trackId, int heightSize) {
    int layout;
    switch (heightSize) {
      case 4:
        layout = R.layout.track_widget_4x4;
        break;
      case 3:
        layout = R.layout.track_widget_4x3;
        break;
      case 2:
        layout = R.layout.track_widget_4x2;
        break;
      case 1:
        layout = R.layout.track_widget_4x1;
        break;
      default:
        layout = R.layout.track_widget_4x2;
        break;
    }
    RemoteViews remoteViews = new RemoteViews(context.getPackageName(), layout);

    // Get the preferences
    long recordingTrackId = PreferencesUtils.getLong(context, R.string.recording_track_id_key);
    boolean isRecording = recordingTrackId != PreferencesUtils.RECORDING_TRACK_ID_DEFAULT;
    boolean isPaused = PreferencesUtils.getBoolean(context, R.string.recording_track_paused_key,
        PreferencesUtils.RECORDING_TRACK_PAUSED_DEFAULT);
    boolean metricUnits = PreferencesUtils.isMetricUnits(context);
    boolean reportSpeed = PreferencesUtils.getBoolean(
        context, R.string.report_speed_key, PreferencesUtils.REPORT_SPEED_DEFAULT);
    int item1 = PreferencesUtils.getInt(
        context, R.string.track_widget_item1, PreferencesUtils.TRACK_WIDGET_ITEM1_DEFAULT);
    int item2 = PreferencesUtils.getInt(
        context, R.string.track_widget_item2, PreferencesUtils.TRACK_WIDGET_ITEM2_DEFAULT);

    // Get track and trip statistics
    MyTracksProviderUtils myTracksProviderUtils = MyTracksProviderUtils.Factory.get(context);
    if (trackId == -1L) {
      trackId = recordingTrackId;
    }
    Track track = trackId != -1L ? myTracksProviderUtils.getTrack(trackId)
        : myTracksProviderUtils.getLastTrack();
    TripStatistics tripStatistics = track == null ? null : track.getTripStatistics();

    updateStatisticsContainer(context, remoteViews, track);
    setItem(context, remoteViews, ITEM1_IDS, item1, tripStatistics, isRecording, isPaused,
        metricUnits, reportSpeed);
    setItem(context, remoteViews, ITEM2_IDS, item2, tripStatistics, isRecording, isPaused,
        metricUnits, reportSpeed);

    updateRecordButton(context, remoteViews, isRecording, isPaused);
    updateStopButton(context, remoteViews, isRecording);
    if (heightSize > 1) {
      int item3 = PreferencesUtils.getInt(
          context, R.string.track_widget_item3, PreferencesUtils.TRACK_WIDGET_ITEM3_DEFAULT);
      int item4 = PreferencesUtils.getInt(
          context, R.string.track_widget_item4, PreferencesUtils.TRACK_WIDGET_ITEM4_DEFAULT);
      setItem(context, remoteViews, ITEM3_IDS, item3, tripStatistics, isRecording, isPaused,
          metricUnits, reportSpeed);
      setItem(context, remoteViews, ITEM4_IDS, item4, tripStatistics, isRecording, isPaused,
          metricUnits, reportSpeed);
      updateRecordStatus(context, remoteViews, isRecording, isPaused);
    }
    return remoteViews;
  }

  /**
   * Sets a widget item.
   * 
   * @param context the context
   * @param remoteViews the remote view
   * @param ids the item's ids
   * @param value the item value
   * @param tripStatistics the trip statistics
   * @param metricUnits true to use metric units
   * @param reportSpeed try to report speed
   */
  private static void setItem(Context context, RemoteViews remoteViews, int[] ids, int value,
      TripStatistics tripStatistics, boolean isRecording, boolean isPaused, boolean metricUnits,
      boolean reportSpeed) {
    switch (value) {
      case 0:
        updateDistance(context, remoteViews, ids, tripStatistics, metricUnits);
        break;
      case 1:
        updateTotalTime(context, remoteViews, ids, tripStatistics, isRecording, isPaused);
        break;
      case 2:
        updateAverageSpeed(context, remoteViews, ids, tripStatistics, metricUnits, reportSpeed);
        break;
      case 3:
        updateMovingTime(context, remoteViews, ids, tripStatistics);
        break;
      case 4:
        updateAverageMovingSpeed(
            context, remoteViews, ids, tripStatistics, metricUnits, reportSpeed);
        break;
      default:
        updateDistance(context, remoteViews, ids, tripStatistics, metricUnits);
        break;

    }
    if (value != 1) {
      remoteViews.setViewVisibility(ids[1], View.VISIBLE);
      remoteViews.setViewVisibility(ids[2], View.VISIBLE);
      remoteViews.setViewVisibility(ids[3], View.GONE);
      remoteViews.setChronometer(ids[3], SystemClock.elapsedRealtime(), null, false);
    }
  }

  /**
   * Updates the statistics container.
   * 
   * @param context the context
   * @param remoteViews the remote views
   * @param track the track
   */
  private static void updateStatisticsContainer(
      Context context, RemoteViews remoteViews, Track track) {
    PendingIntent pendingIntent;
    if (track != null) {
      Intent intent = IntentUtils.newIntent(context, TrackDetailActivity.class)
          .putExtra(TrackDetailActivity.EXTRA_TRACK_ID, track.getId());
      pendingIntent = TaskStackBuilder.create(context)
          .addParentStack(TrackDetailActivity.class).addNextIntent(intent).getPendingIntent(0, 0);
    } else {
      Intent intent = IntentUtils.newIntent(context, TrackListActivity.class);
      pendingIntent = TaskStackBuilder.create(context).addNextIntent(intent).getPendingIntent(0, 0);
    }
    remoteViews.setOnClickPendingIntent(R.id.track_widget_stats_container, pendingIntent);
  }

  /**
   * Updates distance.
   * 
   * @param context the context
   * @param remoteViews the remote views
   * @param ids the item's ids
   * @param tripStatistics the trip statistics
   * @param metricUnits true to use metric units
   */
  private static void updateDistance(Context context, RemoteViews remoteViews, int[] ids,
      TripStatistics tripStatistics, boolean metricUnits) {
    double totalDistance = tripStatistics == null ? Double.NaN : tripStatistics.getTotalDistance();
    String[] totalDistanceParts = StringUtils.getDistanceParts(context, totalDistance, metricUnits);
    if (totalDistanceParts[0] == null) {
      totalDistanceParts[0] = context.getString(R.string.value_unknown);
    }    
    remoteViews.setTextViewText(ids[0], context.getString(R.string.stats_distance));
    remoteViews.setTextViewText(ids[1], totalDistanceParts[0]);
    remoteViews.setTextViewText(ids[2], totalDistanceParts[1]);
  }

  /**
   * Updates total time.
   * 
   * @param context the context
   * @param remoteViews the remote views
   * @param ids the item's ids
   * @param tripStatistics the trip statistics
   */
  private static void updateTotalTime(Context context, RemoteViews remoteViews, int[] ids,
      TripStatistics tripStatistics, boolean isRecording, boolean isPaused) {
    if (isRecording && !isPaused && tripStatistics != null) {
      long time = tripStatistics.getTotalTime() + System.currentTimeMillis()
          - tripStatistics.getStopTime();
      remoteViews.setChronometer(ids[3], SystemClock.elapsedRealtime() - time, null, true);
      remoteViews.setViewVisibility(ids[1], View.GONE);
      remoteViews.setViewVisibility(ids[2], View.GONE);
      remoteViews.setViewVisibility(ids[3], View.VISIBLE);
    } else {
      remoteViews.setChronometer(ids[3], SystemClock.elapsedRealtime(), null, false);
      remoteViews.setViewVisibility(ids[1], View.VISIBLE);
      remoteViews.setViewVisibility(ids[2], View.GONE);
      remoteViews.setViewVisibility(ids[3], View.GONE);

      String totalTime = tripStatistics == null ? context.getString(R.string.value_unknown)
          : StringUtils.formatElapsedTime(tripStatistics.getTotalTime());
      remoteViews.setTextViewText(ids[0], context.getString(R.string.stats_total_time));
      remoteViews.setTextViewText(ids[1], totalTime);
    }
  }

  /**
   * Updates average speed.
   * 
   * @param context the context
   * @param remoteViews the remote views
   * @param ids the item's ids
   * @param tripStatistics the trip statistics
   * @param metricUnits true to use metric units
   * @param reportSpeed true to report speed
   */
  private static void updateAverageSpeed(Context context, RemoteViews remoteViews, int[] ids,
      TripStatistics tripStatistics, boolean metricUnits, boolean reportSpeed) {
    String averageSpeedLabel = context.getString(
        reportSpeed ? R.string.stats_average_speed : R.string.stats_average_pace);
    remoteViews.setTextViewText(ids[0], averageSpeedLabel);

    Double speed = tripStatistics == null ? Double.NaN : tripStatistics.getAverageSpeed();
    String[] speedParts = StringUtils.getSpeedParts(context, speed, metricUnits, reportSpeed);
    
    if (speedParts[0] == null) {
      speedParts[0] = context.getString(R.string.value_unknown);
    }
    
    remoteViews.setTextViewText(ids[1], speedParts[0]);
    remoteViews.setTextViewText(ids[2], speedParts[1]);
  }

  /**
   * Updates moving time.
   * 
   * @param context the context
   * @param remoteViews the remote views
   * @param ids the item's ids
   * @param tripStatistics the trip statistics
   */
  private static void updateMovingTime(
      Context context, RemoteViews remoteViews, int[] ids, TripStatistics tripStatistics) {
    String movingTime = tripStatistics == null ? context.getString(R.string.value_unknown)
        : StringUtils.formatElapsedTime(tripStatistics.getMovingTime());
    remoteViews.setTextViewText(ids[0], context.getString(R.string.stats_moving_time));
    remoteViews.setTextViewText(ids[1], movingTime);
    remoteViews.setViewVisibility(ids[2], View.GONE);
  }

  /**
   * Updates average moving speed.
   * 
   * @param context the context
   * @param remoteViews the remote views
   * @param ids the item's ids
   * @param tripStatistics the trip statistics
   * @param metricUnits true to use metric units
   * @param reportSpeed true to report speed
   */
  private static void updateAverageMovingSpeed(Context context, RemoteViews remoteViews, int[] ids,
      TripStatistics tripStatistics, boolean metricUnits, boolean reportSpeed) {
    String averageMovingSpeedLabel = context.getString(
        reportSpeed ? R.string.stats_average_moving_speed : R.string.stats_average_moving_pace);
    remoteViews.setTextViewText(ids[0], averageMovingSpeedLabel);

    Double speed = tripStatistics == null ? Double.NaN : tripStatistics.getAverageMovingSpeed();
    String[] speedParts = StringUtils.getSpeedParts(context, speed, metricUnits, reportSpeed);
    
    if (speedParts[0] == null) {
      speedParts[0] = context.getString(R.string.value_unknown);
    }
    
    remoteViews.setTextViewText(ids[1], speedParts[0]);
    remoteViews.setTextViewText(ids[2], speedParts[1]);
  }

  /**
   * Updates the record button.
   * 
   * @param context the context
   * @param remoteViews the remote views
   * @param isRecording true if recording
   * @param recordingTrackPaused true if recording track is paused
   */
  private static void updateRecordButton(
      Context context, RemoteViews remoteViews, boolean isRecording, boolean recordingTrackPaused) {
    remoteViews.setImageViewResource(R.id.track_widget_record_button,
        isRecording && !recordingTrackPaused ? R.drawable.button_pause : R.drawable.button_record);
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
  private static void updateStopButton(
      Context context, RemoteViews remoteViews, boolean isRecording) {
    remoteViews.setImageViewResource(R.id.track_widget_stop_button,
        isRecording ? R.drawable.button_stop : R.drawable.ic_button_stop_disabled);
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
   * Updates recording status.
   * 
   * @param context the context
   * @param remoteViews the remote views
   * @param isRecording true if recording
   * @param recordingTrackPaused true if recording track is paused
   */
  private static void updateRecordStatus(
      Context context, RemoteViews remoteViews, boolean isRecording, boolean recordingTrackPaused) {
    String status;
    int colorId;
    if (isRecording) {
      status = context.getString(
          recordingTrackPaused ? R.string.generic_paused : R.string.generic_recording);
      colorId = recordingTrackPaused ? android.R.color.white : R.color.recording_text;
    } else {
      status = "";
      colorId = android.R.color.white;
    }
    remoteViews.setTextColor(
        R.id.track_widget_record_status, context.getResources().getColor(colorId));
    remoteViews.setTextViewText(R.id.track_widget_record_status, status);
  }
}
