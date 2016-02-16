/*
 * Copyright 2012 Google Inc.
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

package com.google.android.apps.mytracks.io.spreadsheets;

import com.google.android.apps.mytracks.util.UnitConversions;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Utilities for sending a track to Google Spreadsheet.
 * 
 * @author Jimmy Shih
 */
public class SendSpreadsheetsUtils {

  // Google Spreadsheet can only parse numbers in the English locale.
  private static final NumberFormat NUMBER_FORMAT = NumberFormat.getNumberInstance(Locale.ENGLISH);
  private static final NumberFormat INTEGER_FORMAT = NumberFormat.getIntegerInstance(
      Locale.ENGLISH);

  static {
    NUMBER_FORMAT.setMaximumFractionDigits(2);
    NUMBER_FORMAT.setMinimumFractionDigits(2);
  }

  private SendSpreadsheetsUtils() {}

  /**
   * Gets the distance. Performs unit conversion and formatting.
   * 
   * @param distanceInMeter the distance in meters
   * @param metricUnits true to use metric
   */
  public static final String getDistance(double distanceInMeter, boolean metricUnits) {
    double distanceInKilometer = distanceInMeter * UnitConversions.M_TO_KM;
    double distance = metricUnits ? distanceInKilometer
        : distanceInKilometer * UnitConversions.KM_TO_MI;
    return NUMBER_FORMAT.format(distance);
  }

  /**
   * Gets the speed. Performs unit conversion and formatting.
   * 
   * @param speedInMeterPerSecond the speed in meters per second
   * @param metricUnits true to use metric
   */
  public static final String getSpeed(double speedInMeterPerSecond, boolean metricUnits) {
    double speedInKilometerPerHour = speedInMeterPerSecond * UnitConversions.MS_TO_KMH;
    double speed = metricUnits ? speedInKilometerPerHour
        : speedInKilometerPerHour * UnitConversions.KM_TO_MI;
    return NUMBER_FORMAT.format(speed);
  }

  /**
   * Gets the elevation. Performs unit conversion and formatting.
   * 
   * @param elevationInMeter the elevation value in meters
   * @param metricUnits true to use metric
   */
  public static final String getElevation(double elevationInMeter, boolean metricUnits) {
    double elevation = metricUnits ? elevationInMeter : elevationInMeter * UnitConversions.M_TO_FT;
    return INTEGER_FORMAT.format(elevation);
  }
}
