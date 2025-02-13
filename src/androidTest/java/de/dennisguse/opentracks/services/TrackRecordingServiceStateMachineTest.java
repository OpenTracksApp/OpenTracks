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
import androidx.test.rule.GrantPermissionRule;
import androidx.test.rule.ServiceTestRule;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import de.dennisguse.opentracks.data.ContentProviderUtils;
import de.dennisguse.opentracks.data.models.Track;
import de.dennisguse.opentracks.sensors.GpsStatusValue;

/**
 * Testing the states of TrackRecordingService.
 * As states are checked against MutableLiveData that are updated via `postValue()` (is asynchronous to be inform other Threads), Thread.sleep() is required.
 */
@RunWith(AndroidJUnit4.class)
public class TrackRecordingServiceStateMachineTest {

    @Rule
    public final ServiceTestRule mServiceRule = ServiceTestRule.withTimeout(5, TimeUnit.SECONDS);

    @Rule
    public GrantPermissionRule mRuntimePermissionRule = GrantPermissionRule.grant(android.Manifest.permission.ACCESS_FINE_LOCATION);

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
    public void initialState() {
        // given
        List<Track> tracks = contentProviderUtils.getTracks();
        assertTrue(tracks.isEmpty());

        // when
        // noop

        // then
        assertFalse(service.isRecording());
        assertEquals(TrackRecordingService.STATUS_DEFAULT, service.getRecordingStatusObservable().getValue());
        assertEquals(TrackRecordingService.NOT_RECORDING, service.getRecordingDataObservable().getValue());
        assertEquals(GpsStatusValue.GPS_NONE, service.getGpsStatusObservable().getValue());
    }

    @MediumTest
    @Test
    public void gps_startStop() throws InterruptedException {
        // given
        assertEquals(GpsStatusValue.GPS_NONE, service.getGpsStatusObservable().getValue());

        // when
        service.tryStartSensors();
        Thread.sleep(1000);

        // then
        assertEquals(TrackRecordingService.STATUS_DEFAULT, service.getRecordingStatusObservable().getValue());
        assertEquals(TrackRecordingService.NOT_RECORDING, service.getRecordingDataObservable().getValue());
        assertEquals(GpsStatusValue.GPS_ENABLED, service.getGpsStatusObservable().getValue());

        // when
        service.stopSensors();
        Thread.sleep(1000);

        // then
        assertEquals(TrackRecordingService.STATUS_DEFAULT, service.getRecordingStatusObservable().getValue());
        assertEquals(TrackRecordingService.NOT_RECORDING, service.getRecordingDataObservable().getValue());
//        assertEquals(GpsStatusValue.GPS_NONE, service.getGpsStatusObservable().getValue()); TODO BUG: GPS can only be stopped if a service is recording.
    }

    @MediumTest
    @Test
    public void recording_startStopResume_no_data() throws InterruptedException {
        // given
        assertFalse(service.isRecording());

        // when
        Track.Id trackId = service.startNewTrack();
        Thread.sleep(1000);

        // then
        assertTrue(service.isRecording());
        assertEquals(new RecordingStatus(trackId), service.getRecordingStatusObservable().getValue());
        assertNotEquals(TrackRecordingService.NOT_RECORDING, service.getRecordingDataObservable().getValue());
        assertEquals(GpsStatusValue.GPS_ENABLED, service.getGpsStatusObservable().getValue());

        service.endCurrentTrack();

        // when
        service.resumeTrack(trackId);
        Thread.sleep(1000);

        // then
        assertTrue(service.isRecording());
        assertEquals(new RecordingStatus(trackId), service.getRecordingStatusObservable().getValue());
        assertNotEquals(TrackRecordingService.NOT_RECORDING, service.getRecordingDataObservable().getValue());
        assertEquals(GpsStatusValue.GPS_ENABLED, service.getGpsStatusObservable().getValue());


        // when
        service.endCurrentTrack();
        Thread.sleep(1000);

        // then
        assertFalse(service.isRecording());
        assertEquals(TrackRecordingService.STATUS_DEFAULT, service.getRecordingStatusObservable().getValue());
        assertNotEquals(TrackRecordingService.NOT_RECORDING, service.getRecordingDataObservable().getValue());
        assertEquals(GpsStatusValue.GPS_NONE, service.getGpsStatusObservable().getValue());


        // when
        service.resumeTrack(trackId);
        Thread.sleep(1000);

        // then
        assertTrue(service.isRecording());
        assertEquals(new RecordingStatus(trackId), service.getRecordingStatusObservable().getValue());
        Thread.sleep(1000); //TODO Figure out why we need to wait here until the update is happening
        assertNotEquals(TrackRecordingService.NOT_RECORDING, service.getRecordingDataObservable().getValue());
        assertEquals(GpsStatusValue.GPS_ENABLED, service.getGpsStatusObservable().getValue());
    }

    @MediumTest
    @Test
    public void cannotResume_non_existing_track() {
        // given
        assertFalse(service.isRecording());
        assertEquals(TrackRecordingService.STATUS_DEFAULT, service.getRecordingStatusObservable().getValue());
        assertEquals(TrackRecordingService.NOT_RECORDING, service.getRecordingDataObservable().getValue());
        assertEquals(GpsStatusValue.GPS_NONE, service.getGpsStatusObservable().getValue());

        // when
        service.resumeTrack(new Track.Id(-1));

        // then
        assertFalse(service.isRecording());
        assertEquals(TrackRecordingService.STATUS_DEFAULT, service.getRecordingStatusObservable().getValue());
        assertEquals(TrackRecordingService.NOT_RECORDING, service.getRecordingDataObservable().getValue());
        assertEquals(GpsStatusValue.GPS_NONE, service.getGpsStatusObservable().getValue());
    }

    @MediumTest
    @Test
    public void cannotEnd_without_starting() throws InterruptedException {
        // given
        assertFalse(service.isRecording());
        assertEquals(TrackRecordingService.STATUS_DEFAULT, service.getRecordingStatusObservable().getValue());
        assertEquals(TrackRecordingService.NOT_RECORDING, service.getRecordingDataObservable().getValue());
        assertEquals(GpsStatusValue.GPS_NONE, service.getGpsStatusObservable().getValue());

        // when
        service.endCurrentTrack();
        Thread.sleep(1000);

        // then
        assertEquals(TrackRecordingService.STATUS_DEFAULT, service.getRecordingStatusObservable().getValue());
        assertEquals(TrackRecordingService.NOT_RECORDING, service.getRecordingDataObservable().getValue());
        assertEquals(GpsStatusValue.GPS_NONE, service.getGpsStatusObservable().getValue());
    }

    @Ignore("TODO Bug: GPS can be stopped although the current track is recording")
    @MediumTest
    @Test
    public void recording_stopGPS_noop() throws InterruptedException {
        // given
        Track.Id trackId = service.startNewTrack();
        assertEquals(GpsStatusValue.GPS_ENABLED, service.getGpsStatusObservable().getValue());

        // when
        service.stopSensors(); //TODO Should be ignored as service is recording
        Thread.sleep(1000);

        // then
        assertTrue(service.isRecording());
        assertEquals(new RecordingStatus(trackId), service.getRecordingStatusObservable().getValue());
        Thread.sleep(1000); //TODO Figure out why we need to wait here until the update is happening
        assertNotEquals(TrackRecordingService.NOT_RECORDING, service.getRecordingDataObservable().getValue());
        assertEquals(GpsStatusValue.GPS_ENABLED, service.getGpsStatusObservable().getValue());
    }

    @MediumTest
    @Test
    public void recording_startRecording_alreadyRecording() throws InterruptedException {
        // given
        Track.Id trackId = service.startNewTrack();
        assertTrue(service.isRecording());

        // when
        Track.Id newTrackId = service.startNewTrack();
        Thread.sleep(1000);

        // then
        assertNotNull(trackId);
        assertNull(newTrackId);
    }
}
