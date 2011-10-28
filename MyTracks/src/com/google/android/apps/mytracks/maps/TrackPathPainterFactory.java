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
import com.google.android.maps.mytracks.R;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * A factory for TrackPathPainters.
 *
 * @author Vangelis S.
 */
public class TrackPathPainterFactory {

  private TrackPathPainterFactory() {
  }

  /**
   * Get a new TrackPathPainter.
   * @param context Context to fetch system preferences.
   * @return The TrackPathPainter that corresponds to the track color mode setting.
   */
  public static TrackPathPainter getTrackPathPainter(Context context) {
    SharedPreferences prefs =
        context.getSharedPreferences(Constants.SETTINGS_NAME, 0);
    if (prefs == null) {
      return new SingleColorTrackPathPainter(context);
    }
    
    String colorMode = prefs.getString(context.getString(R.string.track_color_mode_key), null);
    Log.i(TAG, "Creating track path painter of type: " + colorMode);

    if (colorMode == null || colorMode.equals(context.getString(R.string.track_color_mode_value_none))) {
      return new SingleColorTrackPathPainter(context);
    } else if (colorMode.equals(context.getString(R.string.track_color_mode_value_fixed))) {
      return new DynamicSpeedTrackPathPainter(context, 
          new FixedSpeedTrackPathDescriptor(context));
    } else if (colorMode.equals(context.getString(R.string.track_color_mode_value_dynamic))) {
      return new DynamicSpeedTrackPathPainter(context, 
          new DynamicSpeedTrackPathDescriptor(context));
    } else {
      Log.w(TAG, "Using default track path painter. Unrecognized painter: " + colorMode);
      return new SingleColorTrackPathPainter(context);
    }
  }
}