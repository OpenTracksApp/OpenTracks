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

import android.os.Bundle;
import android.test.InstrumentationTestRunner;
import android.util.Log;

/**
 * End-to-end test test runner.
 * 
 * @author Youtao Liu
 */
public class EndToEndTestRunner extends InstrumentationTestRunner {

  private static final String TAG = EndToEndTestRunner.class.getSimpleName();
  private static final String PORT_KEY = "port";
  private static final String RESOURCE_KEY = "resource";
  private static final String SENSOR_KEY = "sensor";
  private static final String STRESS_KEY = "stress";
  private static final String TRUE_VALUE = "true";

  @Override
  public void onCreate(Bundle bundle) {

    // Get the emulator port parameter for GPS signals
    String port = bundle.getString(PORT_KEY);
    if (port != null) {
      try {
        EndToEndTestUtils.emulatorPort = Integer.parseInt(port);
      } catch (Exception e) {
        Log.e(TAG, "Unable to get emulator port parameter, use the default value.", e);
      }
    }
    Log.d(TAG, "Emulator port: " + EndToEndTestUtils.emulatorPort);

    RunConfiguration runConfiguration = RunConfiguration.getInstance();

    runConfiguration.setRunResourceTest(
        TRUE_VALUE.equalsIgnoreCase(bundle.getString(RESOURCE_KEY)));
    Log.d(TAG, "Run resource test: " + runConfiguration.getRunResourceTest());

    runConfiguration.setRunSensorTest(TRUE_VALUE.equalsIgnoreCase(bundle.getString(SENSOR_KEY)));
    Log.d(TAG, "Run sensor test: " + runConfiguration.getRunSensorTest());

    runConfiguration.setRunStressTest(TRUE_VALUE.equalsIgnoreCase(bundle.getString(STRESS_KEY)));
    Log.d(TAG, "Run stress test: " + runConfiguration.getRunStressTest());

    super.onCreate(bundle);
  }
}
