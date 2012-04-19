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
import com.google.android.apps.mytracks.util.IntentUtils;
import com.google.android.apps.mytracks.util.StringUtils;
import com.google.android.apps.mytracks.util.UnitConversions;
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

      TextView maxSpeedLabel = (TextView) findViewById(R.id.marker_detail_max_speed_label);
      maxSpeedLabel.setText(reportSpeed ? R.string.stat_max_speed : R.string.stat_fastest_pace);
      TextView averageSpeedLabel = (TextView) findViewById(R.id.marker_detail_average_speed_label);
      averageSpeedLabel.setText(reportSpeed ? R.string.stat_average_speed
          : R.string.stat_average_pace);
      TextView averageMovingSpeedLabel = (TextView) findViewById(
          R.id.marker_detail_average_moving_speed_label);
      averageMovingSpeedLabel.setText(reportSpeed ? R.string.stat_average_moving_speed
          : R.string.stat_average_moving_pace);

      TripStatistics tripStatistics = waypoint.getStatistics();

      setDistance(
          R.id.marker_detail_total_distance_value, tripStatistics.getTotalDistance(), metricUnits);
      setSpeed(R.id.marker_detail_max_speed_value, tripStatistics.getMaxSpeed(), reportSpeed,
          metricUnits);

      setTime(R.id.marker_detail_total_time_value, tripStatistics.getTotalTime());
      setSpeed(R.id.marker_detail_average_speed_value, tripStatistics.getAverageSpeed(),
          reportSpeed, metricUnits);

      setTime(R.id.marker_detail_moving_time_value, tripStatistics.getMovingTime());
      setSpeed(R.id.marker_detail_average_moving_speed_value,
          tripStatistics.getAverageMovingSpeed(), reportSpeed, metricUnits);

      setAltitude(
          R.id.marker_detail_elevation_value, waypoint.getLocation().getAltitude(), metricUnits);
      setAltitude(R.id.marker_detail_elevation_gain_value, tripStatistics.getTotalElevationGain(),
          metricUnits);

      setAltitude(
          R.id.marker_detail_min_elevation_value, tripStatistics.getMinElevation(), metricUnits);
      setAltitude(
          R.id.marker_detail_max_elevation_value, tripStatistics.getMaxElevation(), metricUnits);

      setGrade(R.id.marker_detail_min_grade_value, tripStatistics.getMinGrade());
      setGrade(R.id.marker_detail_max_grade_value, tripStatistics.getMaxGrade());
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.marker_detail, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    Intent intent;
    switch (item.getItemId()) {
      case android.R.id.home:
        intent = IntentUtils.newIntent(this, MarkerListActivity.class)
            .putExtra(MarkerListActivity.EXTRA_TRACK_ID, waypoint.getTrackId());
        startActivity(intent);
        return true;
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
        DeleteOneMarkerDialogFragment.newInstance(markerId, waypoint.getTrackId()).show(
            getSupportFragmentManager(),
            DeleteOneMarkerDialogFragment.DELETE_ONE_MARKER_DIALOG_TAG);
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  /**
   * Sets distance.
   * 
   * @param id resource id
   * @param distance distance in meters
   * @param metricUnits true to display in metric units
   */
  private void setDistance(int id, double distance, boolean metricUnits) {
    TextView textView = (TextView) findViewById(id);
    distance *= UnitConversions.M_TO_KM;
    String value;
    if (metricUnits) {
      value = getString(R.string.value_float_kilometer, distance);
    } else {
      distance *= UnitConversions.KM_TO_MI;
      value = getString(R.string.value_float_mile, distance);
    }
    textView.setText(value);
  }

  /**
   * Sets time.
   * 
   * @param id resource id
   * @param time time
   */
  private void setTime(int id, long time) {
    TextView textView = (TextView) findViewById(id);
    textView.setText(StringUtils.formatElapsedTime(time));
  }

  /**
   * Sets speed.
   * 
   * @param id resource id
   * @param speed speed in meters per second
   * @param reportSpeed true to report speed
   * @param metricUnits true to display in metric units
   */
  private void setSpeed(int id, double speed, boolean reportSpeed, boolean metricUnits) {
    TextView textView = (TextView) findViewById(id);
    speed *= UnitConversions.MS_TO_KMH;
    String value;
    if (metricUnits) {
      if (reportSpeed) {
        value = getString(R.string.value_float_kilometer_hour, speed);
      } else {
        double pace = speed == 0 ? 0.0 : 60.0 / speed; // convert from hours to
                                                       // minutes
        value = getString(R.string.value_float_minute_kilometer, pace);
      }
    } else {
      speed *= UnitConversions.KM_TO_MI;
      if (reportSpeed) {
        value = getString(R.string.value_float_mile_hour, speed);
      } else {
        double pace = speed == 0 ? 0.0 : 60.0 / speed; // convert from hours to
                                                       // minutes
        value = getString(R.string.value_float_minute_mile, pace);
      }
    }
    textView.setText(value);
  }

  /**
   * Sets the altitude.
   * 
   * @param id resource id
   * @param altitude altitude in meters
   * @param metricUnits true to display in metric units
   */
  private void setAltitude(int id, double altitude, boolean metricUnits) {
    TextView textView = (TextView) findViewById(id);
    String value;
    if (Double.isNaN(altitude) || Double.isInfinite(altitude)) {
      value = getString(R.string.value_unknown);
    } else {
      if (metricUnits) {
        value = getString(R.string.value_float_meter, altitude);
      } else {
        altitude *= UnitConversions.M_TO_FT;
        value = getString(R.string.value_float_feet, altitude);
      }
    }
    textView.setText(value);
  }

  /**
   * Sets the grade.
   * 
   * @param id resource id
   * @param grade grade in fraction between 0 and 1
   */
  private void setGrade(int id, double grade) {
    TextView textView = (TextView) findViewById(id);
    String value;
    if (Double.isNaN(grade) || Double.isInfinite(grade)) {
      value = getString(R.string.value_unknown);
    } else {
      value = getString(R.string.value_integer_percent, Math.round(grade * 100));
    }
    textView.setText(value);
  }
}
