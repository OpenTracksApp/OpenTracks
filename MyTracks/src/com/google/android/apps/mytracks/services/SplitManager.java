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
package com.google.android.apps.mytracks.services;

import com.google.android.apps.mytracks.MyTracksConstants;
import com.google.android.apps.mytracks.util.UnitConversions;

import android.util.Log;

/**
 * This class manages inserting time or distance splits.
 *
 * @author Sandor Dornbush
 */
public class SplitManager {

  /**
   * The frequency of splits.
   */
  private int splitFrequency = 0;

  /**
   * The next distance when a split should be taken.
   */
  private double nextSplitDistance = 0;

  /**
   * Splits executer.
   */
  private PeriodicTaskExecuter splitExecuter = null;

  private boolean metricUnits;

  private final TrackRecordingService service;

  public SplitManager(TrackRecordingService service) {
    this.service = service;
  }

  /**
   * Restores the manager.
   */
  public void restore() {
    if ((splitFrequency > 0) && (splitExecuter != null)) {
      splitExecuter.scheduleTask(splitFrequency * 60000);
    }
    calculateNextSplit();
  }

  /**
   * Shuts down the manager.
   */
  public void shutdown() {
    if (splitExecuter != null) {
      splitExecuter.shutdown();
    }
  }

  /**
   * Calculates the next distance that a split should be inserted at.
   */
  public void calculateNextSplit() {
    // TODO: Decouple service from this class once and forever.
    if (!service.isRecording()) {
      return;
    }
    
    if (splitFrequency >= 0) {
      nextSplitDistance = Double.MAX_VALUE;
      Log.d(MyTracksConstants.TAG,
            "SplitManager: Distance splits disabled.");
      return;
    }

    double dist = service.getTripStatistics().getTotalDistance() / 1000;
    if (!metricUnits) {
      dist *= UnitConversions.KM_TO_MI;
    }
    // The index will be negative since the frequency is negative.
    int index = (int) (dist / splitFrequency);
    index -= 1;
    nextSplitDistance = splitFrequency * index;
    Log.d(MyTracksConstants.TAG,
          "SplitManager: Next split distance: " + nextSplitDistance);
  }

  /**
   * Updates split manager with new trip statistics.
   */
  public void updateSplits() {
    if (this.splitFrequency >= 0) {
      return;
    }
    // Convert the distance in meters to km or mi.
    double distance = service.getTripStatistics().getTotalDistance() / 1000.0;
    if (!metricUnits) {
      distance *= UnitConversions.KM_TO_MI;
    }

    if (distance > this.nextSplitDistance) {
      service.insertStatisticsMarker(service.getLastLocation());
      calculateNextSplit();
    }
  }

  /**
   * Sets the split frequency.
   * &lt; 0 Use the absolute value as a distance in the current measurement km
   *  or mi
   *   0 Turn off splits
   * &gt; 0 Use the value as a time in minutes
   * @param splitFrequency The frequency in time or distance
   */
  public void setSplitFrequency(int splitFrequency) {
    Log.d(MyTracksConstants.TAG,
        "setSplitFrequency: splitFrequency = " + splitFrequency);
    this.splitFrequency = splitFrequency;
    
    // TODO: Decouple service from this class once and forever.
    if (!service.isRecording()) {
      return;
    }
    
    if (splitFrequency < 1) {
      if (splitExecuter != null) {
        splitExecuter.shutdown();
        splitExecuter = null;
      }
    }
    if (splitFrequency > 0) {
      if (splitExecuter == null) {
        TimeSplitTask splitter = new TimeSplitTask();
        splitter.start();
        splitExecuter = new PeriodicTaskExecuter(splitter, service);
      }
      splitExecuter.scheduleTask(splitFrequency * 60000);
    } else {
      // For distance based splits.
      calculateNextSplit();
    }
  }

  public void setMetricUnits(boolean metricUnits) {
    this.metricUnits = metricUnits;
    calculateNextSplit();
  }
}
