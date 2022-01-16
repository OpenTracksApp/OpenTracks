package de.dennisguse.opentracks.sensors.sensorData;

import androidx.annotation.NonNull;

import de.dennisguse.opentracks.data.models.Power;

public class SensorDataCyclingPower extends SensorData<Power> {

    public SensorDataCyclingPower(String address) {
        super(address);
    }

    public SensorDataCyclingPower(String name, String address, Power power) {
        super(name, address);
        this.value = power;
    }

    @NonNull
    @Override
    public String toString() {
        return super.toString() + " power=" + value;
    }

    @NonNull
    @Override
    protected Power getNoneValue() {
        return Power.of(0f);
    }
}
