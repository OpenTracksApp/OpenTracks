package de.dennisguse.opentracks.sensors.sensorData;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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
    protected Power getNoneValue() {
        return Power.of(0f);
    }

    @NonNull
    @Override
    public String toString() {
        return super.toString() + " data=" + value;
    }

    public record Data(SensorDataCyclingPower power, SensorDataCyclingCadence cadence) {
            public Data(SensorDataCyclingPower power, @Nullable SensorDataCyclingCadence cadence) {
                this.power = power;
                this.cadence = cadence;
            }

            @NonNull
            @Override
            public String toString() {
                return "Data{" +
                        "power=" + power +
                        ", cadence=" + cadence +
                        '}';
            }
        }
}
