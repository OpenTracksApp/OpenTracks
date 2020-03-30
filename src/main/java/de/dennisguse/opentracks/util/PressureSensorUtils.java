package de.dennisguse.opentracks.util;

import android.hardware.SensorManager;

public class PressureSensorUtils {

    //Everything above is considered a meaningful change in elevation.
    private static float ELEVATION_CHANGE_DIFF_M = 5.0f;

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
    }

    /**
     * Computes the elevation gain and elevation loss.
     *
     * @return null if no meaningful elevation change occurred.
     */
    public static ElevationChange computeChanges_m(float lastSensorValue_hPa, float currentSensorValue_hPa) {
        float lastSensorValue_m = SensorManager.getAltitude(SensorManager.PRESSURE_STANDARD_ATMOSPHERE, lastSensorValue_hPa);
        float currentSensorValue_m = SensorManager.getAltitude(SensorManager.PRESSURE_STANDARD_ATMOSPHERE, currentSensorValue_hPa);

        float elevationChange_m = currentSensorValue_m - lastSensorValue_m;
        if (Math.abs(elevationChange_m) < ELEVATION_CHANGE_DIFF_M) {
            return null;
        }

        return new ElevationChange(currentSensorValue_hPa, elevationChange_m);
    }
}
