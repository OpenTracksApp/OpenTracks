/*
 * Copyright 2010 Google Inc.
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
package com.google.android.apps.mytracks.services;

import com.google.android.apps.mytracks.Constants;
import com.google.android.maps.mytracks.R;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Creates a default track name based on the current default track name policy.
 * 
 * @author Matthew Simmons
 */
class DefaultTrackNameFactory {
  private static final String TIMESTAMP_DATE_FORMAT = "yyyy-MM-dd HH:mm";

  private final Context context;

  DefaultTrackNameFactory(Context context) {
    this.context = context;
  }

  /**
   * Creates a new track name.
   * 
   * @param trackId The ID for the current track.
   * @param startTime The start time, in milliseconds since the epoch, of the
   *     current track.
   * @return The new track name.
   */
  String newTrackName(long trackId, long startTime) {
    if (useTimestampTrackName()) {
      SimpleDateFormat formatter = new SimpleDateFormat(TIMESTAMP_DATE_FORMAT);
      return formatter.format(new Date(startTime));
    } else {
      return String.format(context.getString(R.string.new_track_format), trackId);
    }
  }

  /** Determines whether the preferences allow a timestamp-based track name */
  protected boolean useTimestampTrackName() {
    SharedPreferences prefs =
        context.getSharedPreferences(Constants.SETTINGS_NAME, 0);
    return prefs.getBoolean(
        context.getString(R.string.timestamp_track_name_key), true);
  }
}
