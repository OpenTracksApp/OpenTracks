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

import android.util.Log;

import java.time.Duration;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.services.TrackRecordingService;
import de.dennisguse.opentracks.stats.TrackStatistics;
import de.dennisguse.opentracks.util.UnitConversions;

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

    private PeriodicTask periodicTask;

    // Time periodic task executor
    private TimerTaskExecutor timerTaskExecutor = null;

    private boolean metricUnits;

    // The next distance for the distance periodic task
    private double nextTaskDistance = Double.MAX_VALUE;

    public PeriodicTaskExecutor(TrackRecordingService trackRecordingService, PeriodicTaskFactory periodicTaskFactory) {
        this.trackRecordingService = trackRecordingService;
        this.periodicTaskFactory = periodicTaskFactory;

        TASK_FREQUENCY_OFF = Integer.parseInt(trackRecordingService.getBaseContext().getResources().getString(R.string.frequency_off));
        taskFrequency = TASK_FREQUENCY_OFF;
    }

    /**
     * Restores the executor.
     */
    public void restore() {
        if (!trackRecordingService.isRecording() || trackRecordingService.isPaused()) {
            Log.d(TAG, "Not recording or paused.");
            return;
        }

        if (!isTimeFrequency() && timerTaskExecutor != null) {
            timerTaskExecutor.shutdown();
            timerTaskExecutor = null;
        }
        if (taskFrequency == TASK_FREQUENCY_OFF) {
            Log.d(TAG, "Task frequency is off.");
            return;
        }

        periodicTask = periodicTaskFactory.create(trackRecordingService);

        // Returning null is ok
        if (periodicTask == null) {
            Log.d(TAG, "Periodic task is null.");
            return;
        }
        periodicTask.start();

        if (isTimeFrequency()) {
            if (timerTaskExecutor == null) {
                timerTaskExecutor = new TimerTaskExecutor(periodicTask, trackRecordingService);
            }
            timerTaskExecutor.scheduleTask(Duration.ofSeconds(taskFrequency));
        } else {
            // For distance periodic task
            calculateNextTaskDistance();
        }
    }

    /**
     * Shuts down the executor.
     */
    public void shutdown() {
        if (periodicTask != null) {
            periodicTask.shutdown();
            periodicTask = null;
        }
        if (timerTaskExecutor != null) {
            timerTaskExecutor.shutdown();
            timerTaskExecutor = null;
        }
    }

    /**
     * Updates the executor.
     */
    public void update() {
        if (!isDistanceFrequency() || periodicTask == null) {
            return;
        }

        TrackStatistics trackStatistics = trackRecordingService.getTrackStatistics();
        if (trackStatistics == null) {
            return;
        }

        double distance = trackStatistics.getTotalDistance() * UnitConversions.M_TO_KM;
        if (!metricUnits) {
            distance *= UnitConversions.KM_TO_MI;
        }

        if (distance > nextTaskDistance) {
            periodicTask.run(trackRecordingService);
            calculateNextTaskDistance();
        }
    }

    /**
     * Sets task frequency.
     *
     * @param taskFrequency the task frequency
     */
    public void setTaskFrequency(int taskFrequency) {
        this.taskFrequency = taskFrequency;
        restore();
    }

    /**
     * Sets metricUnits.
     *
     * @param metricUnits true to use metric units
     */
    public void setMetricUnits(boolean metricUnits) {
        this.metricUnits = metricUnits;
        calculateNextTaskDistance();
    }

    /**
     * Calculates the next distance for the distance periodic task.
     */
    private void calculateNextTaskDistance() {
        if (!trackRecordingService.isRecording() || trackRecordingService.isPaused() || periodicTask == null) {
            return;
        }

        TrackStatistics trackStatistics = trackRecordingService.getTrackStatistics();
        if (trackStatistics == null) {
            return;
        }

        if (!isDistanceFrequency()) {
            nextTaskDistance = Double.MAX_VALUE;
            Log.d(TAG, "SplitManager: Distance splits disabled.");
            return;
        }

        double distance = trackStatistics.getTotalDistance() * UnitConversions.M_TO_KM;
        if (!metricUnits) {
            distance *= UnitConversions.KM_TO_MI;
        }

        // The index will be negative since the frequency is negative.
        int index = (int) (distance / taskFrequency);
        index -= 1;
        nextTaskDistance = taskFrequency * index;
    }

    /**
     * True if time frequency.
     */
    private boolean isTimeFrequency() {
        return taskFrequency > 0;
    }

    /**
     * True if distance frequency.
     */
    private boolean isDistanceFrequency() {
        return taskFrequency < 0;
    }
}
