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

import com.google.android.apps.mytracks.Constants;
import com.google.android.apps.mytracks.services.MyTracksLocationManager;
import com.google.android.gms.location.LocationListener;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Looper;

/**
 * Data source on the phone.
 * 
 * @author Rodrigo Damazio
 */
public class DataSource {

  private final ContentResolver contentResolver;
  private final MyTracksLocationManager myTracksLocationManager;
  private final SharedPreferences sharedPreferences;
  
  public DataSource(Context context) {
    contentResolver = context.getContentResolver();
    myTracksLocationManager = new MyTracksLocationManager(context, Looper.myLooper());
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
    myTracksLocationManager.requestLocationUpdates(0, 0, listener);
  }

  /**
   * Unregisters a location listener.
   * 
   * @param listener the listener
   */
  public void unregisterLocationListener(LocationListener listener) {
    myTracksLocationManager.removeLocationUpdates(listener);
  }

  /**
   * Request last location.
   */
  public void requestLastLocation(LocationListener locationListener) {
    myTracksLocationManager.requestLastLocation(locationListener);
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