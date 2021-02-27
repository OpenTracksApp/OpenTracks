package de.dennisguse.opentracks.viewmodels;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.content.sensor.SensorData;
import de.dennisguse.opentracks.content.sensor.SensorDataCycling;
import de.dennisguse.opentracks.content.sensor.SensorDataCyclingPower;
import de.dennisguse.opentracks.content.sensor.SensorDataHeartRate;
import de.dennisguse.opentracks.util.StringUtils;

public class SensorDataModel {
    private int labelId;
    private final String sensorValue;
    private String sensorName;
    private int unitId;

    public SensorDataModel(int labelId, int unitId, float sensorValue) {
        this.labelId = labelId;
        this.unitId = unitId;
        this.sensorValue = StringUtils.formatDecimal(sensorValue, 0);
    }

    public SensorDataModel(SensorData sensorData) {
        this.sensorName = sensorData.getSensorNameOrAddress();
        this.sensorValue = sensorData.hasValue() && sensorData.isRecent() ? StringUtils.formatDecimal((float) sensorData.getValue(), 0) : null;
        if (sensorData instanceof SensorDataHeartRate) {
            this.labelId = R.string.sensor_state_heart_rate;
            this.unitId = R.string.sensor_unit_beats_per_minute;
        } else if (sensorData instanceof SensorDataCycling.Cadence) {
            this.labelId = R.string.sensor_state_cadence;
            this.unitId = R.string.sensor_unit_rounds_per_minute;
        } else if (sensorData instanceof SensorDataCyclingPower) {
            this.labelId = R.string.sensor_state_power;
            this.unitId = R.string.sensor_unit_power;
        }
    }

    public int getLabelId() {
        return labelId;
    }

    public int getUnitId() {
        return unitId;
    }

    public String getSensorName() {
        return sensorName;
    }

    public String getSensorValue() {
        return sensorValue;
    }

    public boolean hasValue() {
        return sensorValue != null;
    }
}
