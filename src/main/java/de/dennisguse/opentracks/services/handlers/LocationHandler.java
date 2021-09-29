package de.dennisguse.opentracks.services.handlers;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import java.time.Duration;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.content.data.Distance;
import de.dennisguse.opentracks.content.data.TrackPoint;
import de.dennisguse.opentracks.util.LocationUtils;
import de.dennisguse.opentracks.settings.PreferencesUtils;

@VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
public class LocationHandler implements LocationListener, GpsStatus.GpsStatusListener {

    private final String TAG = LocationHandler.class.getSimpleName();

    private LocationManager locationManager;
    private final TrackPointCreator trackPointCreator;
    private GpsStatus gpsStatus;
    private Duration gpsInterval;
    private Distance thresholdHorizontalAccuracy;
    private TrackPoint lastTrackPoint;

    public LocationHandler(TrackPointCreator trackPointCreator) {
        this.trackPointCreator = trackPointCreator;
    }

    public void onStart(@NonNull Context context) {
        onSharedPreferenceChanged(null);
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
        if (locationManager != null) {
            locationManager.removeUpdates(this);
            locationManager = null;
        }

        if (gpsStatus != null) {
            gpsStatus.stop();
            gpsStatus = null;
        }
    }

    public void onSharedPreferenceChanged(String key) {
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
        trackPointCreator.onNewTrackPoint(trackPoint, thresholdHorizontalAccuracy);
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
        if (locationManager == null) {
            Log.e(TAG, "locationManager is null.");
            return;
        }
        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, gpsInterval.toMillis(), 0, this);
        } catch (SecurityException e) {
            Log.e(TAG, "Could not register location listener; permissions not granted.", e);
        }
    }

    TrackPoint getLastTrackPoint() {
        return lastTrackPoint;
    }

    /**
     * Called from {@link GpsStatus} to inform that GPS status has changed from prevStatus to currentStatus.
     *
     * @param prevStatus    previous {@link GpsStatusValue}.
     * @param currentStatus current {@link GpsStatusValue}.
     */
    @Override
    public void onGpsStatusChanged(GpsStatusValue prevStatus, GpsStatusValue currentStatus) {
        trackPointCreator.sendGpsStatus(currentStatus);
    }
}
