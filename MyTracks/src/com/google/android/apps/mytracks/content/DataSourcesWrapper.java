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

import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.ContentObserver;
import android.hardware.Sensor;
import android.hardware.SensorEventListener;
import android.location.Location;
import android.location.LocationListener;
import android.net.Uri;

/**
 * Interface for abstracting registration of external data source listeners.
 *
 * @author Rodrigo Damazio
 */
interface DataSourcesWrapper {
  // Preferences
  void registerOnSharedPreferenceChangeListener(
      OnSharedPreferenceChangeListener listener);
  void unregisterOnSharedPreferenceChangeListener(
      OnSharedPreferenceChangeListener listener);

  // Content provider
  void registerContentObserver(Uri contentUri, boolean descendents,
      ContentObserver observer);
  void unregisterContentObserver(ContentObserver observer);

  // Sensors
  Sensor getSensor(int type);
  void registerSensorListener(SensorEventListener listener,
      Sensor sensor, int sensorDelay);
  void unregisterSensorListener(SensorEventListener listener);

  // Location
  boolean isLocationProviderEnabled(String provider);
  void requestLocationUpdates(LocationListener listener);
  void removeLocationUpdates(LocationListener listener);
  Location getLastKnownLocation();
} 