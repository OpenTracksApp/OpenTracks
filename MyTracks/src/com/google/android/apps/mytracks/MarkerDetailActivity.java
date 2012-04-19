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
import com.google.android.apps.mytracks.content.Waypoint;
import com.google.android.apps.mytracks.fragments.DeleteOneMarkerDialogFragment;
import com.google.android.apps.mytracks.stats.TripStatistics;
import com.google.android.apps.mytracks.util.ApiAdapterFactory;
import com.google.android.apps.mytracks.util.StatsUtils;
import com.google.android.maps.mytracks.R;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.FragmentActivity;
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
public class MarkerDetailActivity extends FragmentActivity {

  public static final String EXTRA_MARKER_ID = "marker_id";
  private static final String TAG = MarkerDetailActivity.class.getSimpleName();

  private long markerId;
  private Waypoint waypoint;

  @Override
  protected void onCreate(Bundle bundle) {
    super.onCreate(bundle);
    setVolumeControlStream(TextToSpeech.Engine.DEFAULT_STREAM);
    ApiAdapterFactory.getApiAdapter().configureActionBarHomeAsUp(this);
    setContentView(R.layout.marker_detail);

    markerId = getIntent().getLongExtra(EXTRA_MARKER_ID, -1L);
    if (markerId == -1L) {
      Log.d(TAG, "invalid marker id");
      finish();
      return;
    }

    waypoint = MyTracksProviderUtils.Factory.get(this).getWaypoint(markerId);
    if (waypoint == null) {
      Log.d(TAG, "waypoint is null");
      finish();
      return;
    }

    TextView name = (TextView) findViewById(R.id.marker_detail_name);
    name.setText(getString(R.string.marker_detail_name, waypoint.getName()));
    View waypointSection = findViewById(R.id.marker_detail_waypoint_section);
    View statisticsSection = findViewById(R.id.marker_detail_statistics_section);

    if (waypoint.getType() == Waypoint.TYPE_WAYPOINT) {
      waypointSection.setVisibility(View.VISIBLE);
      statisticsSection.setVisibility(View.GONE);

      TextView markerType = (TextView) findViewById(R.id.marker_detail_marker_type);
      markerType.setText(getString(R.string.marker_detail_marker_type, waypoint.getCategory()));
      TextView description = (TextView) findViewById(R.id.marker_detail_description);
      description.setText(getString(R.string.marker_detail_description, waypoint.getDescription()));

    } else {
      waypointSection.setVisibility(View.GONE);
      statisticsSection.setVisibility(View.VISIBLE);

      SharedPreferences preferences = getSharedPreferences(
          Constants.SETTINGS_NAME, Context.MODE_PRIVATE);
      boolean metricUnits = preferences.getBoolean(getString(R.string.metric_units_key), true);
      boolean reportSpeed = preferences.getBoolean(getString(R.string.report_speed_key), true);

      StatsUtils.setSpeedLabel(this, R.id.marker_detail_max_speed_label, R.string.stat_max_speed,
          R.string.stat_fastest_pace, reportSpeed);
      StatsUtils.setSpeedLabel(this, R.id.marker_detail_average_speed_label,
          R.string.stat_average_speed, R.string.stat_average_pace, reportSpeed);
      StatsUtils.setSpeedLabel(this, R.id.marker_detail_average_moving_speed_label,
          R.string.stat_average_moving_speed, R.string.stat_average_moving_pace, reportSpeed);

      TripStatistics tripStatistics = waypoint.getStatistics();

      StatsUtils.setDistanceValue(this, R.id.marker_detail_total_distance_value,
          tripStatistics.getTotalDistance(), metricUnits);
      StatsUtils.setSpeedValue(this, R.id.marker_detail_max_speed_value,
          tripStatistics.getMaxSpeed(), reportSpeed, metricUnits);

      StatsUtils.setTimeValue(
          this, R.id.marker_detail_total_time_value, tripStatistics.getTotalTime());
      StatsUtils.setSpeedValue(this, R.id.marker_detail_average_speed_value,
          tripStatistics.getAverageSpeed(), reportSpeed, metricUnits);

      StatsUtils.setTimeValue(
          this, R.id.marker_detail_moving_time_value, tripStatistics.getMovingTime());
      StatsUtils.setSpeedValue(this, R.id.marker_detail_average_moving_speed_value,
          tripStatistics.getAverageMovingSpeed(), reportSpeed, metricUnits);

      StatsUtils.setAltitudeValue(this, R.id.marker_detail_elevation_value,
          waypoint.getLocation().getAltitude(), metricUnits);
      StatsUtils.setAltitudeValue(this, R.id.marker_detail_elevation_gain_value,
          tripStatistics.getTotalElevationGain(), metricUnits);

      StatsUtils.setAltitudeValue(this, R.id.marker_detail_min_elevation_value,
          tripStatistics.getMinElevation(), metricUnits);
      StatsUtils.setAltitudeValue(this, R.id.marker_detail_max_elevation_value,
          tripStatistics.getMaxElevation(), metricUnits);

      StatsUtils.setGradeValue(
          this, R.id.marker_detail_min_grade_value, tripStatistics.getMinGrade());
      StatsUtils.setGradeValue(
          this, R.id.marker_detail_max_grade_value, tripStatistics.getMaxGrade());
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.marker_detail, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case android.R.id.home:
        startActivity(new Intent(this, MarkerListActivity.class).addFlags(
            Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra(MarkerListActivity.EXTRA_TRACK_ID, waypoint.getTrackId()));
        return true;
      case R.id.marker_detail_show_on_map:
        startActivity(new Intent(this, TrackDetailActivity.class)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra(TrackDetailActivity.EXTRA_MARKER_ID, markerId));
        return true;        
      case R.id.marker_detail_edit:
        startActivity(new Intent(this, MarkerEditActivity.class)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra(MarkerEditActivity.EXTRA_MARKER_ID, markerId));
        return true;
      case R.id.marker_detail_delete:
        DeleteOneMarkerDialogFragment.newInstance(markerId).show(getSupportFragmentManager(),
            DeleteOneMarkerDialogFragment.DELETE_ONE_MARKER_DIALOG_TAG);
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }
}
