package de.dennisguse.opentracks.sensors;

import android.hardware.SensorManager;

import androidx.annotation.VisibleForTesting;

public class PressureSensorUtils {

    //Everything above is considered a meaningful change in altitude.
    private static final float ALTITUDE_CHANGE_DIFF_M = 3.0f;

    private static final float EXPONENTIAL_SMOOTHING = 0.3f;

    private static final float P0 = SensorManager.PRESSURE_STANDARD_ATMOSPHERE;

    private PressureSensorUtils() {
    }

    public static class AltitudeChange {

        private final float currentSensorValueHPa;

        private final float altitudeChangeM;

        public AltitudeChange(float currentSensorValueHPa, float altitudeChangeM) {
            this.currentSensorValueHPa = currentSensorValueHPa;
            this.altitudeChangeM = altitudeChangeM;
        }

        public float getCurrentSensorValueHPa() {
            return currentSensorValueHPa;
        }

        public float getAltitudeChange() {
            return altitudeChangeM;
        }

        public float getAltitudeGain() {
            return altitudeChangeM > 0 ? altitudeChangeM : 0;
        }

        public float getAltitudeLoss() {
            return altitudeChangeM < 0 ? -1 * altitudeChangeM : 0;
        }
    }

    /**
     * Applies exponential smoothing to sensor value before computation.
     */
    public static AltitudeChange computeChangesWithSmoothing(float lastAcceptedSensorValueHPa, float lastSeenSensorValueHPa, float currentSensorValueHPa) {
        float nextSensorValueHPa = EXPONENTIAL_SMOOTHING * currentSensorValueHPa + (1 - EXPONENTIAL_SMOOTHING) * lastSeenSensorValueHPa;

        return computeChanges(lastAcceptedSensorValueHPa, nextSensorValueHPa);
    }

    @VisibleForTesting
    public static AltitudeChange computeChanges(float lastAcceptedSensorValueHPa, float currentSensorValueHPa) {
        float lastSensorValue = SensorManager.getAltitude(P0, lastAcceptedSensorValueHPa);
        float currentSensorValue = SensorManager.getAltitude(P0, currentSensorValueHPa);

        float altitudeChangeM = currentSensorValue - lastSensorValue;
        if (Math.abs(altitudeChangeM) < ALTITUDE_CHANGE_DIFF_M) {
            return null;
        }

        // Limit altitudeC change by ALTITUDE_CHANGE_DIFF and computes pressure value accordingly.
        AltitudeChange altitudeChange = new AltitudeChange(currentSensorValueHPa, altitudeChangeM);
        if (altitudeChange.getAltitudeChange() > 0) {
            return new AltitudeChange(getBarometricPressure(lastSensorValue + ALTITUDE_CHANGE_DIFF_M), ALTITUDE_CHANGE_DIFF_M);
        } else {
            return new AltitudeChange(getBarometricPressure(lastSensorValue - ALTITUDE_CHANGE_DIFF_M), -1 * ALTITUDE_CHANGE_DIFF_M);
        }
    }

    /*
     * Barometeric pressure to altitude estimation; inverts of SensorManager.getAltitude(float, float)
     * https://de.wikipedia.org/wiki/Barometrische_H%C3%B6henformel#Internationale_H%C3%B6henformel

     */
    @VisibleForTesting
    public static float getBarometricPressure(float altitude) {
        return (float) (P0 * Math.pow(1.0 - 0.0065 * altitude / 288.15, 5.255f));
    }
}
