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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
public class VoiceAnnouncementManager {

    private static final String TAG = VoiceAnnouncementManager.class.getSimpleName();

    private final TrackRecordingService trackRecordingService;

    private final int TASK_FREQUENCY_OFF;
    /**
     * The task frequency.
     * A positive value is a time frequency (minutes).
     * A negative value is a distance frequency (km or mi).
     * A zero value is to turn off periodic task.
     */
    private int taskFrequency;

    private VoiceAnnouncement voiceAnnouncement;

    private boolean metricUnits;

    private TrackStatistics trackStatistics;

    private static final Distance DISTANCE_OFF = Distance.of(Double.MAX_VALUE);
    private Distance nextTotalDistance = DISTANCE_OFF;

    private static final Duration TOTALTIME_OFF = Duration.ofMillis(Long.MAX_VALUE);
    private Duration nextTotalTime = TOTALTIME_OFF;

    public VoiceAnnouncementManager(@NonNull TrackRecordingService trackRecordingService) {
        this.trackRecordingService = trackRecordingService;

        TASK_FREQUENCY_OFF = Integer.parseInt(trackRecordingService.getString(R.string.frequency_off));
        taskFrequency = TASK_FREQUENCY_OFF;
    }

    public void restore(@Nullable TrackStatistics trackStatistics) {
        if (taskFrequency == TASK_FREQUENCY_OFF) {
            Log.d(TAG, "Task frequency is off.");
            return;
        }

        voiceAnnouncement = new VoiceAnnouncement(trackRecordingService);
        voiceAnnouncement.start();

        if (isTimeFrequency()) {
            nextTotalTime = trackStatistics != null ? calculateNextDuration(trackStatistics) : TOTALTIME_OFF;
        } else {
            // For distance periodic task
            updateNextTaskDistance(trackStatistics);
        }
    }

    public void update(@NonNull Track track) {
        if (voiceAnnouncement == null) {
            Log.e(TAG, "Cannot update when in status shutdown.");
            return;
        }
        boolean announce = false;
        this.trackStatistics = track.getTrackStatistics();
        if (trackStatistics.getTotalDistance().greaterThan(nextTotalDistance)) {
            updateNextTaskDistance(trackStatistics);
            announce = true;
        }
        if (!trackStatistics.getTotalTime().minus(nextTotalTime).isNegative()) {
            nextTotalTime = calculateNextDuration(trackStatistics);
            announce = true;
        }

        if (announce) {
            voiceAnnouncement.announce(track);
        }
    }

    public void shutdown() {
        if (voiceAnnouncement != null) {
            voiceAnnouncement.shutdown();
            voiceAnnouncement = null;
        }
    }

    public void setTaskFrequency(int taskFrequency) {
        this.taskFrequency = taskFrequency;
        restore(this.trackStatistics);
    }

    public void setMetricUnits(boolean metricUnits) {
        this.metricUnits = metricUnits;
        updateNextTaskDistance(this.trackStatistics);
    }

    private void updateNextTaskDistance(TrackStatistics trackStatistics) {
        if (!isDistanceFrequency()) {
            nextTotalDistance = DISTANCE_OFF;
            Log.d(TAG, "SplitManager: Distance splits disabled.");
            return;
        }

        if (trackStatistics == null) {
            return;
        }

        nextTotalDistance = calculateNextTaskDistance(trackStatistics);
    }

    @VisibleForTesting
    public Distance calculateNextTaskDistance(@NonNull TrackStatistics trackStatistics) {
        Distance distance = trackStatistics.getTotalDistance();

        Distance announcementInterval = Distance.one(metricUnits).multipliedBy(Math.abs(taskFrequency));
        int index = (int) (distance.dividedBy(announcementInterval));
        return announcementInterval.multipliedBy(index + 1);
    }

    private Duration calculateNextDuration(@Nullable TrackStatistics trackStatistics) {
        if (trackStatistics == null) {
            return null;
        }

        if (!isTimeFrequency()) {
            throw new RuntimeException("Using distance frequency as time frequency is impossible.");
        }
        Duration totalTime = trackStatistics.getTotalTime();
        Duration interval = Duration.ofMinutes(taskFrequency);
        Duration intervalMod = Duration.ofMillis(trackStatistics.getTotalTime().toMillis() % interval.toMillis());

        return totalTime.plus(interval.minus(intervalMod));
    }

    private boolean isTimeFrequency() {
        return taskFrequency > 0;
    }

    private boolean isDistanceFrequency() {
        return taskFrequency < 0;
    }
}
