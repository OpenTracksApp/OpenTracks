package de.dennisguse.opentracks.services.sensors;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import de.dennisguse.opentracks.content.data.TrackPoint;
import de.dennisguse.opentracks.util.PressureSensorUtils;

/**
 * Estimates the altitude gain and altitude loss using the device's pressure sensor (i.e., barometer).
 */
public class AltitudeSumManager implements SensorEventListener {

    private static final String TAG = AltitudeSumManager.class.getSimpleName();

    private boolean isConnected = false;

    private float lastAcceptedPressureValue_hPa;

    private float lastSeenSensorValue_hPa;

    private Float altitudeGain_m;
    private Float altitudeLoss_m;

    public void start(Context context) {
        SensorManager sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);

        Sensor pressureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
        if (pressureSensor == null) {
            Log.w(TAG, "No pressure sensor available.");
            isConnected = false;
        } else {
            isConnected = sensorManager.registerListener(this, pressureSensor, SensorManager.SENSOR_DELAY_FASTEST);
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

    @VisibleForTesting
    public void setConnected(boolean isConnected) {
        this.isConnected = isConnected;
    }

    public void fill(@NonNull TrackPoint trackPoint) {
        trackPoint.setAltitudeGain(altitudeGain_m);
        trackPoint.setAltitudeLoss(altitudeLoss_m);
    }

    public @Nullable
    Float getAltitudeGain_m() {
        return isConnected ? altitudeGain_m : null;
    }

    public @Nullable
    Float getAltitudeLoss_m() {
        return isConnected ? altitudeLoss_m : null;
    }

    public void reset() {
        Log.d(TAG, "Reset");
        altitudeGain_m = null;
        altitudeLoss_m = null;
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

        altitudeGain_m = altitudeGain_m != null ? altitudeGain_m : 0;
        altitudeLoss_m = altitudeLoss_m != null ? altitudeLoss_m : 0;

        PressureSensorUtils.AltitudeChange altitudeChange = PressureSensorUtils.computeChangesWithSmoothing_m(lastAcceptedPressureValue_hPa, lastSeenSensorValue_hPa, value_hPa);
        if (altitudeChange != null) {
            altitudeGain_m += altitudeChange.getAltitudeGain_m();

            altitudeLoss_m += altitudeChange.getAltitudeLoss_m();

            lastAcceptedPressureValue_hPa = altitudeChange.getCurrentSensorValue_hPa();
        }

        lastSeenSensorValue_hPa = value_hPa;

        Log.v(TAG, "altitude gain: " + altitudeGain_m + ", altitude loss: " + altitudeLoss_m);
    }
}
