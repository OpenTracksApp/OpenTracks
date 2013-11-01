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

package com.google.android.apps.mytracks.maps;

import com.google.android.apps.mytracks.Constants;
import com.google.android.apps.mytracks.stats.TripStatistics;
import com.google.android.apps.mytracks.util.PreferencesUtils;
import com.google.android.apps.mytracks.util.UnitConversions;
import com.google.android.maps.mytracks.R;
import com.google.common.annotations.VisibleForTesting;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;

/**
 * A dynamic speed path descriptor.
 * 
 * @author Vangelis S.
 */
public class DynamicSpeedTrackPathDescriptor implements TrackPathDescriptor {

  private final OnSharedPreferenceChangeListener
      sharedPreferenceChangeListener = new OnSharedPreferenceChangeListener() {
          @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
          if (key == null || key.equals(PreferencesUtils.getKey(context, R.string.track_color_mode_percentage_key))) {
            speedMargin = PreferencesUtils.getInt(context, R.string.track_color_mode_percentage_key,
                PreferencesUtils.TRACK_COLOR_MODE_PERCENTAGE_DEFAULT);
          }
        }
      };

  private final Context context;
  private int speedMargin;
  private int slowSpeed;
  private int normalSpeed;
  private double averageMovingSpeed;

  @VisibleForTesting
  static final int CRITICAL_DIFFERENCE_PERCENTAGE = 20;

  public DynamicSpeedTrackPathDescriptor(Context context) {
    this.context = context;
    context.getSharedPreferences(Constants.SETTINGS_NAME, Context.MODE_PRIVATE)
        .registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
    sharedPreferenceChangeListener.onSharedPreferenceChanged(null, null);
  }

  @Override
  public int getSlowSpeed() {
    slowSpeed = (int) (averageMovingSpeed - (averageMovingSpeed * speedMargin / 100.0));
    return slowSpeed;
  }

  @Override
  public int getNormalSpeed() {
    normalSpeed = (int) (averageMovingSpeed + (averageMovingSpeed * speedMargin / 100.0));
    return normalSpeed;
  }

  @Override
  public boolean updateState(TripStatistics tripStatistics) {
    double newAverageMovingSpeed = (int) Math.floor(
        tripStatistics.getAverageMovingSpeed() * UnitConversions.MS_TO_KMH);

    if (isDifferenceSignificant(averageMovingSpeed, newAverageMovingSpeed)) {
      averageMovingSpeed = newAverageMovingSpeed;
      return true;
    } else {
      return false;
    }
  }

  /**
   * Returns true if the average moving speed and the new average moving speed
   * are significantly different.
   * 
   * @param oldAverageMovingSpeed
   * @param newAverageMovingSpeed
   */
  @VisibleForTesting
  boolean isDifferenceSignificant(double oldAverageMovingSpeed, double newAverageMovingSpeed) {
    if (oldAverageMovingSpeed == 0) {
      return newAverageMovingSpeed != 0;
    }
    
    // Here, both oldAverageMovingSpeed and newAverageMovingSpeed are not zero.
    double maxValue = Math.max(oldAverageMovingSpeed, newAverageMovingSpeed);
    double differencePercentage = Math.abs(oldAverageMovingSpeed - newAverageMovingSpeed) / maxValue
        * 100.0;
    return differencePercentage >= CRITICAL_DIFFERENCE_PERCENTAGE;
  }

  /**
   * Gets the speed margin.
   */
  @VisibleForTesting
  int getSpeedMargin() {
    return speedMargin;
  }
  
  /**
   * Sets the speed margin.
   */
  @VisibleForTesting
  void setSpeedMargin(int value) {
    speedMargin = value;
  }

  /**
   * Gets the average moving speed.
   */
  @VisibleForTesting
  double getAverageMovingSpeed() {
    return averageMovingSpeed;
  }

  /**
   * Sets the average moving speed.
   * 
   * @param value the value
   */
  @VisibleForTesting
  void setAverageMovingSpeed(double value) {
    averageMovingSpeed = value;
  }
}