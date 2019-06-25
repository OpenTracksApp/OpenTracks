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
 * Listener for track data changes.
 * 
 * @author Rodrigo Damazio
 */
public interface TrackDataListener {

  /**
   * Location state.
   * 
   * @author Jimmy Shih
   */
  enum LocationState {
    DISABLED, NO_FIX, BAD_FIX, GOOD_FIX
  }

  /**
   * Called when the track or its statistics has been updated.
   * 
   * @param track the track
   */
  void onTrackUpdated(Track track);

  /**
   * Called to clear previously-sent track points.
   */
  void clearTrackPoints();

  /**
   * Called when a sampled in track point is read.
   * 
   * @param location the location
   */
  void onSampledInTrackPoint(Location location);

  /**
   * Called when a sampled out track point is read.
   * 
   * @param location the location
   */
  void onSampledOutTrackPoint(Location location);

  /**
   * Called when an invalid track point representing a segment split is read.
   */
  void onSegmentSplit(Location location);

  /**
   * Called when finish sending new track points. This gets called after every
   * batch of calls to {@link #onSampledInTrackPoint(Location)},
   * {@link #onSampledOutTrackPoint(Location)} and
   * {@link #onSegmentSplit(Location)}.
   */
  void onNewTrackPointsDone();

  /**
   * Called to clear previously sent waypoints.
   */
  void clearWaypoints();

  /**
   * Called when a new waypoint is read.
   * 
   * @param waypoint the waypoint
   */
  void onNewWaypoint(Waypoint waypoint);

  /**
   * Called when finish sending new waypoints. This gets called after every
   * batch of calls to {@link #clearWaypoints()} and
   * {@link #onNewWaypoint(Waypoint)}.
   */
  void onNewWaypointsDone();

  /**
   * Called when the metric units preference value is change.
   * 
   * @param metricUnits true to use metric units, false to use imperial units
   * @return true to reload all the data, false otherwise.
   */
  boolean onMetricUnitsChanged(boolean metricUnits);

  /**
   * Called when the report speed preference value is changed.
   * 
   * @param reportSpeed true to report speed, false to report pace
   * @return true to reload all the data, false otherwise.
   */
  boolean onReportSpeedChanged(boolean reportSpeed);

  /**
   * Called when the recording gps accuracy preference value is changed.
   * 
   * @param recordingGpsAccuracy the new value
   * @return true to reload all the data, false otherwise
   */
  boolean onRecordingGpsAccuracy(int recordingGpsAccuracy);
  
  /**
   * Called when the recording distance interval preference value is changed.
   * 
   * @param recordingDistanceInterval the new value
   * @return true to reload all the data, false otherwise.
   */
  boolean onRecordingDistanceIntervalChanged(int recordingDistanceInterval);
  
  /**
   * Called when the map type preference value is changed.
   * 
   * @param mapType the new value
   * @return true to reload all the data, false otherwise.
   */
  boolean onMapTypeChanged(int mapType);
}
