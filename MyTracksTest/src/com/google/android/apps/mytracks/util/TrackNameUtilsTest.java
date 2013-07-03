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

package com.google.android.apps.mytracks.util;

import com.google.android.maps.mytracks.R;

import android.test.AndroidTestCase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Tests {@link TrackNameUtils}.
 * 
 * @author Matthew Simmons
 */
public class TrackNameUtilsTest extends AndroidTestCase {

  private static final long TRACK_ID = 1L;
  private static final long START_TIME = 1288213406000L;

  /**
   * Tests when the track_name_key is
   * settings_recording_track_name_date_local_value.
   */
  public void testTrackName_date_local() {
    PreferencesUtils.setString(getContext(), R.string.track_name_key,
        getContext().getString(R.string.settings_recording_track_name_date_local_value));
    assertEquals(StringUtils.formatDateTime(getContext(), START_TIME),
        TrackNameUtils.getTrackName(getContext(), TRACK_ID, START_TIME, null));
  }

  /**
   * Tests when the track_name_key is
   * settings_recording_track_name_date_iso_8601_value.
   */
  public void testTrackName_date_iso_8601() {
    PreferencesUtils.setString(getContext(), R.string.track_name_key,
        getContext().getString(R.string.settings_recording_track_name_date_iso_8601_value));
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat(
        TrackNameUtils.ISO_8601_FORMAT, Locale.US);
    assertEquals(simpleDateFormat.format(new Date(START_TIME)),
        TrackNameUtils.getTrackName(getContext(), TRACK_ID, START_TIME, null));
  }

  /**
   * Tests when the track_name_key is
   * settings_recording_track_name_number_value.
   */
  public void testTrackName_number() {
    PreferencesUtils.setString(getContext(), R.string.track_name_key,
        getContext().getString(R.string.settings_recording_track_name_number_value));
    assertEquals(
        "Track " + TRACK_ID, TrackNameUtils.getTrackName(getContext(), TRACK_ID, START_TIME, null));
  }
}
