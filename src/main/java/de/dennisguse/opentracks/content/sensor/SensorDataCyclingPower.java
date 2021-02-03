package de.dennisguse.opentracks.content.sensor;

import androidx.annotation.NonNull;

public class SensorDataCyclingPower extends SensorData<Float> {

    public SensorDataCyclingPower(String address) {
        super(address);
    }

    public SensorDataCyclingPower(String name, String address, float power_w) {
        super(name, address);
        this.value = power_w;
    }

    @NonNull
    @Override
    public String toString() {
        return super.toString() + " power=" + value;
    }
}
