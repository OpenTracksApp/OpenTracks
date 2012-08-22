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
import com.google.common.annotations.VisibleForTesting;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;

/**
 * My Tracks Location Manager. Applies Google location settings before allowing
 * access to {@link LocationManager}.
 *
 * @author Jimmy Shih
 */
public class MyTracksLocationManager {

  private static final String TAG = MyTracksLocationManager.class.getSimpleName();

  private static final String GOOGLE_SETTINGS_CONTENT_URI = "content://com.google.settings/partner";
  private static final String USE_LOCATION_FOR_SERVICES = "use_location_for_services";
  
  // User has agreed to use location for Google services.
  @VisibleForTesting
  static final String USE_LOCATION_FOR_SERVICES_ON = "1";

  private static final String NAME = "name";
  private static final String VALUE = "value";

  private final LocationManager locationManager;
  private final ContentResolver contentResolver;
  private final GoogleSettingsObserver observer;
  private boolean isAvailable;
  private boolean isAllowed;

  public MyTracksLocationManager(Context context) {
    locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    contentResolver = context.getContentResolver();
    observer = new GoogleSettingsObserver();
    isAvailable = GoogleLocationUtils.isAvailable(context);
    isAllowed = isUseLocationForServicesOn();

    contentResolver.registerContentObserver(
        Uri.parse(GOOGLE_SETTINGS_CONTENT_URI + "/" + USE_LOCATION_FOR_SERVICES), false, observer);
  }

  /**
   * Closes the {@link MyTracksLocationManager}.
   */
  public void close() {
    contentResolver.unregisterContentObserver(observer);
  }

  /**
   * Observer for Google location settings.
   *
   * @author Jimmy Shih
   */
  private class GoogleSettingsObserver extends ContentObserver {

    public GoogleSettingsObserver() {
      super(new Handler());
    }

    @Override
    public void onChange(boolean selfChange) {
      isAllowed = isUseLocationForServicesOn();
    }
  }

  /**
   * Returns true if the Google location settings allows access to location
   * data.
   */
  public boolean isAllowed() {
    return isAllowed;
  }

  /**
   * @see android.location.LocationManager#isProviderEnabled(java.lang.String)
   */
  public boolean isProviderEnabled(String provider) {
    return isAllowed ? locationManager.isProviderEnabled(provider) : false;
  }

  /**
   * @see android.location.LocationManager#getProvider(java.lang.String)
   */
  public LocationProvider getProvider(String name) {
    return isAllowed ? locationManager.getProvider(name) : null;
  }

  /**
   * @see android.location.LocationManager#getLastKnownLocation(java.lang.String)
   */
  public Location getLastKnownLocation(String provider) {
    return isAllowed ? locationManager.getLastKnownLocation(provider) : null;
  }

  /**
   * @see android.location.LocationManager#requestLocationUpdates(java.lang.String,
   *      long, float, android.location.LocationListener)
   */
  public void requestLocationUpdates(
      String provider, long minTime, float minDistance, LocationListener listener) {
    locationManager.requestLocationUpdates(provider, minTime, minDistance, listener);
  }

  /**
   * @param listener
   * @see android.location.LocationManager#removeUpdates(android.location.LocationListener)
   */
  public void removeUpdates(LocationListener listener) {
    locationManager.removeUpdates(listener);
  }

  /**
   * Returns true if the Google location settings for
   * {@link #USE_LOCATION_FOR_SERVICES} is on.
   */
  private boolean isUseLocationForServicesOn() {
    if (!isAvailable) { return true; }
    Cursor cursor = null;
    try {
      cursor = contentResolver.query(Uri.parse(GOOGLE_SETTINGS_CONTENT_URI), new String[] { VALUE },
          NAME + "=?", new String[] { USE_LOCATION_FOR_SERVICES }, null);
      if (cursor != null && cursor.moveToNext()) { return USE_LOCATION_FOR_SERVICES_ON.equals(
          cursor.getString(0)); }
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
