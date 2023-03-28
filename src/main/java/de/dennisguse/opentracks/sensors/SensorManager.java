package de.dennisguse.opentracks.sensors;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.VisibleForTesting;

import de.dennisguse.opentracks.data.models.TrackPoint;
import de.dennisguse.opentracks.sensors.sensorData.SensorData;
import de.dennisguse.opentracks.sensors.sensorData.SensorDataSet;

public class SensorManager {

    private static final String TAG = SensorManager.class.getSimpleName();

    private BluetoothRemoteSensorManager bluetoothSensorManager;

    private AltitudeSumManager altitudeSumManager;

    public SensorManager(Context context, Handler handler, SensorDataSetChangeObserver observer) {
        bluetoothSensorManager = new BluetoothRemoteSensorManager(context, handler, observer);
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
    }

    public SensorDataSet fill(TrackPoint trackPoint) {
        altitudeSumManager.fill(trackPoint);
        return bluetoothSensorManager.fill(trackPoint);
    }

    public void reset() {
        if (bluetoothSensorManager == null || altitudeSumManager == null) {
            Log.d(TAG, "No recording running and no reset necessary.");
            return;
        }
        bluetoothSensorManager.reset();
        altitudeSumManager.reset();
    }

    @Deprecated
    @VisibleForTesting
    public BluetoothRemoteSensorManager getBluetoothSensorManager() {
        return bluetoothSensorManager;
    }

    @Deprecated
    @VisibleForTesting
    public void setBluetoothSensorManager(BluetoothRemoteSensorManager remoteSensorManager) {
        this.bluetoothSensorManager = remoteSensorManager;
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

    @Deprecated
    public interface SensorDataSetChangeObserver {
        void onChange(SensorDataSet sensorDataSet);
    }
}
