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

import android.app.AlertDialog;
import android.app.Instrumentation;
import android.content.Context;
import android.content.DialogInterface;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;

/**
 * Tests the battery and memory usage of long tracks. As this test may be run a
 * long time test and without connect to desktop computer during test. The test
 * result will be recorded to a file. And at the beginning of test, it need
 * user's confirm by click a button.
 * 
 * @author Youtao Liu
 */
public class ResourceUsageTest extends ActivityInstrumentationTestCase2<TrackListActivity> {

  public ResourceUsageTest() {
    super(TrackListActivity.class);
  }

  private Instrumentation instrumentation;
  private TrackListActivity trackListActivity;
  private boolean runTest = false;
  private static boolean isUserConfirmed = false;
  private Context context;
  /**
   * Default duration for each test is half hour.
   */
  private int TEST_DURATION_IN_MILLISECONDS = 30 * 60 * 1000;
  private int INTERVALE_TO_CHECK = 5 * 60 * 1000;

  @Override
  protected void setUp() throws Exception {
    runTest = BigTestUtils.runResourceUsageTest;
    super.setUp();
    if (!runTest) {
      return;
    }
    instrumentation = getInstrumentation();
    trackListActivity = getActivity();
    context = trackListActivity.getApplicationContext();
    EndToEndTestUtils.setupForDebug(instrumentation, trackListActivity);
    if (!isUserConfirmed) {
      userConfirmTest();
    }
    BigTestUtils.writeToFile("Test case start:" + getName() + "\r\n", true);
  }

  /**
   * Prompts a dialog to make user confirm whether start the test.
   */
  private void userConfirmTest() {
    try {
      trackListActivity.runOnUiThread(new Runnable() {
        @Override
        public void run() {
          new AlertDialog.Builder(EndToEndTestUtils.SOLO.getCurrentActivity())
              .setTitle("Confirm long time test.")
              .setMessage(
                  "Each case need run " + TEST_DURATION_IN_MILLISECONDS / 1000 / 60
                      + " minutes, and the battery/memory usage will be recorded in every "
                      + INTERVALE_TO_CHECK / 60 / 1000 + "minutes.")
              .setPositiveButton("Start Test", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                  isUserConfirmed = true;
                  runTest = true;
                  BigTestUtils.writeToFile("Test start:\r\n", false);
                }
              }).setNegativeButton("Cancel Test", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                  isUserConfirmed = true;
                  runTest = false;
                  fail();
                }
              }).show();
        }
      });
    } catch (Throwable e) {
      Log.i(EndToEndTestUtils.LOG_TAG, "Meet error when create confirm dialog.");
    }
    // Waiting user confirm the testing
    while (!isUserConfirmed) {
      EndToEndTestUtils.sleep(EndToEndTestUtils.TINY_WAIT_TIME);
    }
  }

  /**
   * Tests the resource usage when display track list activity during recording.
   */
  public void testBatteryUsage_showTrackList() {
    if (!runTest) {
      Log.i(EndToEndTestUtils.LOG_TAG, BigTestUtils.DISABLE_MESSAGE);
    }
    EndToEndTestUtils.startRecording();
    // Go back to track list.
    EndToEndTestUtils.SOLO.goBack();
    BigTestUtils.moniterTest(context, INTERVALE_TO_CHECK, TEST_DURATION_IN_MILLISECONDS);
    EndToEndTestUtils.stopRecording(true);
  }

  /**
   * Tests the resource usage when display map view during recording.
   */
  public void testBatteryUsage_showMapView() {
    if (!runTest) {
      Log.i(EndToEndTestUtils.LOG_TAG, BigTestUtils.DISABLE_MESSAGE);
    }
    EndToEndTestUtils.startRecording();
    EndToEndTestUtils.SOLO.clickOnText(trackListActivity.getString(R.string.track_detail_map_tab));
    BigTestUtils.moniterTest(context, INTERVALE_TO_CHECK, TEST_DURATION_IN_MILLISECONDS);
    EndToEndTestUtils.stopRecording(true);
  }

  /**
   * Tests the resource usage when display chart view during recording.
   */
  public void testBatteryUsage_showChartView() {
    if (!runTest) {
      Log.i(EndToEndTestUtils.LOG_TAG, BigTestUtils.DISABLE_MESSAGE);
    }
    EndToEndTestUtils.startRecording();
    EndToEndTestUtils.SOLO
        .clickOnText(trackListActivity.getString(R.string.track_detail_chart_tab));
    BigTestUtils.moniterTest(context, INTERVALE_TO_CHECK, TEST_DURATION_IN_MILLISECONDS);
    EndToEndTestUtils.stopRecording(true);
  }

  /**
   * Tests the resource usage when display stats view during recording.
   */
  public void testBatteryUsage_showStatsView() {
    if (!runTest) {
      Log.i(EndToEndTestUtils.LOG_TAG, BigTestUtils.DISABLE_MESSAGE);
    }
    EndToEndTestUtils.startRecording();
    EndToEndTestUtils.SOLO
        .clickOnText(trackListActivity.getString(R.string.track_detail_stats_tab));
    BigTestUtils.moniterTest(context, INTERVALE_TO_CHECK, TEST_DURATION_IN_MILLISECONDS);
    EndToEndTestUtils.stopRecording(true);
  }

  @Override
  protected void tearDown() throws Exception {
    EndToEndTestUtils.SOLO.finishOpenedActivities();
    BigTestUtils.writeToFile("Test case done:" + getName() + "\r\n\r\n", true);
    super.tearDown();
  }
}