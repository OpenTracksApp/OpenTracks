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
import static com.google.android.apps.mytracks.Constants.TAG;

import com.google.android.apps.mytracks.MyTracks;
import com.google.android.apps.mytracks.content.MyTracksProviderUtils;
import com.google.android.apps.mytracks.content.Track;
import com.google.android.apps.mytracks.content.TracksColumns;
import com.google.android.apps.mytracks.services.ControlRecordingService;
import com.google.android.apps.mytracks.stats.TripStatistics;
import com.google.android.apps.mytracks.util.StringUtils;
import com.google.android.apps.mytracks.util.UnitConversions;
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
import android.util.Log;
import android.widget.RemoteViews;

/**
 * An AppWidgetProvider for displaying key track statistics (distance, time,
 * speed) from the current or most recent track.
 *
 * @author Sandor Dornbush
 * @author Paul R. Saxman
 */
public class TrackWidgetProvider
    extends AppWidgetProvider
    implements OnSharedPreferenceChangeListener {

  class TrackObserver extends ContentObserver {

    public TrackObserver() {
      super(contentHandler);
    }

    public void onChange(boolean selfChange) {
      updateTrack(null);
    }
  }

  private final Handler contentHandler;
  private MyTracksProviderUtils providerUtils;
  private Context context;
  private String unknown;
  private String distanceLabel;
  private String speedLabel;
  private String paceLabel;
  private TrackObserver trackObserver;
  private boolean isMetric;
  private boolean reportSpeed;
  private long selectedTrackId;
  private SharedPreferences sharedPreferences;
  private String TRACK_STARTED_ACTION;
  private String TRACK_STOPPED_ACTION;

  public TrackWidgetProvider() {
    super();
    contentHandler = new Handler();
    selectedTrackId = -1;
  }

  private void initialize(Context aContext) {
    if (this.context != null) {
      return;
    }
    this.context = aContext;
    trackObserver = new TrackObserver();
    providerUtils = MyTracksProviderUtils.Factory.get(context);
    unknown = context.getString(R.string.unknown);

    sharedPreferences = context.getSharedPreferences(SETTINGS_NAME, 0);
    sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    onSharedPreferenceChanged(sharedPreferences, null);

    context.getContentResolver().registerContentObserver(
        TracksColumns.CONTENT_URI, true, trackObserver);
    TRACK_STARTED_ACTION = context.getString(R.string.track_started_broadcast_action);
    TRACK_STOPPED_ACTION = context.getString(R.string.track_stopped_broadcast_action);
  }

  @Override
  public void onReceive(Context aContext, Intent intent) {
    super.onReceive(aContext, intent);
    initialize(aContext);

    selectedTrackId = intent.getLongExtra(
        context.getString(R.string.track_id_broadcast_extra), selectedTrackId);
    String action = intent.getAction();
    Log.d(TAG,
        "TrackWidgetProvider.onReceive: trackId=" + selectedTrackId + ", action=" + action);

    if (AppWidgetManager.ACTION_APPWIDGET_ENABLED.equals(action)
        || AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(action)
        || TRACK_STARTED_ACTION.equals(action)
        || TRACK_STOPPED_ACTION.equals(action)) {
      updateTrack(action);
    }
  }

  @Override
  public void onDisabled(Context aContext) {
    if (trackObserver != null) {
      aContext.getContentResolver().unregisterContentObserver(trackObserver);
    }
    if (sharedPreferences != null) {
      sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
    }
  }

  private void updateTrack(String action) {
    Track track = null;
    if (selectedTrackId != -1) {
      Log.d(TAG, "TrackWidgetProvider.updateTrack: Retrieving specified track.");
      track = providerUtils.getTrack(selectedTrackId);
    } else {
      Log.d(TAG, "TrackWidgetProvider.updateTrack: Attempting to retrieve previous track.");
      // TODO we should really read the pref.
      track = providerUtils.getLastTrack();
    }

    AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
    ComponentName widget = new ComponentName(context, TrackWidgetProvider.class);
    RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.track_widget);

    // Make all of the stats open the mytracks activity.
    Intent intent = new Intent(context, MyTracks.class);
    PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
    views.setOnClickPendingIntent(R.id.appwidget_track_statistics, pendingIntent);

    if (action != null) {
      updateViewButton(views, action);
    }
    updateViewTrackStatistics(views, track);
    int[] appWidgetIds = appWidgetManager.getAppWidgetIds(widget);
    for (int appWidgetId : appWidgetIds) {
      appWidgetManager.updateAppWidget(appWidgetId, views);
    }
  }

  /**
   * Update the widget's button with the appropriate intent and icon.
   *
   * @param views The RemoteViews containing the button
   * @param action The action broadcast from the track service
   */
  private void updateViewButton(RemoteViews views, String action) {
    if (TRACK_STARTED_ACTION.equals(action)) {
      // If a new track is started by this appwidget or elsewhere,
      // toggle the button to active and have it disable the track if pressed.
      setButtonIntent(
          views, R.string.end_current_track_action, R.drawable.appwidget_button_enabled);
    } else {
      // If a track is stopped by this appwidget or elsewhere,
      // toggle the button to inactive and have it start a new track if pressed.
      setButtonIntent(
          views, R.string.start_new_track_action, R.drawable.appwidget_button_disabled);
    }
  }

  /**
   * Set up the main widget button.
   *
   * @param views The widget views
   * @param action The resource id of the action to fire when the button is pressed
   * @param icon The resource id of the icon to show for the button
   */
  private void setButtonIntent(RemoteViews views, int action, int icon) {
    Intent intent = new Intent(context, ControlRecordingService.class);
    intent.setAction(context.getString(action));
    PendingIntent pendingIntent = PendingIntent.getService(context, 0,
        intent, PendingIntent.FLAG_UPDATE_CURRENT);
    views.setOnClickPendingIntent(R.id.appwidget_button, pendingIntent);
    views.setImageViewResource(R.id.appwidget_button, icon);
  }

  /**
   * Update the specified widget's view with the distance, time, and speed of
   * the specified track.
   *
   * @param views The RemoteViews to update with statistics
   * @param track The track to extract statistics from.
   */
  protected void updateViewTrackStatistics(RemoteViews views, Track track) {
    if (track == null) {
      views.setTextViewText(R.id.appwidget_distance_text, unknown);
      views.setTextViewText(R.id.appwidget_time_text, unknown);
      views.setTextViewText(R.id.appwidget_speed_text, unknown);
      return;
    }

    TripStatistics stats = track.getStatistics();

    // TODO replace this with format strings and miles.
    // convert meters to kilometers
    double displayDistance = stats.getTotalDistance() / 1000;
    if (!isMetric) {
      displayDistance *= UnitConversions.KM_TO_MI;
    }
    String distance =
        StringUtils.formatSingleDecimalPlace(displayDistance) + " " + this.distanceLabel;

    // convert ms to minutes
    String time = StringUtils.formatTime(stats.getMovingTime());
    String speed = unknown;
    if (!Double.isNaN(stats.getAverageMovingSpeed())) {
      // Convert m/s to km/h
      double displaySpeed = stats.getAverageMovingSpeed() * 3.6;
      if (!isMetric) {
        displaySpeed *= UnitConversions.KMH_TO_MPH;
      }
      if (reportSpeed) {
        speed = StringUtils.formatSingleDecimalPlace(displaySpeed) + " " + this.speedLabel;
      } else {
        long displayPace = (long) (3600000.0 / displaySpeed);
        speed = StringUtils.formatTime(displayPace) + " " + paceLabel;
      }
    }

    views.setTextViewText(R.id.appwidget_distance_text, distance);
    views.setTextViewText(R.id.appwidget_time_text, time);
    views.setTextViewText(R.id.appwidget_speed_text, speed);
  }

  @Override
  public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
    String metricUnitsKey = context.getString(R.string.metric_units_key);
    if (key == null || key.equals(metricUnitsKey)) {
      isMetric = prefs.getBoolean(metricUnitsKey, true);
      distanceLabel = context.getString(isMetric ? R.string.kilometer : R.string.mile);
      speedLabel = context.getString(isMetric ? R.string.kilometer_per_hour : R.string.mile_per_hour);
      paceLabel = context.getString(isMetric ? R.string.min_per_kilometer : R.string.min_per_mile);
    }

    String reportSpeedKey = context.getString(R.string.report_speed_key);
    if (key == null || key.equals(reportSpeedKey)) {
      reportSpeed = prefs.getBoolean(reportSpeedKey, true);
    }

    String selectedTrackKey = context.getString(R.string.selected_track_key);
    if (key == null || key.equals(selectedTrackKey)) {
      selectedTrackId = prefs.getLong(selectedTrackKey, -1);
      Log.d(TAG, "TrackWidgetProvider setting selecting track from preference: " + selectedTrackId);
    }
  }
}
