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

/**
 * Statistical data about a trip.
 * The data in this class should be filled out by TripStatisticsBuilder.
 *
 * @author Rodrigo Damazio
 */
public class TripStatistics {

    // The min and max elevation (meters) seen on this trip.
    private final ExtremityMonitor elevationExtremities = new ExtremityMonitor();
    // The min and max grade seen on this trip.
    @Deprecated //Remove grade min/max completely
    private final ExtremityMonitor gradeExtremities = new ExtremityMonitor();

    // The trip start time. This is the system time, might not match the GPs time.
    private long startTime = -1L;
    // The trip stop time. This is the system time, might not match the GPS time.
    private long stopTime = -1L;

    private double totalDistance_m;
    // Updated when new points are received, may be stale.
    private long totalTime_ms;
    // Based on when we believe the user is traveling.
    private long movingTime_ms;
    // The maximum speed (meters/second) that we believe is valid.
    private double maxSpeed_mps;
    // The total elevation gained (meters).
    private double totalElevationGain_m;

    public TripStatistics() {
    }

    /**
     * Copy constructor.
     *
     * @param other another statistics data object to copy from
     */
    public TripStatistics(TripStatistics other) {
        startTime = other.startTime;
        stopTime = other.stopTime;
        totalDistance_m = other.totalDistance_m;
        totalTime_ms = other.totalTime_ms;
        movingTime_ms = other.movingTime_ms;
        maxSpeed_mps = other.maxSpeed_mps;
        elevationExtremities.set(other.elevationExtremities.getMin(), other.elevationExtremities.getMax());
        totalElevationGain_m = other.totalElevationGain_m;
        gradeExtremities.set(other.gradeExtremities.getMin(), other.gradeExtremities.getMax());
    }

    /**
     * Combines these statistics with those from another object.
     * This assumes that the time periods covered by each do not intersect.
     *
     * @param other another statistics data object
     */
    public void merge(TripStatistics other) {
        startTime = Math.min(startTime, other.startTime);
        stopTime = Math.max(stopTime, other.stopTime);
        totalDistance_m += other.totalDistance_m;
        totalTime_ms += other.totalTime_ms;
        movingTime_ms += other.movingTime_ms;
        maxSpeed_mps = Math.max(maxSpeed_mps, other.maxSpeed_mps);
        if (other.elevationExtremities.hasData()) {
            elevationExtremities.update(other.elevationExtremities.getMin());
            elevationExtremities.update(other.elevationExtremities.getMax());
        }
        totalElevationGain_m += other.totalElevationGain_m;
        if (other.gradeExtremities.hasData()) {
            gradeExtremities.update(other.gradeExtremities.getMin());
            gradeExtremities.update(other.gradeExtremities.getMax());
        }
    }

    /**
     * Gets the trip start time. The number of milliseconds since epoch.
     */
    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getStopTime() {
        return stopTime;
    }

    public void setStopTime(long stopTime) {
        this.stopTime = stopTime;
    }

    public double getTotalDistance() {
        return totalDistance_m;
    }

    public void setTotalDistance(double totalDistance_m) {
        this.totalDistance_m = totalDistance_m;
    }

    public void addTotalDistance(double distance_m) {
        totalDistance_m += distance_m;
    }

    /**
     * Gets the total time in milliseconds that this track has been active.
     * This statistic is only updated when a new point is added to the statistics, so it may be off.
     * If you need to calculate the proper total time, use {@link #getStartTime} with the current time.
     */
    public long getTotalTime() {
        return totalTime_ms;
    }

    public void setTotalTime(long totalTime_ms) {
        this.totalTime_ms = totalTime_ms;
    }

    public long getMovingTime() {
        return movingTime_ms;
    }

    public void setMovingTime(long movingTime_ms) {
        this.movingTime_ms = movingTime_ms;
    }

    public void addMovingTime(long time_ms) {
        movingTime_ms += time_ms;
    }

    /**
     * Gets the average speed in meters/second.
     * This calculation only takes into account the displacement until the last point that was accounted for in statistics.
     */
    public double getAverageSpeed() {
        if (totalTime_ms == 0L) {
            return 0.0;
        }
        return totalDistance_m / ((double) totalTime_ms / 1000.0);
    }

    /**
     * Gets the average moving speed in meters/second.
     */
    public double getAverageMovingSpeed() {
        if (movingTime_ms == 0L) {
            return 0.0;
        }
        return totalDistance_m / ((double) movingTime_ms / 1000.0);
    }

    /**
     * Gets the maximum speed in meters/second.
     */
    public double getMaxSpeed() {
        return Math.max(maxSpeed_mps, getAverageMovingSpeed());
    }

    /**
     * Sets the maximum speed.
     *
     * @param maxSpeed the maximum speed in meters/second
     */
    public void setMaxSpeed(double maxSpeed) {
        this.maxSpeed_mps = maxSpeed;
    }

    /**
     * Gets the minimum elevation.
     * This is calculated from the smoothed elevation, so this can actually be more than the current elevation.
     */
    public double getMinElevation() {
        return elevationExtremities.getMin();
    }

    /**
     * Sets the minimum elevation.
     *
     * @param elevation the minimum elevation in meters
     */
    public void setMinElevation(double elevation) {
        elevationExtremities.setMin(elevation);
    }

    /**
     * Gets the maximum elevation.
     * This is calculated from the smoothed elevation, so this can actually be less than the current elevation.
     */
    public double getMaxElevation() {
        return elevationExtremities.getMax();
    }

    /**
     * Sets the maximum elevation.
     *
     * @param elevation_m the maximum elevation in meters
     */
    public void setMaxElevation(double elevation_m) {
        elevationExtremities.setMax(elevation_m);
    }

    /**
     * Updates a new elevation.
     *
     * @param elevation_m the elevation value in meters
     */
    public void updateElevationExtremities(double elevation_m) {
        elevationExtremities.update(elevation_m);
    }

    /**
     * Gets the total elevation gain in meters. This is calculated as the sum of all positive differences in the smoothed elevation.
     */
    public double getTotalElevationGain() {
        return totalElevationGain_m;
    }

    public void setTotalElevationGain(double totalElevationGain_m) {
        this.totalElevationGain_m = totalElevationGain_m;
    }

    public void addTotalElevationGain(double gain_m) {
        totalElevationGain_m += gain_m;
    }

    public double getMinGrade() {
        return gradeExtremities.getMin();
    }

    public void setMinGrade(double grade) {
        gradeExtremities.setMin(grade);
    }

    public double getMaxGrade() {
        return gradeExtremities.getMax();
    }

    /**
     * Sets the maximum grade.
     *
     * @param grade the grade as a fraction (1.0 would mean vertical upwards)
     */
    public void setMaxGrade(double grade) {
        gradeExtremities.setMax(grade);
    }

    public void updateGradeExtremities(double grade) {
        gradeExtremities.update(grade);
    }

    @NonNull
    @Override
    public String toString() {
        return "TripStatistics { Start Time: " + getStartTime() + "; Stop Time: " + getStopTime()
                + "; Total Distance: " + getTotalDistance() + "; Total Time: " + getTotalTime()
                + "; Moving Time: " + getMovingTime() + "; Max Speed: " + getMaxSpeed()
                + "; Min Elevation: " + getMinElevation() + "; Max Elevation: " + getMaxElevation()
                + "; Elevation Gain: " + getTotalElevationGain() + "; Min Grade: " + getMinGrade()
                + "; Max Grade: " + getMaxGrade() + "}";
    }
}