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
package com.google.android.apps.mytracks.services.tasks;

import static com.google.android.apps.mytracks.Constants.TAG;

import com.google.android.apps.mytracks.services.TrackRecordingService;
import com.google.android.apps.mytracks.util.UnitConversions;

import android.util.Log;

/**
 * Execute a task on a time or distance schedule.
 *
 * @author Sandor Dornbush
 */
public class PeriodicTaskExecutor {

  /**
   * The frequency of the task.
   * A value greater than zero is a frequency in time.
   * A value less than zero is considered a frequency in distance.
   */
  private int taskFrequency = 0;

  /**
   * The next distance when the task should execute.
   */
  private double nextTaskDistance = 0;

  /**
   * Time based executor.
   */
  private TimerTaskExecutor timerExecutor = null;

  private boolean metricUnits;

  private final TrackRecordingService service;

  private final PeriodicTaskFactory factory;

  private PeriodicTask task;

  public PeriodicTaskExecutor(TrackRecordingService service, PeriodicTaskFactory factory) {
    this.service = service;
    this.factory = factory;
  }

  /**
   * Restores the manager.
   */
  public void restore() {
    // TODO: Decouple service from this class once and forever.
    if (!service.isRecording()) {
      return;
    }

    if (!isTimeFrequency()) {
      if (timerExecutor != null) {
        timerExecutor.shutdown();
        timerExecutor = null;
      }
    }
    if (taskFrequency == 0) {
      return;
    }

    // Try to make the task.
    task = factory.create(service);
    // Returning null is ok.
    if (task == null) {
      return;
    }
    task.start();

    if (isTimeFrequency()) {
      if (timerExecutor == null) {
        timerExecutor = new TimerTaskExecutor(task, service);
      }
      timerExecutor.scheduleTask(taskFrequency * 60000L);
    } else {
      // For distance based splits.
      calculateNextTaskDistance();
    }
  }

  /**
   * Shuts down the manager.
   */
  public void shutdown() {
    if (task != null) {
      task.shutdown();
      task = null;
    }
    if (timerExecutor != null) {
      timerExecutor.shutdown();
      timerExecutor = null;
    }
  }

  /**
   * Calculates the next distance when the task should execute.
   */
  void calculateNextTaskDistance() {
    // TODO: Decouple service from this class once and forever.
    if (!service.isRecording() || task == null) {
      return;
    }

    if (!isDistanceFrequency()) {
      nextTaskDistance = Double.MAX_VALUE;
      Log.d(TAG, "SplitManager: Distance splits disabled.");
      return;
    }

    double distance = service.getTripStatistics().getTotalDistance() / 1000;
    if (!metricUnits) {
      distance *= UnitConversions.KM_TO_MI;
    }
    // The index will be negative since the frequency is negative.
    int index = (int) (distance / taskFrequency);
    index -= 1;
    nextTaskDistance = taskFrequency * index;
    Log.d(TAG, "SplitManager: Next split distance: " + nextTaskDistance);
  }

  /**
   * Updates executer with new trip statistics.
   */
  public void update() {
    if (!isDistanceFrequency() || task == null) {
      return;
    }
    // Convert the distance in meters to km or mi.
    double distance = service.getTripStatistics().getTotalDistance() / 1000.0;
    if (!metricUnits) {
      distance *= UnitConversions.KM_TO_MI;
    }

    if (distance > nextTaskDistance) {
      task.run(service);
      calculateNextTaskDistance();
    }
  }

  private boolean isTimeFrequency() {
    return taskFrequency > 0;
  }

  private boolean isDistanceFrequency() {
    return taskFrequency < 0;
  }

  /**
   * Sets the task frequency.
   * &lt; 0 Use the absolute value as a distance in the current measurement km
   *  or mi
   *   0 Turn off the task
   * &gt; 0 Use the value as a time in minutes
   * @param taskFrequency The frequency in time or distance
   */
  public void setTaskFrequency(int taskFrequency) {
    Log.d(TAG, "setTaskFrequency: taskFrequency = " + taskFrequency);
    this.taskFrequency = taskFrequency;
    restore();
  }

  public void setMetricUnits(boolean metricUnits) {
    this.metricUnits = metricUnits;
    calculateNextTaskDistance();
  }

  double getNextTaskDistance() {
    return nextTaskDistance;
  }
}
