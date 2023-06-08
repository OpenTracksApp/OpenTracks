package de.dennisguse.opentracks.sensors;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import de.dennisguse.opentracks.data.models.AtmosphericPressure;
import de.dennisguse.opentracks.data.models.TrackPoint;
import de.dennisguse.opentracks.sensors.driver.BarometerInternal;

/**
 * Estimates the altitude gain and altitude loss using the device's pressure sensor (i.e., barometer).
 */
public class AltitudeSumManager implements SensorConnector {

    private static final String TAG = AltitudeSumManager.class.getSimpleName();

    private final BarometerInternal driver;

    private AtmosphericPressure lastAcceptedSensorValue;

    private AtmosphericPressure lastSeenSensorValue;

    private Float altitudeGain_m;
    private Float altitudeLoss_m;

    public AltitudeSumManager() {
        driver = new BarometerInternal();
    }

    @VisibleForTesting
    public AltitudeSumManager(BarometerInternal mock) {
        this.driver = mock;
    }

    public void start(Context context, Handler handler) {
        driver.connect(context, handler, this);

        lastAcceptedSensorValue = null;
        reset();
    }

    public void stop(Context context) {
        Log.d(TAG, "Stop");

        driver.disconnect(context);
        reset();
    }

    public void fill(@NonNull TrackPoint trackPoint) {
        trackPoint.setAltitudeGain(altitudeGain_m);
        trackPoint.setAltitudeLoss(altitudeLoss_m);
    }

    @Nullable
    public Float getAltitudeGain_m() {
        return driver.isConnected() ? altitudeGain_m : null;
    }

    @VisibleForTesting
    public void setAltitudeGain_m(float altitudeGain_m) {
        this.altitudeGain_m = altitudeGain_m;
    }

    @Nullable
    public Float getAltitudeLoss_m() {
        return driver.isConnected() ? altitudeLoss_m : null;
    }

    @VisibleForTesting
    public void setAltitudeLoss_m(float altitudeLoss_m) {
        this.altitudeLoss_m = altitudeLoss_m;
    }

    public void reset() {
        Log.d(TAG, "Reset");
        altitudeGain_m = null;
        altitudeLoss_m = null;
    }

    public void onSensorValueChanged(AtmosphericPressure currentSensorValue) {
        if (lastAcceptedSensorValue == null) {
            lastAcceptedSensorValue = currentSensorValue;
            lastSeenSensorValue = currentSensorValue;
            return;
        }

        altitudeGain_m = altitudeGain_m != null ? altitudeGain_m : 0;
        altitudeLoss_m = altitudeLoss_m != null ? altitudeLoss_m : 0;

        PressureSensorUtils.AltitudeChange altitudeChange = PressureSensorUtils.computeChangesWithSmoothing_m(lastAcceptedSensorValue, lastSeenSensorValue, currentSensorValue);
        if (altitudeChange != null) {
            altitudeGain_m += altitudeChange.getAltitudeGain_m();

            altitudeLoss_m += altitudeChange.getAltitudeLoss_m();

            lastAcceptedSensorValue = altitudeChange.getCurrentSensorValue();
        }

        lastSeenSensorValue = currentSensorValue;

        Log.v(TAG, "altitude gain: " + altitudeGain_m + ", altitude loss: " + altitudeLoss_m);
    }
}
