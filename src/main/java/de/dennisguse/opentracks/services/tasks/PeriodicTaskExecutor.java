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
package de.dennisguse.opentracks.services.tasks;

import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import java.time.Duration;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.content.data.Distance;
import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.services.TrackRecordingService;
import de.dennisguse.opentracks.stats.TrackStatistics;

/**
 * Execute a periodic task on a time or distance schedule.
 *
 * @author Sandor Dornbush
 */
public class PeriodicTaskExecutor {

    private static final String TAG = PeriodicTaskExecutor.class.getSimpleName();

    private final TrackRecordingService trackRecordingService;
    private final PeriodicTaskFactory periodicTaskFactory;

    private final int TASK_FREQUENCY_OFF;
    /**
     * The task frequency.
     * A positive value is a time frequency (minutes).
     * A negative value is a distance frequency (km or mi).
     * A zero value is to turn off periodic task.
     */
    private int taskFrequency;

    private PeriodicTaskFactory.Task periodicTask;

    private Handler handler;

    private boolean metricUnits;

    private TrackStatistics trackStatistics;
    private Distance nextTaskDistance = Distance.of(Double.MAX_VALUE);

    private final Runnable timer = new Runnable() {
        @Override
        public void run() {
            if (trackRecordingService != null && periodicTask != null) {
                trackRecordingService.run(periodicTask);
                handler.postDelayed(this, getNextDuration());
            }
        }
    };

    public PeriodicTaskExecutor(@NonNull TrackRecordingService trackRecordingService, PeriodicTaskFactory periodicTaskFactory) {
        this.trackRecordingService = trackRecordingService;
        this.periodicTaskFactory = periodicTaskFactory;

        TASK_FREQUENCY_OFF = Integer.parseInt(trackRecordingService.getString(R.string.frequency_off));
        taskFrequency = TASK_FREQUENCY_OFF;
    }

    public void restore() {
        if (!trackRecordingService.isRecording() || trackRecordingService.isPaused()) {
            Log.d(TAG, "Not recording or paused.");
            return;
        }

        if (!isTimeFrequency() && handler != null) {
            handler.removeCallbacks(timer);
            handler = null;
        }
        if (taskFrequency == TASK_FREQUENCY_OFF) {
            Log.d(TAG, "Task frequency is off.");
            return;
        }

        periodicTask = periodicTaskFactory.create(trackRecordingService);
        periodicTask.start();

        if (isTimeFrequency()) {
            if (handler == null) {
                handler = new Handler();
            }
            handler.postDelayed(timer, getNextDuration());
        } else {
            // For distance periodic task
            updateNextTaskDistance();
        }
    }

    public void shutdown() {
        if (periodicTask != null) {
            periodicTask.shutdown();
            periodicTask = null;
        }
        if (handler != null) {
            handler.removeCallbacks(timer);
            handler = null;
        }
    }

    public void update(@NonNull Track.Id trackId, @NonNull TrackStatistics trackStatistics) {
        if (!isDistanceFrequency() || periodicTask == null) {
            return;
        }

        if (trackStatistics.getTotalDistance().greaterThan(nextTaskDistance)) {
            periodicTask.run(trackId, trackStatistics);
            this.trackStatistics = trackStatistics;
            updateNextTaskDistance();
        }
    }

    public void setTaskFrequency(int taskFrequency) {
        this.taskFrequency = taskFrequency;
        restore();
    }

    public void setMetricUnits(boolean metricUnits) {
        this.metricUnits = metricUnits;
        updateNextTaskDistance();
    }

    private void updateNextTaskDistance() {
        if (!trackRecordingService.isRecording() || trackRecordingService.isPaused() || periodicTask == null) {
            return;
        }

        if (!isDistanceFrequency()) {
            nextTaskDistance = Distance.of(Double.MAX_VALUE);
            Log.d(TAG, "SplitManager: Distance splits disabled.");
            return;
        }

        if (trackStatistics == null) {
            return;
        }

        nextTaskDistance = calculateNextTaskDistance(trackStatistics);
    }

    @VisibleForTesting
    public Distance calculateNextTaskDistance(TrackStatistics trackStatistics) {
        Distance distance = trackStatistics.getTotalDistance();

        Distance announcementInterval = Distance.one(metricUnits).multipliedBy(Math.abs(taskFrequency));
        int index = (int) (distance.dividedBy(announcementInterval));
        return announcementInterval.multipliedBy(index + 1);
    }

    private boolean isTimeFrequency() {
        return taskFrequency > 0;
    }

    private boolean isDistanceFrequency() {
        return taskFrequency < 0;
    }

    private long getNextDuration() {
        if (!isTimeFrequency()) {
            throw new RuntimeException("Using distance frequency as time frequency is impossible.");
        }
        Duration interval = Duration.ofMinutes(taskFrequency);
        return interval.toMillis() - (trackRecordingService.getTotalTime().toMillis() % interval.toMillis());
    }
}
