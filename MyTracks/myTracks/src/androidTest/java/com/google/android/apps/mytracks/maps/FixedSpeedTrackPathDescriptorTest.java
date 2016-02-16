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
package com.google.android.apps.mytracks.maps;

import com.google.android.apps.mytracks.util.PreferencesUtils;
import com.google.android.apps.mytracks.util.UnitConversions;
import com.google.android.maps.mytracks.R;

import android.content.Context;
import android.test.AndroidTestCase;

/**
 * Tests for the {@link FixedSpeedTrackPathDescriptor}.
 * 
 * @author Youtao Liu
 */
public class FixedSpeedTrackPathDescriptorTest extends AndroidTestCase {

  @Override
  protected void tearDown() throws Exception {
    Context context = getContext();
    PreferencesUtils.setString(
        context, R.string.stats_units_key, PreferencesUtils.STATS_UNITS_DEFAULT);
    PreferencesUtils.setInt(context, R.string.track_color_mode_slow_key,
        PreferencesUtils.TRACK_COLOR_MODE_SLOW_DEFAULT);
    PreferencesUtils.setInt(context, R.string.track_color_mode_medium_key,
        PreferencesUtils.TRACK_COLOR_MODE_MEDIUM_DEFAULT);
    super.tearDown();
  }

  /**
   * Tests when the slow speed and the normal speed are both zero.
   */
  public void testGetSpeed_zero() {
    testSpeed(0, 0);
  }

  /**
   * Tests when the slow speed and the normal speed are both one.
   */
  public void testGetSpeed_one() {
    testSpeed(1, 1);
  }

  /**
   * Tests when the slow speed and the normal speed are both large number. E.g.,
   * 99 and 100.
   */
  public void testGetSpeed_large() {
    testSpeed(99, 100);
  }

  /**
   * Tests when the slow speed and the normal speed are both the default value.
   */
  public void testGetSpeed_default() {
    testSpeed(PreferencesUtils.TRACK_COLOR_MODE_SLOW_DEFAULT,
        PreferencesUtils.TRACK_COLOR_MODE_MEDIUM_DEFAULT);
  }

  private void testSpeed(int slowSpeed, int normalSpeed) {
    testSpeed(slowSpeed, normalSpeed, true);
    testSpeed(slowSpeed, normalSpeed, false);
  }

  private void testSpeed(int slowSpeed, int normalSpeed, boolean metric) {
    Context context = getContext();
    PreferencesUtils.setString(context, R.string.stats_units_key,
        context.getString(metric ? R.string.stats_units_metric : R.string.stats_units_imperial));
    PreferencesUtils.setInt(context, R.string.track_color_mode_slow_key, slowSpeed);
    PreferencesUtils.setInt(context, R.string.track_color_mode_medium_key, normalSpeed);
    FixedSpeedTrackPathDescriptor fixedSpeedTrackPathDescriptor = new FixedSpeedTrackPathDescriptor(
        context);
    double expectedSlowSpeed = metric ? slowSpeed : slowSpeed * UnitConversions.MI_TO_KM;
    double expectedNormalSpeed = metric ? normalSpeed : normalSpeed * UnitConversions.MI_TO_KM;
    assertEquals(expectedSlowSpeed, fixedSpeedTrackPathDescriptor.getSlowSpeed());
    assertEquals(expectedNormalSpeed, fixedSpeedTrackPathDescriptor.getNormalSpeed());
  }
}