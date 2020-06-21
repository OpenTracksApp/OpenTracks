package de.dennisguse.opentracks.services;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.util.PreferencesUtils;
import de.dennisguse.opentracks.util.UnitConversions;

public class LocationHandler implements LocationListener {

    private String TAG = LocationHandler.class.getSimpleName();

    private LocationManager locationManager;
    private Context context;
    private LocationListenerPolicy locationListenerPolicy;

    private final SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences preferences, String key) {
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
        }
    };

    public void onStart(Context context) {
        this.context = context;
        PreferencesUtils.register(context, sharedPreferenceChangeListener);
        sharedPreferenceChangeListener.onSharedPreferenceChanged(null, null);
        locationManager = (LocationManager) this.context.getSystemService(Context.LOCATION_SERVICE);
        registerLocationListener();
    }

    public void onStop() {
        PreferencesUtils.unregister(context, sharedPreferenceChangeListener);
        unregisterLocationListener();
        locationManager = null;
    }

    private void registerLocationListener() {
        if (locationManager == null) {
            Log.e(TAG, "locationManager is null.");
            return;
        }
        try {
            long interval = locationListenerPolicy.getDesiredPollingInterval();
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, interval, locationListenerPolicy.getMinDistance_m(), this);
        } catch (SecurityException e) {
            Log.e(TAG, "Could not register location listener; permissions not granted.", e);
        }
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        new Thread(() -> onLocationChangedAsync(location)).run();
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

    private void onLocationChangedAsync(Location location) {
        HandlerServer.getInstance(context).sendLocation(location);
    }

    private void unregisterLocationListener() {
        if (locationManager == null) {
            Log.e(TAG, "locationManager is null.");
            return;
        }
        locationManager.removeUpdates(this);
    }
}
