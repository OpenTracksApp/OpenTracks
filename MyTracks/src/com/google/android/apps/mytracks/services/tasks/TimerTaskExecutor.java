/*
 * Copyright 2009 Google Inc.
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

import com.google.android.apps.mytracks.services.TrackRecordingService;
import com.google.android.apps.mytracks.stats.TripStatistics;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

/**
 * This class will periodically perform a task.
 * 
 * @author Sandor Dornbush
 */
public class TimerTaskExecutor {

  private final PeriodicTask periodicTask;
  private final TrackRecordingService trackRecordingService;

  private TimerTask timerTask;
  private Timer timer;

  public TimerTaskExecutor(PeriodicTask periodicTask, TrackRecordingService trackRecordingService) {
    this.periodicTask = periodicTask;
    this.trackRecordingService = trackRecordingService;
  }

  /**
   * Schedules the periodic task at an interval.
   * 
   * @param interval the interval in milliseconds
   */
  public void scheduleTask(long interval) {
    if (interval <= 0) {
      return;
    }

    if (!trackRecordingService.isRecording() || trackRecordingService.isPaused()) {
      return;
    }

    TripStatistics tripStatistics = trackRecordingService.getTripStatistics();
    if (tripStatistics == null) {
      return;
    }

    shutdown();
    periodicTask.start();
    timerTask = new TimerTask() {
        @Override
      public void run() {
        periodicTask.run(trackRecordingService);
      }
    };
    timer = new Timer(TimerTaskExecutor.class.getSimpleName());
    long next = System.currentTimeMillis() + interval - (tripStatistics.getTotalTime() % interval);
    timer.scheduleAtFixedRate(timerTask, new Date(next), interval);
  }

  /**
   * Shuts down.
   */
  public void shutdown() {
    if (timerTask != null) {
      timerTask.cancel();
      timerTask = null;
    }
    if (timer != null) {
      timer.cancel();
      timer.purge();
      timer = null;
    }
    periodicTask.shutdown();
  }
}
