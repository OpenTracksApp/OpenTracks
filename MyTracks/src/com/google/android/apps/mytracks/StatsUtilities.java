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

import com.google.android.apps.mytracks.stats.TripStatistics;
import com.google.android.apps.mytracks.util.StringUtils;
import com.google.android.apps.mytracks.util.UnitConversions;
import com.google.android.maps.mytracks.R;

import android.app.Activity;
import android.util.Log;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.text.NumberFormat;

/**
 * Various utility functions for views that display statistics information.
 *
 * @author Sandor Dornbush
 */
public class StatsUtilities {

  private final Activity activity;
  private static final NumberFormat LAT_LONG_FORMAT =
      new DecimalFormat("##,###.00000");
  private static final NumberFormat ALTITUDE_FORMAT =
      new DecimalFormat("###,###");
  private static final NumberFormat SPEED_FORMAT =
      new DecimalFormat("#,###,###.00");
  private static final NumberFormat GRADE_FORMAT =
      new DecimalFormat("##.0%");

  /**
   * True if distances should be displayed in metric units (from shared
   * preferences).
   */
  private boolean metricUnits = true;

  /**
   * True  - report speed
   * False - report pace
   */
  private boolean reportSpeed = true;

  public StatsUtilities(Activity a) {
    this.activity = a;
  }

  public boolean isMetricUnits() {
    return metricUnits;
  }

  public void setMetricUnits(boolean metricUnits) {
    this.metricUnits = metricUnits;
  }

  public boolean isReportSpeed() {
    return reportSpeed;
  }

  public void setReportSpeed(boolean reportSpeed) {
    this.reportSpeed = reportSpeed;
  }

  public void setUnknown(int id) {
    ((TextView) activity.findViewById(id)).setText(R.string.unknown);
  }

  public void setText(int id, double d, NumberFormat format) {
    if (!Double.isNaN(d) && !Double.isInfinite(d)) {
      setText(id, format.format(d));
    }
  }

  public void setText(int id, String s) {
    int lengthLimit = 8;
    String displayString = s.length() > lengthLimit
      ? s.substring(0, lengthLimit - 3) + "..."
      : s;
      ((TextView) activity.findViewById(id)).setText(displayString);
  }

  public void setLatLong(int id, double d) {
    TextView msgTextView = (TextView) activity.findViewById(id);
    msgTextView.setText(LAT_LONG_FORMAT.format(d));
  }

  public void setAltitude(int id, double d) {
    setText(id, (metricUnits ? d : (d * UnitConversions.M_TO_FT)),
        ALTITUDE_FORMAT);
  }

  public void setDistance(int id, double d) {
    setText(id, (metricUnits ? d : (d * UnitConversions.KM_TO_MI)),
        SPEED_FORMAT);
  }

  public void setSpeed(int id, double d) {
    if (d == 0) {
      setUnknown(id);
      return;
    }
    double speed = metricUnits ? d : d * UnitConversions.KM_TO_MI;
    if (reportSpeed) {
      setText(id, speed, SPEED_FORMAT);
    } else {
      // Format as milliseconds per unit
      long pace =  (long) (3600000.0 / speed);
      setTime(id, pace);
    }
  }

  public void setAltitudeUnits(int unitLabelId) {
    TextView unitTextView = (TextView) activity.findViewById(unitLabelId);
    unitTextView.setText(metricUnits ? R.string.meter : R.string.feet);
  }

  public void setDistanceUnits(int unitLabelId) {
    TextView unitTextView = (TextView) activity.findViewById(unitLabelId);
    unitTextView.setText(metricUnits ? R.string.kilometer : R.string.mile);
  }

  public void setSpeedUnits(int unitLabelId, int unitLabelBottomId) {
    TextView unitTextView = (TextView) activity.findViewById(unitLabelId);
    unitTextView.setText(reportSpeed
        ? (metricUnits ? R.string.kilometer : R.string.mile)
        : R.string.min);

    unitTextView = (TextView) activity.findViewById(unitLabelBottomId);
    unitTextView.setText(reportSpeed
        ? R.string.hr
        : (metricUnits ? R.string.kilometer : R.string.mile));
  }

  public void setTime(int id, long l) {
    setText(id, StringUtils.formatTime(l));
  }

  public void setGrade(int id, double d) {
    setText(id, d, GRADE_FORMAT);
  }

  /**
   * Updates the unit fields.
   */
  public void updateUnits() {
    setSpeedUnits(R.id.speed_unit_label_top, R.id.speed_unit_label_bottom);
    updateWaypointUnits();
  }

  /**
   * Updates the units fields used by waypoints.
   */
  public void updateWaypointUnits() {
    setSpeedUnits(R.id.average_moving_speed_unit_label_top,
                  R.id.average_moving_speed_unit_label_bottom);
    setSpeedUnits(R.id.average_speed_unit_label_top,
                  R.id.average_speed_unit_label_bottom);
    setDistanceUnits(R.id.total_distance_unit_label);
    setSpeedUnits(R.id.max_speed_unit_label_top,
                  R.id.max_speed_unit_label_bottom);
    setAltitudeUnits(R.id.elevation_unit_label);
    setAltitudeUnits(R.id.elevation_gain_unit_label);
    setAltitudeUnits(R.id.min_elevation_unit_label);
    setAltitudeUnits(R.id.max_elevation_unit_label);
  }

  /**
   * Sets all fields to "-" (unknown).
   */
  public void setAllToUnknown() {
    // "Instant" values:
    setUnknown(R.id.elevation_register);
    setUnknown(R.id.latitude_register);
    setUnknown(R.id.longitude_register);
    setUnknown(R.id.speed_register);
    // Values from provider:
    setUnknown(R.id.total_time_register);
    setUnknown(R.id.moving_time_register);
    setUnknown(R.id.total_distance_register);
    setUnknown(R.id.average_speed_register);
    setUnknown(R.id.average_moving_speed_register);
    setUnknown(R.id.max_speed_register);
    setUnknown(R.id.min_elevation_register);
    setUnknown(R.id.max_elevation_register);
    setUnknown(R.id.elevation_gain_register);
    setUnknown(R.id.min_grade_register);
    setUnknown(R.id.max_grade_register);
  }

  public void setAllStats(long movingTime, double totalDistance,
      double averageSpeed, double averageMovingSpeed, double maxSpeed,
      double minElevation, double maxElevation, double elevationGain,
      double minGrade, double maxGrade) {
    setTime(R.id.moving_time_register, movingTime);
    setDistance(R.id.total_distance_register, totalDistance / 1000);
    setSpeed(R.id.average_speed_register, averageSpeed * 3.6);
    setSpeed(R.id.average_moving_speed_register, averageMovingSpeed * 3.6);
    setSpeed(R.id.max_speed_register, maxSpeed * 3.6);
    setAltitude(R.id.min_elevation_register, minElevation);
    setAltitude(R.id.max_elevation_register, maxElevation);
    setAltitude(R.id.elevation_gain_register, elevationGain);
    setGrade(R.id.min_grade_register, minGrade);
    setGrade(R.id.max_grade_register, maxGrade);
  }

  public void setAllStats(TripStatistics stats) {
    setTime(R.id.moving_time_register, stats.getMovingTime());
    setDistance(R.id.total_distance_register, stats.getTotalDistance() / 1000);
    setSpeed(R.id.average_speed_register, stats.getAverageSpeed() * 3.6);
    setSpeed(R.id.average_moving_speed_register,
        stats.getAverageMovingSpeed() * 3.6);
    setSpeed(R.id.max_speed_register, stats.getMaxSpeed() * 3.6);
    setAltitude(R.id.min_elevation_register, stats.getMinElevation());
    setAltitude(R.id.max_elevation_register, stats.getMaxElevation());
    setAltitude(R.id.elevation_gain_register, stats.getTotalElevationGain());
    setGrade(R.id.min_grade_register, stats.getMinGrade());
    setGrade(R.id.max_grade_register, stats.getMaxGrade());
  }

  public void setSpeedLabel(int id, int speedString, int paceString) {
    Log.w(MyTracksConstants.TAG, "Setting view " + id +
          " to " + reportSpeed +
          " speed: " + speedString +
          " pace: " + paceString);
    TextView tv = ((TextView) activity.findViewById(id));
    if (tv != null) {
      tv.setText(reportSpeed ?  speedString : paceString);
    } else {
      Log.w(MyTracksConstants.TAG, "Could not find id: " + id);
    }
  }

  public void setSpeedLabels() {
    setSpeedLabel(R.id.average_speed_label,
                  R.string.average_speed_label,
                  R.string.average_pace_label);
    setSpeedLabel(R.id.average_moving_speed_label,
                  R.string.average_moving_speed_label,
                  R.string.average_moving_pace_label);
    setSpeedLabel(R.id.max_speed_label,
                  R.string.max_speed_label,
                  R.string.min_pace_label);
  }
}
