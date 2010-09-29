/*
 * Copyright 2009 Google Inc.
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

import com.google.android.apps.mytracks.util.MyTracksUtils;

import android.location.Location;

/**
 * A buffer of Locations from a track.
 * This class contains some additional information about the loaded points.
 *
 * @author Sandor Dornbush
 */
public class TrackBuffer {

  /**
   * An array of location objects that are part of a track.
   */
  private final Location[] buffer;

  /**
   * The id of the last location read from the track.
   */
  private long lastLocationRead = 0;

  /**
   * The number of locations loaded into the buffer.
   */
  private int locationsLoaded = 0;

  /**
   * Create a TrackBuffer with size elements.
   *
   * @param size The size of the buffer
   */
  public TrackBuffer(int size) {
    buffer = new Location[size];
  }

  /**
   * Reset the track to a state with no locations.
   */
  public void reset() {
    resetAt(0);
  }

  /**
   * Reset the track at the given starting location id.
   */
  public void resetAt(long lastLocation) {
    lastLocationRead = lastLocation;
    locationsLoaded = 0;
  }

  public void setInvalid() {
    lastLocationRead = Integer.MAX_VALUE;
  }

  /**
   * @return The number of locations that can be stored in this buffer
   */
  public int getSize() {
    return buffer.length;
  }

  /**
   * @param index The index of the location to fetch
   * @return The location for the given index
   */
  public Location get(int index) {
    return buffer[index];
  }

  /**
   * Adds a location to the end of the buffer.
   * @param location The location to add.
   * @param id The id of the location to be added.
   */
  public void add(Location location, long id) {
    buffer[locationsLoaded++] = location;
    lastLocationRead = Math.max(lastLocationRead, id);
  }

  /**
   * @return The id of the last location loaded into the buffer
   */
  public long getLastLocationRead() {
    return lastLocationRead;
  }

  /**
   * @return The number of locations loaded into the buffer.
   */
  public int getLocationsLoaded() {
    return locationsLoaded;
  }

  /**
   * Finds the start location, i.e. the one which is the first point of a
   * segment with at least two points.
   *
   * @return the start location
   */
  public Location findStartLocation() {
    int numValidLocations = 0;
    for (int i = 0; i < getLocationsLoaded(); i++) {
      Location location = buffer[i];
      if (MyTracksUtils.isValidLocation(location)) {
        numValidLocations++;
        if (numValidLocations == 2) {
          return buffer[i - 1];
        }
      } else {
        numValidLocations = 0;
      }
    }
    return null;
  }
}
