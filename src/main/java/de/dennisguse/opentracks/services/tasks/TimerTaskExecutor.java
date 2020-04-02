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

package de.dennisguse.opentracks.services.tasks;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import de.dennisguse.opentracks.services.TrackRecordingService;
import de.dennisguse.opentracks.stats.TrackStatistics;

/**
 * This class will periodically perform a task.
 *
 * @author Sandor Dornbush
 */
class TimerTaskExecutor {

    private final PeriodicTask periodicTask;
    private final TrackRecordingService trackRecordingService;

    private TimerTask timerTask;
    private Timer timer;

    public TimerTaskExecutor(PeriodicTask periodicTask, TrackRecordingService trackRecordingService) {
        this.periodicTask = periodicTask;
        this.trackRecordingService = trackRecordingService;
    }

    /**
     * Schedules the periodic task in milliseconds.
     *
     * @param interval_ms the interval_ms
     */
    void scheduleTask(long interval_ms) {
        if (interval_ms <= 0) {
            return;
        }

        if (!trackRecordingService.isRecording() || trackRecordingService.isPaused()) {
            return;
        }

        TrackStatistics trackStatistics = trackRecordingService.getTrackStatistics();
        if (trackStatistics == null) {
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
        long next = System.currentTimeMillis() + interval_ms - (trackStatistics.getTotalTime() % interval_ms);
        timer.scheduleAtFixedRate(timerTask, new Date(next), interval_ms);
    }

    void shutdown() {
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
