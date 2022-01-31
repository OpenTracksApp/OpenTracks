package de.dennisguse.opentracks.services.handlers;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import java.time.Duration;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.data.models.Distance;
import de.dennisguse.opentracks.data.models.TrackPoint;
import de.dennisguse.opentracks.settings.PreferencesUtils;
import de.dennisguse.opentracks.util.LocationUtils;
import de.dennisguse.opentracks.util.PermissionUtils;

@VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
public class GPSHandler implements LocationListener, GpsStatus.GpsStatusListener, SharedPreferences.OnSharedPreferenceChangeListener {

    private final String TAG = GPSHandler.class.getSimpleName();

    private final TrackPointCreator trackPointCreator;
    private Context context;
    private LocationManager locationManager;
    private GpsStatus gpsStatus;
    private Duration gpsInterval;
    private Distance thresholdHorizontalAccuracy;
    private TrackPoint lastTrackPoint;

    public GPSHandler(TrackPointCreator trackPointCreator) {
        this.trackPointCreator = trackPointCreator;
    }

    public void onStart(@NonNull Context context) {
        this.context = context;
        PreferencesUtils.registerOnSharedPreferenceChangeListener(this);
        gpsStatus = new GpsStatus(context, this);
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        registerLocationListener();
        gpsStatus.start();
    }

    private boolean isStarted() {
        return locationManager != null;
    }

    @SuppressWarnings({"MissingPermission"})
    //TODO upgrade to AGP7.0.0/API31 started complaining about removeUpdates.
    public void onStop() {
        lastTrackPoint = null;
        if (locationManager != null && context != null) {
            if (PermissionUtils.hasGPSPermission(context)) {
                locationManager.removeUpdates(this);
            }
            locationManager = null;
            context = null;
        }

        if (gpsStatus != null) {
            gpsStatus.stop();
            gpsStatus = null;
        }
        PreferencesUtils.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        boolean registerListener = false;

        if (PreferencesUtils.isKey(R.string.min_recording_interval_key, key)) {
            registerListener = true;

            gpsInterval = PreferencesUtils.getMinRecordingInterval();

            if (gpsStatus != null) {
                gpsStatus.onMinRecordingIntervalChanged(gpsInterval);
            }
        }
        if (PreferencesUtils.isKey(R.string.recording_gps_accuracy_key, key)) {
            thresholdHorizontalAccuracy = PreferencesUtils.getThresholdHorizontalAccuracy();
        }
        if (PreferencesUtils.isKey(R.string.recording_distance_interval_key, key)) {
            registerListener = true;

            if (gpsStatus != null) {
                Distance gpsMinDistance = PreferencesUtils.getRecordingDistanceInterval();
                gpsStatus.onRecordingDistanceChanged(gpsMinDistance);
            }
        }

        if (registerListener) {
            registerLocationListener();
        }
    }

    /**
     * Checks if location is valid and builds a track point that will be send through TrackPointCreator.
     *
     * @param location {@link Location} object.
     */
    @Override
    public void onLocationChanged(@NonNull Location location) {
        if (!isStarted()) {
            Log.w(TAG, "Location is ignored; not started.");
            return;
        }

        TrackPoint trackPoint = new TrackPoint(location, trackPointCreator.createNow());
        boolean isAccurate = trackPoint.fulfillsAccuracy(thresholdHorizontalAccuracy);
        boolean isValid = LocationUtils.isValidLocation(location);

        if (gpsStatus != null) {
            gpsStatus.onLocationChanged(trackPoint);
        }

        if (!isValid) {
            Log.w(TAG, "Ignore newTrackPoint. location is invalid.");
            return;
        }

        if (!isAccurate) {
            Log.d(TAG, "Ignore newTrackPoint. Poor accuracy.");
            return;
        }

        lastTrackPoint = trackPoint;
        trackPointCreator.onNewTrackPoint(trackPoint);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onProviderEnabled(@NonNull String provider) {
        if (gpsStatus != null) {
            gpsStatus.onGpsEnabled();
        }
    }

    @Override
    public void onProviderDisabled(@NonNull String provider) {
        if (gpsStatus != null) {
            gpsStatus.onGpsDisabled();
        }
    }

    private void registerLocationListener() {
        if (locationManager == null || context == null) {
            Log.e(TAG, "Not started.");
            return;
        }
        if (PermissionUtils.hasGPSPermission(context)) {
            try {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, gpsInterval.toMillis(), 0, this);
            } catch (SecurityException e) {
                Log.e(TAG, "Could not register location listener; permissions not granted.", e);
            }
        }
    }

    TrackPoint getLastTrackPoint() {
        return lastTrackPoint;
    }

    Distance getThresholdHorizontalAccuracy() {
        return thresholdHorizontalAccuracy;
    }

    @Override
    public void onGpsStatusChanged(GpsStatusValue prevStatus, GpsStatusValue currentStatus) {
        trackPointCreator.sendGpsStatus(currentStatus);
    }

    void reset() {
        lastTrackPoint = null;
    }
}
