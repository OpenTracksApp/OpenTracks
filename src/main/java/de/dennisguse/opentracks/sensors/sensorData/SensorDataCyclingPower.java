package de.dennisguse.opentracks.sensors.sensorData;

import androidx.annotation.NonNull;

import de.dennisguse.opentracks.data.models.Power;

public class SensorDataCyclingPower extends SensorData<Power, Power> {

    public SensorDataCyclingPower(String address) {
        super(address);
    }

    public SensorDataCyclingPower(String name, String address) {
        super(name, address);
    }

    @Override
    public void computeValue(Raw<Power> current) {
        this.value = current.value();
    }

    @NonNull
    @Override
    protected Power getNoneValue() {
        return Power.of(0f);
    }
}
