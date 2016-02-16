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
package com.google.android.apps.mytracks.endtoendtest.common;

import com.google.android.apps.mytracks.TrackListActivity;
import com.google.android.apps.mytracks.content.MyTracksProviderUtils;
import com.google.android.apps.mytracks.content.MyTracksProviderUtils.LocationIterator;
import com.google.android.apps.mytracks.endtoendtest.EndToEndTestUtils;
import com.google.android.apps.mytracks.services.TrackRecordingService;

import android.app.Instrumentation;
import android.location.Location;
import android.test.ActivityInstrumentationTestCase2;

/**
 * Tests pause and resume during a recording.
 * 
 * @author Youtao Liu
 */
public class PauseResumeTest extends ActivityInstrumentationTestCase2<TrackListActivity> {

  private static final int SEND_INTERVAL = 100; // ms

  private Instrumentation instrumentation;
  private TrackListActivity trackListActivity;

  public PauseResumeTest() {
    super(TrackListActivity.class);
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    instrumentation = getInstrumentation();
    trackListActivity = getActivity();
    EndToEndTestUtils.setupForAllTest(instrumentation, trackListActivity);
  }

  @Override
  protected void tearDown() throws Exception {
    EndToEndTestUtils.SOLO.finishOpenedActivities();
    super.tearDown();
  }

  /**
   * Tests stop after a pause.
   */
  public void testStopAfterPause() {
    int firstBatch = 3;
    int secondBatch = 10;

    EndToEndTestUtils.checkNotRecording();

    // Start a recording
    EndToEndTestUtils.startRecording();
    EndToEndTestUtils.checkUnderRecording();
    EndToEndTestUtils.sendGps(firstBatch);

    // Pause the recording
    EndToEndTestUtils.pauseRecording();
    EndToEndTestUtils.checkUnderPaused();
    EndToEndTestUtils.sendGps(secondBatch, firstBatch);

    // Stop the recording
    EndToEndTestUtils.stopRecording(true);
    checkTrackLocation(firstBatch, 1, 0);
    EndToEndTestUtils.SOLO.goBack();
    EndToEndTestUtils.checkNotRecording();
  }

  /**
   * Tests stop after a resume.
   */
  public void testStopAfterResume() {
    int firstBatch = 3;
    int secondBatch = 5;
    int thirdBatch = 7;

    EndToEndTestUtils.checkNotRecording();

    // Start a recording
    EndToEndTestUtils.startRecording();
    EndToEndTestUtils.checkUnderRecording();
    EndToEndTestUtils.sendGps(firstBatch);

    // Pause the recording
    EndToEndTestUtils.pauseRecording();
    EndToEndTestUtils.checkUnderPaused();

    // Send gps signal after pause
    EndToEndTestUtils.sendGps(secondBatch, firstBatch, SEND_INTERVAL);

    // Resume the recording
    EndToEndTestUtils.resumeRecording();
    EndToEndTestUtils.checkUnderRecording();
    EndToEndTestUtils.sendGps(thirdBatch, firstBatch + secondBatch);

    // Stop the recordinga
    EndToEndTestUtils.stopRecording(true);
    checkTrackLocation(firstBatch + secondBatch, 1, 1);
    EndToEndTestUtils.SOLO.goBack();
    EndToEndTestUtils.checkNotRecording();
  }

  /**
   * Check the last track's location.
   * 
   * @param expectedPoint the number of expected points
   * @param expectedPause the number of expected pauses
   * @param expectedResume the number of expected resumes
   */
  private void checkTrackLocation(int expectedPoint, int expectedPause, int expectedResume) {
    if (EndToEndTestUtils.isEmulator) {
      int point = 0;
      int pause = 0;
      int resume = 0;

      MyTracksProviderUtils myTracksProviderUtils = MyTracksProviderUtils.Factory.get(
          trackListActivity.getApplicationContext());
      long trackId = myTracksProviderUtils.getLastTrack().getId();
      LocationIterator locationIterator = myTracksProviderUtils.getTrackPointLocationIterator(
          trackId, 0, false, MyTracksProviderUtils.DEFAULT_LOCATION_FACTORY);
      while (locationIterator.hasNext()) {
        Location location = locationIterator.next();
        double latitude = location.getLatitude();
        if (latitude == TrackRecordingService.PAUSE_LATITUDE) {
          pause++;
        } else if (latitude == TrackRecordingService.RESUME_LATITUDE) {
          resume++;
        } else {
          point++;
        }
      }
      assertEquals(expectedPoint, point);
      assertEquals(expectedPause, pause);
      assertEquals(expectedResume, resume);
    }
  }
}
