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

import android.location.Location;

/**
 * Listener to be invoked when {@link DataSource} changes.
 * 
 * @author Jimmy Shih
 */
public interface DataSourceListener {

  /**
   * Notifies when the tracks table is updated.
   */
  public void notifyTracksTableUpdated();

  /**
   * Notifies when the waypoints table is updated.
   */
  public void notifyWaypointsTableUpdated();

  /**
   * Notifies when the track points table is updated.
   */
  public void notifyTrackPointsTableUpdated();

  /**
   * Notifies when the current location changes.
   * 
   * @param location the location
   */
  public void notifyLocationChanged(Location location);
  
  
  /**
   * Notifies when the location provider enabled status changes.
   * 
   * @param enabled true if enabled, false if not
   */
  public void notifyLocationProviderEnabled(boolean enabled);

  /**
   * Notifies when the location provider availability status changes.
   * 
   * @param available true if available, false if not
   */
  public void notifyLocationProviderAvailable(boolean available);

  /**
   * Notifies when the compass heading changes.
   * 
   * @param heading the new heading
   */
  public void notifyHeadingChanged(float heading);
  
  /**
   * Notifies when a preference changes.
   * 
   * @param key the preference key
   */
  public void notifyPreferenceChanged(String key);
}