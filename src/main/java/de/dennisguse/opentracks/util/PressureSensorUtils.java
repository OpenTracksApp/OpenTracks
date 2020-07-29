package de.dennisguse.opentracks.util;

import android.hardware.SensorManager;

public class PressureSensorUtils {

    //Everything above is considered a meaningful change in elevation.
    private static float ELEVATION_CHANGE_DIFF_M = 3.0f;

    private static float EXPONENTIAL_SMOOTHING = 0.3f;

    private PressureSensorUtils() {
    }

    public static class ElevationChange {

        private float currentSensorValue_hPa;

        private float elevationChange_m;

        public ElevationChange(float currentSensorValue_hPa, float elevationChange_m) {
            this.currentSensorValue_hPa = currentSensorValue_hPa;
            this.elevationChange_m = elevationChange_m;
        }

        public float getCurrentSensorValue_hPa() {
            return currentSensorValue_hPa;
        }

        public float getElevationChange_m() {
            return elevationChange_m;
        }

        public float getElevationGain_m() {
            return elevationChange_m > 0 ? elevationChange_m : 0;
        }

        public float getElevationLoss_m() {
            return elevationChange_m < 0 ? elevationChange_m : 0;
        }
    }

    /**
     * Applies exponential smoothing to sensor value before computation.
     */
    public static ElevationChange computeChangesWithSmoothing_m(float lastAcceptedSensorValue_hPa, float lastSeenSensorValue_hPa, float currentSensorValue_hPa) {
        float nextSensorValue_hPa = EXPONENTIAL_SMOOTHING * currentSensorValue_hPa + (1 - EXPONENTIAL_SMOOTHING) * lastSeenSensorValue_hPa;

        return computeChanges_m(lastAcceptedSensorValue_hPa, nextSensorValue_hPa);
    }

    /**
     * Computes the elevation gain and elevation loss.
     *
     * @return null if no meaningful elevation change occurred.
     */
    public static ElevationChange computeChanges_m(float lastAcceptedSensorValue_hPa, float currentSensorValue_hPa) {
        float lastSensorValue_m = SensorManager.getAltitude(SensorManager.PRESSURE_STANDARD_ATMOSPHERE, lastAcceptedSensorValue_hPa);
        float currentSensorValue_m = SensorManager.getAltitude(SensorManager.PRESSURE_STANDARD_ATMOSPHERE, currentSensorValue_hPa);

        float elevationChange_m = currentSensorValue_m - lastSensorValue_m;
        if (Math.abs(elevationChange_m) < ELEVATION_CHANGE_DIFF_M) {
            return null;
        }

        // Limit elevation change by ELEVATION_CHANGE_DIFF and computes pressure value accordingly.
        ElevationChange elevationChange = new ElevationChange(currentSensorValue_hPa, elevationChange_m);
        if (elevationChange.getElevationGain_m() > 0) {
            return new ElevationChange(getBarometricPressure(SensorManager.PRESSURE_STANDARD_ATMOSPHERE, lastSensorValue_m + ELEVATION_CHANGE_DIFF_M), ELEVATION_CHANGE_DIFF_M);
        } else {
            return new ElevationChange(getBarometricPressure(SensorManager.PRESSURE_STANDARD_ATMOSPHERE, lastSensorValue_m - ELEVATION_CHANGE_DIFF_M), -1 * ELEVATION_CHANGE_DIFF_M);
        }
    }

    /*
     * Barometeric pressure to elevation estimation; inverts of SensorManager.getAltitude(float, float)
     * https://de.wikipedia.org/wiki/Barometrische_H%C3%B6henformel#Internationale_H%C3%B6henformel
     * {\color{White} p(h)} = p_0 \cdot \left( 1 - \frac{0{,}0065 \frac{\mathrm K}{\mathrm m} \cdot h}{T_0\ } \right)^{5{,}255}
     */
    private static float getBarometricPressure(float p0, float altitude_m) {
        return (float) (p0 * Math.pow(1.0 - 0.0065 * altitude_m / 288.15, 5.255f));
    }
}
