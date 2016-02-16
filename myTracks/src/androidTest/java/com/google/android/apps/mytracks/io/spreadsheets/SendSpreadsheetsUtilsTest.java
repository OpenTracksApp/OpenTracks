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

package com.google.android.apps.mytracks.io.spreadsheets;

import com.google.android.apps.mytracks.io.spreadsheets.SendSpreadsheetsUtils;

import android.test.AndroidTestCase;

/**
 * Tests {@link SendSpreadsheetsUtils}.
 * 
 * @author Jimmy Shih
 */
public class SendSpreadsheetsUtilsTest extends AndroidTestCase {

  /**
   * Tests {@link SendSpreadsheetsUtils#getDistance(double, boolean)} with metric units.
   */
  public void testGetDistance_metric() {
    assertEquals("1.22", SendSpreadsheetsUtils.getDistance(1222.3, true));
  }

  /**
   * Tests {@link SendSpreadsheetsUtils#getDistance(double, boolean)} with imperial
   * units.
   */
  public void testGetDistance_imperial() {
    assertEquals("0.76", SendSpreadsheetsUtils.getDistance(1222.3, false));
  }

  /**
   * Tests {@link SendSpreadsheetsUtils#getSpeed(double, boolean)} with metric units.
   */
  public void testGetSpeed_metric() {
    assertEquals("15.55", SendSpreadsheetsUtils.getSpeed(4.32, true));
  }

  /**
   * Tests {@link SendSpreadsheetsUtils#getSpeed(double, boolean)} with imperial units.
   */
  public void testGetSpeed_imperial() {
    assertEquals("9.66", SendSpreadsheetsUtils.getSpeed(4.32, false));
  }

  /**
   * Tests {@link SendSpreadsheetsUtils#getElevation(double, boolean)} with metric
   * units.
   */
  public void testGetElevation_metric() {
    assertEquals("3", SendSpreadsheetsUtils.getElevation(3.456, true));
  }

  /**
   * Tests {@link SendSpreadsheetsUtils#getElevation(double, boolean)} with imperial
   * units.
   */
  public void testGetElevation_imperial() {
    assertEquals("11", SendSpreadsheetsUtils.getElevation(3.456, false));
  }
}
