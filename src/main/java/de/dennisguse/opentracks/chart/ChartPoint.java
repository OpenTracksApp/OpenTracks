package de.dennisguse.opentracks.chart;

import androidx.annotation.NonNull;

import de.dennisguse.opentracks.data.models.Distance;
import de.dennisguse.opentracks.data.models.Speed;
import de.dennisguse.opentracks.data.models.TrackPoint;
import de.dennisguse.opentracks.settings.UnitSystem;
import de.dennisguse.opentracks.stats.TrackStatistics;

public record ChartPoint(
        //X-axis
        double timeOrDistance,

        //Y-axis
        Double altitude,
        Double speed,
        Double pace,
        Double heartRate,
        Double cadence,
        Double power
) {


    public static ChartPoint create(@NonNull TrackStatistics trackStatistics, @NonNull TrackPoint trackPoint, Speed smoothedSpeed, boolean chartByDistance, UnitSystem unitSystem) {
        return new ChartPoint(
                chartByDistance
                        ? trackStatistics.getTotalDistance().toKM_Miles(unitSystem)
                        : trackStatistics.getTotalTime().toMillis(),
                trackPoint.hasAltitude()
                        ? Distance.of(trackPoint.getAltitude().toM()).toM_FT(unitSystem)
                        : null,
                smoothedSpeed != null
                        ? smoothedSpeed.to(unitSystem)
                        : null,
                smoothedSpeed != null
                        ? smoothedSpeed.toPace(unitSystem).toSeconds() / 60d
                        : null,
                trackPoint.hasHeartRate()
                        ? (double) trackPoint.getHeartRate().getBPM()
                        : null,
                trackPoint.hasCadence()
                        ? (double) trackPoint.getCadence().getRPM()
                        : null,
                trackPoint.hasPower()
                        ? (double) trackPoint.getPower().getW()
                        : null
        );
    }
}
