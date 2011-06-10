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

import com.google.android.apps.mytracks.content.Track;
import com.google.android.apps.mytracks.content.Waypoint;

import android.location.Location;

/**
 * Listener for track data, for both initial and incremental loading.
 *
 * @author Rodrigo Damazio
 */
public interface TrackDataListener {

  /** States for the GPS location provider. */
  public enum ProviderState {
    DISABLED,
    NO_FIX,
    BAD_FIX,
    GOOD_FIX;
  }

  /**
   * Called when the location provider changes state.
   */
  void onProviderStateChange(ProviderState state);

  /**
   * Called when the current location changes.
   * This is meant for immediate location display only - track point data is
   * delivered by other methods below, such as {@link #onNewTrackPoint}.
   *
   * @param loc the last known location
   */
  void onCurrentLocationChanged(Location loc);

  /**
   * Called when the current heading changes.
   *
   * @param heading the current heading, already accounting magnetic declination
   */
  void onCurrentHeadingChanged(double heading);

  /**
   * Called when the currently-selected track changes.
   * This will be followed by calls to data methods such as
   * {@link #onTrackUpdated}, {@link #clearTrackPoints},
   * {@link #onNewTrackPoint(Location)}, etc., even if no track is currently
   * selected (in which case you'll only get calls to clear the current data).
   * 
   * @param track the selected track, or null if no track is selected
   * @param isRecording whether we're currently recording the selected track
   */
  void onSelectedTrackChanged(Track track, boolean isRecording);

  /**
   * Called when the track and/or its statistics have been updated.
   *
   * @param track the updated version of the track
   */
  void onTrackUpdated(Track track);

  /**
   * Called to clear any previously-sent track points.
   * This can be called at any time that we decide the data needs to be
   * reloaded, such as when it needs to be resampled.
   */
  void clearTrackPoints();

  /**
   * Called when a new interesting track point is read.
   * In this case, interesting means that the point has already undergone
   * sampling and invalid point filtering.
   *
   * @param loc the new track point
   */
  void onNewTrackPoint(Location loc);

  /**
   * Called when a uninteresting track point is read.
   * Uninteresting points are all points that get sampled out of the track.
   *
   * @param loc the new track point
   */
  void onSampledOutTrackPoint(Location loc);

  /**
   * Called when an invalid point (representing a segment split) is read.
   */
  void onSegmentSplit();

  /**
   * Called when we're done (for the time being) sending new points.
   * This gets called after every batch of calls to {@link #onNewTrackPoint},
   * {@link #onSampledOutTrackPoint} and {@link #onSegmentSplit}.
   */
  void onNewTrackPointsDone();

  /**
   * Called to clear any previously-sent waypoints.
   * This can be called at any time that we decide the data needs to be
   * reloaded.
   */
  void clearWaypoints();

  /**
   * Called when a new waypoint is read.
   *
   * @param wpt the new waypoint
   */
  void onNewWaypoint(Waypoint wpt);

  /**
   * Called when we're done (for the time being) sending new waypoints.
   * This gets called after every batch of calls to {@link #clearWaypoints} and
   * {@link #onNewWaypoint}.
   */
  void onNewWaypointsDone();

  /**
   * Called when the display units are changed by the user.
   *
   * @param metric true if the units are metric, false if imperial
   * @return true to reload all the data, false otherwise
   */
  boolean onUnitsChanged(boolean metric);

  /**
   * Called when the speed/pace display unit is changed by the user.
   *
   * @param reportSpeed true to report speed, false for pace
   * @return true to reload all the data, false otherwise
   */
  boolean onReportSpeedChanged(boolean reportSpeed);
}
