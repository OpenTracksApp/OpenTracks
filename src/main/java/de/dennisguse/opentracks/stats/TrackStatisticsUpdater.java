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

package de.dennisguse.opentracks.stats;

import androidx.annotation.NonNull;

import java.time.Duration;
import java.util.List;

import de.dennisguse.opentracks.data.models.Distance;
import de.dennisguse.opentracks.data.models.HeartRate;
import de.dennisguse.opentracks.data.models.Power;
import de.dennisguse.opentracks.data.models.Speed;
import de.dennisguse.opentracks.data.models.TrackPoint;
import de.dennisguse.opentracks.settings.PreferencesUtils;

/**
 * Updater for {@link TrackStatistics}.
 * For updating track {@link TrackStatistics} as new {@link TrackPoint}s are added.
 * NOTE: Some of the locations represent pause/resume separator.
 * NOTE: Has still support for segments (at the moment unused).
 *
 * @author Sandor Dornbush
 * @author Rodrigo Damazio
 */
public class TrackStatisticsUpdater {

    private static final String TAG = TrackStatisticsUpdater.class.getSimpleName();

    private final TrackStatistics trackStatistics;

    private float averageHeartRateBPM;
    private Duration totalHeartRateDuration = Duration.ZERO;
    private float averagePowerW;
    private Duration totalPowerDuration = Duration.ZERO;

    // The current segment's statistics
    private final TrackStatistics currentSegment;
    // Current segment's last trackPoint
    private TrackPoint lastTrackPoint;

    public TrackStatisticsUpdater() {
        this(new TrackStatistics());
    }

    public TrackStatisticsUpdater(TrackStatistics trackStatistics) {
        this.trackStatistics = trackStatistics;
        this.currentSegment = new TrackStatistics();

        resetAverageHeartRate();
    }

    public TrackStatisticsUpdater(TrackStatisticsUpdater toCopy) {
        this.currentSegment = new TrackStatistics(toCopy.currentSegment);
        this.trackStatistics = new TrackStatistics(toCopy.trackStatistics);

        this.lastTrackPoint = toCopy.lastTrackPoint;
        resetAverageHeartRate();
    }

    public TrackStatistics getTrackStatistics() {
        // Take a snapshot - we don't want anyone messing with our trackStatistics
        TrackStatistics stats = new TrackStatistics(trackStatistics);
        stats.merge(currentSegment);
        return stats;
    }

    public void addTrackPoints(List<TrackPoint> trackPoints) {
        trackPoints.stream().forEachOrdered(this::addTrackPoint);
    }

    public void addTrackPoint(TrackPoint trackPoint) {
        if (trackPoint.isSegmentManualStart()) {
            reset(trackPoint);
        }

        if (!currentSegment.isInitialized()) {
            currentSegment.setStartTime(trackPoint.getTime());
        }

        // Always update time
        currentSegment.setStopTime(trackPoint.getTime());
        currentSegment.setTotalTime(Duration.between(currentSegment.getStartTime(), trackPoint.getTime()));

        // Process sensor data: barometer
        if (trackPoint.hasAltitudeGain()) {
            currentSegment.addTotalAltitudeGain(trackPoint.getAltitudeGain());
        }

        if (trackPoint.hasAltitudeLoss()) {
            currentSegment.addTotalAltitudeLoss(trackPoint.getAltitudeLoss());
        }

        //Update absolute (GPS-based) altitude
        if (trackPoint.hasAltitude()) {
            currentSegment.updateAltitudeExtremities(trackPoint.getAltitude());
        }

        // Update heart rate
        if (trackPoint.hasHeartRate() && lastTrackPoint != null) {
            Duration trackPointDuration = Duration.between(lastTrackPoint.getTime(), trackPoint.getTime());
            Duration newTotalDuration = totalHeartRateDuration.plus(trackPointDuration);

            averageHeartRateBPM = (totalHeartRateDuration.toMillis() * averageHeartRateBPM + trackPointDuration.toMillis() * trackPoint.getHeartRate().getBPM()) / newTotalDuration.toMillis();
            totalHeartRateDuration = newTotalDuration;

            currentSegment.setAverageHeartRate(HeartRate.of(averageHeartRateBPM));
        }

        // Update power
        if (trackPoint.hasPower() && lastTrackPoint != null) {
            Duration trackPointDuration = Duration.between(lastTrackPoint.getTime(), trackPoint.getTime());
            Duration newTotalDuration = totalPowerDuration.plus(trackPointDuration);

            averagePowerW = (totalPowerDuration.toMillis() * averagePowerW + trackPointDuration.toMillis() * trackPoint.getPower().getW()) / newTotalDuration.toMillis();
            totalPowerDuration = newTotalDuration;

            currentSegment.setAveragePower(Power.of(averagePowerW));
        }

        {
            // Update total distance
            Distance movingDistance = null;
            if (trackPoint.hasSensorDistance()) {
                movingDistance = trackPoint.getSensorDistance();
            } else if (lastTrackPoint != null
                    && lastTrackPoint.hasLocation()
                    && trackPoint.hasLocation()) {
                // GPS-based distance/speed
                movingDistance = trackPoint.distanceToPrevious(lastTrackPoint);
            }
            if (movingDistance != null) {
                currentSegment.addTotalDistance(movingDistance);
            }

            if (!currentSegment.isIdle()) {
                if (!trackPoint.isSegmentManualStart() && lastTrackPoint != null) {
                    currentSegment.addMovingTime(trackPoint, lastTrackPoint);
                }
            }

            if (trackPoint.isIdleTriggered()) {
                currentSegment.setIdle(true);
            } else if (currentSegment.isIdle()) {
                // Shall we switch to non-idle?
                if (movingDistance != null
                        && movingDistance.greaterOrEqualThan(PreferencesUtils.getRecordingDistanceInterval())) {
                    currentSegment.setIdle(false);
                }
            }

            if (trackPoint.hasSpeed()) {
                updateSpeed(trackPoint);
            }
        }

        if (trackPoint.isSegmentManualEnd()) {
            reset(trackPoint);
            return;
        }

        lastTrackPoint = trackPoint;
    }

    private void reset(TrackPoint trackPoint) {
        if (currentSegment.isInitialized()) {
            trackStatistics.merge(currentSegment);
        }
        currentSegment.reset(trackPoint.getTime());

        lastTrackPoint = null;
        resetAverageHeartRate();
    }

    private void resetAverageHeartRate() {
        averageHeartRateBPM = 0.0f;
        totalHeartRateDuration = Duration.ZERO;
    }

    /**
     * Updates a speed reading while assuming the user is moving.
     */
    private void updateSpeed(@NonNull TrackPoint trackPoint) {
        Speed currentSpeed = trackPoint.getSpeed();
        if (currentSpeed.greaterThan(currentSegment.getMaxSpeed())) {
            currentSegment.setMaxSpeed(currentSpeed);
        }
    }

    @NonNull
    @Override
    public String toString() {
        return "TrackStatisticsUpdater{" +
                "trackStatistics=" + trackStatistics +
                '}';
    }
}
