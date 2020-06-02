package de.dennisguse.opentracks.services;

import android.content.Context;
import android.location.Location;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.util.PreferencesUtils;

/**
 * This class handle GPS status according to received locations and some thresholds.
 */
class GpsStatus {

    private static final String TAG = GpsStatus.class.getSimpleName();

    // If not set in preferences it uses 10 seconds like signal lost threshold.
    private static final int SIGNAL_LOST_THRESHOLD_DEFAULT = 10000;

    private static double SIGNAL_BAD_THRESHOLD;
    private static int SIGNAL_LOST_THRESHOLD;

    private GpsStatusValue gpsStatus = GpsStatusValue.GPS_NONE;
    private GpsStatusListener service;
    private Context context;
    private Location lastLocation = null;

    public GpsStatus(Context context, GpsStatusListener service) {
        this.service = service;
        this.context = context;
        SIGNAL_BAD_THRESHOLD = PreferencesUtils.getMaxRecordingDistance(context);
        SIGNAL_LOST_THRESHOLD = getRecordingIntervalInMillis(PreferencesUtils.getMinRecordingInterval(context));
    }

    public void onRecordingDistanceChanged(int value) {
        SIGNAL_BAD_THRESHOLD = value;
    }

    public void onMinRecordingIntervalChanged(int value) {
        SIGNAL_LOST_THRESHOLD = getRecordingIntervalInMillis(value);
    }

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

    public void onLocationChanged(final Location location) {
        if (lastLocation != null) {
            if (System.currentTimeMillis() - lastLocation.getTime() > SIGNAL_LOST_THRESHOLD) {
                // So much time without receiving signal -> signal lost.
                if (gpsStatus != GpsStatusValue.GPS_SIGNAL_LOST) {
                    GpsStatusValue oldStatus = gpsStatus;
                    gpsStatus = GpsStatusValue.GPS_SIGNAL_LOST;
                    service.onGpsStatusChanged(oldStatus, gpsStatus);
                }
            } else if (lastLocation.getAccuracy() > SIGNAL_BAD_THRESHOLD) {
                // Too little accuracy -> bad signal.
                if (gpsStatus != GpsStatusValue.GPS_SIGNAL_BAD) {
                    GpsStatusValue oldStatus = gpsStatus;
                    gpsStatus = GpsStatusValue.GPS_SIGNAL_BAD;
                    service.onGpsStatusChanged(oldStatus, gpsStatus);
                }
            } else {
                // Otherwise -> gps fix (the first time) or signal okay.
                if (gpsStatus == GpsStatusValue.GPS_NONE) {
                    GpsStatusValue oldStatus = gpsStatus;
                    gpsStatus = GpsStatusValue.GPS_FIRST_FIX;
                    service.onGpsStatusChanged(oldStatus, gpsStatus);
                    gpsStatus = GpsStatusValue.GPS_FIRST_FIX;
                } else if (gpsStatus != GpsStatusValue.GPS_SIGNAL_OKAY) {
                    GpsStatusValue oldStatus = gpsStatus;
                    gpsStatus = GpsStatusValue.GPS_SIGNAL_OKAY;
                    service.onGpsStatusChanged(oldStatus, gpsStatus);
                }
            }
        }

        lastLocation = location;
    }

    public void onGpsEnabled() {
        if (gpsStatus != GpsStatusValue.GPS_ENABLED) {
            GpsStatusValue oldStatus = gpsStatus;
            gpsStatus = GpsStatusValue.GPS_ENABLED;
            service.onGpsStatusChanged(oldStatus, gpsStatus);
        }
    }

    public void onGpsDisabled() {
        if (gpsStatus != GpsStatusValue.GPS_DISABLED) {
            GpsStatusValue oldStatus = gpsStatus;
            gpsStatus = GpsStatusValue.GPS_DISABLED;
            service.onGpsStatusChanged(oldStatus, gpsStatus);
        }
    }

    public GpsStatusValue getGpsStatus() {
        return gpsStatus;
    }

    public interface GpsStatusListener {
        void onGpsStatusChanged(GpsStatusValue oldStatus, GpsStatusValue newStatus);
    }
}
