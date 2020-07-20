package de.dennisguse.opentracks.services.handlers;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.content.data.TrackPoint;
import de.dennisguse.opentracks.util.LocationUtils;
import de.dennisguse.opentracks.util.PreferencesUtils;
import de.dennisguse.opentracks.util.TrackPointUtils;
import de.dennisguse.opentracks.util.UnitConversions;

class LocationHandler implements HandlerServer.Handler, LocationListener {

    private final String TAG = LocationHandler.class.getSimpleName();

    private LocationManager locationManager;
    private final HandlerServer handlerServer;
    private LocationListenerPolicy locationListenerPolicy;
    private long currentRecordingInterval;
    private int recordingGpsAccuracy;
    private TrackPoint lastValidTrackPoint;

    public LocationHandler(HandlerServer handlerServer) {
        this.handlerServer = handlerServer;
    }

    @Override
    public void onStart(Context context) {
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        registerLocationListener();
    }

    @Override
    public void onStop(Context context) {
        locationManager = null;
        unregisterLocationListener();
    }

    @Override
    public void onSharedPreferenceChanged(Context context, SharedPreferences preferences, String key) {
        if (PreferencesUtils.isKey(context, R.string.min_recording_interval_key, key)) {
            int minRecordingInterval = PreferencesUtils.getMinRecordingInterval(context);
            if (minRecordingInterval == PreferencesUtils.getMinRecordingIntervalAdaptBatteryLife(context)) {
                // Choose battery life over moving time accuracy.
                locationListenerPolicy = new AdaptiveLocationListenerPolicy(30 * UnitConversions.ONE_SECOND_MS, 5 * UnitConversions.ONE_MINUTE_MS, 5);
            } else if (minRecordingInterval == PreferencesUtils.getMinRecordingIntervalAdaptAccuracy(context)) {
                // Get all the updates.
                locationListenerPolicy = new AdaptiveLocationListenerPolicy(UnitConversions.ONE_SECOND_MS, 30 * UnitConversions.ONE_SECOND_MS, 0);
            } else {
                locationListenerPolicy = new AbsoluteLocationListenerPolicy(minRecordingInterval * UnitConversions.ONE_SECOND_MS);
            }

            if (locationManager != null) {
                registerLocationListener();
            }
        }
        if (PreferencesUtils.isKey(context, R.string.recording_gps_accuracy_key, key)) {
            recordingGpsAccuracy = PreferencesUtils.getRecordingGPSAccuracy(context);
        }
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        // TODO do we still need to process the location processing in an asynchronous manner? Let's go to check it out.
        computeLocation(location);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(@NonNull String provider) {
    }

    @Override
    public void onProviderDisabled(@NonNull String provider) {
    }

    /**
     * Checks if location is valid and builds a track point that will be send through HandlerServer.
     *
     * @param location {@link Location} object.
     */
    private void computeLocation(Location location) {
        if (!LocationUtils.isValidLocation(location)) {
            Log.w(TAG, "Ignore newTrackPoint. location is invalid.");
            return;
        }

        TrackPoint trackPoint = new TrackPoint(location);

        if (!TrackPointUtils.fulfillsAccuracy(trackPoint, recordingGpsAccuracy)) {
            Log.d(TAG, "Ignore newTrackPoint. Poor accuracy.");
            return;
        }

        long idleTime = 0L;
        if (TrackPointUtils.after(trackPoint, lastValidTrackPoint)) {
            idleTime = trackPoint.getTime() - lastValidTrackPoint.getTime();
        }

        locationListenerPolicy.updateIdleTime(idleTime);
        if (currentRecordingInterval != locationListenerPolicy.getDesiredPollingInterval()) {
            registerLocationListener();
        }

        lastValidTrackPoint = trackPoint;
        handlerServer.sendTrackPoint(trackPoint, recordingGpsAccuracy);
    }

    private void registerLocationListener() {
        if (locationManager == null) {
            Log.e(TAG, "locationManager is null.");
            return;
        }
        try {
            long interval = locationListenerPolicy.getDesiredPollingInterval();
            currentRecordingInterval = interval;
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, interval, locationListenerPolicy.getMinDistance_m(), this);
        } catch (SecurityException e) {
            Log.e(TAG, "Could not register location listener; permissions not granted.", e);
        }
    }

    private void unregisterLocationListener() {
        if (locationManager == null) {
            Log.e(TAG, "locationManager is null.");
            return;
        }
        locationManager.removeUpdates(this);
        locationManager = null;
    }
}
