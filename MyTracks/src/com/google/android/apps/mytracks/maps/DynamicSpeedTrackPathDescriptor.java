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

import static com.google.android.apps.mytracks.Constants.TAG;

import com.google.android.apps.mytracks.Constants;
import com.google.android.apps.mytracks.content.MyTracksProviderUtils;
import com.google.android.apps.mytracks.content.Track;
import com.google.android.apps.mytracks.stats.TripStatistics;
import com.google.android.apps.mytracks.util.UnitConversions;
import com.google.android.maps.mytracks.R;
import com.google.common.annotations.VisibleForTesting;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.util.Log;

/**
 * A dynamic speed path descriptor.
 * 
 * @author Vangelis S.
 */
public class DynamicSpeedTrackPathDescriptor implements TrackPathDescriptor,
    OnSharedPreferenceChangeListener {

  private int slowSpeed;
  private int normalSpeed;
  private int speedMargin;
  private final int speedMarginDefault;
  private double averageMovingSpeed;
  private final Context context;
  @VisibleForTesting
  static final int CRITICAL_DIFFERENCE_PERCENTAGE = 20;

  public DynamicSpeedTrackPathDescriptor(Context context) {
    this.context = context;
    speedMarginDefault = Integer.parseInt(context
        .getString(R.string.color_mode_dynamic_percentage_default));
    SharedPreferences prefs = context.getSharedPreferences(Constants.SETTINGS_NAME,
        Context.MODE_PRIVATE);

    if (prefs == null) {
      speedMargin = speedMarginDefault;
      return;
    }
    prefs.registerOnSharedPreferenceChangeListener(this);

    speedMargin = getSpeedMargin(prefs);
  }

  @VisibleForTesting
  int getSpeedMargin(SharedPreferences sharedPreferences) {
    try {
      return Integer.parseInt(sharedPreferences.getString(
          context.getString(R.string.track_color_mode_dynamic_speed_variation_key),
          Integer.toString(speedMarginDefault)));
    } catch (NumberFormatException e) {
      return speedMarginDefault;
    }
  }

  /**
   * Get the slow speed calculated based on the % below the average speed.
   * 
   * @return The speed limit considered as slow.
   */
  public int getSlowSpeed() {
    slowSpeed = (int) (averageMovingSpeed - (averageMovingSpeed * speedMargin / 100));
    return slowSpeed;
  }

  /**
   * Gets the medium speed calculated based on the % above the average speed.
   * 
   * @return The speed limit considered as normal.
   */
  public int getNormalSpeed() {
    normalSpeed = (int) (averageMovingSpeed + (averageMovingSpeed * speedMargin / 100));
    return normalSpeed;
  }

  @Override
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    Log.d(TAG, "DynamicSpeedTrackPathDescriptor: onSharedPreferences changed " + key);
    if (key == null
        || !key.equals(context.getString(R.string.track_color_mode_dynamic_speed_variation_key))) { return; }
    SharedPreferences prefs = context.getSharedPreferences(Constants.SETTINGS_NAME,
        Context.MODE_PRIVATE);

    if (prefs == null) {
      speedMargin = speedMarginDefault;
      return;
    }

    speedMargin = getSpeedMargin(prefs);
  }

  @Override
  public boolean needsRedraw() {
    SharedPreferences prefs = context.getSharedPreferences(Constants.SETTINGS_NAME,
        Context.MODE_PRIVATE);
    long currentTrackId = prefs.getLong(context.getString(R.string.selected_track_key), -1);
    if (currentTrackId == -1) {
      // Could not find track.
      return false;
    }
    Track track = MyTracksProviderUtils.Factory.get(context).getTrack(currentTrackId);
    TripStatistics stats = track.getStatistics();
    double newAverageMovingSpeed = (int) Math.floor(
        stats.getAverageMovingSpeed() * UnitConversions.MS_TO_KMH);

    return isDifferenceSignificant(averageMovingSpeed, newAverageMovingSpeed);
  }

  /**
   * Checks whether the old speed and the new speed differ significantly or not.
   */
  public boolean isDifferenceSignificant(double oldAverageMovingSpeed, double newAverageMovingSpeed) {
    if (oldAverageMovingSpeed == 0) {
      if (newAverageMovingSpeed == 0) {
        return false;
      } else {
        averageMovingSpeed = newAverageMovingSpeed;
        return true;
      }
    }

    // Here, both oldAverageMovingSpeed and newAverageMovingSpeed are not zero.
    double maxValue = Math.max(oldAverageMovingSpeed, newAverageMovingSpeed);
    double differencePercentage = Math.abs(oldAverageMovingSpeed - newAverageMovingSpeed)
        / maxValue * 100;
    if (differencePercentage >= CRITICAL_DIFFERENCE_PERCENTAGE) {
      averageMovingSpeed = newAverageMovingSpeed;
      return true;
    }
    return false;
  }

  /**
   * Gets the value of variable speedMargin to check the result of test.  
   * @return the value of speedMargin.
   */
  @VisibleForTesting
  int getSpeedMargin() {
    return speedMargin;
  }

  /**
   * Sets the value of newAverageMovingSpeed to test the method isDifferenceSignificant.
   * @param newAverageMovingSpeed the value to set.
   */
  @VisibleForTesting
  void setAverageMovingSpeed(double newAverageMovingSpeed) {
    averageMovingSpeed = newAverageMovingSpeed;
  }

  /**
   * Gets the value of averageMovingSpeed to check the result of test.
   * 
   * @return the value of averageMovingSpeed
   */
  @VisibleForTesting
  double getAverageMovingSpeed() {
    return averageMovingSpeed;
  }
}