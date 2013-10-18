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

import com.google.android.apps.mytracks.util.GoogleLocationUtils;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient.ConnectionCallbacks;
import com.google.android.gms.common.GooglePlayServicesClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.common.annotations.VisibleForTesting;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

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
      isAllowed = isUseLocationForServicesOn();
    }
  }

  private static final String TAG = MyTracksLocationManager.class.getSimpleName();

  private static final String GOOGLE_SETTINGS_CONTENT_URI = "content://com.google.settings/partner";
  private static final String USE_LOCATION_FOR_SERVICES = "use_location_for_services";

  // User has agreed to use location for Google services.
  @VisibleForTesting
  static final String USE_LOCATION_FOR_SERVICES_ON = "1";

  private static final String NAME = "name";
  private static final String VALUE = "value";

  private final ConnectionCallbacks connectionCallbacks = new ConnectionCallbacks() {
      @Override
    public void onDisconnected() {}

      @Override
    public void onConnected(Bundle bunlde) {
      handler.post(new Runnable() {
          @Override
        public void run() {
          if (requestLastLocation != null && locationClient.isConnected()) {
            requestLastLocation.onLocationChanged(locationClient.getLastLocation());
            requestLastLocation = null;
          }
          if (requestLocationUpdates != null && locationClient.isConnected()) {
            LocationRequest locationRequest = new LocationRequest().setPriority(
                LocationRequest.PRIORITY_HIGH_ACCURACY).setInterval(requestLocationUpdatesTime)
                .setFastestInterval(requestLocationUpdatesTime)
                .setSmallestDisplacement(requestLocationUpdatesDistance);
            locationClient.requestLocationUpdates(
                locationRequest, requestLocationUpdates, handler.getLooper());
          }
        }
      });
    }
  };

  private final OnConnectionFailedListener
      onConnectionFailedListener = new OnConnectionFailedListener() {
          @Override
        public void onConnectionFailed(ConnectionResult connectionResult) {}
      };

  private final Handler handler;
  private final LocationClient locationClient;
  private final LocationManager locationManager;
  private final ContentResolver contentResolver;
  private final GoogleSettingsObserver observer;

  private boolean isAvailable;
  private boolean isAllowed;
  private LocationListener requestLastLocation;
  private LocationListener requestLocationUpdates;
  private float requestLocationUpdatesDistance;
  private long requestLocationUpdatesTime;

  public MyTracksLocationManager(Context context, Looper looper, boolean enableLocaitonClient) {
    this.handler = new Handler(looper);

    if (enableLocaitonClient) {
      locationClient = new LocationClient(context, connectionCallbacks, onConnectionFailedListener);
      locationClient.connect();
    } else {
      locationClient = null;
    }

    locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    contentResolver = context.getContentResolver();
    observer = new GoogleSettingsObserver(handler);

    isAvailable = GoogleLocationUtils.isAvailable(context);
    isAllowed = isUseLocationForServicesOn();

    contentResolver.registerContentObserver(
        Uri.parse(GOOGLE_SETTINGS_CONTENT_URI + "/" + USE_LOCATION_FOR_SERVICES), false, observer);
  }

  /**
   * Closes the {@link MyTracksLocationManager}.
   */
  public void close() {
    if (locationClient != null) {
      locationClient.disconnect();
    }
    contentResolver.unregisterContentObserver(observer);
  }

  /**
   * Returns true if allowed to access the location manager. Returns true if
   * there is no Google location settings or the Google location settings allows
   * access to location data.
   */
  public boolean isAllowed() {
    return isAllowed;
  }

  /**
   * Returns true if gps provider is enabled.
   */
  public boolean isGpsProviderEnabled() {
    if (!isAllowed) {
      return false;
    }
    String provider = LocationManager.GPS_PROVIDER;
    if (locationManager.getProvider(provider) == null) {
      return false;
    }
    return locationManager.isProviderEnabled(provider);
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
        if (!isAllowed) {
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
   * @param minTime the minimal time
   * @param minDistance the minimal distance
   * @param locationListener the location listener
   */
  public void requestLocationUpdates(
      final long minTime, final float minDistance, final LocationListener locationListener) {
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
  public void removeLocationUpdates(final LocationListener locationListener) {
    handler.post(new Runnable() {
        @Override
      public void run() {
        requestLocationUpdates = null;
        if (locationClient != null && locationClient.isConnected()) {
          locationClient.removeLocationUpdates(locationListener);
        }
      }
    });
  }

  /**
   * Returns true if the Google location settings for
   * {@link #USE_LOCATION_FOR_SERVICES} is on.
   */
  private boolean isUseLocationForServicesOn() {
    if (!isAvailable) {
      return true;
    }
    Cursor cursor = null;
    try {
      cursor = contentResolver.query(Uri.parse(GOOGLE_SETTINGS_CONTENT_URI), new String[] { VALUE },
          NAME + "=?", new String[] { USE_LOCATION_FOR_SERVICES }, null);
      if (cursor != null && cursor.moveToNext()) {
        return USE_LOCATION_FOR_SERVICES_ON.equals(cursor.getString(0));
      }
    } catch (RuntimeException e) {
      Log.w(TAG, "Failed to read " + USE_LOCATION_FOR_SERVICES, e);
    } finally {
      if (cursor != null) {
        cursor.close();
      }
    }
    return false;
  }
}
