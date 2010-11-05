/*
 * Copyright 2008 Google Inc.
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

import com.google.android.apps.mytracks.content.Waypoint;

/**
 * MyTracks service.
 * This service is the process that actually records and manages tracks.
 */
interface ITrackRecordingService {

  /**
   * Starts recording a new track.
   *
   * @return the track ID of the new track
   */
  long startNewTrack();

  /**
   * Checks and returns whether we're currently recording a track.
   */
  boolean isRecording();

  /**
   * Returns the track ID of the track currently being recorded, or -1 if none
   * is being recorded. This ID can then be used to read track data from the
   * content source.
   */
  long getRecordingTrackId();

  /**
   * Inserts a waypoint marker in the track being recorded.
   *
   * @param waypoint the waypoint to insert
   * @return the unique ID of the inserted marker
   */
  long insertWaypointMarker(in Waypoint waypoint);

  /**
   * Inserts a statistics marker in the track being recorded.
   *
   * @param location the location at which to insert the marker
   * @return the unique ID of the inserted marker
   */
  long insertStatisticsMarker(in Location location);

  /**
   * Inserts a location in the track being recorded.
   *
   * When recording, locations detected by the GPS are already automatically
   * added to the track, so this should be used only for adding special points
   * or for testing.
   *
   * @param loc the location to insert
   */
  void recordLocation(in Location loc);

  /**
   * Stops recording the current track.
   */
  void endCurrentTrack();

  /**
   * Returns whether at least one track has been or is being recorded (and was
   * not deleted).
   */
  boolean hasRecorded();

  /**
   * Deletes all the stored tracks.
   */
  void deleteAllTracks();

  /**
   * Notifies the service that its preferences may have been changed.
   * This is necessary because the service running on a separate process cannot
   * listen to the changes itself.
   *
   * @param key the preference key which may have changed
   */
  void sharedPreferenceChanged(in String key);
}
