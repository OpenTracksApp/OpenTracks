/*
 * Copyright 2011 Google Inc.
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

package com.google.android.apps.mytracks.content;

import static com.google.android.apps.mytracks.Constants.MAX_LOCATION_AGE_MS;

import com.google.android.apps.mytracks.Constants;
import com.google.android.apps.mytracks.services.MyTracksLocationManager;
import com.google.android.apps.mytracks.util.GoogleLocationUtils;
import com.google.android.maps.mytracks.R;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.ContentObserver;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

/**
 * Data source on the phone.
 * 
 * @author Rodrigo Damazio
 */
public class DataSource {

  private static final int NETWORK_PROVIDER_MIN_TIME = 5 * 60 * 1000; // 5 minutes
  private static final String TAG = DataSource.class.getSimpleName();

  private final Context context;
  private final ContentResolver contentResolver;
  private final MyTracksLocationManager myTracksLocationManager;
  private final SharedPreferences sharedPreferences;
  
  public DataSource(Context context) {
    this.context = context;
    contentResolver = context.getContentResolver();
    myTracksLocationManager = new MyTracksLocationManager(context);
    sharedPreferences = context.getSharedPreferences(Constants.SETTINGS_NAME, Context.MODE_PRIVATE);
  }

  public void close() {
    myTracksLocationManager.close();
  }

  public boolean isAllowed() {
    return myTracksLocationManager.isAllowed();
  }

  public boolean isGpsProviderEnabled() {
    return myTracksLocationManager.isGpsProviderEnabled();
  }

  /**
   * Registers a content observer.
   * 
   * @param uri the uri
   * @param observer the observer
   */
  public void registerContentObserver(Uri uri, ContentObserver observer) {
    contentResolver.registerContentObserver(uri, false, observer);
  }

  /**
   * Unregisters a content observer.
   * 
   * @param observer the observer
   */
  public void unregisterContentObserver(ContentObserver observer) {
    contentResolver.unregisterContentObserver(observer);
  }

  /**
   * Registers a location listener.
   * 
   * @param listener the listener
   */
  public void registerLocationListener(LocationListener listener) {

    // Listen for GPS location
    myTracksLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, listener);

    // Listen for network location
    try {
      myTracksLocationManager.requestLocationUpdates(
          LocationManager.NETWORK_PROVIDER, NETWORK_PROVIDER_MIN_TIME, 0, listener);
    } catch (RuntimeException e) {
      // Network location is optional, so just log the exception
      Log.w(TAG, "Could not register for network location.", e);
    }
  }

  /**
   * Unregisters a location listener.
   * 
   * @param listener the listener
   */
  public void unregisterLocationListener(LocationListener listener) {
    myTracksLocationManager.removeUpdates(listener);
  }

  /**
   * Gets the last known location.
   */
  public Location getLastKnownLocation() {
    Location location = myTracksLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
    if (!isLocationRecent(location)) {
      // Try network location
      location = myTracksLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
      String toast;
      if (isLocationRecent(location)) {
        toast = context.getString(R.string.my_location_approximate_location);
      } else {
        String setting = context.getString(
            GoogleLocationUtils.isAvailable(context) ? R.string.gps_google_location_settings
                : R.string.gps_location_access);
        toast = context.getString(R.string.my_location_no_gps, setting);
      }
      Toast.makeText(context, toast, Toast.LENGTH_LONG).show();
    }
    return location;
  }

  /**
   * Returns true if the location is recent.
   * 
   * @param location the location
   */
  private boolean isLocationRecent(Location location) {
    if (location == null) {
      return false;
    }
    return location.getTime() > System.currentTimeMillis() - MAX_LOCATION_AGE_MS;
  }

  /**
   * Registers a shared preference change listener.
   * 
   * @param listener the listener
   */
  public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
    sharedPreferences.registerOnSharedPreferenceChangeListener(listener);
  }

  /**
   * Unregisters a shared preference change listener.
   * 
   * @param listener the listener
   */
  public void unregisterOnSharedPreferenceChangeListener(
      OnSharedPreferenceChangeListener listener) {
    sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener);
  }
}