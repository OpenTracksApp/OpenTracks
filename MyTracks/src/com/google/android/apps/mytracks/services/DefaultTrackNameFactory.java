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
import com.google.android.apps.mytracks.util.StringUtils;
import com.google.android.maps.mytracks.R;
import com.google.common.annotations.VisibleForTesting;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;

/**
 * Creates a default track name based on the track name setting.
 *
 * @author Matthew Simmons
 */
public class DefaultTrackNameFactory {

  @VisibleForTesting
  static final String ISO_8601_FORMAT = "yyyy-MM-dd HH:mm";
  private final Context context;

  public DefaultTrackNameFactory(Context context) {
    this.context = context;
  }

  /**
   * Gets the default track name.
   *
   * @param trackId the track id
   * @param startTime the track start time
   */
  public String getDefaultTrackName(long trackId, long startTime) {
    String trackNameSetting = getTrackNameSetting();

    if (trackNameSetting.equals(
        context.getString(R.string.settings_recording_track_name_date_local_value))) {
      return StringUtils.formatDateTime(context, startTime);
    } else if (trackNameSetting.equals(
        context.getString(R.string.settings_recording_track_name_date_iso_8601_value))) {
      SimpleDateFormat dateFormat = new SimpleDateFormat(ISO_8601_FORMAT);
      return dateFormat.format(startTime);
    } else {
      // trackNameSetting equals
      // R.string.settings_recording_track_name_number_value
      return String.format(context.getString(R.string.track_name_format), trackId);
    }
  }

  /**
   * Gets the track name setting.
   */
  @VisibleForTesting
  String getTrackNameSetting() {
    SharedPreferences sharedPreferences = context.getSharedPreferences(
        Constants.SETTINGS_NAME, Context.MODE_PRIVATE);
    return sharedPreferences.getString(
        context.getString(R.string.track_name_key),
        context.getString(R.string.settings_recording_track_name_date_local_value));
  }
}
