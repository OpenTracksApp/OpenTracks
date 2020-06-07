package de.dennisguse.opentracks.services;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.os.Handler;

import de.dennisguse.opentracks.util.PreferencesUtils;
import de.dennisguse.opentracks.util.UnitConversions;

/**
 * This class handle GPS status according to received locations and some thresholds.
 */
class GpsStatus {

    private static final String TAG = GpsStatus.class.getSimpleName();

    // The quantity of milliseconds that GpsStatus waits from minimal interval to consider GPS lost.
    private static final int SIGNAL_LOST_THRESHOLD = 10000;

    // Threshold for accuracy.
    private double signalBadThreshold;
    // Threshold for time without points.
    private int signalLostThreshold;

    private GpsStatusValue gpsStatus = GpsStatusValue.GPS_NONE;
    private GpsStatusListener service;
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
     * @param service              The service.
     * @param minRecordingInterval Value of min recording interval preference.
     */
    public GpsStatus(Context context, GpsStatusListener service, int minRecordingInterval) {
        this.service = service;
        this.context = context;
        signalBadThreshold = PreferencesUtils.getRecordingDistanceInterval(context);
        signalLostThreshold = minRecordingInterval > 0 ? minRecordingInterval * (int) UnitConversions.ONE_SECOND_MS + SIGNAL_LOST_THRESHOLD : SIGNAL_LOST_THRESHOLD;
        gpsStatusHandler = new Handler();
    }

    /**
     * The service that uses GpsStatus has to call this method to stop the Runnable if needed.
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
     * This method must be called from service every time a new location is received.
     * Receive new location and calculate the new status if needed.
     * Also, it'll run the runnable if signal is bad or stop it if the signal is lost.
     */
    public void onLocationChanged(final Location location) {
        if (lastLocation != null) {
            if (System.currentTimeMillis() - lastLocation.getTime() > signalLostThreshold && gpsStatus != GpsStatusValue.GPS_SIGNAL_LOST) {
                // So much time without receiving signal -> signal lost.
                GpsStatusValue oldStatus = gpsStatus;
                gpsStatus = GpsStatusValue.GPS_SIGNAL_LOST;
                service.onGpsStatusChanged(oldStatus, gpsStatus);
                stopStatusRunner();
            } else if (lastLocation.getAccuracy() > signalBadThreshold && gpsStatus != GpsStatusValue.GPS_SIGNAL_BAD) {
                // Too little accuracy -> bad signal.
                GpsStatusValue oldStatus = gpsStatus;
                gpsStatus = GpsStatusValue.GPS_SIGNAL_BAD;
                service.onGpsStatusChanged(oldStatus, gpsStatus);
                startStatusRunner();
            } else if (gpsStatus != GpsStatusValue.GPS_SIGNAL_FIX) {
                // Gps okay.
                GpsStatusValue oldStatus = gpsStatus;
                gpsStatus = GpsStatusValue.GPS_SIGNAL_FIX;
                service.onGpsStatusChanged(oldStatus, gpsStatus);
                startStatusRunner();
            }
        } else if (lastValidLocation != null && System.currentTimeMillis() - lastValidLocation.getTime() > signalLostThreshold) {
            // Too much time without locations -> lost signal? (wait signalLostThreshold from last valid location).
            GpsStatusValue oldStatus = gpsStatus;
            gpsStatus = GpsStatusValue.GPS_SIGNAL_LOST;
            service.onGpsStatusChanged(oldStatus, gpsStatus);
            stopStatusRunner();
            lastValidLocation = null;
        }

        lastLocation = location;
        if (location != null) {
            lastValidLocation = location;
        }
    }

    /**
     * This method must be called from service every time the GPS sensor is enabled.
     * Anyway, it checks that GPS is enabled because the service assumes that if it's on then GPS is enabled but user can disable GPS by hand.
     */
    public void onGpsEnabled() {
        if (gpsStatus != GpsStatusValue.GPS_ENABLED) {
            LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            if (locationManager != null && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                GpsStatusValue oldStatus = gpsStatus;
                gpsStatus = GpsStatusValue.GPS_ENABLED;
                service.onGpsStatusChanged(oldStatus, gpsStatus);
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
            service.onGpsStatusChanged(oldStatus, gpsStatus);
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
        void onGpsStatusChanged(GpsStatusValue oldStatus, GpsStatusValue newStatus);
    }
}
