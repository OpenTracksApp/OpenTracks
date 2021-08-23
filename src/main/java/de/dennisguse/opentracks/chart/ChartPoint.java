package de.dennisguse.opentracks.chart;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import de.dennisguse.opentracks.content.data.Distance;
import de.dennisguse.opentracks.content.data.Speed;
import de.dennisguse.opentracks.content.data.TrackPoint;
import de.dennisguse.opentracks.stats.TrackStatistics;
import de.dennisguse.opentracks.util.UnitConversions;

public class ChartPoint {
    //X-axis
    private double timeOrDistance;

    //Y-axis
    private final double altitude;
    private Double speed;
    private Double pace;
    private Double heartRate;
    private Double cadence;
    private Double power;

    @Deprecated
    @VisibleForTesting
    ChartPoint(double altitude) {
        this.altitude = altitude;
    }

    public ChartPoint(@NonNull TrackStatistics trackStatistics, @NonNull TrackPoint trackPoint, Speed smoothedSpeed, double smoothedAltitude_m, boolean chartByDistance, boolean metricUnits) {
        if (chartByDistance) {
            timeOrDistance = trackStatistics.getTotalDistance().toKM_Miles(metricUnits);
        } else {
            timeOrDistance = trackStatistics.getTotalTime().toMillis();
        }

        altitude = Distance.of(smoothedAltitude_m).toM_FT(metricUnits);

        speed = smoothedSpeed.to(metricUnits);
        pace = smoothedSpeed.toPace(metricUnits).toMillis() * UnitConversions.MS_TO_S * UnitConversions.S_TO_MIN;
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

    @NonNull
    @Override
    public String toString() {
        return "ChartPoint{" + "timeOrDistance=" + timeOrDistance + '}';
    }
}
