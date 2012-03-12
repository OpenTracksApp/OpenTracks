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

import com.google.android.apps.mytracks.ChartView.Mode;
import com.google.android.maps.mytracks.R;

import android.test.ActivityInstrumentationTestCase2;

/**
 * Tests the {@link ChartSettingsDialog}.
 * 
 * @author Youtao Liu
 */
public class ChartSettingsDialogTest extends ActivityInstrumentationTestCase2<ChartActivity> {

  private ChartSettingsDialog chartSettingsDialog;

  public ChartSettingsDialogTest() {
    super(ChartActivity.class);
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    chartSettingsDialog = new ChartSettingsDialog(getActivity());
    chartSettingsDialog.show();
  }

  /**
   * Tests the {@link ChartSettingsDialog#setMode} and check the result by
   * {@link ChartSettingsDialog#getMode}. Gets all modes of Mode, then set and
   * get each mode.
   */
  public void testSetMode() {
    Mode[] modes = Mode.values();
    for (Mode mode : modes) {
      chartSettingsDialog.setMode(mode);
      assertEquals(mode, chartSettingsDialog.getMode());
    }
  }

  /**
   * Tests the {@link ChartSettingsDialog#setDisplaySpeed}.
   */
  public void testSetDisplaySpeed() {
    chartSettingsDialog.setDisplaySpeed(true);
    assertEquals(getActivity().getString(R.string.stat_speed),
        chartSettingsDialog.getSeries()[ChartView.SPEED_SERIES].getText());
    chartSettingsDialog.setDisplaySpeed(false);
    assertEquals(getActivity().getString(R.string.stat_pace),
        chartSettingsDialog.getSeries()[ChartView.SPEED_SERIES].getText());
  }

  /**
   * Tests the {@link ChartSettingsDialog#setSeriesEnabled} and check the result
   * by {@link ChartSettingsDialog#isSeriesEnabled}.
   */
  public void testSetSeriesEnabled() {
    for (int i = 0; i < ChartView.NUM_SERIES; i++) {
      chartSettingsDialog.setSeriesEnabled(i, true);
      assertEquals(true, chartSettingsDialog.getSeries()[i].isChecked());
      assertEquals(true, chartSettingsDialog.isSeriesEnabled(i));
      chartSettingsDialog.setSeriesEnabled(i, false);
      assertEquals(false, chartSettingsDialog.getSeries()[i].isChecked());
      assertEquals(false, chartSettingsDialog.isSeriesEnabled(i));
    }
  }
}