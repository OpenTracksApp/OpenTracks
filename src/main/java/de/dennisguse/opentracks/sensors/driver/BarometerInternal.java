package de.dennisguse.opentracks.sensors.driver;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.util.Log;

import java.util.concurrent.TimeUnit;

import de.dennisguse.opentracks.data.models.AtmosphericPressure;
import de.dennisguse.opentracks.sensors.GainManager;

public class BarometerInternal {

    private static final String TAG = BarometerInternal.class.getSimpleName();

    private static final int SAMPLING_PERIOD = (int) TimeUnit.SECONDS.toMicros(5);

    private GainManager observer;

    private final SensorEventListener listener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (!isConnected()) {
                Log.w(TAG, "Not connected to sensor, cannot process data.");
                return;
            }

            observer.onSensorValueChanged(AtmosphericPressure.ofHPA(event.values[0]));
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            Log.w(TAG, "Sensor accuracy changes are (currently) ignored.");
        }
    };

    public void connect(Context context, Handler handler, GainManager observer) {
        SensorManager sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        Sensor pressureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
        if (pressureSensor == null) {
            Log.w(TAG, "No pressure sensor available.");
            this.observer = null;
        }

        if (sensorManager.registerListener(listener, pressureSensor, SAMPLING_PERIOD, handler)) {
            this.observer = observer;
            return;
        }

        disconnect(context);
    }

    public void disconnect(Context context) {
        SensorManager sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        sensorManager.unregisterListener(listener);
        observer = null;
    }

    public boolean isConnected() {
        return observer != null;
    }
}
