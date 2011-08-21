/*
 * Copyright 2010 Google Inc.
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
 * Storage of a history of readings of the Zephyr stride counter,
 * in order to derive a correct cadence value from it,
 * working around an issue with the HxM's firmware. 
 *
 * @author Dominik Ršttsches
 */
public class StrideReadings {
  private static class StrideReading {
    // TODO: Check whether 1Hz assumption is okay for cadence calculation
    // otherwise use timeMs, which is taken from heart beat timestamp.
    @SuppressWarnings("unused")
    public int timeMs;
    public int numStrides;

    StrideReading(int newTimeMs, int newNumStrides) {
      timeMs = newTimeMs;
      numStrides = newNumStrides;
    }
  }

  private static final int NUM_READINGS_FOR_AVERAGE = 10;
  private static final int MIN_READINGS_FOR_AVERAGE = 5;

  protected static final int CADENCE_NOT_AVAILABLE = -1;

  private List<StrideReading> strideReadingsHistory;

  public StrideReadings() {
    strideReadingsHistory = new LinkedList<StrideReading>();
  }

  public void updateStrideReading(int timeInMs, int numStrides) {
    // HRM/HxM documentation says, transmission frequency is 1 Hz, 
    // let's keep last NUM_READINGS_FOR_AVERAGE readings.
    // TODO: Calibrate this using a reliable footpod / cadence sensor, 
    // otherwise use heartbeat timestamp for calculation. 
    strideReadingsHistory.add(0, new StrideReading(timeInMs, numStrides));
    while(strideReadingsHistory.size() > NUM_READINGS_FOR_AVERAGE) {
      strideReadingsHistory.remove(strideReadingsHistory.size()-1);
    }
  }

  public int getCadence() {
    if(strideReadingsHistory.size() < MIN_READINGS_FOR_AVERAGE) {
      // Bail out if we cannot really get a meaningful average yet.
      return CADENCE_NOT_AVAILABLE;
    }
    // compute assuming 1 stride reading/second
    int timeSinceOldestReadingSecs = strideReadingsHistory.size() - 1; 
    int stridesThen = strideReadingsHistory
      .get(strideReadingsHistory.size()-1).numStrides;
    int stridesNow = strideReadingsHistory
      .get(0).numStrides;
    // Contrary to documentation stride value seems to roll over every 127 strides.
    return Math.round( (float)((stridesNow - stridesThen) % 127) / 
        timeSinceOldestReadingSecs * 60);
  }
}
