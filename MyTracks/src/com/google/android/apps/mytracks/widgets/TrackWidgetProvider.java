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
 * A track widget to start/stop recording, launch My Tracks, and display track
 * statistics (distance, time, speed) for the selected track or the last track.
 * 
 * @author Sandor Dornbush
 * @author Paul R. Saxman
 */
public class TrackWidgetProvider extends AppWidgetProvider
    implements OnSharedPreferenceChangeListener {

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
      updateTrack(null);
    }
  }

  private final Handler handler;
  private Context context;
  private TrackObserver trackObserver;
  private MyTracksProviderUtils myTracksProviderUtils;

  private String unknown;
  private String trackStartedBroadcastAction;
  private String trackStoppedBroadcastAction;
  
  private SharedPreferences sharedPreferences;
  private long selectedTrackId;
  private boolean metricUnits;
  private boolean reportSpeed;
  private boolean useTotalTime;

  public TrackWidgetProvider() {
    super();
    handler = new Handler();
  }

  @Override
  public void onSharedPreferenceChanged(SharedPreferences preferences, String key) {
    if (key == null || PreferencesUtils.getKey(context, R.string.metric_units_key).equals(key)) {
      metricUnits = PreferencesUtils.getBoolean(
          context, R.string.metric_units_key, PreferencesUtils.METRIC_UNITS_DEFAULT);
    }
    if (key == null || PreferencesUtils.getKey(context, R.string.report_speed_key).equals(key)) {
      reportSpeed = PreferencesUtils.getBoolean(
          context, R.string.report_speed_key, PreferencesUtils.REPORT_SPEED_DEFAULT);
    }
    if (key == null
        || PreferencesUtils.getKey(context, R.string.stats_use_total_time_key).equals(key)) {
      useTotalTime = PreferencesUtils.getBoolean(context, R.string.stats_use_total_time_key,
          PreferencesUtils.STATS_USE_TOTAL_TIME_DEFAULT);
    }
    if (key == null
        || PreferencesUtils.getKey(context, R.string.selected_track_id_key).equals(key)) {
      selectedTrackId = PreferencesUtils.getLong(context, R.string.selected_track_id_key);
    }
    if (key != null) {
      updateTrack(null);
    }
  }

  @Override
  public void onReceive(Context aContext, Intent intent) {
    super.onReceive(aContext, intent);
    initialize(aContext);

    selectedTrackId = intent.getLongExtra(
        context.getString(R.string.track_id_broadcast_extra), selectedTrackId);
    String action = intent.getAction();

    if (AppWidgetManager.ACTION_APPWIDGET_ENABLED.equals(action)
        || AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(action)
        || trackStartedBroadcastAction.equals(action)
        || trackStoppedBroadcastAction.equals(action)) {
      updateTrack(action);
    }
  }

  @Override
  public void onDisabled(Context aContext) {
    if (sharedPreferences != null) {
      sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
    }
    if (trackObserver != null) {
      aContext.getContentResolver().unregisterContentObserver(trackObserver);
    }
  }

  /**
   * Initializes the widget.
   * 
   * @param aContext a context
   */
  private void initialize(Context aContext) {
    if (context != null) {
      // Already initialized
      return;
    }
    context = aContext;
    trackObserver = new TrackObserver();
    myTracksProviderUtils = MyTracksProviderUtils.Factory.get(context);
    
    unknown = context.getString(R.string.value_unknown);
    trackStartedBroadcastAction = context.getString(R.string.track_started_broadcast_action);
    trackStoppedBroadcastAction = context.getString(R.string.track_stopped_broadcast_action);
  
    sharedPreferences = context.getSharedPreferences(SETTINGS_NAME, Context.MODE_PRIVATE);
    sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    onSharedPreferenceChanged(sharedPreferences, null);
  
    context.getContentResolver()
        .registerContentObserver(TracksColumns.CONTENT_URI, true, trackObserver);
  }

  /**
   * Updates the widget.
   * 
   * @param action the action
   */
  private void updateTrack(String action) {
    RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.track_widget);

    if (action != null) {
      updateButton(remoteViews, action);
    }
    updateView(remoteViews);

    AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
    int[] appWidgetIds = appWidgetManager.getAppWidgetIds(
        new ComponentName(context, TrackWidgetProvider.class));
    for (int appWidgetId : appWidgetIds) {
      appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
    }
  }

  /**
   * Updates button to start/stop recording.
   * 
   * @param remoteViews the remote views
   * @param action the action
   */
  private void updateButton(RemoteViews remoteViews, String action) {
    boolean isRecording = trackStartedBroadcastAction.equals(action);
    remoteViews.setViewVisibility(R.id.track_widget_record_button, isRecording ? View.GONE
        : View.VISIBLE);
    remoteViews.setViewVisibility(R.id.track_widget_stop_button, isRecording ? View.VISIBLE
        : View.INVISIBLE);
    Intent intent = new Intent(context, ControlRecordingService.class).setAction(
        context.getString(isRecording ? R.string.track_action_end : R.string.track_action_start));
    PendingIntent pendingIntent = PendingIntent.getService(
        context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    remoteViews.setOnClickPendingIntent(isRecording ? R.id.track_widget_stop_button
        : R.id.track_widget_record_button, pendingIntent);
  }

  /**
   * Updates view.
   * 
   * @param remoteViews the remote views
   */
  private void updateView(RemoteViews remoteViews) {
    Track track = selectedTrackId != PreferencesUtils.SELECTED_TRACK_ID_DEFAULT 
        ? myTracksProviderUtils.getTrack(selectedTrackId) : myTracksProviderUtils.getLastTrack();
    TripStatistics tripStatistics = track == null ? null : track.getTripStatistics();
    String distance = tripStatistics == null ? unknown
        : StringUtils.formatDistance(context, tripStatistics.getTotalDistance(), metricUnits);
    int timeLabelId = useTotalTime ? R.string.stats_total_time : R.string.stats_moving_time;
    String time = tripStatistics == null ? unknown : StringUtils.formatElapsedTime(
        useTotalTime ? tripStatistics.getTotalTime()
            : tripStatistics.getMovingTime());
    int speedLabelId;
    if (useTotalTime) {
      speedLabelId = reportSpeed ? R.string.stats_average_speed : R.string.stats_average_pace;
    } else {
      speedLabelId = reportSpeed ? R.string.stats_average_moving_speed
          : R.string.stats_average_moving_pace;
    }

    String speed = tripStatistics == null ? unknown : StringUtils.formatSpeed(
        context, useTotalTime ? tripStatistics.getAverageSpeed()
            : tripStatistics.getAverageMovingSpeed(), metricUnits, reportSpeed);
    remoteViews.setTextViewText(R.id.track_widget_distance_value, distance);
    remoteViews.setTextViewText(R.id.track_widget_time_label, context.getString(timeLabelId));
    remoteViews.setTextViewText(R.id.track_widget_time_value, time);
    remoteViews.setTextViewText(R.id.track_widget_speed_label, context.getString(speedLabelId));
    remoteViews.setTextViewText(R.id.track_widget_speed_value, speed);

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
    remoteViews.setOnClickPendingIntent(
        R.id.track_widget_statistics, pendingIntent);
    remoteViews.setOnClickPendingIntent(R.id.track_widget_icon, pendingIntent);
  }
}
