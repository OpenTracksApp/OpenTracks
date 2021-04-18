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
    private Double speed;
    private Double pace;
    private Double heartRate;
    private Double cadence;
    private Double power;

    @VisibleForTesting
    ChartPoint(double altitude) {
        this.altitude = altitude;
    }

    public ChartPoint(@NonNull TrackStatisticsUpdater trackStatisticsUpdater, TrackPoint trackPoint, boolean chartByDistance, boolean metricUnits) {
        TrackStatistics trackStatistics = trackStatisticsUpdater.getTrackStatistics();

        if (chartByDistance) {
            timeOrDistance = trackStatistics.getTotalDistance().to(metricUnits);
        } else {
            timeOrDistance = trackStatistics.getTotalTime().toMillis();
        }

        altitude = trackStatisticsUpdater.getSmoothedAltitude();
        if (!metricUnits) {
            altitude *= UnitConversions.M_TO_FT;
        }

        speed = trackStatisticsUpdater.getSmoothedSpeed().to(metricUnits);
        pace = trackStatisticsUpdater.getSmoothedSpeed().toPace(metricUnits).toMillis() * UnitConversions.MS_TO_S * UnitConversions.S_TO_MIN;
        if (trackPoint != null) {
            if (trackPoint.hasHeartRate()) {
                heartRate = (double) trackPoint.getHeartRate_bpm();
            }
            if (trackPoint.hasCyclingCadence()) {
                cadence = (double) trackPoint.getCyclingCadence_rpm();
            }
            if (trackPoint.hasPower()) {
                power = (double) trackPoint.getPower();
            }
        }
    }

    public double getTimeOrDistance() {
        return timeOrDistance;
    }

    public Double getAltitude() {
        return altitude;
    }

    public Double getSpeed() {
        return speed;
    }

    public Double getPace() {
        return pace;
    }

    public Double getHeartRate() {
        return heartRate;
    }

    public Double getCadence() {
        return cadence;
    }

    public Double getPower() {
        return power;
    }
}
