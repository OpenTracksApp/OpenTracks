package de.dennisguse.opentracks.sensors;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.VisibleForTesting;

import de.dennisguse.opentracks.data.models.TrackPoint;
import de.dennisguse.opentracks.sensors.sensorData.SensorData;
import de.dennisguse.opentracks.sensors.sensorData.SensorDataSet;
import de.dennisguse.opentracks.services.handlers.TrackPointCreator;

public class SensorManager {

    private static final String TAG = SensorManager.class.getSimpleName();

    //TODO Should be final and not be visible for testing
    @VisibleForTesting
    public SensorDataSet sensorDataSet = new SensorDataSet();

    private final TrackPointCreator observer;

    private final SensorDataChangedObserver listener = new SensorDataChangedObserver() {

        @Override
        public void onChange(SensorData<?> sensorData) {
            sensorDataSet.set(sensorData);
            observer.onChange(new SensorDataSet(sensorDataSet));
        }

        @Override
        public void onDisconnect(SensorData<?> sensorData) {
            sensorDataSet.remove(sensorData);
            observer.onChange(new SensorDataSet(sensorDataSet));
        }
    };

    private BluetoothRemoteSensorManager bluetoothSensorManager;

    private AltitudeSumManager altitudeSumManager;

    public SensorManager(Context context, Handler handler, TrackPointCreator observer) {
        this.observer = observer;
        bluetoothSensorManager = new BluetoothRemoteSensorManager(context, handler, listener);
        altitudeSumManager = new AltitudeSumManager();
    }

    public void start(Context context, Handler handler) {
        bluetoothSensorManager.start(context, handler);
        altitudeSumManager.start(context, handler);
    }

    public void stop(Context context) {
        if (bluetoothSensorManager != null) {
            bluetoothSensorManager.stop(context);
            bluetoothSensorManager = null;
        }

        if (altitudeSumManager != null) {
            altitudeSumManager.stop(context);
            altitudeSumManager = null;
        }

        sensorDataSet.clear();
    }

    public SensorDataSet fill(TrackPoint trackPoint) {
        altitudeSumManager.fill(trackPoint);
        sensorDataSet.fillTrackPoint(trackPoint);
        return new SensorDataSet(sensorDataSet);
    }

    public void reset() {
        if (bluetoothSensorManager == null || altitudeSumManager == null) {
            Log.d(TAG, "No recording running and no reset necessary.");
            return;
        }
        sensorDataSet.reset();
        altitudeSumManager.reset();
    }

    @Deprecated
    @VisibleForTesting
    public BluetoothRemoteSensorManager getBluetoothSensorManager() {
        return bluetoothSensorManager;
    }

    @Deprecated
    @VisibleForTesting
    public AltitudeSumManager getAltitudeSumManager() {
        return altitudeSumManager;
    }

    @Deprecated
    @VisibleForTesting
    public void setAltitudeSumManager(AltitudeSumManager altitudeSumManager) {
        this.altitudeSumManager = altitudeSumManager;
    }

    public interface SensorDataChangedObserver {
        void onChange(SensorData<?> sensorData);

        void onDisconnect(SensorData<?> sensorData);
    }
}
