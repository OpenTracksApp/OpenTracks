/*
 * Copyright 2010 Google Inc.
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
package com.google.android.apps.mytracks;

import static com.google.android.apps.mytracks.Constants.CHART_TAB_TAG;
import static com.google.android.apps.mytracks.Constants.MAP_TAB_TAG;

import com.google.android.maps.mytracks.R;

import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;

/**
 * Manage the application menus.
 *
 * @author Sandor Dornbush
 */
class MenuManager {

  private final TrackDetailActivity activity;
  private MenuItem recordTrack;
  private MenuItem stopRecording;
  
  public MenuManager(TrackDetailActivity activity) {
    this.activity = activity;
  }

  /**
   * Fills the menu with its initial options.
   *
   * @param menu the menu to fill
   * @return true on success, false otherwise
   */
  public boolean onCreateOptionsMenu(Menu menu) {
    activity.getMenuInflater().inflate(R.menu.track_detail, menu);

    // TODO: Replace search button with search widget if API level >= 11
    return true;
  }

  /**
   * Prepares the menu for display.
   *
   * @param menu the menu to update for display
   * @param hasRecorded whether any track has been recorded
   * @param isRecording whether we're recording a track now
   * @param hasSelectedTrack whether there's a track currently selected for display
   * @param isSatelliteMode whether the map is currently in satellite mode
   * @param currentTabTag the tag for the currently-displayed tab
   */
  public void onPrepareOptionsMenu(Menu menu, boolean hasRecorded,
      boolean isRecording, boolean hasSelectedTrack,
      boolean isSatelliteMode, String currentTabTag) {
    menu.findItem(R.id.menu_markers)
        .setEnabled(hasRecorded && hasSelectedTrack);
    
    recordTrack = menu.findItem(R.id.menu_record_track);
    stopRecording = menu.findItem(R.id.menu_stop_recording);
    updateActionItems(isRecording);
    
    menu.findItem(R.id.menu_chart_settings)
        .setVisible(CHART_TAB_TAG.equals(currentTabTag));

    boolean isMapTab = MAP_TAB_TAG.equals(currentTabTag);
    menu.findItem(R.id.menu_my_location)
        .setVisible(isMapTab);
    menu.findItem(R.id.menu_satellite_mode)
        .setVisible(isMapTab)
        .setTitle(isSatelliteMode ? R.string.menu_map_mode : R.string.menu_satellite_mode);
  }

  /**
   * Updates the action items.<br>
   * TODO: also update the action items when
   * <ul>
   * <li>start/stop a recording from the home screen widget.<il>
   * <li>receive a direct service call to start/stop a recording.<il>
   * </ul>
   *
   * @param isRecording true if recording a track
   */
  private void updateActionItems(boolean isRecording) {
    recordTrack.setEnabled(!isRecording).setVisible(!isRecording);
    stopRecording.setEnabled(isRecording).setVisible(isRecording);
  }

  /**
   * Called when an option from the menu is selected.
   *
   * @param item the selected item
   * @return true if the action was handled, false otherwise
   */
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.menu_record_track: {
        activity.startRecording();
        updateActionItems(true);
        return true;
      }
      case R.id.menu_stop_recording: {
        activity.stopRecording();
        updateActionItems(false);
        return true;
      }
      case R.id.menu_tracks: {
	    activity.startActivityForResult(new Intent(activity, TrackListActivity.class),
	    		Constants.SHOW_TRACK);
        return true;
      }
      case R.id.menu_markers: {
        Intent startIntent = new Intent(activity, WaypointsList.class);
        startIntent.putExtra("trackid", activity.getSelectedTrackId());
        activity.startActivityForResult(startIntent, Constants.SHOW_WAYPOINT);
        return true;
      }
      case R.id.menu_sensor_state: {
        return startActivity(SensorStateActivity.class);
      }
      case R.id.menu_settings: {
        return startActivity(SettingsActivity.class);
      }
      case R.id.menu_aggregated_statistics: {
        return startActivity(AggregatedStatsActivity.class);
      }
      case R.id.menu_help: {
        return startActivity(WelcomeActivity.class);
      }
      case R.id.menu_search: {
        // TODO: Pass the current track ID and current location to do some fancier ranking.
        activity.onSearchRequested();
        return true;
      }
      case R.id.menu_chart_settings: {
        activity.showChartSettings();
        return true;
      }
      case R.id.menu_my_location: {
        activity.showMyLocation();
        return true;
      }
      case R.id.menu_satellite_mode: {
        activity.toggleSatelliteView();
        return true;
      }
    }

    return false;
  }

  /**
   * Starts an activity with the given class.
   */
  private boolean startActivity(Class<? extends Activity> activityClass) {
    activity.startActivity(new Intent(activity, activityClass));
    return true;
  }
}
