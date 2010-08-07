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

import com.google.android.maps.mytracks.R;

import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;

/**
 * Manage the application menus.
 * 
 * @author Sandor Dornbush
 */
public class MenuManager {

  private MyTracks activity;

  public MenuManager(MyTracks activity) {
    this.activity = activity;
  }
  
  public boolean onCreateOptionsMenu(Menu menu) {
    activity.getMenuInflater().inflate(R.menu.main, menu);
    return true;
  }

  public void onPrepareOptionsMenu(Menu menu, boolean hasRecorded,
      boolean isRecording, boolean hasSelectedTrack) {
    menu.findItem(R.id.menu_list_tracks).setEnabled(hasRecorded);
    menu.findItem(R.id.menu_list_markers)
        .setEnabled(hasRecorded && hasSelectedTrack);
    MenuItem startRecording = menu.findItem(R.id.menu_start_recording);
    startRecording.setEnabled(!isRecording);
    startRecording.setVisible(!isRecording);
    MenuItem stopRecording = menu.findItem(R.id.menu_stop_recording);
    stopRecording.setEnabled(isRecording);
    stopRecording.setVisible(isRecording);
  }
  
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.menu_start_recording: {
        activity.startRecording();
        return true;
      }
      case R.id.menu_stop_recording: {
        activity.stopRecording();
        return true;
      }
      case R.id.menu_list_tracks: {
        Intent startIntent = new Intent(activity, MyTracksList.class);
        activity.startActivityForResult(startIntent, MyTracksConstants.SHOW_TRACK);
        return true;
      }
      case R.id.menu_list_markers: {
        Intent startIntent = new Intent(activity, MyTracksWaypointsList.class);
        startIntent.putExtra("trackid", activity.getSelectedTrack());
        activity.startActivityForResult(startIntent, MyTracksConstants.SHOW_WAYPOINT);
        return true;
      }
      case R.id.menu_settings: {
        Intent startIntent = new Intent(activity, MyTracksSettings.class);
        activity.startActivity(startIntent);
        return true;
      }
      case R.id.menu_help: {
        Intent startIntent = new Intent(activity, WelcomeActivity.class);
        activity.startActivity(startIntent);
        return true;
      }
      case MyTracksConstants.MENU_CLEAR_MAP: {
        activity.setSelectedTrack(-1);
        return true;
      }
    }
    return false;
  }
}
