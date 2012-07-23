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

import java.util.EnumSet;

/**
 * State for a registered {@link TrackDataListener}.
 * 
 * @author Jimmy Shih
 */
public class ListenerState {
  
  private TrackDataListener trackDataListener;
  private EnumSet<TrackDataType> trackDataTypes;

  private long lastTrackId;
  private long lastPointId;
  private int lastSamplingFrequency;
  private int numberOfLoadedPoints;

  public ListenerState(
      TrackDataListener trackDataListener, EnumSet<TrackDataType> trackDataTypes) {
    this.trackDataListener = trackDataListener;
    this.trackDataTypes = trackDataTypes;
  }

  /**
   * Gets the {@link TrackDataListener}.
   */
  public TrackDataListener getTrackDataListener() {
    return trackDataListener;
  }

  /**
   * Gets the track data types.
   */
  public EnumSet<TrackDataType> getTrackDataTypes() {
    return trackDataTypes;
  }

  /**
   * Gets the last track id.
   */
  public long getLastTrackId() {
    return lastTrackId;
  }

  /**
   * Gets the last point id.
   */
  public long getLastPointId() {
    return lastPointId;
  }

  /**
   * Gets the last sampling frequency.
   */
  public int getLastSamplingFrequency() {
    return lastSamplingFrequency;
  }

  /**
   * Get the number of loaded points.
   */
  public int getNumberOfLoadedPoints() {
    return numberOfLoadedPoints;
  }

  /**
   * Sets the listener state.
   * 
   * @param trackId track id
   * @param pointId point id
   * @param samplingFrequency sampling frequency
   * @param loadedPoints number of loaded points
   */
  public void setState(long trackId, long pointId, int samplingFrequency, int loadedPoints) {
    lastTrackId = trackId;
    lastPointId = pointId;
    lastSamplingFrequency = samplingFrequency;
    numberOfLoadedPoints = loadedPoints;
  }

  /**
   * Resets state.
   */
  public void resetState() {
    setState(0L, 0L, 0, 0);
  }
}