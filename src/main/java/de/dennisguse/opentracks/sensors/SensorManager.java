package de.dennisguse.opentracks.sensors;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import java.time.Instant;

import de.dennisguse.opentracks.data.models.TrackPoint;
import de.dennisguse.opentracks.sensors.sensorData.Aggregator;
import de.dennisguse.opentracks.sensors.sensorData.Raw;
import de.dennisguse.opentracks.sensors.sensorData.SensorDataSet;
import de.dennisguse.opentracks.services.handlers.TrackPointCreator;

public class SensorManager implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = SensorManager.class.getSimpleName();

    //TODO Should be final and not be visible for testing
    @VisibleForTesting
    public SensorDataSet sensorDataSet;

    private final TrackPointCreator observer;

    private final SensorDataChangedObserver listener = new SensorDataChangedObserver() {

        @Override
        public void onConnect(Aggregator<?, ?> aggregator) {
            sensorDataSet.add(aggregator);
        }

        @Override
        public void onChange(Raw<?> data) {
            sensorDataSet.update(data);
            observer.onChange(new SensorDataSet(sensorDataSet));
        }

        @Override
        public void onDisconnect(Aggregator<?, ?> aggregator) {
            sensorDataSet.add(aggregator);
        }

        @Override
        public void onRemove(Aggregator<?, ?> aggregator) {
            sensorDataSet.remove(aggregator);
        }

        @Override
        public Instant getNow() {
            return observer.createNow();
        }
    };

    private BluetoothRemoteSensorManager bluetoothSensorManager;

    private GainManager altitudeSumManager;

    private GpsManager gpsManager;

    public SensorManager(TrackPointCreator observer) {
        this.observer = observer;
        this.sensorDataSet = new SensorDataSet(observer);
    }

    public void start(Context context, Handler handler) {
        if (gpsManager != null) {
            throw new RuntimeException("SensorManager cannot be started twice; stop first.");
        }

        gpsManager = new GpsManager(observer, listener);
        altitudeSumManager = new GainManager(listener);
        bluetoothSensorManager = new BluetoothRemoteSensorManager(context, handler, listener);

        onSharedPreferenceChanged(null, null);

        gpsManager.start(context, handler);
        altitudeSumManager.start(context, handler);
        bluetoothSensorManager.start(context, handler);
    }

    public void stop(Context context) {
        bluetoothSensorManager.stop(context);
        bluetoothSensorManager = null;

        altitudeSumManager.stop(context);
        altitudeSumManager = null;

        gpsManager.stop(context);
        gpsManager = null;

        sensorDataSet.clear();
    }

    public SensorDataSet fill(TrackPoint trackPoint) {
        sensorDataSet.fillTrackPoint(trackPoint);
        return new SensorDataSet(sensorDataSet);
    }

    public void reset() {
        if (bluetoothSensorManager == null || altitudeSumManager == null) {
            Log.d(TAG, "No recording running and no reset necessary.");
            return;
        }
        sensorDataSet.reset();
    }

    @VisibleForTesting
    public void onChanged(Raw<?> data) {
        listener.onChange(data);
    }

    public GpsManager getGpsManager() {
        return gpsManager;
    }

    @Deprecated
    @VisibleForTesting
    public GainManager getAltitudeSumManager() {
        return altitudeSumManager;
    }

    @Deprecated
    @VisibleForTesting
    public void setAltitudeSumManager(GainManager altitudeSumManager) {
        this.altitudeSumManager = altitudeSumManager;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, @Nullable String key) {
        if (gpsManager != null) {
            gpsManager.onSharedPreferenceChanged(sharedPreferences, key);
            bluetoothSensorManager.onSharedPreferenceChanged(sharedPreferences, key);
        }
    }

    public interface SensorDataChangedObserver {

        void onConnect(Aggregator<?, ?> sensorData);
        void onChange(Raw<?> sensorData);

        void onDisconnect(Aggregator<?, ?> sensorData);

        void onRemove(Aggregator<?, ?> sensorData);

        Instant getNow();
    }
}
