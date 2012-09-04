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
import com.google.android.apps.mytracks.util.StringUtils;
import com.google.android.maps.mytracks.R;

import android.annotation.TargetApi;
import android.app.Instrumentation;
import android.location.Location;
import android.test.ActivityInstrumentationTestCase2;
import android.widget.TextView;

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

    checkNotRecording();
    // Start recording
    EndToEndTestUtils.startRecording();
    checkUnderRecording();
    EndToEndTestUtils.sendGps(gpsSignalNumber);

    // Pause
    EndToEndTestUtils.pauseRecording();
    checkUnderPaused();
    EndToEndTestUtils.sendGps(gpsSignalNumber, gpsSignalNumber);

    // Stop
    EndToEndTestUtils.stopRecording(true);
    checkPointsInPausedTrack(gpsSignalNumber, 1, 0);
    EndToEndTestUtils.SOLO.goBack();
    checkNotRecording();
  }

  /**
   * Tests the pause recording feature. Stops the recording after resume.
   */
  public void testPauseRecording_stopAfterResume() {
    int gpsSignalNumber = 3;

    checkNotRecording();
    // Start recording
    EndToEndTestUtils.startRecording();
    checkUnderRecording();
    EndToEndTestUtils.sendGps(gpsSignalNumber);

    // Pause
    EndToEndTestUtils.pauseRecording();
    checkUnderPaused();

    // Send Gps signal after pause.
    // Add 10 to make these signals are apparently different.
    EndToEndTestUtils.sendGps(gpsSignalNumber, gpsSignalNumber + 10, 10);

    // Resume
    EndToEndTestUtils.resumeRecording();
    checkUnderRecording();
    EndToEndTestUtils.sendGps(gpsSignalNumber, gpsSignalNumber);

    // Stop
    EndToEndTestUtils.stopRecording(true);
    checkPointsInPausedTrack(gpsSignalNumber * 2, 1, 1);
    EndToEndTestUtils.SOLO.goBack();
    checkNotRecording();
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
          assertEquals(numberOfPoints, (int) ((latitude - EndToEndTestUtils.START_LATITUDE)
              / EndToEndTestUtils.DELTA_LADITUDE + 0.5));
          assertEquals(numberOfPoints, (int) ((longitude - EndToEndTestUtils.START_LONGITUDE)
              / EndToEndTestUtils.DELTA_LONGITUDE + 0.5));
          numberOfPoints++;
        }
      }

      assertEquals(expectPointNumber, numberOfPoints);
      assertEquals(expectPauseNumber, numberOfPausePoint);
      assertEquals(expectResumeNumber, numberOfResumePoint);
    }
  }

  /**
   * Checks whether the recording is not started.
   */
  static void checkNotRecording() {
    EndToEndTestUtils.instrumentation.waitForIdleSync();
    assertFalse(EndToEndTestUtils.SOLO.getCurrentActivity()
        .findViewById(R.id.track_controller_stop).isEnabled());
    assertNull(EndToEndTestUtils.findTextView(EndToEndTestUtils.activityMytracks
        .getString(R.string.generic_recording)));
    assertNull(EndToEndTestUtils.findTextView(EndToEndTestUtils.activityMytracks
        .getString(R.string.generic_paused)));
    TextView totalTime = (TextView) EndToEndTestUtils.activityMytracks
        .findViewById(R.id.track_controller_total_time);
    assertEquals(StringUtils.formatElapsedTimeWithHour(0), totalTime.getText().toString());
  }

  /**
   * Checks whether the MyTracks is under recording.
   */
  static void checkUnderRecording() {
    EndToEndTestUtils.instrumentation.waitForIdleSync();
    assertTrue(EndToEndTestUtils.SOLO.getCurrentActivity().findViewById(R.id.track_controller_stop)
        .isEnabled());
    assertNotNull(EndToEndTestUtils.findTextView(EndToEndTestUtils.activityMytracks
        .getString(R.string.generic_recording)));
    assertNull(EndToEndTestUtils.findTextView(EndToEndTestUtils.activityMytracks
        .getString(R.string.generic_paused)));

    String totalTimeOld = ((TextView) EndToEndTestUtils.SOLO.getCurrentActivity().findViewById(
        R.id.track_controller_total_time)).getText().toString();
    EndToEndTestUtils.sleep(2000);
    String totalTimeNew = ((TextView) EndToEndTestUtils.SOLO.getCurrentActivity().findViewById(
        R.id.track_controller_total_time)).getText().toString();
    assertFalse(totalTimeOld.equalsIgnoreCase(totalTimeNew));
  }

  /**
   * Checks whether the recording is paused.
   */
  static void checkUnderPaused() {
    EndToEndTestUtils.instrumentation.waitForIdleSync();
    assertTrue(EndToEndTestUtils.SOLO.getCurrentActivity().findViewById(R.id.track_controller_stop)
        .isEnabled());
    assertNull(EndToEndTestUtils.findTextView(EndToEndTestUtils.activityMytracks
        .getString(R.string.generic_recording)));
    assertNotNull(EndToEndTestUtils.findTextView(EndToEndTestUtils.activityMytracks
        .getString(R.string.generic_paused)));

    String totalTimeOld = ((TextView) EndToEndTestUtils.activityMytracks
        .findViewById(R.id.track_controller_total_time)).getText().toString();
    EndToEndTestUtils.sleep(2000);
    String totalTimeNew = ((TextView) EndToEndTestUtils.activityMytracks
        .findViewById(R.id.track_controller_total_time)).getText().toString();
    assertTrue(totalTimeOld.equalsIgnoreCase(totalTimeNew));
  }

  @Override
  protected void tearDown() throws Exception {
    // In normally, the activities of MyTracks should be closed after every test
    // is completed. But this case is different, send two go back event to make
    // it will not block following tests.
    EndToEndTestUtils.SOLO.goBack();
    EndToEndTestUtils.SOLO.goBack();
    super.tearDown();
  }

}
