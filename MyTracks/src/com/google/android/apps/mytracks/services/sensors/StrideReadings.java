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

import java.util.LinkedList;
import java.util.List;

/**
 * A history of Zephyr stride counter reading.
 * These can be used as an alternate method to calculate the correct cadence.
 * This is a work around for an issue with some HxM firmware.
 *
 * @author Dominik Rottsches
 */
public class StrideReadings {

  // visible for testing
  protected static final int NUM_READINGS_FOR_AVERAGE = 10;
  protected static final int MIN_READINGS_FOR_AVERAGE = 5;

  protected static final int CADENCE_NOT_AVAILABLE = -1;

  // TODO: Check whether 1Hz assumption is okay for cadence calculation
  // otherwise add heart beat timestamp to this list and compute
  // cadence from these timestamps.
  private final List<Integer> strideReadingsHistory;

  public StrideReadings() {
    strideReadingsHistory = new LinkedList<Integer>();
  }

  public void updateStrideReading(int numStrides) {
    // HRM/HxM documentation says, transmission frequency is 1 Hz, 
    // let's keep last NUM_READINGS_FOR_AVERAGE readings.
    // TODO: Calibrate this using a reliable footpod / cadence sensor, 
    // otherwise use heartbeat timestamp for calculation. 
    strideReadingsHistory.add(0, numStrides);
    while (strideReadingsHistory.size() > NUM_READINGS_FOR_AVERAGE) {
      strideReadingsHistory.remove(strideReadingsHistory.size()-1);
    }
  }

  public int getCadence() {
    if (strideReadingsHistory.size() < MIN_READINGS_FOR_AVERAGE) {
      // Bail out if we cannot really get a meaningful average yet.
      return CADENCE_NOT_AVAILABLE;
    }
    // Compute assuming 1 stride reading/second.
    int timeSinceOldestReadingSecs = strideReadingsHistory.size() - 1; 
    int stridesThen = strideReadingsHistory.get(strideReadingsHistory.size()-1);
    int stridesNow = strideReadingsHistory.get(0);
    // Contrary to documentation stride value seems to roll over at 128.
    return Math.round( (float)(mod((stridesNow - stridesThen), 128)) /
        timeSinceOldestReadingSecs * 60);
  }

  /**
   * Modulo operation with positive return values, Java's remainder operator doesn't change sign.
   *
   * @return x mod y
   */
  private static int mod(int x, int y)
  {
      int result = x % y;
      return result < 0 ? result + y : result;
  }
}
