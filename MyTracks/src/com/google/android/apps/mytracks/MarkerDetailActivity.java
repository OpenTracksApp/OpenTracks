/*
 * Copyright 2009 Google Inc.
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

import com.google.android.apps.mytracks.content.MyTracksProviderUtils;
import com.google.android.apps.mytracks.content.Track;
import com.google.android.apps.mytracks.content.Waypoint;
import com.google.android.apps.mytracks.content.Waypoint.WaypointType;
import com.google.android.apps.mytracks.fragments.DeleteMarkerDialogFragment;
import com.google.android.apps.mytracks.fragments.DeleteMarkerDialogFragment.DeleteMarkerCaller;
import com.google.android.apps.mytracks.util.IntentUtils;
import com.google.android.apps.mytracks.util.StatsUtils;
import com.google.android.maps.mytracks.R;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

/**
 * An activity to display marker detail info.
 * 
 * @author Leif Hendrik Wilden
 */
public class MarkerDetailActivity extends AbstractMyTracksActivity implements DeleteMarkerCaller {

  public static final String EXTRA_MARKER_ID = "marker_id";
  private static final String TAG = MarkerDetailActivity.class.getSimpleName();

  private MyTracksProviderUtils myTracksProviderUtils;
  private long markerId;
  private Waypoint waypoint;

  private TextView name;
  private View waypointSection;
  private View statisticsSection;

  @Override
  protected void onCreate(Bundle bundle) {
    super.onCreate(bundle);

    myTracksProviderUtils = MyTracksProviderUtils.Factory.get(this);
    markerId = getIntent().getLongExtra(EXTRA_MARKER_ID, -1L);
    if (markerId == -1L) {
      Log.d(TAG, "invalid marker id");
      finish();
      return;
    }
    name = (TextView) findViewById(R.id.marker_detail_name);
    waypointSection = findViewById(R.id.marker_detail_waypoint_section);
    statisticsSection = findViewById(R.id.marker_detail_statistics_section);
  }

  @Override
  protected int getLayoutResId() {
    return R.layout.marker_detail;
  }

  @Override
  protected void onResume() {
    super.onResume();
    waypoint = MyTracksProviderUtils.Factory.get(this).getWaypoint(markerId);
    if (waypoint == null) {
      Log.d(TAG, "waypoint is null");
      finish();
      return;
    }
    name.setText(getString(R.string.generic_name_line, waypoint.getName()));
    if (waypoint.getType() == WaypointType.WAYPOINT) {
      waypointSection.setVisibility(View.VISIBLE);
      statisticsSection.setVisibility(View.GONE);

      TextView markerType = (TextView) findViewById(R.id.marker_detail_waypoint_marker_type);
      markerType.setText(
          getString(R.string.marker_detail_waypoint_marker_type, waypoint.getCategory()));
      TextView description = (TextView) findViewById(R.id.marker_detail_waypoint_description);
      description.setText(getString(R.string.generic_description_line, waypoint.getDescription()));
    } else {
      waypointSection.setVisibility(View.GONE);
      statisticsSection.setVisibility(View.VISIBLE);
      StatsUtils.setTripStatisticsValues(this, waypoint.getTripStatistics());
      StatsUtils.setLocationValues(this, waypoint.getLocation(), false);
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.marker_detail, menu);

    Track track = myTracksProviderUtils.getTrack(waypoint.getTrackId());
    boolean isSharedWithMe = track != null ? track.isSharedWithMe() : true;
    
    menu.findItem(R.id.marker_detail_edit).setVisible(!isSharedWithMe);
    menu.findItem(R.id.marker_detail_delete).setVisible(!isSharedWithMe);
    return true;
  }

  @Override
  protected void onHomeSelected() {
    Intent intent = IntentUtils.newIntent(this, MarkerListActivity.class)
        .putExtra(MarkerListActivity.EXTRA_TRACK_ID, waypoint.getTrackId());
    startActivity(intent);
    finish();
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    Intent intent;
    switch (item.getItemId()) {
      case R.id.marker_detail_show_on_map:
        intent = IntentUtils.newIntent(this, TrackDetailActivity.class)
            .putExtra(TrackDetailActivity.EXTRA_MARKER_ID, markerId);
        startActivity(intent);
        return true;
      case R.id.marker_detail_edit:
        intent = IntentUtils.newIntent(this, MarkerEditActivity.class)
            .putExtra(MarkerEditActivity.EXTRA_MARKER_ID, markerId);
        startActivity(intent);
        return true;
      case R.id.marker_detail_delete:
        DeleteMarkerDialogFragment.newInstance(new long[] { markerId })
            .show(getSupportFragmentManager(), DeleteMarkerDialogFragment.DELETE_MARKER_DIALOG_TAG);
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  @Override
  public void onDeleteMarkerDone() {
    runOnUiThread(new Runnable() {
        @Override
      public void run() {
        finish();
      }
    });
  }
}
