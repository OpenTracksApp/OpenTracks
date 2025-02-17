package de.dennisguse.opentracks.sensors.sensorData;

import androidx.annotation.NonNull;

import de.dennisguse.opentracks.data.models.Altitude;
import de.dennisguse.opentracks.data.models.AltitudeGainLoss;
import de.dennisguse.opentracks.data.models.AtmosphericPressure;
import de.dennisguse.opentracks.sensors.PressureSensorUtils;

public class AggregatorBarometer extends Aggregator<AtmosphericPressure, AltitudeGainLoss> {

    private AtmosphericPressure lastAcceptedSensorValue;

    public AggregatorBarometer(String sensorAddress, String sensorName) {
        super(sensorAddress, sensorName);
    }

    @Override
    protected void computeValue(Raw<AtmosphericPressure> current) {
        if (previous == null) {
            lastAcceptedSensorValue = current.value();
            aggregatedValue = getNoneValue();
            return;
        }

        PressureSensorUtils.AltitudeChange altitudeChange = PressureSensorUtils.computeChangesWithSmoothing_m(lastAcceptedSensorValue, previous.value(), current.value());
        if (altitudeChange != null) {
            aggregatedValue = new AltitudeGainLoss(aggregatedValue.gain_m() + altitudeChange.getAltitudeGain_m(), aggregatedValue.loss_m() + altitudeChange.getAltitudeLoss_m());

            lastAcceptedSensorValue = altitudeChange.currentSensorValue();
        }
    }

    @Override
    protected void resetImmediate() {
    }

    @Override
    public void resetAggregated() {
        aggregatedValue = getNoneValue();
    }

    @NonNull
    @Override
    protected AltitudeGainLoss getNoneValue() {
        return new AltitudeGainLoss(0f, 0f);
    }

    public record Data(Altitude gain, Altitude loss) {}
}
