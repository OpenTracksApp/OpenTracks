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
import com.google.android.maps.mytracks.R;

import android.content.Context;
import android.test.AndroidTestCase;

/**
 * Tests for the {@link DynamicSpeedTrackPathDescriptor}.
 * 
 * @author Youtao Liu
 */
public class FixedSpeedTrackPathDescriptorTest extends AndroidTestCase {

  private Context context;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    context = getContext();
  }

  /**
   * Tests the initialization of slowSpeed and normalSpeed in {@link DynamicSpeedTrackPathDescriptor#DynamicSpeedTrackPathDescriptor(Context)}
   * .
   */
  public void testConstructor() {
    int[] slowSpeedExpectations = { 0, 1, 99, PreferencesUtils.TRACK_COLOR_MODE_SLOW_DEFAULT };
    int[] normalSpeedExpectations = { 0, 1, 99, PreferencesUtils.TRACK_COLOR_MODE_MEDIUM_DEFAULT };
    for (int i = 0; i < slowSpeedExpectations.length; i++) {
      PreferencesUtils.setInt(
          context, R.string.track_color_mode_slow_key, slowSpeedExpectations[i]);
      PreferencesUtils.setInt(
          context, R.string.track_color_mode_medium_key, normalSpeedExpectations[i]);
      FixedSpeedTrackPathDescriptor fixedSpeedTrackPathDescriptor = new FixedSpeedTrackPathDescriptor(
          context);
      assertEquals(slowSpeedExpectations[i], fixedSpeedTrackPathDescriptor.getSlowSpeed());
      assertEquals(normalSpeedExpectations[i], fixedSpeedTrackPathDescriptor.getNormalSpeed());
    }
  }

  /**
   * Tests {@link FixedSpeedTrackPathDescriptor#getSlowSpeed()} and
   * {@link FixedSpeedTrackPathDescriptor#getNormalSpeed()}.
   */
  public void testGetSpeed() {
    FixedSpeedTrackPathDescriptor fixedSpeedTrackPathDescriptor = new FixedSpeedTrackPathDescriptor(
        context);
    int slowSpeed = fixedSpeedTrackPathDescriptor.getSlowSpeed();
    int normalSpeed = fixedSpeedTrackPathDescriptor.getNormalSpeed();
    // Change value in shared preferences
    PreferencesUtils.setInt(context, R.string.track_color_mode_slow_key, slowSpeed + 2);
    PreferencesUtils.setInt(context, R.string.track_color_mode_medium_key, normalSpeed + 2);
    assertEquals(slowSpeed, fixedSpeedTrackPathDescriptor.getSlowSpeed());
    assertEquals(normalSpeed, fixedSpeedTrackPathDescriptor.getNormalSpeed());
  }
}