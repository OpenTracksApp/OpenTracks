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
package com.google.android.apps.mytracks;

import com.google.android.maps.mytracks.R;

import android.test.AndroidTestCase;

/**
 * Tests for the AntPreference.
 * 
 * @author Youtao Liu
 */
public class AntPreferenceTest extends AndroidTestCase {

  public void testNotPaired() {

    AntPreference antPreference = new AntPreference(getContext()) {
      @Override
      protected int getPersistedInt(int defaultReturnValue) {
        return 0;
      }
    };
    assertEquals(getContext().getString(R.string.settings_sensor_ant_not_paired),
        antPreference.getSummary());
  }

  public void testPaired() {
    int persistInt = 1;
    AntPreference antPreference = new AntPreference(getContext()) {
      @Override
      protected int getPersistedInt(int defaultReturnValue) {
        return 1;
      }
    };
    assertEquals(
        getContext().getString(R.string.settings_sensor_ant_paired, persistInt),
        antPreference.getSummary());
  }
}