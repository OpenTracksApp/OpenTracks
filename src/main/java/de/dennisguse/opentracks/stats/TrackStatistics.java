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

package de.dennisguse.opentracks.stats;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import java.time.Duration;
import java.time.Instant;

import de.dennisguse.opentracks.content.data.Distance;
import de.dennisguse.opentracks.content.data.Speed;

/**
 * Statistical data about a {@link de.dennisguse.opentracks.content.data.Track}.
 * The data in this class should be filled out by {@link TrackStatisticsUpdater}.
 *
 * @author Rodrigo Damazio
 */
//TODO Use null instead of Double.isInfinite
//TODO Check that data ranges are valid (not less than zero etc.)
public class TrackStatistics {

    // The min and max altitude (meters) seen on this track.
    private final ExtremityMonitor altitudeExtremities = new ExtremityMonitor();

    // The track start time.
    private Instant startTime;
    // The track stop time.
    private Instant stopTime;

    private Distance totalDistance;
    // Updated when new points are received, may be stale.
    private Duration totalTime;
    // Based on when we believe the user is traveling.
    private Duration movingTime;
    // The maximum speed (meters/second) that we believe is valid.
    private Speed maxSpeed;
    private Float totalAltitudeGain_m = null;
    private Float totalAltitudeLoss_m = null;

    public TrackStatistics() {
        reset();
    }

    /**
     * Copy constructor.
     *
     * @param other another statistics data object to copy from
     */
    public TrackStatistics(TrackStatistics other) {
        startTime = other.startTime;
        stopTime = other.stopTime;
        totalDistance = other.totalDistance;
        totalTime = other.totalTime;
        movingTime = other.movingTime;
        maxSpeed = other.maxSpeed;
        altitudeExtremities.set(other.altitudeExtremities.getMin(), other.altitudeExtremities.getMax());
        totalAltitudeGain_m = other.totalAltitudeGain_m;
        totalAltitudeLoss_m = other.totalAltitudeLoss_m;
    }

    /**
     * Combines these statistics with those from another object.
     * This assumes that the time periods covered by each do not intersect.
     *
     * @param other another statistics data object
     */
    public void merge(TrackStatistics other) {
        if (startTime == null) {
            startTime = other.startTime;
        } else {
            startTime = startTime.isBefore(other.startTime) ? startTime : other.startTime;
        }
        if (stopTime == null) {
            stopTime = other.stopTime;
        } else {
            stopTime = stopTime.isAfter(other.stopTime) ? stopTime : other.stopTime;
        }

        totalDistance = totalDistance.plus(other.totalDistance);
        totalTime = totalTime.plus(other.totalTime);
        movingTime = movingTime.plus(other.movingTime);
        maxSpeed = Speed.max(maxSpeed, other.maxSpeed);
        if (other.altitudeExtremities.hasData()) {
            altitudeExtremities.update(other.altitudeExtremities.getMin());
            altitudeExtremities.update(other.altitudeExtremities.getMax());
        }
        if (totalAltitudeGain_m == null) {
            if (other.totalAltitudeGain_m != null) {
                totalAltitudeGain_m = other.totalAltitudeGain_m;
            }
        } else {
            if (other.totalAltitudeGain_m != null) {
                totalAltitudeGain_m += other.totalAltitudeGain_m;
            }
        }
        if (totalAltitudeLoss_m == null) {
            if (other.totalAltitudeLoss_m != null) {
                totalAltitudeLoss_m = other.totalAltitudeLoss_m;
            }
        } else {
            if (other.totalAltitudeLoss_m != null) {
                totalAltitudeLoss_m += other.totalAltitudeLoss_m;
            }
        }
    }

    public boolean isInitialized() {
        return startTime != null;
    }

    public void reset() {
        startTime = null;
        stopTime = null;

        setTotalDistance(Distance.of(0));
        setTotalTime(Duration.ofSeconds(0));
        setMovingTime(Duration.ofSeconds(0));
        setMaxSpeed(Speed.zero());
        setTotalAltitudeGain(null);
        setTotalAltitudeLoss(null);
    }

    public void reset(Instant startTime) {
        reset();
        setStartTime(startTime);
    }

    public Instant getStartTime() {
        return startTime;
    }

    /**
     * Should only be called on start.
     */
    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
        setStopTime(startTime);
    }

    public Instant getStopTime() {
        return stopTime;
    }

    public void setStopTime(Instant stopTime) {
        if (stopTime.isBefore(startTime)) {
            throw new RuntimeException("stopTime cannot be less than startTime: " + startTime + " " + stopTime);
        }
        this.stopTime = stopTime;
    }

    public Distance getTotalDistance() {
        return totalDistance;
    }

    public void setTotalDistance(Distance totalDistance_m) {
        this.totalDistance = totalDistance_m;
    }

    public void addTotalDistance(Distance distance_m) {
        totalDistance = totalDistance.plus(distance_m);
    }

    /**
     * Gets the total time in milliseconds that this track has been active.
     * This statistic is only updated when a new point is added to the statistics, so it may be off.
     * If you need to calculate the proper total time, use {@link #getStartTime} with the current time.
     */
    public Duration getTotalTime() {
        return totalTime;
    }

    public void setTotalTime(Duration totalTime) {
        this.totalTime = totalTime;
    }

    public Duration getMovingTime() {
        return movingTime;
    }

    public void setMovingTime(Duration movingTime) {
        this.movingTime = movingTime;
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
    public void addMovingTime(Duration time) {
        movingTime = movingTime.plus(time);
    }

    /**
     * Gets the average speed.
     * This calculation only takes into account the displacement until the last point that was accounted for in statistics.
     */
    public Speed getAverageSpeed() {
        if (totalDistance.isZero() && totalDistance.isZero()) {
            return Speed.of(0);
        }
        return Speed.of(totalDistance.toM() / totalTime.getSeconds());
    }

    public Speed getAverageMovingSpeed() {
        return Speed.of(totalDistance, movingTime);
    }

    public Speed getMaxSpeed() {
        return Speed.max(maxSpeed, getAverageMovingSpeed());
    }

    public void setMaxSpeed(Speed maxSpeed) {
        this.maxSpeed = maxSpeed;
    }

    public boolean hasAltitudeMin() {
        return !Double.isInfinite(getMinAltitude());
    }

    public double getMinAltitude() {
        return altitudeExtremities.getMin();
    }

    public void setMinAltitude(double altitude_m) {
        altitudeExtremities.setMin(altitude_m);
    }

    public boolean hasAltitudeMax() {
        return !Double.isInfinite(getMaxAltitude());
    }

    /**
     * Gets the maximum altitude.
     * This is calculated from the smoothed altitude, so this can actually be less than the current altitude.
     */
    public double getMaxAltitude() {
        return altitudeExtremities.getMax();
    }

    public void setMaxAltitude(double altitude_m) {
        altitudeExtremities.setMax(altitude_m);
    }

    public void updateAltitudeExtremities(double altitude_m) {
        altitudeExtremities.update(altitude_m);
    }

    public boolean hasTotalAltitudeGain() {
        return totalAltitudeGain_m != null;
    }

    @Nullable
    public Float getTotalAltitudeGain() {
        return totalAltitudeGain_m;
    }

    public void setTotalAltitudeGain(Float totalAltitudeGain_m) {
        this.totalAltitudeGain_m = totalAltitudeGain_m;
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
    public void addTotalAltitudeGain(float gain_m) {
        if (totalAltitudeGain_m == null) {
            totalAltitudeGain_m = 0f;
        }
        totalAltitudeGain_m += gain_m;
    }

    public boolean hasTotalAltitudeLoss() {
        return totalAltitudeLoss_m != null;
    }

    @Nullable
    public Float getTotalAltitudeLoss() {
        return totalAltitudeLoss_m;
    }

    public void setTotalAltitudeLoss(Float totalAltitudeLoss_m) {
        this.totalAltitudeLoss_m = totalAltitudeLoss_m;
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
    public void addTotalAltitudeLoss(float loss_m) {
        if (totalAltitudeLoss_m == null) {
            totalAltitudeLoss_m = 0f;
        }
        totalAltitudeLoss_m += loss_m;
    }

    @NonNull
    @Override
    public String toString() {
        return "TrackStatistics { Start Time: " + getStartTime() + "; Stop Time: " + getStopTime()
                + "; Total Distance: " + getTotalDistance() + "; Total Time: " + getTotalTime()
                + "; Moving Time: " + getMovingTime() + "; Max Speed: " + getMaxSpeed()
                + "; Min Altitude: " + getMinAltitude() + "; Max Altitude: " + getMaxAltitude()
                + "; Altitude Gain: " + getTotalAltitudeGain()
                + "; Altitude Loss: " + getTotalAltitudeLoss() + "}";
    }
}