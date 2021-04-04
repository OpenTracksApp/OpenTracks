package de.dennisguse.opentracks.chart;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import de.dennisguse.opentracks.content.data.TrackPoint;
import de.dennisguse.opentracks.stats.TrackStatistics;
import de.dennisguse.opentracks.stats.TrackStatisticsUpdater;
import de.dennisguse.opentracks.util.UnitConversions;

public class ChartPoint {
    //X-axis
    private double timeOrDistance;

    //Y-axis
    private double altitude;
    private double speed;
    private double pace;
    private double heartRate = Double.NaN;
    private double cadence = Double.NaN;
    private double power = Double.NaN;

    @VisibleForTesting
    ChartPoint(double altitude) {
        this.altitude = altitude;
    }

    public ChartPoint(@NonNull TrackStatisticsUpdater trackStatisticsUpdater, TrackPoint trackPoint, boolean chartByDistance, boolean metricUnits) {
        TrackStatistics trackStatistics = trackStatisticsUpdater.getTrackStatistics();

        if (chartByDistance) {
            double distance = trackStatistics.getTotalDistance() * UnitConversions.M_TO_KM;
            if (!metricUnits) {
                distance *= UnitConversions.KM_TO_MI;
            }
            timeOrDistance = distance;
        } else {
            timeOrDistance = trackStatistics.getTotalTime().toMillis();
        }

        altitude = trackStatisticsUpdater.getSmoothedAltitude();
        if (!metricUnits) {
            altitude *= UnitConversions.M_TO_FT;
        }

        speed = trackStatisticsUpdater.getSmoothedSpeed() * UnitConversions.MPS_TO_KMH;
        if (!metricUnits) {
            speed *= UnitConversions.KM_TO_MI;
        }
        pace = speed == 0 ? 0.0 : 60.0 / speed;
        if (trackPoint != null) {
            if (trackPoint.hasHeartRate()) {
                heartRate = trackPoint.getHeartRate_bpm();
            }
            if (trackPoint.hasCyclingCadence()) {
                cadence = trackPoint.getCyclingCadence_rpm();
            }
            if (trackPoint.hasPower()) {
                power = trackPoint.getPower();
            }
        }
    }

    public double getTimeOrDistance() {
        return timeOrDistance;
    }

    public double getAltitude() {
        return altitude;
    }

    public double getSpeed() {
        return speed;
    }

    public double getPace() {
        return pace;
    }

    public boolean hasHeartRate() {
        return Double.isNaN(heartRate);
    }

    public double getHeartRate() {
        return heartRate;
    }

    public boolean hasCadence() {
        return Double.isNaN(cadence);
    }

    public double getCadence() {
        return cadence;
    }

    public boolean hasPower() {
        return Double.isNaN(power);
    }

    public double getPower() {
        return power;
    }
}
