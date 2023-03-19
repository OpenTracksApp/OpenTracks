package de.dennisguse.opentracks.chart;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import de.dennisguse.opentracks.data.models.Distance;
import de.dennisguse.opentracks.data.models.Speed;
import de.dennisguse.opentracks.data.models.TrackPoint;
import de.dennisguse.opentracks.settings.UnitSystem;
import de.dennisguse.opentracks.stats.TrackStatistics;

public class ChartPoint {
    //X-axis
    private double timeOrDistance;

    //Y-axis
    private Double altitude;
    private Double speed;
    private Double pace;
    private Double heartRate;
    private Double cadence;
    private Double power;
    private Double avgMovingPace; // new variable
    private Double avgMovingSpeed; // new variable

    @Deprecated
    @VisibleForTesting
    ChartPoint(double altitude) {
        this.altitude = altitude;
    }

    public ChartPoint(@NonNull TrackStatistics trackStatistics, @NonNull TrackPoint trackPoint, Speed smoothedSpeed, boolean chartByDistance, UnitSystem unitSystem) {
        if (chartByDistance) {
            timeOrDistance = trackStatistics.getTotalDistance().toKM_Miles(unitSystem);
        } else {
            timeOrDistance = trackStatistics.getTotalTime().toMillis();
        }

        if (trackPoint.hasAltitude()) {
            altitude = Distance.of(trackPoint.getAltitude().toM()).toM_FT(unitSystem);
        }

        if (smoothedSpeed != null) {
            speed = smoothedSpeed.to(unitSystem);
            pace = smoothedSpeed.toPace(unitSystem).toSeconds() / 60d;
        }
        if (trackPoint.hasHeartRate()) {
            heartRate = (double) trackPoint.getHeartRate().getBPM();
        }
        if (trackPoint.hasCadence()) {
            cadence = (double) trackPoint.getCadence().getRPM();
        }
        if (trackPoint.hasPower()) {
            power = (double) trackPoint.getPower().getW();
        }
        try{
            Speed ams = trackStatistics.getAverageMovingSpeed();

            avgMovingSpeed = ams.to(unitSystem); // getting value from trackstatistics and converting to unit system

        }catch (NullPointerException ne){
            avgMovingSpeed = 0.0;
        }
        try{
            Speed amp = trackStatistics.getAverageMovingPace();

            avgMovingPace = amp.toPace(unitSystem).toSeconds() / 60d; // getting value from trackstatistics and converting to unit system

        }catch (NullPointerException ne){
            avgMovingPace = 0.0;
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

    public Double getAvgMovingSpeed(){return avgMovingSpeed;} // to get average moving speed
    public Double getAvgMovingPace(){return avgMovingPace;} // to get average moving pace

    @NonNull
    @Override
    public String toString() {
        return "ChartPoint{" + "timeOrDistance=" + timeOrDistance + '}';
    }
}
