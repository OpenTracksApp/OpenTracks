package de.dennisguse.opentracks.services.sensors;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import androidx.annotation.Nullable;
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

    private float lastAcceptedPressureValue_hPa;

    private float lastSeenSensorValue_hPa;

    private float elevationGain_m;
    private float elevationLoss_m;

    public void start(Context context) {
        SensorManager sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);

        Sensor pressureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
        if (pressureSensor == null) {
            Log.w(TAG, "No pressure sensor available.");
            isConnected = false;
        } else {
            isConnected = sensorManager.registerListener(this, pressureSensor, SAMPLING_RATE);
        }

        lastAcceptedPressureValue_hPa = Float.NaN;
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

    @VisibleForTesting
    public void setConnected(boolean isConnected) {
        this.isConnected = isConnected;
    }

    public @Nullable
    Float getElevationGain_m() {
        return isConnected ? elevationGain_m : null;
    }

    public @Nullable
    Float getElevationLoss_m() {
        return isConnected ? elevationLoss_m : null;
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
        if (!isConnected) {
            Log.w(TAG, "Not connected to sensor, cannot process data.");
            return;
        }
        onSensorValueChanged(event.values[0]);
    }

    @VisibleForTesting
    void onSensorValueChanged(float value_hPa) {
        if (Float.isNaN(lastAcceptedPressureValue_hPa)) {
            lastAcceptedPressureValue_hPa = value_hPa;
            lastSeenSensorValue_hPa = value_hPa;
            return;
        }

        PressureSensorUtils.ElevationChange elevationChange = PressureSensorUtils.computeChangesWithSmoothing_m(lastAcceptedPressureValue_hPa, lastSeenSensorValue_hPa, value_hPa);
        if (elevationChange != null) {
            elevationGain_m += elevationChange.getElevationGain_m();
            elevationLoss_m += elevationChange.getElevationLoss_m();
            lastAcceptedPressureValue_hPa = elevationChange.getCurrentSensorValue_hPa();
        }

        lastSeenSensorValue_hPa = value_hPa;

        Log.v(TAG, "elevation gain: " + elevationGain_m + ", elevation loss: " + elevationLoss_m);
    }
}
