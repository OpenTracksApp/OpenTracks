package de.dennisguse.opentracks.services.sensors;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import androidx.annotation.VisibleForTesting;

import de.dennisguse.opentracks.util.PressureSensorUtils;
import de.dennisguse.opentracks.util.UnitConversions;

/**
 * Estimates the elevation gain and elevation loss using the device's pressure sensor (i.e., barometer).
 */
public class ElevationSumManager implements SensorEventListener {

    private static final String TAG = ElevationSumManager.class.getSimpleName();

    private static final int SAMPLING_RATE = 3 * (int) UnitConversions.ONE_SECOND_US;

    private boolean isConnected = false;

    private float lastUsedPressureValue_hPa;

    private float elevationGain_m;
    private float elevationLoss_m;

    public void start(Context context) {
        SensorManager sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);

        Sensor pressureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
        if (pressureSensor == null) {
            Log.w(TAG, "No pressure sensor available.");
            isConnected = false;
        }

        isConnected = sensorManager.registerListener(this, pressureSensor, SAMPLING_RATE);
        lastUsedPressureValue_hPa = Float.NaN;
        reset();
    }

    public void stop(Context context) {
        Log.d(TAG, "Stop");

        SensorManager sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        sensorManager.unregisterListener(this);

        isConnected = false;
        reset();
    }

    public boolean isConnected() {
        return isConnected;
    }

    public float getElevationGain_m() {
        return elevationGain_m;
    }

    public float getElevationLoss_m() {
        return elevationLoss_m;
    }

    public void reset() {
        Log.d(TAG, "Reset");
        elevationGain_m = 0;
        elevationLoss_m = 0;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.w(TAG, "Sensor accuracy changes are (currently) ignored.");
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        onSensorValueChanged(event.values[0]);
    }

    @VisibleForTesting
    void onSensorValueChanged(float value_hPa) {
        if (Float.isNaN(lastUsedPressureValue_hPa)) {
            lastUsedPressureValue_hPa = value_hPa;
            return;
        }

        PressureSensorUtils.ElevationChange elevationChange = PressureSensorUtils.computeChanges_m(lastUsedPressureValue_hPa, value_hPa);
        if (elevationChange != null) {
            if (elevationChange.getElevationChange_m() > 0) {
                elevationGain_m += elevationChange.getElevationChange_m();
            } else {
                elevationLoss_m += elevationChange.getElevationChange_m();
            }
            lastUsedPressureValue_hPa = elevationChange.getCurrentSensorValue_hPa();
        }

        Log.v(TAG, "elevation gain: " + elevationGain_m);
    }
}
