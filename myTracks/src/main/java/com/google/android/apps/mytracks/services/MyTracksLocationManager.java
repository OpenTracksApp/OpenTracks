/*
 * Copyright 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.android.apps.mytracks.services;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.Nullable;

import com.google.android.apps.mytracks.util.GoogleLocationUtils;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;


/**
 * My Tracks Location Manager. Applies Google location settings before allowing
 * access to {@link LocationManager}.
 *
 * @author Jimmy Shih
 */
public class MyTracksLocationManager {

    /**
     * Observer for Google location settings.
     *
     * @author Jimmy Shih
     */
    private class GoogleSettingsObserver extends ContentObserver {

        public GoogleSettingsObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            isAllowed = GoogleLocationUtils.isAllowed(context);
        }
    }

    private final ConnectionCallbacks connectionCallbacks = new ConnectionCallbacks() {
        @Override
        public void onConnected(@Nullable Bundle bundle) {
            handler.post(new Runnable() {
                //Permissions should be already acquired by TrackListActivity during start up.
                @SuppressLint("MissingPermission")
                @Override
                public void run() {
                    if (requestLastLocation != null) {
                        requestLastLocation.onLocationChanged(locationClient.getLastLocation().getResult());
                        requestLastLocation = null;
                    }
                    if (requestLocationUpdates != null) {
                        LocationRequest locationRequest = new LocationRequest()
                                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                                .setInterval(requestLocationUpdatesTime)
                                .setFastestInterval(requestLocationUpdatesTime)
                                .setSmallestDisplacement(requestLocationUpdatesDistance);
                        locationClient.requestLocationUpdates(locationRequest, requestLocationUpdates, handler.getLooper());
                    }
                }
            });
        }

        @Override
        public void onConnectionSuspended(int i) {
            handler.post(new Runnable() {
                @SuppressLint("MissingPermission")
                @Override
                public void run() {
                    if (requestLastLocation != null) {
                        locationClient.getLastLocation().addOnSuccessListener(
                                new OnSuccessListener<Location>() {
                                    @Override
                                    public void onSuccess(Location location) {
                                        // GPS location can be null if GPS is switched off
                                        if (location != null) {
                                            requestLastLocation.onLocationChanged(location);
                                        }
                                        requestLastLocation = null;
                                    }
                                });
                    }
                    if (requestLocationUpdates != null) {
                        LocationRequest locationRequest = new LocationRequest()
                                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                                .setInterval(requestLocationUpdatesTime)
                                .setFastestInterval(requestLocationUpdatesTime)
                                .setSmallestDisplacement(requestLocationUpdatesDistance);

                        locationClient.requestLocationUpdates(locationRequest, requestLocationUpdates, handler.getLooper());
                    }
                }
            });
        }
    };
    private final Context context;
    private final Handler handler;
    private final FusedLocationProviderClient locationClient;
    private final LocationManager locationManager;
    private final ContentResolver contentResolver;
    private final GoogleSettingsObserver observer;

    private boolean isAllowed;
    private LocationListener requestLastLocation;
    private LocationCallback requestLocationUpdates;
    private float requestLocationUpdatesDistance;
    private long requestLocationUpdatesTime;

    public MyTracksLocationManager(Context context, Looper looper, boolean enableLocationClient) {
        this.context = context;
        this.handler = new Handler(looper);

        if (enableLocationClient) {
            locationClient = LocationServices.getFusedLocationProviderClient(context);
        } else {
            locationClient = null;
        }

        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        contentResolver = context.getContentResolver();
        observer = new GoogleSettingsObserver(handler);

        isAllowed = GoogleLocationUtils.isAllowed(context);

        contentResolver.registerContentObserver(
                GoogleLocationUtils.USE_LOCATION_FOR_SERVICES_URI, false, observer);
    }

    /**
     * Closes the {@link MyTracksLocationManager}.
     */
    public void close() {
        contentResolver.unregisterContentObserver(observer);
    }

    /**
     * Returns true if allowed to access the location manager. Returns true if
     * there is no enforcement or the Google location settings allows access to
     * location data.
     */
    public boolean isAllowed() {
        return isAllowed;
    }

    /**
     * Returns true if gps provider is enabled.
     */
    public boolean isGpsProviderEnabled() {
        if (!isAllowed()) {
            return false;
        }
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    /**
     * Request last location.
     *
     * @param locationListener location listener
     */
    public void requestLastLocation(final LocationListener locationListener) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (!isAllowed()) {
                    requestLastLocation = null;
                    locationListener.onLocationChanged(null);
                } else {
                    requestLastLocation = locationListener;
                    connectionCallbacks.onConnected(null);
                }
            }
        });
    }

    /**
     * Requests location updates. This is an ongoing request, thus the caller
     * needs to check the status of {@link #isAllowed}.
     *
     * @param minTime          the minimal time
     * @param minDistance      the minimal distance
     * @param locationListener the location listener
     */
    public void requestLocationUpdates(
            final long minTime, final float minDistance, final LocationCallback locationListener) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                requestLocationUpdatesTime = minTime;
                requestLocationUpdatesDistance = minDistance;
                requestLocationUpdates = locationListener;
                connectionCallbacks.onConnected(null);
            }
        });
    }

    /**
     * Removes location updates.
     *
     * @param locationListener the location listener
     */
    public void removeLocationUpdates(final LocationCallback locationListener) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                requestLocationUpdates = null;
                if (locationClient != null) {
                    locationClient.removeLocationUpdates(locationListener);
                }
            }
        });
    }
}
