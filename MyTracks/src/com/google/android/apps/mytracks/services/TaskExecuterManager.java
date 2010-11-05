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

import android.util.Log;

import com.google.android.apps.mytracks.MyTracksConstants;

/**
 * This class will manage a period task executer.
 *
 * @author Sandor Dornbush
 */
public class TaskExecuterManager {

  int frequency;
  PeriodicTask task;
  PeriodicTaskExecuter executer;

  public TaskExecuterManager(int frequency,
                             PeriodicTask task,
                             TrackRecordingService service) {
    this.task = task;
    setFrequency(frequency, service);
  }

  public int getFrequency() {
    return frequency;
  }

  /**
   * Sets the frequency that the task should be run at.
   * If needed the task will be scheduled.
   *
   * @param frequency The frequency in minutes for the task to run
   * @param service The service to run the task on
   */
  public void setFrequency(int frequency, TrackRecordingService service) {
    this.frequency = frequency;
    Log.i(MyTracksConstants.TAG, "Frequency set to: " + frequency);

    if (frequency == -1) {
      if (executer != null) {
        executer.shutdown();
        executer = null;
        Log.i(MyTracksConstants.TAG,
            "Shut down service: " + task.getClass().getSimpleName());
      }
    } else {
      if (executer == null) {
        task.start();
        executer = new PeriodicTaskExecuter(task, service);
      }
      executer.scheduleTask(frequency * 60000);
    }
  }

  /**
   * Restore the task at the current frequency.
   */
  public void restore() {
    if (frequency > 0) {
      task.start();
      executer.scheduleTask(frequency * 60000);
    }
  }
}
