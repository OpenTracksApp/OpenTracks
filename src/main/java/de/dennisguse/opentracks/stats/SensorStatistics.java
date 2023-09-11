package de.dennisguse.opentracks.stats;

import de.dennisguse.opentracks.data.models.Cadence;
import de.dennisguse.opentracks.data.models.HeartRate;
import de.dennisguse.opentracks.data.models.Power;

public class SensorStatistics {
    private final HeartRate maxHr;
    private final HeartRate avgHr;
    private final Cadence maxCadence;
    private final Cadence avgCadence;
    private final Power maxPower;
    private final Power avgPower;

    public SensorStatistics(HeartRate maxHr, HeartRate avgHr, Cadence maxCadence, Cadence avgCadence, Power maxPower, Power avgPower) {
        this.maxHr = maxHr;
        this.avgHr = avgHr;
        this.maxCadence = maxCadence;
        this.avgCadence = avgCadence;
        this.maxPower = maxPower;
        this.avgPower = avgPower;
    }

    public boolean hasHeartRate() {
        return avgHr != null && maxHr != null;
    }

    public HeartRate getMaxHeartRate() {
        return maxHr;
    }

    public HeartRate getAvgHeartRate() {
        return avgHr;
    }

    public boolean hasCadence() {
        return avgCadence != null && maxCadence != null;
    }

    public Cadence getMaxCadence() {
        return maxCadence;
    }

    public Cadence getAvgCadence() {
        return avgCadence;
    }

    public boolean hasPower() {
        return avgPower != null;
    }

    public Power getMaxPower() {
        return maxPower;
    }

    public Power getAvgPower() {
        return avgPower;
    }
}
