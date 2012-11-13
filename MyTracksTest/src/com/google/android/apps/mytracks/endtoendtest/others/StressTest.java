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
import com.google.android.maps.mytracks.R;

import android.app.Instrumentation;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;

/**
 * Tests Rotation of MyTracks.
 * 
 * @author Youtao Liu
 */
public class StressTest extends ActivityInstrumentationTestCase2<TrackListActivity> {

  /**
   * Set to false as default.
   */
  public static boolean testStress = false;
  public static final int MINUTES_FOR_EACH_TEST = 30;
  public static final int MILLI_SECOND_IN_ONE_MINUTE = 60 * 1000;
  long startTime = 0;
  int trackNumber;

  public StressTest() {
    super(TrackListActivity.class);
  }

  private Instrumentation instrumentation;
  private TrackListActivity activityMyTracks;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    instrumentation = getInstrumentation();
    activityMyTracks = getActivity();
    EndToEndTestUtils.setupForDebug(instrumentation, activityMyTracks);
    startTime = System.currentTimeMillis();
    EndToEndTestUtils.emulatorPort = 5568;
  }

  /**
   * Records tracks and delete them when there are more than 30 tracks..
   */
  public void testRecordAndDeleteTracks() {
    if (!testStress) {
      Log.i(EndToEndTestUtils.LOG_TAG, SensorTest.DISABLE_MESSAGE);
      return;
    }
    for (int i = 0; (System.currentTimeMillis() - startTime) / MILLI_SECOND_IN_ONE_MINUTE < MINUTES_FOR_EACH_TEST; i++) {
      EndToEndTestUtils.startRecording();
      // Points in a tracks keep increasing.
      EndToEndTestUtils.sendGps(i * 10, 0, 10);
      EndToEndTestUtils.stopRecording(true);
      EndToEndTestUtils.SOLO.goBack();
      EndToEndTestUtils.instrumentation.waitForIdleSync();
      trackNumber = EndToEndTestUtils.SOLO.getCurrentListViews().get(0).getCount();
      if (trackNumber > 30) {
        EndToEndTestUtils.deleteAllTracks();
      }
      logStatus(i);
    }
  }

  /**
   * Rotates screen when display map view.
   */
  public void testRotateMapViewInTrackDetailActivity() {
    if (!testStress) {
      Log.i(EndToEndTestUtils.LOG_TAG, SensorTest.DISABLE_MESSAGE);
      return;
    }
    EndToEndTestUtils.startRecording();
    for (int i = 0; (System.currentTimeMillis() - startTime) / MILLI_SECOND_IN_ONE_MINUTE < MINUTES_FOR_EACH_TEST; i++) {
      EndToEndTestUtils.sendGps(10, i * 10);
      EndToEndTestUtils.rotateCurrentActivity();
    }
  }

  /**
   * Switches view between CHART, MAP and STAT.
   */
  public void testSwitchTabs() {
    if (!testStress) {
      Log.i(EndToEndTestUtils.LOG_TAG, SensorTest.DISABLE_MESSAGE);
      return;
    }
    EndToEndTestUtils.startRecording();
    int i = 0;
    while ((System.currentTimeMillis() - startTime) / MILLI_SECOND_IN_ONE_MINUTE < MINUTES_FOR_EACH_TEST) {
      EndToEndTestUtils.SOLO.clickOnText(activityMyTracks.getString(R.string.track_detail_map_tab));
      instrumentation.waitForIdleSync();
      EndToEndTestUtils.SOLO.clickOnText(activityMyTracks
          .getString(R.string.track_detail_chart_tab));
      instrumentation.waitForIdleSync();
      EndToEndTestUtils.SOLO.clickOnText(activityMyTracks
          .getString(R.string.track_detail_stats_tab));
      instrumentation.waitForIdleSync();
      logStatus(++i);
    }
  }

  /**
   * Displays log message.
   * 
   * @param times this test has been run
   */
  private void logStatus(int times) {
    Log.i(EndToEndTestUtils.LOG_TAG, "This test has run "
        + times
        + " times and will be finished in "
        + (MINUTES_FOR_EACH_TEST - (System.currentTimeMillis() - startTime)
            / MILLI_SECOND_IN_ONE_MINUTE) + " minutes!");
    Log.i(EndToEndTestUtils.LOG_TAG, "There are " + trackNumber + " tracks!");
  }

}
