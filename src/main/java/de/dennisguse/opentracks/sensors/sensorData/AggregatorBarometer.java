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
            value = getNoneValue();
            return;
        }

        PressureSensorUtils.AltitudeChange altitudeChange = PressureSensorUtils.computeChangesWithSmoothing_m(lastAcceptedSensorValue, previous.value(), current.value());
        if (altitudeChange != null) {
            value = new AltitudeGainLoss(value.gain_m() + altitudeChange.getAltitudeGain_m(), value.loss_m() + altitudeChange.getAltitudeLoss_m());

            lastAcceptedSensorValue = altitudeChange.currentSensorValue();
        }
    }

    @NonNull
    @Override
    protected AltitudeGainLoss getNoneValue() {
        return new AltitudeGainLoss(0f, 0f);
    }

    @Override
    public void reset() {
        value = getNoneValue();
    }

    public record Data(Altitude gain, Altitude loss) {}
}
