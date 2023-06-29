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

import static org.junit.Assert.assertEquals;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.OffsetDateTime;
import java.util.Locale;

import de.dennisguse.opentracks.LocaleRule;
import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.data.models.Track;
import de.dennisguse.opentracks.settings.PreferencesUtils;

/**
 * Tests {@link TrackNameUtils}.
 *
 * @author Matthew Simmons
 */
@RunWith(AndroidJUnit4.class)
public class TrackNameUtilsTest {

    @Rule
    public final LocaleRule mLocaleRule = new LocaleRule(Locale.ENGLISH);

    private static final Track.Id TRACK_ID = new Track.Id(1L);
    private static final OffsetDateTime START_TIME = OffsetDateTime.parse("2022-01-02T10:15:30.1234+01:00");

    private static final Context CONTEXT = ApplicationProvider.getApplicationContext();

    /**
     * Tests when the track_name_key is settings_recording_track_name_date_local_value.
     */
    @Test
    public void testTrackName_date_local() {
        PreferencesUtils.setString(R.string.track_name_key, CONTEXT.getString(R.string.settings_recording_track_name_date_local_value));
        assertEquals(StringUtils.formatDateTimeWithOffset(START_TIME), TrackNameUtils.getTrackName(CONTEXT, TRACK_ID, START_TIME));
    }

    /**
     * Tests when the track_name_key is settings_recording_track_name_date_iso_8601_value.
     */
    @Test
    public void testTrackName_date_iso_8601() {
        PreferencesUtils.setString(R.string.track_name_key, CONTEXT.getString(R.string.settings_recording_track_name_date_iso_8601_value));
        assertEquals("2022-01-02T10:15+01", TrackNameUtils.getTrackName(CONTEXT, TRACK_ID, START_TIME));
    }

    /**
     * Tests when the track_name_key is settings_recording_track_name_number_value.
     */
    @Test
    public void testTrackName_number() {
        PreferencesUtils.setString(R.string.track_name_key, CONTEXT.getString(R.string.settings_recording_track_name_number_value));
        assertEquals("Track " + TRACK_ID.id(), TrackNameUtils.getTrackName(CONTEXT, TRACK_ID, START_TIME));
    }
}
