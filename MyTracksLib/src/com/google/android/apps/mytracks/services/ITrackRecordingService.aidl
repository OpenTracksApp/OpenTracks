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

import com.google.android.apps.mytracks.content.WaypointCreationRequest;

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
   * @param request Details for the waypoint to be inserted.
   * @return the unique ID of the inserted marker
   */
  long insertWaypoint(in WaypointCreationRequest request);

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
   * The current sensor data.
   * The data is returned as a byte array which is a binary version of a
   * Sensor.SensorDataSet object.
   * @return the current sensor data or null if there is none.
   */
  byte[] getSensorData();

  /**
   * The current state of the sensor manager.
   * The value is the value of a Sensor.SensorState enum.
   */
  int getSensorState();
}
