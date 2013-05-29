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
import com.google.android.maps.mytracks.R;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;

/**
 * A fixed speed path descriptor.
 * 
 * @author Vangelis S.
 */
public class FixedSpeedTrackPathDescriptor implements TrackPathDescriptor {

  private final OnSharedPreferenceChangeListener
      sharedPreferenceChangeListener = new OnSharedPreferenceChangeListener() {
          @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
          if (key == null
              || key.equals(PreferencesUtils.getKey(context, R.string.track_color_mode_slow_key))) {
            slowSpeed = PreferencesUtils.getInt(context, R.string.track_color_mode_slow_key,
                PreferencesUtils.TRACK_COLOR_MODE_SLOW_DEFAULT);
          }
          if (key == null || key.equals(
              PreferencesUtils.getKey(context, R.string.track_color_mode_medium_key))) {
            normalSpeed = PreferencesUtils.getInt(context, R.string.track_color_mode_medium_key,
                PreferencesUtils.TRACK_COLOR_MODE_MEDIUM_DEFAULT);
          }
        }
      };

  private final Context context;
  private int slowSpeed;
  private int normalSpeed;

  public FixedSpeedTrackPathDescriptor(Context context) {
    this.context = context;

    context.getSharedPreferences(Constants.SETTINGS_NAME, Context.MODE_PRIVATE)
        .registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
    sharedPreferenceChangeListener.onSharedPreferenceChanged(null, null);
  }

  @Override
  public int getSlowSpeed() {
    return slowSpeed;
  }

  @Override
  public int getNormalSpeed() {
    return normalSpeed;
  }

  @Override
  public boolean updateState(TripStatistics tripStatistics) {
    return false;
  }
}