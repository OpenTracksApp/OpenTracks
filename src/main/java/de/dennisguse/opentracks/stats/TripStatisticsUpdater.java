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

import android.location.Location;
import android.util.Log;

import androidx.annotation.VisibleForTesting;

import de.dennisguse.opentracks.content.data.TrackPoint;
import de.dennisguse.opentracks.content.data.TrackPointsColumns;
import de.dennisguse.opentracks.content.provider.TrackPointIterator;
import de.dennisguse.opentracks.util.LocationUtils;

import static de.dennisguse.opentracks.services.TrackRecordingService.MAX_NO_MOVEMENT_SPEED;

/**
 * Updater for {@link TripStatistics}.
 * For updating track trip statistics as new locations are added.
 * NOTE:Some of the locations represent pause/resume separator.
 *
 * @author Sandor Dornbush
 * @author Rodrigo Damazio
 */
public class TripStatisticsUpdater {

    /**
     * The number of grade readings to smooth to get a somewhat accurate signal.
     */
    public static final int GRADE_SMOOTHING_FACTOR = 5;
    /**
     * The number of elevation readings to smooth to get a somewhat accurate
     * signal.
     */
    @VisibleForTesting
    private static final int ELEVATION_SMOOTHING_FACTOR = 25;

    /**
     * The number of run readings to smooth for calculating grade.
     */
    @VisibleForTesting
    private static final int RUN_SMOOTHING_FACTOR = 25;
    /**
     * The number of speed reading to smooth to get a somewhat accurate signal.
     */
    @VisibleForTesting
    private static final int SPEED_SMOOTHING_FACTOR = 25;

    private static final String TAG = TripStatisticsUpdater.class.getSimpleName();
    /**
     * Ignore any acceleration faster than this.
     * Will ignore any speeds that imply acceleration greater than 2g's
     * 2g = 19.6 m/s^2 = 0.0002 m/ms^2 = 0.02 m/(m*ms)
     */
    private static final double MAX_ACCELERATION = 0.02;

    // The track's trip statistics
    private final TripStatistics tripStatistics;

    // A buffer of the recent elevation readings (m)
    private final DoubleBuffer elevationBuffer = new DoubleBuffer(ELEVATION_SMOOTHING_FACTOR);
    // A buffer of the recent run readings (m) for calculating grade
    private final DoubleBuffer runBuffer = new DoubleBuffer(RUN_SMOOTHING_FACTOR);
    // A buffer of the recent grade calculations (%)
    private final DoubleBuffer gradeBuffer = new DoubleBuffer(GRADE_SMOOTHING_FACTOR);
    // A buffer of the recent speed readings (m/s) for calculating max speed
    private final DoubleBuffer speedBuffer = new DoubleBuffer(SPEED_SMOOTHING_FACTOR);

    // The current segment's trip statistics
    private TripStatistics currentSegment;
    // Current segment's last location.
    private Location lastLocation;
    // Current segment's last moving location
    private Location lastMovingLocation;

    /**
     * Creates a new trip statistics updater.
     *
     * @param startTime the start time
     */
    public TripStatisticsUpdater(long startTime) {
        tripStatistics = init(startTime);
        currentSegment = init(startTime);
    }

    /**
     * Creates a new trip statistics updater with a trip statistics already existed.
     *
     * @param tripStatistics a trip statistics.
     */
    public TripStatisticsUpdater(TripStatistics tripStatistics) {
        this.tripStatistics = tripStatistics;
        currentSegment = init(System.currentTimeMillis());
    }

    public void updateTime(long time) {
        currentSegment.setStopTime(time);
        currentSegment.setTotalTime(time - currentSegment.getStartTime());
    }

    /**
     * Gets the track's trip statistics.
     */
    public TripStatistics getTripStatistics() {
        // Take a snapshot - we don't want anyone messing with our tripStatistics
        TripStatistics stats = new TripStatistics(tripStatistics);
        stats.merge(currentSegment);
        return stats;
    }

    /**
     * Adds a trackPoint.
     * TODO: This assume trackPoint has a valid time.
     *
     * @param trackPoint             the trackPoint
     * @param minRecordingDistance the min recording distance
     */
    public void addTrackPoint(TrackPoint trackPoint, int minRecordingDistance) {
        // Always update time
        updateTime(trackPoint.getTime());
        if (!LocationUtils.isValidLocation(trackPoint)) {
            // Either pause or resume marker
            if (trackPoint.getLatitude() == TrackPointsColumns.PAUSE_LATITUDE) {
                if (lastLocation != null && lastMovingLocation != null && lastLocation != lastMovingLocation) {
                    currentSegment.addTotalDistance(lastMovingLocation.distanceTo(lastLocation));
                }
                tripStatistics.merge(currentSegment);
            }
            currentSegment = init(trackPoint.getTime());
            lastLocation = null;
            lastMovingLocation = null;
            elevationBuffer.reset();
            runBuffer.reset();
            gradeBuffer.reset();
            speedBuffer.reset();
            return;
        }

        //TODO Use Barometer to compute elevation gain.
        double elevationDifference = trackPoint.hasAltitude() ? updateElevation(trackPoint.getAltitude()) : 0.0;

        if (lastLocation == null || lastMovingLocation == null) {
            lastLocation = trackPoint;
            lastMovingLocation = trackPoint;
            return;
        }

        double movingDistance = lastMovingLocation.distanceTo(trackPoint);
        if (movingDistance < minRecordingDistance && (!trackPoint.hasSpeed() || trackPoint.getSpeed() < MAX_NO_MOVEMENT_SPEED)) {
            speedBuffer.reset();
            lastLocation = trackPoint;
            return;
        }
        long movingTime = trackPoint.getTime() - lastLocation.getTime();
        if (movingTime < 0) {
            lastLocation = trackPoint;
            return;
        }

        // Update total distance
        currentSegment.addTotalDistance(movingDistance);

        // Update moving time
        currentSegment.addMovingTime(movingTime);

        // Update grade
        double run = lastLocation.distanceTo(trackPoint);
        updateGrade(run, elevationDifference);

        // Update max speed
        if (trackPoint.hasSpeed() && lastLocation.hasSpeed()) {
            updateSpeed(trackPoint.getTime(), trackPoint.getSpeed(), lastLocation.getTime(), lastLocation.getSpeed());
        }

        lastLocation = trackPoint;
        lastMovingLocation = trackPoint;
    }

    public void addTrackPoint(TrackPointIterator iterator, int minRecordingDistance) {
        while (iterator.hasNext()) {
            TrackPoint location = iterator.next();
            addTrackPoint(location, minRecordingDistance);
        }
    }

    /**
     * Gets the smoothed elevation over several readings.
     * The elevation readings is noisy so the smoothed elevation is better than the raw elevation for many tasks.
     */
    public double getSmoothedElevation() {
        return elevationBuffer.getAverage();
    }

    public double getSmoothedSpeed() {
        return speedBuffer.getAverage();
    }

    /**
     * Updates a speed reading while assuming the user is moving.
     *
     * @param time              the time
     * @param speed             the speed
     * @param lastLocationTime  the last location time
     * @param lastLocationSpeed the last location speed
     */
    @VisibleForTesting
    private void updateSpeed(long time, double speed, long lastLocationTime, double lastLocationSpeed) {
        if (speed < MAX_NO_MOVEMENT_SPEED) {
            speedBuffer.reset();
        } else if (isValidSpeed(time, speed, lastLocationTime, lastLocationSpeed)) {
            speedBuffer.setNext(speed);
            if (speedBuffer.getAverage() > currentSegment.getMaxSpeed()) {
                currentSegment.setMaxSpeed(speedBuffer.getAverage());
            }
        } else {
            Log.d(TAG, "Invalid speed. speed: " + speed + " lastLocationSpeed: " + lastLocationSpeed);
        }
    }

    /**
     * Updates an elevation reading. Returns the difference.
     *
     * @param elevation the elevation
     */
    @VisibleForTesting
    private double updateElevation(double elevation) {
        // Update elevation using the smoothed average
        double oldAverage = elevationBuffer.getAverage();
        elevationBuffer.setNext(elevation);
        double newAverage = elevationBuffer.getAverage();

        currentSegment.updateElevationExtremities(newAverage);
        double difference = newAverage - oldAverage;
        if (difference > 0) {
            currentSegment.addTotalElevationGain(difference);
        }
        return difference;
    }

    /**
     * Updates a grade reading.
     *
     * @param run  the run
     * @param rise the rise
     */
    @VisibleForTesting
    private void updateGrade(double run, double rise) {
        runBuffer.setNext(run);

        double smoothedRun = runBuffer.getAverage();

        // With the error in the altitude measurement, it is dangerous to divide by * anything less than 5.
        if (smoothedRun < 5.0) {
            return;
        }
        gradeBuffer.setNext(rise / smoothedRun);
        currentSegment.updateGradeExtremities(gradeBuffer.getAverage());
    }

    private TripStatistics init(long time) {
        TripStatistics stats = new TripStatistics();
        stats.setStartTime(time);
        stats.setStopTime(time);
        return stats;
    }

    /**
     * Returns true if the speed is valid.
     *
     * @param time              the time
     * @param speed             the speed
     * @param lastLocationTime  the last location time
     * @param lastLocationSpeed the last location speed
     */
    private boolean isValidSpeed(long time, double speed, long lastLocationTime, double lastLocationSpeed) {
        // There are a lot of noisy speed readings. Do the cheapest checks first, most expensive last.
        if (speed == 0) {
            return false;
        }

        // The following code will ignore unlikely readings. 128 m/s seems to be an internal android error code.
        if (Math.abs(speed - 128) < 1) {
            return false;
        }

        // See if the speed seems physically likely. Ignore any speeds that imply acceleration greater than 2g.
        long timeDifference = time - lastLocationTime;
        double speedDifference = Math.abs(lastLocationSpeed - speed);
        if (speedDifference > MAX_ACCELERATION * timeDifference) {
            return false;
        }

        // Only check if the speed buffer is full. Check that the speed is less than 10X the smoothed average and the speed difference doesn't imply 2g acceleration.
        if (speedBuffer.isFull()) {
            double average = speedBuffer.getAverage();
            double diff = Math.abs(average - speed);
            return (speed < average * 10) && (diff < MAX_ACCELERATION * timeDifference);
        }

        return true;
    }
}
