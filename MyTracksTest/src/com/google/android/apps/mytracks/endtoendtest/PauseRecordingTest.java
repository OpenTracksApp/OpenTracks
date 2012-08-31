/*
 * Copyright 2012 Google Inc.
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
package com.google.android.apps.mytracks.endtoendtest;

import com.google.android.apps.mytracks.TrackListActivity;
import com.google.android.apps.mytracks.content.MyTracksProviderUtils;
import com.google.android.apps.mytracks.content.MyTracksProviderUtils.LocationIterator;
import com.google.android.apps.mytracks.services.TrackRecordingService;
import com.google.android.maps.mytracks.R;

import android.annotation.TargetApi;
import android.app.Instrumentation;
import android.location.Location;
import android.test.ActivityInstrumentationTestCase2;

/**
 * Tests the pause and resume of recording.
 * 
 * @author Youtao Liu
 */
public class PauseRecordingTest extends ActivityInstrumentationTestCase2<TrackListActivity> {

  private Instrumentation instrumentation;
  private TrackListActivity activityMyTracks;

  @TargetApi(15)
  public PauseRecordingTest() {
    super(TrackListActivity.class);
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    instrumentation = getInstrumentation();
    activityMyTracks = getActivity();
    EndToEndTestUtils.setupForAllTest(instrumentation, activityMyTracks);
  }

  /**
   * Tests the pause recording feature. Stops the recording after pause.
   */
  public void testPauseRecording_stopAfterPause() {
    int gpsSignalNumber = 3;

    // Start recording
    EndToEndTestUtils.startRecording();
    assertNotNull(EndToEndTestUtils.findMenuItem(
        activityMyTracks.getString(R.string.menu_pause_track), false));
    assertNotNull(EndToEndTestUtils.findMenuItem(
        activityMyTracks.getString(R.string.menu_stop_recording), false));
    EndToEndTestUtils.SOLO.goBack();
    assertNotNull(EndToEndTestUtils.findMenuItem(
        activityMyTracks.getString(R.string.menu_pause_track), false));
    assertNotNull(EndToEndTestUtils.findMenuItem(
        activityMyTracks.getString(R.string.menu_stop_recording), false));
    EndToEndTestUtils.sendGps(gpsSignalNumber);

    // Pause
    EndToEndTestUtils.findMenuItem(activityMyTracks.getString(R.string.menu_pause_track), true);
    assertNotNull(EndToEndTestUtils.findMenuItem(
        activityMyTracks.getString(R.string.menu_record_track), false));
    assertNotNull(EndToEndTestUtils.findMenuItem(
        activityMyTracks.getString(R.string.menu_stop_recording), false));
    EndToEndTestUtils.sendGps(gpsSignalNumber, gpsSignalNumber, -1);

    // Stop
    EndToEndTestUtils.stopRecording(true);
    checkPointsInPausedTrack(gpsSignalNumber, 1, 0);
  }

  /**
   * Tests the pause recording feature. Stops the recording after resume.
   */
  public void testPauseRecording_stopAfterResume() {
    int gpsSignalNumber = 3;

    // Start recording
    EndToEndTestUtils.startRecording();
    assertNotNull(EndToEndTestUtils.findMenuItem(
        activityMyTracks.getString(R.string.menu_pause_track), false));
    assertNotNull(EndToEndTestUtils.findMenuItem(
        activityMyTracks.getString(R.string.menu_stop_recording), false));
    EndToEndTestUtils.SOLO.goBack();
    assertNotNull(EndToEndTestUtils.findMenuItem(
        activityMyTracks.getString(R.string.menu_pause_track), false));
    assertNotNull(EndToEndTestUtils.findMenuItem(
        activityMyTracks.getString(R.string.menu_stop_recording), false));
    EndToEndTestUtils.sendGps(gpsSignalNumber);

    // Pause
    EndToEndTestUtils.findMenuItem(activityMyTracks.getString(R.string.menu_pause_track), true);
    assertNotNull(EndToEndTestUtils.findMenuItem(
        activityMyTracks.getString(R.string.menu_record_track), false));
    assertNotNull(EndToEndTestUtils.findMenuItem(
        activityMyTracks.getString(R.string.menu_stop_recording), false));

    // Send Gps signal after pause.
    // Add 10 to make these signals are apparently different.
    EndToEndTestUtils.sendGps(gpsSignalNumber, gpsSignalNumber + 10, 10);

    // Resume
    EndToEndTestUtils.findMenuItem(activityMyTracks.getString(R.string.menu_record_track), true);
    assertNotNull(EndToEndTestUtils.findMenuItem(
        activityMyTracks.getString(R.string.menu_pause_track), false));
    assertNotNull(EndToEndTestUtils.findMenuItem(
        activityMyTracks.getString(R.string.menu_stop_recording), false));
    EndToEndTestUtils.sendGps(gpsSignalNumber, gpsSignalNumber, -1);

    // Stop
    EndToEndTestUtils.stopRecording(true);
    checkPointsInPausedTrack(gpsSignalNumber * 2, 1, 1);
  }

  /**
   * Check the points in the last track.
   * 
   * @param expectPointNumber the number of location points
   * @param expectPauseNumber the number of pause points
   * @param expectResumeNumber the number of resume points
   */
  private void checkPointsInPausedTrack(int expectPointNumber, int expectPauseNumber,
      int expectResumeNumber) {
    if (EndToEndTestUtils.isEmulator) {
      int numberOfPoints = 0;
      int numberOfPausePoint = 0;
      int numberOfResumePoint = 0;

      MyTracksProviderUtils providerUtils = MyTracksProviderUtils.Factory.get(activityMyTracks
          .getApplicationContext());
      long trackId = providerUtils.getLastTrack().getId();
      LocationIterator points = providerUtils.getTrackPointLocationIterator(trackId, 0, false,
          MyTracksProviderUtils.DEFAULT_LOCATION_FACTORY);
      while (points.hasNext()) {
        Location onePoint = points.next();
        double latitude = onePoint.getLatitude();
        double longitude = onePoint.getLongitude();
        if (latitude == TrackRecordingService.PAUSE_LATITUDE) {
          numberOfPausePoint++;
        } else if (latitude == TrackRecordingService.RESUME_LATITUDE) {
          numberOfResumePoint++;
        } else {
          System.out.println(latitude + ":" + longitude);
          assertEquals(
              numberOfPoints,
              (int) ((latitude - EndToEndTestUtils.START_LATITUDE) / EndToEndTestUtils.DELTA_LADITUDE + 0.5));
          assertEquals(
              numberOfPoints,
              (int) ((longitude - EndToEndTestUtils.START_LONGITUDE) / EndToEndTestUtils.DELTA_LONGITUDE + 0.5));
          numberOfPoints++;
        }
      }

      assertEquals(expectPointNumber, numberOfPoints);
      assertEquals(expectPauseNumber, numberOfPausePoint);
      assertEquals(expectResumeNumber, numberOfResumePoint);
    }
  }

}
