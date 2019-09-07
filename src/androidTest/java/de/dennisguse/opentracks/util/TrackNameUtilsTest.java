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

package de.dennisguse.opentracks.util;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import de.dennisguse.opentracks.R;

/**
 * Tests {@link TrackNameUtils}.
 *
 * @author Matthew Simmons
 */
@RunWith(AndroidJUnit4.class)
public class TrackNameUtilsTest {

    private static final long TRACK_ID = 1L;
    private static final long START_TIME = 1288213406000L;

    private static final Context CONTEXT = ApplicationProvider.getApplicationContext();

    /**
     * Tests when the track_name_key is settings_recording_track_name_date_local_value.
     */
    @Test
    public void testTrackName_date_local() {
        PreferencesUtils.setString(CONTEXT, R.string.track_name_key, CONTEXT.getString(R.string.settings_recording_track_name_date_local_value));
        Assert.assertEquals(StringUtils.formatDateTime(CONTEXT, START_TIME), TrackNameUtils.getTrackName(CONTEXT, TRACK_ID, START_TIME));
    }

    /**
     * Tests when the track_name_key is settings_recording_track_name_date_iso_8601_value.
     */
    @Test
    public void testTrackName_date_iso_8601() {
        PreferencesUtils.setString(CONTEXT, R.string.track_name_key, CONTEXT.getString(R.string.settings_recording_track_name_date_iso_8601_value));
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(TrackNameUtils.ISO_8601_FORMAT, Locale.US);
        Assert.assertEquals(simpleDateFormat.format(new Date(START_TIME)), TrackNameUtils.getTrackName(CONTEXT, TRACK_ID, START_TIME));
    }

    /**
     * Tests when the track_name_key is settings_recording_track_name_number_value.
     */
    @Test
    public void testTrackName_number() {
        PreferencesUtils.setString(CONTEXT, R.string.track_name_key, CONTEXT.getString(R.string.settings_recording_track_name_number_value));
        Assert.assertEquals("Track " + TRACK_ID, TrackNameUtils.getTrackName(CONTEXT, TRACK_ID, START_TIME));
    }
}
