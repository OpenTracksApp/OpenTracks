package de.dennisguse.opentracks.util;

import android.hardware.SensorManager;

import androidx.annotation.VisibleForTesting;

public class PressureSensorUtils {

    //Everything above is considered a meaningful change in altitude.
    private static final float ALTITUDE_CHANGE_DIFF_M = 3.0f;

    private static final float EXPONENTIAL_SMOOTHING = 0.3f;

    private static final float p0 = SensorManager.PRESSURE_STANDARD_ATMOSPHERE;

    private PressureSensorUtils() {
    }

    public static class AltitudeChange {

        private final float currentSensorValue_hPa;

        private final float altitudeChange_m;

        public AltitudeChange(float currentSensorValue_hPa, float altitudeChange_m) {
            this.currentSensorValue_hPa = currentSensorValue_hPa;
            this.altitudeChange_m = altitudeChange_m;
        }

        public float getCurrentSensorValue_hPa() {
            return currentSensorValue_hPa;
        }

        public float getAltitudeChange_m() {
            return altitudeChange_m;
        }

        public float getAltitudeGain_m() {
            return altitudeChange_m > 0 ? altitudeChange_m : 0;
        }

        public float getAltitudeLoss_m() {
            return altitudeChange_m < 0 ? -1 * altitudeChange_m : 0;
        }
    }

    /**
     * Applies exponential smoothing to sensor value before computation.
     */
    public static AltitudeChange computeChangesWithSmoothing_m(float lastAcceptedSensorValue_hPa, float lastSeenSensorValue_hPa, float currentSensorValue_hPa) {
        float nextSensorValue_hPa = EXPONENTIAL_SMOOTHING * currentSensorValue_hPa + (1 - EXPONENTIAL_SMOOTHING) * lastSeenSensorValue_hPa;

        return computeChanges(lastAcceptedSensorValue_hPa, nextSensorValue_hPa);
    }

    @VisibleForTesting
    public static AltitudeChange computeChanges(float lastAcceptedSensorValue_hPa, float currentSensorValue_hPa) {
        float lastSensorValue_m = SensorManager.getAltitude(p0, lastAcceptedSensorValue_hPa);
        float currentSensorValue_m = SensorManager.getAltitude(p0, currentSensorValue_hPa);

        float altitudeChange_m = currentSensorValue_m - lastSensorValue_m;
        if (Math.abs(altitudeChange_m) < ALTITUDE_CHANGE_DIFF_M) {
            return null;
        }

        // Limit altitudeC change by ALTITUDE_CHANGE_DIFF and computes pressure value accordingly.
        AltitudeChange altitudeChange = new AltitudeChange(currentSensorValue_hPa, altitudeChange_m);
        if (altitudeChange.getAltitudeChange_m() > 0) {
            return new AltitudeChange(getBarometricPressure(lastSensorValue_m + ALTITUDE_CHANGE_DIFF_M), ALTITUDE_CHANGE_DIFF_M);
        } else {
            return new AltitudeChange(getBarometricPressure(lastSensorValue_m - ALTITUDE_CHANGE_DIFF_M), -1 * ALTITUDE_CHANGE_DIFF_M);
        }
    }

    /*
     * Barometeric pressure to altitude estimation; inverts of SensorManager.getAltitude(float, float)
     * https://de.wikipedia.org/wiki/Barometrische_H%C3%B6henformel#Internationale_H%C3%B6henformel
     * {\color{White} p(h)} = p_0 \cdot \left( 1 - \frac{0{,}0065 \frac{\mathrm K}{\mathrm m} \cdot h}{T_0\ } \right)^{5{,}255}
     */
    private static float getBarometricPressure(float altitude_m) {
        return (float) (p0 * Math.pow(1.0 - 0.0065 * altitude_m / 288.15, 5.255f));
    }
}
