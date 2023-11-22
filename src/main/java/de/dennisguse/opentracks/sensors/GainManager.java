package de.dennisguse.opentracks.sensors;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import de.dennisguse.opentracks.data.models.AtmosphericPressure;
import de.dennisguse.opentracks.sensors.driver.BarometerInternal;
import de.dennisguse.opentracks.sensors.sensorData.AggregatorBarometer;
import de.dennisguse.opentracks.sensors.sensorData.Raw;

/**
 * Estimates the altitude gain and altitude loss using the device's pressure sensor (i.e., barometer).
 */
public class GainManager implements SensorConnector {

    private static final String TAG = GainManager.class.getSimpleName();

    private final BarometerInternal driver;

    private final SensorManager.SensorDataChangedObserver listener;

    public GainManager(SensorManager.SensorDataChangedObserver listener) {
        this.listener = listener;
        driver = new BarometerInternal();
    }

    public void start(Context context, Handler handler) {
        driver.connect(context, handler, this);

        if (driver.isConnected()) {
            listener.onConnect(new AggregatorBarometer("internal"));
        }
    }

    public void stop(Context context) {
        Log.d(TAG, "Stop");

        driver.disconnect(context);
        listener.onDisconnect(new AggregatorBarometer("internal"));
    }

    public void onSensorValueChanged(AtmosphericPressure currentSensorValue) {
        listener.onChange(new Raw<>(currentSensorValue));
    }
}
