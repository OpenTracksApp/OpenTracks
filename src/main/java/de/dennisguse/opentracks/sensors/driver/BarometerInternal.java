package de.dennisguse.opentracks.sensors.driver;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.concurrent.TimeUnit;

import de.dennisguse.opentracks.data.models.AtmosphericPressure;
import de.dennisguse.opentracks.sensors.SensorManager;
import de.dennisguse.opentracks.sensors.sensorData.AggregatorBarometer;
import de.dennisguse.opentracks.sensors.sensorData.Raw;

public class BarometerInternal implements Driver {

    private static final String TAG = BarometerInternal.class.getSimpleName();

    private static final int SAMPLING_PERIOD = (int) TimeUnit.SECONDS.toMicros(5);

    private final SensorManager.SensorDataChangedObserver listener;

    private Context context;

    private final SensorEventListener sensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (!isConnected()) {
                Log.w(TAG, "Not connected to sensor, cannot process data.");
                return;
            }

            listener.onChange(new Raw<>(AtmosphericPressure.ofHPA(event.values[0])));
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            Log.w(TAG, "Sensor accuracy changes are (currently) ignored.");
        }
    };

    public BarometerInternal(@NonNull SensorManager.SensorDataChangedObserver listener) {
        this.listener = listener;

    }

    @Override
    public void connect(Context context, Handler handler, String addressIgnored) {

        android.hardware.SensorManager sensorManager = (android.hardware.SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        Sensor pressureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
        if (pressureSensor == null) {
            Log.w(TAG, "No pressure sensor available.");
            return;
        }

        if (sensorManager.registerListener(sensorEventListener, pressureSensor, SAMPLING_PERIOD, handler)) {
            this.context = context;
            listener.onConnect(new AggregatorBarometer("internal", null));
            return;
        }

        disconnect();
    }

    public boolean isConnected() {
        return context != null;
    }

    @Override
    public void disconnect() {
        if (!isConnected()) return;

        android.hardware.SensorManager sensorManager = (android.hardware.SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        sensorManager.unregisterListener(sensorEventListener);
        this.context = null;
    }
}
