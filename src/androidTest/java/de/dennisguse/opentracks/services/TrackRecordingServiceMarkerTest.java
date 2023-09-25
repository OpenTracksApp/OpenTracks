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
package de.dennisguse.opentracks.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.Intent;
import android.os.Looper;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.test.rule.ServiceTestRule;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.data.ContentProviderUtils;
import de.dennisguse.opentracks.data.models.Marker;
import de.dennisguse.opentracks.data.models.Track;
import de.dennisguse.opentracks.data.models.TrackPoint;
import de.dennisguse.opentracks.services.handlers.TrackPointCreator;

@RunWith(AndroidJUnit4.class)
public class TrackRecordingServiceMarkerTest {

    @Rule
    public final ServiceTestRule mServiceRule = ServiceTestRule.withTimeout(5, TimeUnit.SECONDS);

    private final Context context = ApplicationProvider.getApplicationContext();
    private ContentProviderUtils contentProviderUtils;

    private TrackRecordingService service;

    @BeforeClass
    public static void preSetUp() {
        // Prepare looper for Android's message queue
        if (Looper.myLooper() == null) Looper.prepare();
    }

    @AfterClass
    public static void finalTearDown() {
        if (Looper.myLooper() != null) Looper.myLooper().quit();
    }

    private TrackRecordingService startService() throws TimeoutException {
        Intent startIntent = new Intent(context, TrackRecordingService.class);
        return ((TrackRecordingService.Binder) mServiceRule.bindService(startIntent))
                .getService();
    }

    @Before
    public void setUp() throws TimeoutException {
        contentProviderUtils = new ContentProviderUtils(context);
        service = startService();
        tearDown();
    }

    @After
    public void tearDown() {
        // Ensure that the database is empty after every test
        contentProviderUtils.deleteAllTracks(context);
    }

    @MediumTest
    @Test
    public void notRecording_testInsertMarker() {
        // given
        assertFalse(service.isRecording());

        // when
        Marker.Id markerId = service.insertMarker(null, null, null, null);

        // then
        assertNull(markerId);
    }

    @MediumTest
    @Test
    public void recording_noGPSfix_cannotCreateMarker() {
        // given
        service.startNewTrack();

        // when
        Marker.Id markerId = service.insertMarker(null, null, null, null);

        // then
        assertNull(markerId);
    }

    @MediumTest
    @Test
    public void recording_GPSfix_createsMarker() {
        // given
        TrackPointCreator trackPointCreator = service.getTrackPointCreator();

        trackPointCreator.setClock("2020-02-02T02:02:02Z");
        Track.Id trackId = service.startNewTrack();
        service.stopUpdateRecordingData();

        assertTrue(service.isRecording());
        trackPointCreator.onNewTrackPoint(
                new TrackPoint(TrackPoint.Type.TRACKPOINT, Instant.parse("2020-02-02T02:02:03Z"))
                        .setLatitude(10)
                        .setLongitude(10)
        );

        // when
        Marker.Id markerId = service.insertMarker(null, null, null, null);

        // then
        assertNotEquals(new Marker.Id(-1L), markerId);
        Marker wpt = contentProviderUtils.getMarker(markerId);
        assertEquals(context.getString(R.string.marker_icon_url), wpt.getIcon());
        assertEquals(context.getString(R.string.marker_name_format, 1), wpt.getName());
        assertEquals(trackId, wpt.getTrackId());
        assertEquals(0.0, wpt.getLength().toM(), 0.01);
        assertNotNull(wpt.getLocation());

        trackPointCreator.setClock("2020-02-02T02:02:04Z");
        service.endCurrentTrack();
    }
}
