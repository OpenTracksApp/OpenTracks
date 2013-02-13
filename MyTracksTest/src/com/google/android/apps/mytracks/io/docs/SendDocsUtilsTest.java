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

package com.google.android.apps.mytracks.io.docs;

import android.test.AndroidTestCase;

/**
 * Tests {@link SendDocsUtils}.
 * 
 * @author Jimmy Shih
 */
public class SendDocsUtilsTest extends AndroidTestCase {

  /**
   * Tests {@link SendDocsUtils#getDistance(double, boolean)} with metric units.
   */
  public void testGetDistance_metric() {
    assertEquals("1.22", SendDocsUtils.getDistance(1222.3, true));
  }

  /**
   * Tests {@link SendDocsUtils#getDistance(double, boolean)} with imperial
   * units.
   */
  public void testGetDistance_imperial() {
    assertEquals("0.76", SendDocsUtils.getDistance(1222.3, false));
  }

  /**
   * Tests {@link SendDocsUtils#getSpeed(double, boolean)} with metric units.
   */
  public void testGetSpeed_metric() {
    assertEquals("15.55", SendDocsUtils.getSpeed(4.32, true));
  }

  /**
   * Tests {@link SendDocsUtils#getSpeed(double, boolean)} with imperial units.
   */
  public void testGetSpeed_imperial() {
    assertEquals("9.66", SendDocsUtils.getSpeed(4.32, false));
  }

  /**
   * Tests {@link SendDocsUtils#getElevation(double, boolean)} with metric
   * units.
   */
  public void testGetElevation_metric() {
    assertEquals("3", SendDocsUtils.getElevation(3.456, true));
  }

  /**
   * Tests {@link SendDocsUtils#getElevation(double, boolean)} with imperial
   * units.
   */
  public void testGetElevation_imperial() {
    assertEquals("11", SendDocsUtils.getElevation(3.456, false));
  }
}
