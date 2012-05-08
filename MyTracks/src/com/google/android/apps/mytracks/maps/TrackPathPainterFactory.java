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

import com.google.android.apps.mytracks.util.PreferencesUtils;
import com.google.android.maps.mytracks.R;

import android.content.Context;
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
    String trackColorMode = PreferencesUtils.getString(context, R.string.track_color_mode_key,
        context.getString(R.string.settings_map_track_color_mode_single_value));
    Log.i(TAG, "Creating track path painter of type: " + trackColorMode);

    if (context.getString(R.string.settings_map_track_color_mode_fixed_value)
        .equals(trackColorMode)) {
      return new DynamicSpeedTrackPathPainter(context, new FixedSpeedTrackPathDescriptor(context));
    } else if (context.getString(R.string.settings_map_track_color_mode_dynamic_value)
        .equals(trackColorMode)) {
      return new DynamicSpeedTrackPathPainter(context, new DynamicSpeedTrackPathDescriptor(
          context));
    } else {
      return new SingleColorTrackPathPainter(context);
    }
  }
}