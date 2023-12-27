package de.dennisguse.opentracks.sensors;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;

import de.dennisguse.opentracks.data.models.AtmosphericPressure;
import de.dennisguse.opentracks.sensors.driver.BarometerInternal;
import de.dennisguse.opentracks.sensors.sensorData.AggregatorBarometer;
import de.dennisguse.opentracks.sensors.sensorData.Raw;
import de.dennisguse.opentracks.settings.PreferencesUtils;

/**
 * Estimates the altitude gain and altitude loss using the device's pressure sensor (i.e., barometer).
 */
public class GainManager implements SensorConnector {

    private static final String TAG = GainManager.class.getSimpleName();

    private BarometerInternal driver;

    private final SensorManager.SensorDataChangedObserver listener;

    private final SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceChangeListener = (sharedPreferences, key) -> {
        connect();
    };

    private Context context;
    private Handler handler;

    public GainManager(SensorManager.SensorDataChangedObserver listener) {
        this.listener = listener;
        driver = new BarometerInternal();
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

        onDisconnect(context);
    }

    public void onSensorValueChanged(AtmosphericPressure currentSensorValue) {
        listener.onChange(new Raw<>(currentSensorValue));
    }

    private void connect() {
        onDisconnect(context);

        String address = PreferencesUtils.getBarometerSensorAddress();
        switch (PreferencesUtils.getSensorType(address)) {
            case NONE -> driver = null;
            case INTERNAL -> driver = new BarometerInternal();
            case REMOTE -> throw new RuntimeException("Not implemented"); //TODO #1424
            default -> throw new RuntimeException("Not implemented");
        }

        if (driver != null) {
            driver.connect(context, handler, this);

            if (driver.isConnected()) {
                listener.onConnect(new AggregatorBarometer("internal", null));
            }
        }
    }

    private void onDisconnect(@NonNull Context context) {
        if (driver == null) return;

        driver.disconnect(context);
        listener.onDisconnect(new AggregatorBarometer("internal", null));
    }
}
