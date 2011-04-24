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

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;

import com.google.android.apps.mytracks.Constants;
import com.google.android.apps.mytracks.content.MyTracksProviderUtils;
import com.google.android.apps.mytracks.content.Track;
import com.google.android.apps.mytracks.services.TrackRecordingService;
import com.google.android.apps.mytracks.stats.TripStatistics;
import com.google.android.apps.mytracks.util.StringUtils;
import com.google.android.maps.mytracks.R;

/**
 * An AppWidgetProvider for displaying key track statistics (distance, time,
 * speed) from the current or most recent track.
 *
 * @author Paul R. Saxman
 */
public class MyTracksAppWidgetProvider extends AppWidgetProvider {
  @Override
  public void onReceive(Context context, Intent intent) {
    super.onReceive(context, intent);

    long trackId = intent.getLongExtra(
        context.getString(R.string.track_id_broadcast_extra), -1);
    String action = intent.getAction();

    Log.d(Constants.TAG,
        "MyTracksAppWidgetProvider.onReceive: trackId=" + trackId + ", action="
            + action);

    Track track = null;
    if (trackId != -1) {
      Log.d(Constants.TAG,
          "MyTracksAppWidgetProvider.onReceive: Retrieving specified track.");
      track = MyTracksProviderUtils.Factory.get(context).getTrack(trackId);
    } else {
      Log.d(Constants.TAG,
          "MyTracksAppWidgetProvider.onReceive: Attempting to retrieve previous track.");
      track = MyTracksProviderUtils.Factory.get(context).getLastTrack();
    }

    AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
    ComponentName widget = new ComponentName(context,
        MyTracksAppWidgetProvider.class);
    RemoteViews views = new RemoteViews(context.getPackageName(),
        R.layout.mytracks_appwidget);

    int[] appWidgetIds = appWidgetManager.getAppWidgetIds(widget);
    for (int appWidgetId : appWidgetIds) {
      updateViewButton(views, context, action);
      updateViewTrackStatistics(views, track, context);
      appWidgetManager.updateAppWidget(appWidgetId, views);
    }
  }

  /**
   * Update the widget's button with the appropriate intent and icon.
   *
   * @param views The RemoteViews containing the button
   * @param context The Context of the AppWidget
   * @param action The action broadcast from the track service
   */
  protected void updateViewButton(RemoteViews views, Context context, String action) {
    if (context.getString(R.string.track_started_broadcast_action).equals(action)
        || context.getString(R.string.track_updated_broadcast_action).equals(action)) {
      Intent intent = new Intent(context, TrackRecordingService.class);
      intent.setAction(context.getString(R.string.end_current_track_action));
      PendingIntent pendingIntent = PendingIntent.getService(context, 0,
          intent, PendingIntent.FLAG_UPDATE_CURRENT);
      views.setImageViewResource(R.id.mytracks_appwidget_button,
          R.drawable.widget_button_enabled);
      views.setOnClickPendingIntent(R.id.mytracks_appwidget_button,
          pendingIntent);
    } else {
      Intent intent = new Intent(context, TrackRecordingService.class);
      intent.setAction(context.getString(R.string.start_new_track_action));
      PendingIntent pendingIntent = PendingIntent.getService(context, 0,
          intent, PendingIntent.FLAG_UPDATE_CURRENT);
      views.setImageViewResource(R.id.mytracks_appwidget_button,
          R.drawable.widget_button_disabled);
      views.setOnClickPendingIntent(R.id.mytracks_appwidget_button,
          pendingIntent);
    }
  }

  /**
   * Update the specified widget's view with the distance, time, and speed of
   * the specified track.
   *
   * @param views The RemoteViews to update with statistics
   * @param context The Context of the AppWidget
   * @param track The track to extract statistics from.
   */
  protected void updateViewTrackStatistics(RemoteViews views, Track track,
      Context context) {
    String distance = "NA";
    String time = "NA";
    String speed = "NA";

    if (track != null) {
      // TODO(saxman) Re-enable once good strategy for displaying details is found.
      // Since we have a track, display stats details if stats pressed
//      Intent i = new Intent(context, StatsActivity.class);
//      PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, i, 0);
//      views.setOnClickPendingIntent(R.id.statistics, pendingIntent);

      TripStatistics stats = track.getStatistics();

      // convert meters to kilometers
      distance = StringUtils.formatDecimal(stats.getTotalDistance() / 1000)
          + " " + context.getString(R.string.kilometer);

      // convert ms to minutes
      time = StringUtils.formatDecimal(stats.getMovingTime() / 60000d)
          + " mins";

      if (!Double.isNaN(stats.getAverageMovingSpeed())) {
        // convert m/s to km/h
        speed = StringUtils.formatDecimal(stats.getAverageMovingSpeed() * 3.6)
            + " " + context.getString(R.string.kilometer_per_hour);
      }
    }

    views.setTextViewText(R.id.mytracks_appwidget_distance_text, distance);
    views.setTextViewText(R.id.mytracks_appwidget_time_text, time);
    views.setTextViewText(R.id.mytracks_appwidget_speed_text, speed);
  }
}
