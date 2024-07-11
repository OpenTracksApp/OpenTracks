package de.dennisguse.opentracks.sensors;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.util.Log;

import de.dennisguse.opentracks.sensors.driver.BarometerInternal;
import de.dennisguse.opentracks.sensors.driver.Driver;
import de.dennisguse.opentracks.sensors.sensorData.AggregatorBarometer;
import de.dennisguse.opentracks.settings.PreferencesUtils;

/**
 * Estimates the altitude gain and altitude loss using the device's pressure sensor (i.e., barometer).
 */
public class GainManager implements SensorConnector {

    private static final String TAG = GainManager.class.getSimpleName();


    private final SensorManager.SensorDataChangedObserver listener;

    private final SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceChangeListener = (sharedPreferences, key) -> connect();

    private Context context;
    private Handler handler;
    private Driver driver;

    public GainManager(SensorManager.SensorDataChangedObserver listener) {
        this.listener = listener;
    }

    public void start(Context context, Handler handler) {
        this.context = context;
        this.handler = handler;
        PreferencesUtils.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
    }

    public void stop(Context context) {
        Log.d(TAG, "Stop");
        this.context = null;
        this.handler = null;
        PreferencesUtils.unregisterOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);

        onDisconnect();
    }

    private void connect() {
        onDisconnect();

        String address = PreferencesUtils.getBarometerSensorAddress();
        switch (PreferencesUtils.getSensorType(address)) {
            case NONE -> {
                driver = null;
                listener.onRemove(new AggregatorBarometer(null, null));
                return;
            }
            case INTERNAL -> driver = new BarometerInternal(listener);
            case REMOTE -> driver =
                    new BluetoothConnectionManager(
                            BluetoothUtils.getAdapter(context),
                            listener,
                            new BluetoothHandlerBarometricPressure()
                    );
            default -> throw new RuntimeException("Not implemented");
        }

        driver.connect(context, handler, address);
    }

    private void onDisconnect() {
        if (driver == null) return;

        driver.disconnect();
        listener.onDisconnect(new AggregatorBarometer("GainManager", null));
    }
}
