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

import com.google.android.apps.mytracks.util.StringUtils;
import com.google.android.maps.mytracks.R;

import android.test.AndroidTestCase;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Tests {@link DefaultTrackNameFactory}
 *
 * @author Matthew Simmons
 */
public class DefaultTrackNameFactoryTest extends AndroidTestCase {

  private static final long TRACK_ID = 1L;
  private static final long START_TIME = 1288213406000L;

  public void testDefaultTrackName_date_local() {
    DefaultTrackNameFactory defaultTrackNameFactory = new DefaultTrackNameFactory(getContext()) {
      @Override
      String getTrackNameSetting() {
        return getContext().getString(R.string.settings_recording_track_name_date_local_value);
      }
    };
    assertEquals(StringUtils.formatDateTime(getContext(), START_TIME),
        defaultTrackNameFactory.getDefaultTrackName(TRACK_ID, START_TIME));
  }

  public void testDefaultTrackName_date_iso_8601() {
    DefaultTrackNameFactory defaultTrackNameFactory = new DefaultTrackNameFactory(getContext()) {
      @Override
      String getTrackNameSetting() {
        return getContext().getString(R.string.settings_recording_track_name_date_iso_8601_value);
      }
    };
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat(
        DefaultTrackNameFactory.ISO_8601_FORMAT);
    assertEquals(simpleDateFormat.format(new Date(START_TIME)),
        defaultTrackNameFactory.getDefaultTrackName(TRACK_ID, START_TIME));
  }

  public void testDefaultTrackName_number() {
    DefaultTrackNameFactory defaultTrackNameFactory = new DefaultTrackNameFactory(getContext()) {
      @Override
      String getTrackNameSetting() {
        return getContext().getString(R.string.settings_recording_track_name_number_value);
      }
    };
    assertEquals(
        "Track " + TRACK_ID, defaultTrackNameFactory.getDefaultTrackName(TRACK_ID, START_TIME));
  }
}
