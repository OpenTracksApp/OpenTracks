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
import android.content.Context;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Handler;
import android.os.Looper;

/**
 * Handles connection to {@link LocationManager}.
 *
 * @author Jimmy Shih
 */
public class LocationManagerConnector {

    private final Handler handler;
    private final LocationManager locationManager;

    private LocationListener requestLocationUpdates;
    private float requestLocationUpdatesDistance;
    private long requestLocationUpdatesTime;

    public LocationManagerConnector(Context context, Looper looper) {
        this.handler = new Handler(looper);

        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }

    /**
     * Requests location updates.
     *
     * @param minTime          the minimal time
     * @param minDistance      the minimal distance
     * @param locationListener the location listener
     */
    public void requestLocationUpdates(final long minTime, final float minDistance, final LocationListener locationListener) {
        handler.post(new Runnable() {
            @Override
            @SuppressLint("MissingPermission")
            public void run() {
                requestLocationUpdatesTime = minTime;
                requestLocationUpdatesDistance = minDistance;
                requestLocationUpdates = locationListener;
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, requestLocationUpdatesTime, requestLocationUpdatesDistance, locationListener);
            }
        });
    }

    /**
     * Removes location updates.
     *
     * @param locationListener the location listener
     */
    public void removeLocationUpdates(final LocationListener locationListener) {
        locationManager.removeUpdates(locationListener);
        requestLocationUpdates = null;
    }
}
