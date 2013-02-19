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
 * Places all the test configuration in this class.
 * 
 * @author Youtao Liu
 */
public class RunConfiguration {
  private static RunConfiguration instance = null;
  
  /**
   * Set to false as default. True to run the test. Default to false since this
   * test can take a long time.
   */
  public static boolean runStressTest = true;
  public static boolean runResourceUsageTest = true;
  public static boolean runSensorTest = true;
  public boolean runSyncTest = false;

  private RunConfiguration() {
    runSyncTest = canRunSyncTest();
  }

  public static RunConfiguration getInstance() {
    if (instance == null) {
      instance = new RunConfiguration();
    }
    return instance;
  }

  /**
   * Runs sync tests when both test accounts are bound with the devices.
   * 
   * @return true means can run sync tests in this device
   */
  public static boolean canRunSyncTest() {
    EndToEndTestUtils.findMenuItem(
        EndToEndTestUtils.activityMytracks.getString(R.string.menu_settings), true);
    EndToEndTestUtils.SOLO.clickOnText(EndToEndTestUtils.activityMytracks
        .getString(R.string.settings_google));
    EndToEndTestUtils.SOLO.clickOnText(EndToEndTestUtils.activityMytracks
        .getString(R.string.settings_google_account_title));
    boolean canRunSyncE2ETest = EndToEndTestUtils.SOLO.waitForText(GoogleUtils.ACCOUNT_NAME_1, 1,
        EndToEndTestUtils.SHORT_WAIT_TIME)
        && EndToEndTestUtils.SOLO.waitForText(GoogleUtils.ACCOUNT_NAME_2, 1,
            EndToEndTestUtils.TINY_WAIT_TIME);
    EndToEndTestUtils.SOLO.clickOnText(EndToEndTestUtils.activityMytracks
        .getString(R.string.generic_cancel));
    EndToEndTestUtils.SOLO.goBack();
    EndToEndTestUtils.SOLO.goBack();
    return canRunSyncE2ETest;
  }
}
