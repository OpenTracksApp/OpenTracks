/*
 * Copyright 2013 Google Inc.
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

import com.google.android.maps.mytracks.R;

/**
 * Run configuration for various tests.
 * 
 * @author Youtao Liu
 */
public class RunConfiguration {

  private static RunConfiguration instance;

  /**
   * Gets an instance of {@link RunConfiguration}.
   */
  public static RunConfiguration getInstance() {
    if (instance == null) {
      instance = new RunConfiguration();
    }
    return instance;
  }

  /*
   * Set to false as default. True to run the test. Default to false since these
   * tests can take a long time.
   */
  private boolean runResourceTest = false;
  private boolean runSensorTest = false;
  private boolean runStressTest = false;
  private boolean runSyncTest = false;

  private RunConfiguration() {
    // Sets run sync test in run-time
    setRunSyncTest(canRunSyncTest());
  }

  /**
   * Returns true to run the resource test.
   */
  public boolean getRunResourceTest() {
    return runResourceTest;
  }

  /**
   * Sets run resource test.
   * 
   * @param run true to run
   */
  public void setRunResourceTest(boolean run) {
    runResourceTest = run;
  }

  /**
   * Returns true to run the sensor test.
   */
  public boolean getRunSensorTest() {
    return runSensorTest;
  }

  /**
   * Sets run sensor test.
   * 
   * @param run true to run
   */
  public void setRunSensorTest(boolean run) {
    runSensorTest = run;
  }

  /**
   * Returns true to run the stress test.
   */
  public boolean getRunStressTest() {
    return runStressTest;
  }

  /**
   * Sets run stress test.
   * 
   * @param run true to run
   */
  public void setRunStressTest(boolean run) {
    runStressTest = run;
  }

  /**
   * Returns true to run the sync test.
   */
  public boolean getRunSyncTest() {
    return runSyncTest;
  }

  /**
   * Sets run sync test.
   * 
   * @param run true to run
   */
  public void setRunSyncTest(boolean run) {
    runSyncTest = run;
  }

  /**
   * Returns true if can run the sync test. Needs two specific test accounts on
   * the device.
   */
  private boolean canRunSyncTest() {
    EndToEndTestUtils.findMenuItem(
        EndToEndTestUtils.trackListActivity.getString(R.string.menu_sync_drive), true);
    boolean canRun = EndToEndTestUtils.SOLO.waitForText(
        GoogleUtils.ACCOUNT_1, 1, EndToEndTestUtils.SHORT_WAIT_TIME)
        && EndToEndTestUtils.SOLO.waitForText(
            GoogleUtils.ACCOUNT_2, 1, EndToEndTestUtils.TINY_WAIT_TIME);
    EndToEndTestUtils.SOLO.clickOnText(
        EndToEndTestUtils.trackListActivity.getString(R.string.generic_cancel));
    return canRun;
  }
}
