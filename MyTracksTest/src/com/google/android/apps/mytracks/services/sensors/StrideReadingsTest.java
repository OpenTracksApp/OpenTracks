/*
 * Copyright 2011 Google Inc.
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
package com.google.android.apps.mytracks.services.sensors;

import junit.framework.TestCase;

/**
 * @author Dominik Ršttsches
 */
public class StrideReadingsTest extends TestCase {
  
  public void testNoReadingOnStartup() {
    StrideReadings strideReadings = new StrideReadings();
    assertEquals(StrideReadings.CADENCE_NOT_AVAILABLE, strideReadings.getCadence());
  }
  
  public void testAverageCadenceAvailable() {
    StrideReadings strideReadings = new StrideReadings();
    // 2 steps / second => Cadence is 120 / minute
    for (int i = 1; i <= 30; i++) {
      strideReadings.updateStrideReading(i*2);
      if (i >= StrideReadings.MIN_READINGS_FOR_AVERAGE) {
        assertEquals(120, strideReadings.getCadence());
      } else {
        assertEquals(StrideReadings.CADENCE_NOT_AVAILABLE, strideReadings.getCadence());
      }
    }
  }
  
  /**
   * Tests for correct calculation after rolling over at 128 strides,
   * just like the HxM seems to do it.
   */
  public void testRollover() {
    StrideReadings strideReadings = new StrideReadings();
    // 1 step per second => Cadence is 60 / minute
    // Updating readings counting upwards from initialStrides -
    // initialStrides set to a value below 128 to ensure rollover.
    int initialStrides = 128 - StrideReadings.NUM_READINGS_FOR_AVERAGE - 5;

    for (int i = 1; i <= StrideReadings.NUM_READINGS_FOR_AVERAGE + 10; i++) {
      strideReadings.updateStrideReading((initialStrides + i) % 128);
      if (i >= StrideReadings.MIN_READINGS_FOR_AVERAGE) {
        assertEquals(60, strideReadings.getCadence());
      } else {
        assertEquals(StrideReadings.CADENCE_NOT_AVAILABLE, strideReadings.getCadence());
      }
    }
  }
}
