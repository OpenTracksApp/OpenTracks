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
import androidx.annotation.VisibleForTesting;

import java.time.Duration;
import java.util.List;

import de.dennisguse.opentracks.data.models.Altitude;
import de.dennisguse.opentracks.data.models.Distance;
import de.dennisguse.opentracks.data.models.HeartRate;
import de.dennisguse.opentracks.data.models.Speed;
import de.dennisguse.opentracks.data.models.TrackPoint;

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

    /**
     * The number of altitude readings to smooth to get a somewhat accurate signal.
     */
    @VisibleForTesting
    private static final int ALTITUDE_SMOOTHING_FACTOR = 25;

    private static final String TAG = TrackStatisticsUpdater.class.getSimpleName();

    private final TrackStatistics trackStatistics;

    private final AltitudeRingBuffer altitudeBuffer;
    private float averageHeartRateBPM;
    private Duration totalHeartRateDuration = Duration.ZERO;

    // The current segment's statistics
    private final TrackStatistics currentSegment;
    // Current segment's last trackPoint
    private TrackPoint lastTrackPoint;

    public TrackStatisticsUpdater() {
        this(new TrackStatistics());
    }

    /**
     * Creates a new{@link TrackStatisticsUpdater} with a {@link TrackStatisticsUpdater} already existed.
     *
     * @param trackStatistics a {@link TrackStatisticsUpdater}
     */
    public TrackStatisticsUpdater(TrackStatistics trackStatistics) {
        this.trackStatistics = trackStatistics;
        this.currentSegment = new TrackStatistics();

        altitudeBuffer = new AltitudeRingBuffer(ALTITUDE_SMOOTHING_FACTOR);
        resetAverageHeartRate();
    }

    public TrackStatisticsUpdater(TrackStatisticsUpdater toCopy) {
        this.currentSegment = new TrackStatistics(toCopy.currentSegment);
        this.trackStatistics = new TrackStatistics(toCopy.trackStatistics);

        this.altitudeBuffer = new AltitudeRingBuffer(toCopy.altitudeBuffer);

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

    /**
     *
     */
    public void addTrackPoint(TrackPoint trackPoint) {
        if (trackPoint.isSegmentStart()) {
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
            // Update altitude using the smoothed average
            altitudeBuffer.setNext(trackPoint.getAltitude());
            Altitude newAverage = altitudeBuffer.getAverage();

            currentSegment.updateAltitudeExtremities(newAverage);
        }

        // Update heart rate
        if (trackPoint.hasHeartRate() && lastTrackPoint != null) {
            Duration trackPointDuration = Duration.between(lastTrackPoint.getTime(), trackPoint.getTime());
            Duration newTotalDuration = totalHeartRateDuration.plus(trackPointDuration);

            averageHeartRateBPM = (totalHeartRateDuration.toMillis() * averageHeartRateBPM + trackPointDuration.toMillis() * trackPoint.getHeartRate().getBPM()) / newTotalDuration.toMillis();
            totalHeartRateDuration = newTotalDuration;

            currentSegment.setAverageHeartRate(HeartRate.of(averageHeartRateBPM));
        }

        // Update total distance
        if (trackPoint.hasSensorDistance()) {
            // Sensor-based distance/speed
            currentSegment.addTotalDistance(trackPoint.getSensorDistance());
        } else if (lastTrackPoint != null
                && lastTrackPoint.hasLocation()
                && trackPoint.hasLocation() && trackPoint.isMoving()) {
            // GPS-based distance/speed
            // Assumption: we ignore TrackPoints that are not moving as those are likely imprecise GPS measurements
            Distance movingDistance = trackPoint.distanceToPrevious(lastTrackPoint);
            currentSegment.addTotalDistance(movingDistance);
        }


        // Update moving time
        if (trackPoint.isMoving() && lastTrackPoint != null && lastTrackPoint.isMoving()) {
            currentSegment.addMovingTime(trackPoint, lastTrackPoint);

            // Update max speed
            updateMaxSpeed(trackPoint, lastTrackPoint);
        }


        if (trackPoint.isSegmentEnd()) {
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
        altitudeBuffer.reset();
        resetAverageHeartRate();
    }

    private void resetAverageHeartRate() {
        averageHeartRateBPM = 0.0f;
        totalHeartRateDuration = Duration.ZERO;
    }

    /**
     * Gets the smoothed altitude over several readings.
     * The altitude readings is noisy so the smoothed altitude is better than the raw altitude for many tasks.
     */
    public Altitude getSmoothedAltitude() {
        return altitudeBuffer.getAverage();
    }

    /**
     * Updates a speed reading while assuming the user is moving.
     */
    @VisibleForTesting
    private void updateMaxSpeed(@NonNull TrackPoint trackPoint, @NonNull TrackPoint lastTrackPoint) {
        Speed speed = Speed.of(lastTrackPoint, trackPoint);
        if (speed.greaterThan(currentSegment.getMaxSpeed())) {
            currentSegment.setMaxSpeed(speed);
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
