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
package com.google.android.apps.mytracks.endtoendtest.others;

import com.google.android.apps.mytracks.TrackListActivity;
import com.google.android.apps.mytracks.endtoendtest.EndToEndTestUtils;
import com.google.android.apps.mytracks.endtoendtest.RunConfiguration;
import com.google.android.maps.mytracks.R;

import android.app.Instrumentation;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;
import android.widget.ListView;

/**
 * Stress test of MyTracks.
 * 
 * @author Youtao Liu
 */
public class StressTest extends ActivityInstrumentationTestCase2<TrackListActivity> {

  private static final int TEST_DURATION_IN_MILLISECONDS = 30 * 60 * 1000;
  private static final int MAX_TRACK_NUMBER = 30;
  private boolean runTest = false;

  private long startTime = 0;
  private int numberOfTracks;

  public StressTest() {
    super(TrackListActivity.class);
  }

  private Instrumentation instrumentation;
  private TrackListActivity trackListActivity;

  @Override
  protected void setUp() throws Exception {
    runTest = RunConfiguration.runStressTest;
    super.setUp();
    if (!runTest) {
      return;
    }
    instrumentation = getInstrumentation();
    trackListActivity = getActivity();
    EndToEndTestUtils.setupForAllTest(instrumentation, trackListActivity);
    BigTestUtils.unlockAndWakeupDevice();
    startTime = System.currentTimeMillis();
  }

  /**
   * Records tracks and deletes them when there are more than
   * {@link #MAX_TRACK_NUMBER} tracks. Keeps sending different GPS locations in
   * each track, making it closer to the real stress.
   */
  public void testRecordAndDeleteTracks() {
    if (!runTest) {
      Log.i(EndToEndTestUtils.LOG_TAG, BigTestUtils.DISABLE_MESSAGE);
      return;
    }
    for (int i = 0; (System.currentTimeMillis() - startTime) < TEST_DURATION_IN_MILLISECONDS; i++) {
      EndToEndTestUtils.startRecording();
      // Points in a tracks keep increasing.
      EndToEndTestUtils.sendGps(i * 10, 0, 10);
      EndToEndTestUtils.stopRecording(true);
      EndToEndTestUtils.SOLO.goBack();
      EndToEndTestUtils.instrumentation.waitForIdleSync();
      numberOfTracks = EndToEndTestUtils.SOLO.getCurrentViews(ListView.class).get(0).getCount();
      if (numberOfTracks > MAX_TRACK_NUMBER) {
        EndToEndTestUtils.deleteAllTracks();
      }
      logStatus(i);
    }
  }

  /**
   * Rotates screen when display map view. Keeps sending different GPS locations
   * in each track, making it closer to the real stress.
   */
  public void testRotateMapViewInTrackDetailActivity() {
    if (!runTest) {
      Log.i(EndToEndTestUtils.LOG_TAG, BigTestUtils.DISABLE_MESSAGE);
      return;
    }
    EndToEndTestUtils.startRecording();
    for (int i = 0; (System.currentTimeMillis() - startTime) < TEST_DURATION_IN_MILLISECONDS; i++) {
      EndToEndTestUtils.sendGps(10, i * 10);
      EndToEndTestUtils.rotateCurrentActivity();
      Log.i(EndToEndTestUtils.LOG_TAG, String.format("Totate %d times in %d minutes!", i,
          (System.currentTimeMillis() - startTime) / 1000 / 60));
    }
  }

  /**
   * Switches view between MAP, CHART and STAT.
   */
  public void testSwitchTabs() {
    if (!runTest) {
      Log.i(EndToEndTestUtils.LOG_TAG, BigTestUtils.DISABLE_MESSAGE);
      return;
    }
    EndToEndTestUtils.startRecording();
    int i = 0;
    while ((System.currentTimeMillis() - startTime) < TEST_DURATION_IN_MILLISECONDS) {
      EndToEndTestUtils.SOLO
          .clickOnText(trackListActivity.getString(R.string.track_detail_map_tab));
      instrumentation.waitForIdleSync();
      EndToEndTestUtils.SOLO.clickOnText(trackListActivity
          .getString(R.string.track_detail_chart_tab));
      instrumentation.waitForIdleSync();
      EndToEndTestUtils.SOLO.clickOnText(trackListActivity
          .getString(R.string.track_detail_stats_tab));
      instrumentation.waitForIdleSync();
      logStatus(++i);
    }
    EndToEndTestUtils.stopRecording(true);
  }

  /**
   * Records a long track.
   */
  public void testRecordLongTrack() {
    if (!runTest) {
      Log.i(EndToEndTestUtils.LOG_TAG, BigTestUtils.DISABLE_MESSAGE);
      return;
    }
    EndToEndTestUtils.startRecording();
    int gpsNumber = 5000;
    for (int i = 0; i < gpsNumber / 10; i++) {
      EndToEndTestUtils.sendGps(10, i * 10);
    }
    EndToEndTestUtils.stopRecording(true);
  }

  /**
   * Switches view between MAP, CHART and STAT. And create way points, pause and
   * resume the track during recording.
   */
  public void testSwitchTabs_wayPoints() {
    if (!runTest) {
      Log.i(EndToEndTestUtils.LOG_TAG, BigTestUtils.DISABLE_MESSAGE);
      return;
    }
    EndToEndTestUtils.startRecording();

    int i = 0;
    while ((System.currentTimeMillis() - startTime) < TEST_DURATION_IN_MILLISECONDS) {
      // Create one way points.
      EndToEndTestUtils.sendGps(10, 10 * i);
      EndToEndTestUtils.createWaypoint(i);
      instrumentation.waitForIdleSync();
      EndToEndTestUtils.SOLO.waitForText(trackListActivity.getString(R.string.generic_recording));
      EndToEndTestUtils.pauseRecording();

      EndToEndTestUtils.SOLO
          .clickOnText(trackListActivity.getString(R.string.track_detail_map_tab));
      instrumentation.waitForIdleSync();
      EndToEndTestUtils.SOLO.clickOnText(trackListActivity
          .getString(R.string.track_detail_chart_tab));
      instrumentation.waitForIdleSync();
      EndToEndTestUtils.SOLO.clickOnText(trackListActivity
          .getString(R.string.track_detail_stats_tab));
      instrumentation.waitForIdleSync();
      EndToEndTestUtils.SOLO.waitForText(trackListActivity.getString(R.string.generic_paused));
      EndToEndTestUtils.resumeRecording();
      logStatus(++i);
    }
    EndToEndTestUtils.stopRecording(true);
  }

  /**
   * Logs status.
   * 
   * @param times the number of times this test has been run
   */
  private void logStatus(int times) {
    int minutes = (int) (TEST_DURATION_IN_MILLISECONDS - (System.currentTimeMillis() - startTime)) / 60 / 1000;
    Log.i(EndToEndTestUtils.LOG_TAG, String.format(
        "This test has run %d times and will be finished in %d minutes!", times, minutes));
    Log.i(EndToEndTestUtils.LOG_TAG, String.format("There are %d tracks!", numberOfTracks));
  }
}