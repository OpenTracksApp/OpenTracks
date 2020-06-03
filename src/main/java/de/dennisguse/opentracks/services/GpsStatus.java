package de.dennisguse.opentracks.services;

import android.content.Context;
import android.location.Location;
import android.os.Handler;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.util.PreferencesUtils;

/**
 * This class handle GPS status according to received locations and some thresholds.
 */
class GpsStatus {

    private static final String TAG = GpsStatus.class.getSimpleName();

    // If recording interval not set in preferences then it uses 10 seconds like signal lost threshold.
    private static final int SIGNAL_LOST_THRESHOLD_DEFAULT = 10000;

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
                if (!stopped) {
                    onLocationChanged(null);
                    gpsStatusHandler.postDelayed(gpsStatusRunner, getIntervalThreshold());
                }
            }
        }

        public void stop() {
            stopped = true;
        }
    }

    private Handler gpsStatusHandler = null;
    private GpsStatusRunner gpsStatusRunner = null;

    public GpsStatus(Context context, GpsStatusListener service) {
        this.service = service;
        this.context = context;
        signalBadThreshold = PreferencesUtils.getRecordingDistanceInterval(context);
        signalLostThreshold = getRecordingIntervalInMillis(PreferencesUtils.getMinRecordingInterval(context));
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
     * @param value New preference value to signalLostThreshold.
     */
    public void onMinRecordingIntervalChanged(int value) {
        signalLostThreshold = getRecordingIntervalInMillis(value);
    }

    /**
     * @param value the preference value for recording interval.
     * @return the preference value in milliseconds or SIGNAL_LOST_THRESHOLD_DEFAULT if the preference value is a default one.
     */
    private int getRecordingIntervalInMillis(int value) {
        String minDefault = context.getString(R.string.min_recording_interval_default);
        String minAdaptAccuracy = context.getString(R.string.min_recording_interval_adapt_accuracy);
        String minAdaptBatteryLife = context.getString(R.string.min_recording_interval_adapt_battery_life);
        if (value != Integer.valueOf(minDefault) &&
                value != Integer.valueOf(minAdaptAccuracy) &&
                value != Integer.valueOf(minAdaptBatteryLife)) {
            return value * 1000; // in millis.
        } else {
            return SIGNAL_LOST_THRESHOLD_DEFAULT;
        }
    }

    /**
     * This method must be called from service every time a new location is received.
     * Receive new location and calculate the new status if needed.
     * Also, it'll run the runnable if signal is bad or stop it if the signal is lost.
     */
    public void onLocationChanged(final Location location) {
        if (lastLocation != null) {
            if (System.currentTimeMillis() - lastLocation.getTime() > signalLostThreshold) {
                // So much time without receiving signal -> signal lost.
                if (gpsStatus != GpsStatusValue.GPS_SIGNAL_LOST) {
                    GpsStatusValue oldStatus = gpsStatus;
                    gpsStatus = GpsStatusValue.GPS_SIGNAL_LOST;
                    service.onGpsStatusChanged(oldStatus, gpsStatus);
                    stopStatusRunner();
                }
            } else if (lastLocation.getAccuracy() > signalBadThreshold) {
                // Too little accuracy -> bad signal.
                if (gpsStatus != GpsStatusValue.GPS_SIGNAL_BAD) {
                    GpsStatusValue oldStatus = gpsStatus;
                    gpsStatus = GpsStatusValue.GPS_SIGNAL_BAD;
                    service.onGpsStatusChanged(oldStatus, gpsStatus);
                    startStatusRunner();
                }
            } else {
                // Otherwise -> gps fix (the first time) or signal okay.
                if (gpsStatus == GpsStatusValue.GPS_NONE) {
                    GpsStatusValue oldStatus = gpsStatus;
                    gpsStatus = GpsStatusValue.GPS_FIRST_FIX;
                    service.onGpsStatusChanged(oldStatus, gpsStatus);
                    gpsStatus = GpsStatusValue.GPS_FIRST_FIX;
                    startStatusRunner();
                } else if (gpsStatus != GpsStatusValue.GPS_SIGNAL_OKAY) {
                    GpsStatusValue oldStatus = gpsStatus;
                    gpsStatus = GpsStatusValue.GPS_SIGNAL_OKAY;
                    service.onGpsStatusChanged(oldStatus, gpsStatus);
                    startStatusRunner();
                }
            }
        } else if (lastValidLocation != null) {
            // Too much time without locations -> lost signal? (wait signalLostThreshold from last valid location).
            if (System.currentTimeMillis() - lastValidLocation.getTime() > signalLostThreshold) {
                if (gpsStatus != GpsStatusValue.GPS_SIGNAL_LOST) {
                    GpsStatusValue oldStatus = gpsStatus;
                    gpsStatus = GpsStatusValue.GPS_SIGNAL_LOST;
                    service.onGpsStatusChanged(oldStatus, gpsStatus);
                    stopStatusRunner();
                }
                lastValidLocation = null;
            }
        }

        lastLocation = location;
        if (location != null) {
            lastValidLocation = location;
        }
    }

    /**
     * This method must be called from service every time the GPS sensor is enabled.
     */
    public void onGpsEnabled() {
        if (gpsStatus != GpsStatusValue.GPS_ENABLED) {
            GpsStatusValue oldStatus = gpsStatus;
            gpsStatus = GpsStatusValue.GPS_ENABLED;
            service.onGpsStatusChanged(oldStatus, gpsStatus);
            startStatusRunner();
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
