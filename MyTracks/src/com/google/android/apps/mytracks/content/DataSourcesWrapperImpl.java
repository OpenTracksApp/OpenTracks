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
import static com.google.android.apps.mytracks.Constants.MAX_NETWORK_AGE_MS;

import com.google.android.apps.mytracks.Constants;
import com.google.android.apps.mytracks.services.MyTracksLocationManager;
import com.google.android.apps.mytracks.util.GoogleLocationUtils;
import com.google.android.maps.mytracks.R;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.ContentObserver;
import android.hardware.Sensor;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

/**
 * Real implementation of the data sources, which talks to system services.
 *
 * @author Rodrigo Damazio
 */
class DataSourcesWrapperImpl implements DataSourcesWrapper {
  // System services
  private final SensorManager sensorManager;
  private final MyTracksLocationManager myTracksLocationManager;
  private final ContentResolver contentResolver;
  private final SharedPreferences sharedPreferences;
  private final Context context;

  DataSourcesWrapperImpl(Context context, SharedPreferences sharedPreferences) {
    this.context = context;
    this.sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
    this.myTracksLocationManager = new MyTracksLocationManager(context);
    this.contentResolver = context.getContentResolver();
    this.sharedPreferences = sharedPreferences;
  }

  @Override
  public void close() {
    myTracksLocationManager.close();    
  }
  
  @Override
  public boolean isAllowed() {
    return myTracksLocationManager.isAllowed();
  }
  
  @Override
  public void registerOnSharedPreferenceChangeListener(
      OnSharedPreferenceChangeListener listener) {
    sharedPreferences.registerOnSharedPreferenceChangeListener(listener);
  }

  @Override
  public void unregisterOnSharedPreferenceChangeListener(
      OnSharedPreferenceChangeListener listener) {
    sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener);
  }

  @Override
  public void registerContentObserver(Uri contentUri, boolean descendents,
      ContentObserver observer) {
    contentResolver.registerContentObserver(contentUri, descendents, observer);
  }

  @Override
  public void unregisterContentObserver(ContentObserver observer) {
    contentResolver.unregisterContentObserver(observer);
  }

  @Override
  public Sensor getSensor(int type) {
    return sensorManager.getDefaultSensor(type);
  }

  @Override
  public void registerSensorListener(SensorEventListener listener,
      Sensor sensor, int sensorDelay) {
    sensorManager.registerListener(listener, sensor, sensorDelay);
  }

  @Override
  public void unregisterSensorListener(SensorEventListener listener) {
    sensorManager.unregisterListener(listener);
  }

  @Override
  public boolean isLocationProviderEnabled(String provider) {
    return myTracksLocationManager.isProviderEnabled(provider);
  }

  @Override
  public void requestLocationUpdates(LocationListener listener) {
    // Check if the provider exists.
    LocationProvider gpsProvider = myTracksLocationManager.getProvider(LocationManager.GPS_PROVIDER);
    if (gpsProvider == null) {
      listener.onProviderDisabled(LocationManager.GPS_PROVIDER);
      myTracksLocationManager.removeUpdates(listener);
      return;
    }

    // Listen to GPS location.
    String providerName = gpsProvider.getName();
    Log.d(Constants.TAG, "TrackDataHub: Using location provider " + providerName);
    myTracksLocationManager.requestLocationUpdates(providerName,
        0 /*minTime*/, 0 /*minDist*/, listener);

    // Give an initial update on provider state.
    if (myTracksLocationManager.isProviderEnabled(providerName)) {
      listener.onProviderEnabled(providerName);
    } else {
      listener.onProviderDisabled(providerName);
    }

    // Listen to network location
    try {
      myTracksLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
          1000 * 60 * 5 /*minTime*/, 0 /*minDist*/, listener);
    } catch (RuntimeException e) {
      // If anything at all goes wrong with getting a cell location do not
      // abort. Cell location is not essential to this app.
      Log.w(Constants.TAG, "Could not register network location listener.", e);
    }
  }

  @Override
  public Location getLastKnownLocation() {
    // TODO: Let's look at more advanced algorithms to determine the best
    // current location.

    Location loc = myTracksLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
    final long now = System.currentTimeMillis();
    if (loc == null || loc.getTime() < now - MAX_LOCATION_AGE_MS) {
      // We don't have a recent GPS fix, just use cell towers if available
      loc = myTracksLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

      String toast = context.getString(R.string.my_location_approximate_location);
      if (loc == null || loc.getTime() < now - MAX_NETWORK_AGE_MS) {
        // We don't have a recent cell tower location, let the user know:
        String setting = context.getString(
            GoogleLocationUtils.isAvailable(context) ? R.string.gps_google_location_settings
                : R.string.gps_location_access);
        toast = context.getString(R.string.my_location_no_location, setting);
      }

      // Let the user know we have only an approximate location:
     Toast.makeText(context, toast, Toast.LENGTH_LONG).show();
    }
    return loc;
  }

  @Override
  public void removeLocationUpdates(LocationListener listener) {
    myTracksLocationManager.removeUpdates(listener);
  }
}