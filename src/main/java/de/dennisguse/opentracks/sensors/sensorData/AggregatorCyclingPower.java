package de.dennisguse.opentracks.sensors.sensorData;

import androidx.annotation.NonNull;

import de.dennisguse.opentracks.data.models.Power;

public class AggregatorCyclingPower extends Aggregator<Power, Power> {

    public AggregatorCyclingPower(String address) {
        super(address);
    }

    public AggregatorCyclingPower(String name, String address) {
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
