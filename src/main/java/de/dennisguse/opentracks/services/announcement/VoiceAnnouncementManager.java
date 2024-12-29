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
package de.dennisguse.opentracks.services.announcement;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.Spannable;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.mediarouter.media.MediaRouter;

import java.time.Duration;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.data.ContentProviderUtils;
import de.dennisguse.opentracks.data.TrackPointIterator;
import de.dennisguse.opentracks.data.models.Distance;
import de.dennisguse.opentracks.data.models.Track;
import de.dennisguse.opentracks.data.models.TrackPoint;
import de.dennisguse.opentracks.sensors.sensorData.SensorDataSet;
import de.dennisguse.opentracks.settings.PreferencesUtils;
import de.dennisguse.opentracks.stats.SensorStatistics;
import de.dennisguse.opentracks.stats.TrackStatistics;
import de.dennisguse.opentracks.ui.intervals.IntervalStatistics;

/**
 * Execute a periodic task on a time or distance schedule.
 *
 * @author Sandor Dornbush
 */
public class VoiceAnnouncementManager implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = VoiceAnnouncementManager.class.getSimpleName();

    private final Context context;

    private TTSManager voiceAnnouncement;

    private TrackStatistics trackStatistics;

    private static final Distance DISTANCE_OFF = Distance.of(Double.MAX_VALUE);
    private Distance distanceFrequency = DISTANCE_OFF;
    @NonNull
    private Distance nextTotalDistance = DISTANCE_OFF;

    private static final Duration TOTALTIME_OFF = Duration.ofMillis(Long.MAX_VALUE);
    private Duration totalTimeFrequency = TOTALTIME_OFF;
    @NonNull
    private Duration nextTotalTime = TOTALTIME_OFF;


    private final ContentProviderUtils contentProviderUtils;
    private TrackPoint.Id startTrackPointId = null;

    private IntervalStatistics intervalStatistics;
    private Distance intervalDistance;

    public VoiceAnnouncementManager(@NonNull Context context) {
        this.context = context;
        contentProviderUtils = new ContentProviderUtils(context);
        intervalDistance = PreferencesUtils.getVoiceAnnouncementDistance();
        intervalStatistics = new IntervalStatistics(intervalDistance);
    }

    public void start(@Nullable TrackStatistics trackStatistics) {
        voiceAnnouncement = new TTSManager(context);
        voiceAnnouncement.start();
        update(trackStatistics);
    }

    void update(@Nullable TrackStatistics trackStatistics) {
        this.trackStatistics = trackStatistics;
        updateNextDuration();
        updateNextTaskDistance();
    }

    private boolean shouldNotAnnounce() {
        if (voiceAnnouncement == null) {
            Log.e(TAG, "Cannot update when in status shutdown.");
            return true;
        }

        if (!PreferencesUtils.shouldVoiceAnnouncementOnDeviceSpeaker()
                && MediaRouter.getInstance(context)
                .getSelectedRoute()
                .isDeviceSpeaker()) {
            Log.i(TAG, "No voice announcement on device speaker.");
            return true;
        }

        return false;
    }

    public void announceIdle() {
        if (shouldNotAnnounce()) {
            return;
        }

        if (!PreferencesUtils.shouldVoiceAnnouncementIdle()) {
            return;
        }

        voiceAnnouncement.speak(VoiceAnnouncementUtils.createIdle(context));
    }

    public void announceStatisticsIfNeeded(@NonNull Track track, @NonNull SensorDataSet sensorDataSet) {
        if (shouldNotAnnounce()) {
            return;
        }

        boolean announce = false;
        this.trackStatistics = track.getTrackStatistics();
        if (trackStatistics.getTotalDistance().greaterThan(nextTotalDistance)) {
            updateNextTaskDistance();
            announce = true;
        }
        if (!trackStatistics.getTotalTime().minus(nextTotalTime).isNegative()) {
            updateNextDuration();
            announce = true;
        }

        if (announce) {
            voiceAnnouncement.speak(createAnnouncement(track, sensorDataSet));
        }
    }

    private Spannable createAnnouncement(Track track, SensorDataSet sensorDataSet) {
        Distance currentIntervalDistance = PreferencesUtils.getVoiceAnnouncementDistance();
        if (currentIntervalDistance != intervalDistance) {
            intervalStatistics = new IntervalStatistics(currentIntervalDistance);
            intervalDistance = currentIntervalDistance;
            startTrackPointId = null;
        }

        TrackPointIterator trackPointIterator = new TrackPointIterator(contentProviderUtils, track.getId(), startTrackPointId);
        startTrackPointId = intervalStatistics.addTrackPoints(trackPointIterator);
        IntervalStatistics.Interval lastInterval = intervalStatistics.getLastInterval();
        SensorStatistics sensorStatistics = null;
        if (track.getId() != null) {
            sensorStatistics = contentProviderUtils.getSensorStats(track.getId());
        }

        return VoiceAnnouncementUtils.createStatistics(context, track, sensorDataSet, PreferencesUtils.getUnitSystem(), PreferencesUtils.isReportSpeed(track), lastInterval, sensorStatistics);
    }

    public void stop() {
        if (voiceAnnouncement != null) {
            voiceAnnouncement.stop();
            voiceAnnouncement = null;
        }
    }

    public void setFrequency(Duration frequency) {
        this.totalTimeFrequency = frequency;
        update(this.trackStatistics);
    }

    public void setFrequency(Distance frequency) {
        this.distanceFrequency = frequency;
        update(this.trackStatistics);
    }

    public void updateNextTaskDistance() {
        if (trackStatistics == null || distanceFrequency.isZero()) {
            nextTotalDistance = DISTANCE_OFF;
        } else {

            Distance distance = trackStatistics.getTotalDistance();

            int index = (int) (distance.dividedBy(distanceFrequency));
            nextTotalDistance = distanceFrequency.multipliedBy(index + 1);
        }

    }

    private void updateNextDuration() {
        if (trackStatistics == null || totalTimeFrequency.isZero()) {
            nextTotalTime = TOTALTIME_OFF;
        } else {

            Duration totalTime = trackStatistics.getTotalTime();
            Duration intervalMod = Duration.ofMillis(trackStatistics.getTotalTime().toMillis() % totalTimeFrequency.toMillis());

            nextTotalTime = totalTime.plus(totalTimeFrequency.minus(intervalMod));
        }
    }

    @VisibleForTesting
    @NonNull
    public Duration getNextTotalTime() {
        return nextTotalTime;
    }

    @VisibleForTesting
    @NonNull
    public Distance getNextTotalDistance() {
        return nextTotalDistance;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (PreferencesUtils.isKey(R.string.voice_announcement_frequency_key, key)) {
            setFrequency(PreferencesUtils.getVoiceAnnouncementFrequency());
        }

        if (PreferencesUtils.isKey(new int[]{R.string.voice_announcement_distance_key, R.string.stats_units_key}, key)) {
            setFrequency(PreferencesUtils.getVoiceAnnouncementDistance());
        }
    }
}
