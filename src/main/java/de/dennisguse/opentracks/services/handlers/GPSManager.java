package de.dennisguse.opentracks.services.handlers;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.core.location.LocationListenerCompat;
import androidx.core.location.LocationManagerCompat;
import androidx.core.location.LocationRequestCompat;

import java.time.Duration;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.data.models.Distance;
import de.dennisguse.opentracks.data.models.TrackPoint;
import de.dennisguse.opentracks.sensors.SensorConnector;
import de.dennisguse.opentracks.settings.PreferencesUtils;
import de.dennisguse.opentracks.util.LocationUtils;
import de.dennisguse.opentracks.util.PermissionRequester;

@VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
public class GPSManager implements SensorConnector, LocationListenerCompat, GpsStatusManager.GpsStatusListener, SharedPreferences.OnSharedPreferenceChangeListener {

    private final String TAG = GPSManager.class.getSimpleName();

    private static final String LOCATION_PROVIDER = LocationManager.GPS_PROVIDER;

    private TrackPointCreator trackPointCreator;
    private Context context;
    private Handler handler;

    private LocationManager locationManager;
    private GpsStatusManager gpsStatusManager;
    private Duration gpsInterval;
    private Distance thresholdHorizontalAccuracy;

    public GPSManager(TrackPointCreator trackPointCreator) {
        this.trackPointCreator = trackPointCreator;
    }

    public void start(@NonNull Context context, @NonNull Handler handler) {
        this.context = context;
        this.handler = handler;

        onSharedPreferenceChanged(null, null);

        gpsStatusManager = new GpsStatusManager(context, this, handler);
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        registerLocationListener();
        gpsStatusManager.start();
    }

    private boolean isStarted() {
        return locationManager != null;
    }

    @SuppressWarnings({"MissingPermission"})
    public void stop(Context context) {
        if (isStarted()) {
            LocationManagerCompat.removeUpdates(locationManager, this);
        }
        locationManager = null;
        this.context = null;
        handler = null;

        gpsStatusManager.stop();
        gpsStatusManager = null;

        trackPointCreator = null;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        boolean registerListener = false;

        if (PreferencesUtils.isKey(R.string.min_recording_interval_key, key)) {
            registerListener = true;

            gpsInterval = PreferencesUtils.getMinRecordingInterval();

            if (gpsStatusManager != null) {
                gpsStatusManager.onMinRecordingIntervalChanged(gpsInterval);
            }
        }
        if (PreferencesUtils.isKey(R.string.recording_gps_accuracy_key, key)) {
            thresholdHorizontalAccuracy = PreferencesUtils.getThresholdHorizontalAccuracy();
        }
        if (PreferencesUtils.isKey(R.string.recording_distance_interval_key, key)) {
            registerListener = true;

            if (gpsStatusManager != null) {
                Distance gpsMinDistance = PreferencesUtils.getRecordingDistanceInterval();
                gpsStatusManager.onRecordingDistanceChanged(gpsMinDistance);
            }
        }

        if (registerListener && isStarted()) {
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
        // Send each update to the status; please note that this TrackPoint is not stored.
        TrackPoint trackPoint = new TrackPoint(location, trackPointCreator.createNow());
        gpsStatusManager.onNewTrackPoint(trackPoint);

        if (!LocationUtils.isValidLocation(location)) {
            Log.w(TAG, "Ignore newTrackPoint. location is invalid.");
            return;
        }

        if (!LocationUtils.fulfillsAccuracy(location, thresholdHorizontalAccuracy)) {
            Log.d(TAG, "Ignore newTrackPoint. Poor accuracy.");
            return;
        }

        trackPointCreator.onChange(location);
    }

    @Override
    public void onStatusChanged(@NonNull String provider, int status, Bundle extras) {
    }

    @Override
    public void onProviderEnabled(@NonNull String provider) {
        gpsStatusManager.onGpsEnabled();
    }

    @Override
    public void onProviderDisabled(@NonNull String provider) {
        gpsStatusManager.onGpsDisabled();
    }

    private void registerLocationListener() {
        if (!LocationManagerCompat.hasProvider(locationManager, LOCATION_PROVIDER)) {
            Log.e(TAG, "Device doesn't have GPS.");
            return;
        }

        LocationRequestCompat locationRequest = new LocationRequestCompat.Builder(gpsInterval.toMillis())
                .setQuality(LocationRequestCompat.QUALITY_HIGH_ACCURACY)
                .setMaxUpdateDelayMillis(0)
                .build();

        if (PermissionRequester.GPS.hasPermission(context)) {
            try {
                Log.i(TAG, "Register for location updates " + context);
                LocationManagerCompat.requestLocationUpdates(locationManager, LOCATION_PROVIDER, locationRequest, handler::post, this);
            } catch (SecurityException e) {
                Log.e(TAG, "Could not register location listener; permissions not granted.", e);
            }
        }
    }

    @Override
    public void onGpsStatusChanged(GpsStatusValue currentStatus) {
        trackPointCreator.sendGpsStatus(currentStatus);
    }
}
