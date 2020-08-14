package de.dennisguse.opentracks.services.handlers;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.os.Handler;
import android.util.Log;

import de.dennisguse.opentracks.util.PreferencesUtils;
import de.dennisguse.opentracks.util.UnitConversions;

/**
 * This class handle GPS status according to received locations and some thresholds.
 */
// TODO 2020-07-17 Delete all Log.d messages before merge with main branch. For now it's useful for debugging.
class GpsStatus {

    private static final String TAG = GpsStatus.class.getSimpleName();

    // The quantity of milliseconds that GpsStatus waits from minimal interval to consider GPS lost.
    private static final int SIGNAL_LOST_THRESHOLD = 10000;

    // Threshold for accuracy.
    private double signalBadThreshold;
    // Threshold for time without points.
    private int signalLostThreshold;

    private GpsStatusValue gpsStatus = GpsStatusValue.GPS_NONE;
    private GpsStatusListener client;
    private Context context;
    private Location lastLocation = null;
    private Location lastValidLocation = null;

    private class GpsStatusRunner implements Runnable {
        private boolean stopped = false;

        @Override
        public void run() {
            if (gpsStatus != null && !stopped) {
                onLocationChanged(null);
                gpsStatusHandler.postDelayed(gpsStatusRunner, getIntervalThreshold());
            }
        }

        public void stop() {
            stopped = true;
        }
    }

    private Handler gpsStatusHandler = null;
    private GpsStatusRunner gpsStatusRunner = null;

    /**
     * @param context              The context object.
     * @param client               The client.
     * @param minRecordingInterval Value of min recording interval preference.
     */
    public GpsStatus(Context context, GpsStatusListener client, int minRecordingInterval) {
        this.client = client;
        this.context = context;
        signalBadThreshold = PreferencesUtils.getRecordingDistanceInterval(context);
        signalLostThreshold = minRecordingInterval > 0 ? minRecordingInterval * (int) UnitConversions.ONE_SECOND_MS + SIGNAL_LOST_THRESHOLD : SIGNAL_LOST_THRESHOLD;
        gpsStatusHandler = new Handler();
    }

    /**
     * The client that uses GpsStatus has to call this method to stop the Runnable if needed.
     */
    public void stop() {
        if (gpsStatusRunner != null) {
            gpsStatusRunner.stop();
            gpsStatusRunner = null;
        }
    }

    /**
     * Method to change the bad threshold from outside.
     *
     * @param value New preference value to signalBadThreshold.
     */
    public void onRecordingDistanceChanged(int value) {
        signalBadThreshold = value;
    }

    /**
     * Method to change the lost threshold from outside.
     *
     * @param value Minimal recording interval preference value in seconds or an special value: -1, -2, 0.
     */
    public void onMinRecordingIntervalChanged(int value) {
        signalLostThreshold = value > 0 ? value * (int) UnitConversions.ONE_SECOND_MS + SIGNAL_LOST_THRESHOLD : SIGNAL_LOST_THRESHOLD;
    }

    /**
     * This method must be called from the client every time a new location is received.
     * Receive new location and calculate the new status if needed.
     * It look for GPS changes in lastLocation if it's not null. If it's null then look for in lastValidLocation if any.
     */
    public void onLocationChanged(final Location location) {
        if (lastLocation != null) {
            checkStatusFromLastLocation();
        } else if (lastValidLocation != null) {
            checkStatusFromLastValidLocation();
        }

        lastLocation = location;
        if (location != null) {
            lastValidLocation = location;
        }
    }

    /**
     * Checks if lastLocation has new GPS status looking up time and accuracy.
     * It depends of signalLostThreshold and signalBadThreshold.
     * If there is any change then it does the change.
     * Also, it'll run the runnable if signal is bad or stop it if the signal is lost.
     */
    private void checkStatusFromLastLocation() {
        if (System.currentTimeMillis() - lastLocation.getTime() > signalLostThreshold && gpsStatus != GpsStatusValue.GPS_SIGNAL_LOST) {
            // So much time without receiving signal -> signal lost.
            Log.d(TAG, "Signal LOST. signalLostThreshold: " + signalLostThreshold + " - System.currentTimeMillis() - lastLocation.getTime() > " + (System.currentTimeMillis() - lastLocation.getTime()));
            GpsStatusValue oldStatus = gpsStatus;
            gpsStatus = GpsStatusValue.GPS_SIGNAL_LOST;
            client.onGpsStatusChanged(oldStatus, gpsStatus);
            stopStatusRunner();
        } else if (lastLocation.getAccuracy() > signalBadThreshold && gpsStatus != GpsStatusValue.GPS_SIGNAL_BAD) {
            // Too little accuracy -> bad signal.
            Log.d(TAG, "Signal BAD. signalBadThreshold: " + signalBadThreshold + " - lastLocation.getAccuracy() = " + lastLocation.getAccuracy());
            GpsStatusValue oldStatus = gpsStatus;
            gpsStatus = GpsStatusValue.GPS_SIGNAL_BAD;
            client.onGpsStatusChanged(oldStatus, gpsStatus);
            startStatusRunner();
        } else if (lastLocation.getAccuracy() <= signalBadThreshold && gpsStatus != GpsStatusValue.GPS_SIGNAL_FIX) {
            Log.d(TAG, "Signal FIX. signalBadThreshold: " + signalBadThreshold + " - lastLocation.getAccuracy() = " + lastLocation.getAccuracy());
            // Gps okay.
            GpsStatusValue oldStatus = gpsStatus;
            gpsStatus = GpsStatusValue.GPS_SIGNAL_FIX;
            client.onGpsStatusChanged(oldStatus, gpsStatus);
            startStatusRunner();
        }
    }

    /**
     * Checks if lastValidLocation has a new GPS status looking up time.
     * It depends on signalLostThreshold.
     * If there is any change then it does the change.
     */
    private void checkStatusFromLastValidLocation() {
        if (System.currentTimeMillis() - lastValidLocation.getTime() > signalLostThreshold) {
            // Too much time without locations -> lost signal? (wait signalLostThreshold from last valid location).
            Log.d(TAG, "Signal LOST. signalLostThreshold: " + signalLostThreshold + " - System.currentTimeMillis() - lastValidLocation.getTime() > " + signalLostThreshold);
            GpsStatusValue oldStatus = gpsStatus;
            gpsStatus = GpsStatusValue.GPS_SIGNAL_LOST;
            client.onGpsStatusChanged(oldStatus, gpsStatus);
            stopStatusRunner();
            lastValidLocation = null;
        }
    }

    /**
     * This method must be called from the client every time the GPS sensor is enabled.
     * Anyway, it checks that GPS is enabled because the client assumes that if it's on then GPS is enabled but user can disable GPS by hand.
     */
    public void onGpsEnabled() {
        if (gpsStatus != GpsStatusValue.GPS_ENABLED) {
            LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            if (locationManager != null && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                GpsStatusValue oldStatus = gpsStatus;
                gpsStatus = GpsStatusValue.GPS_ENABLED;
                client.onGpsStatusChanged(oldStatus, gpsStatus);
                startStatusRunner();
            } else {
                onGpsDisabled();
            }
        }
    }

    /**
     * This method must be called from service every time the GPS sensor is disabled.
     */
    public void onGpsDisabled() {
        if (gpsStatus != GpsStatusValue.GPS_DISABLED) {
            GpsStatusValue oldStatus = gpsStatus;
            gpsStatus = GpsStatusValue.GPS_DISABLED;
            client.onGpsStatusChanged(oldStatus, gpsStatus);
            lastLocation = null;
            lastValidLocation = null;
            stopStatusRunner();
        }
    }

    private void startStatusRunner() {
        if (gpsStatusRunner == null) {
            gpsStatusRunner = new GpsStatusRunner();
            gpsStatusRunner.run();
        }
    }

    private void stopStatusRunner() {
        if (gpsStatusRunner != null) {
            gpsStatusRunner.stop();
            gpsStatusRunner = null;
        }
    }

    public int getIntervalThreshold() {
        return signalLostThreshold;
    }

    public GpsStatusValue getGpsStatus() {
        return gpsStatus;
    }

    public interface GpsStatusListener {
        void onGpsStatusChanged(GpsStatusValue prevStatus, GpsStatusValue currentStatus);
    }
}
