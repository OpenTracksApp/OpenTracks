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
package de.dennisguse.opentracks.services;

import androidx.annotation.VisibleForTesting;

import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.content.data.TrackPoint;
import de.dennisguse.opentracks.content.data.Waypoint;
import de.dennisguse.opentracks.content.sensor.SensorDataSet;
import de.dennisguse.opentracks.services.handlers.GpsStatusValue;
import de.dennisguse.opentracks.services.sensors.BluetoothRemoteSensorManager;

/**
 * App's service.
 * This service is the process that actually records and manages tracks.
 */
public interface TrackRecordingServiceInterface {

    /**
     * Starts gps.
     */
    void startGps();

    /**
     * Stops gps.
     */
    void stopGps();

    /**
     * Starts recording a new track.
     *
     * @return the track ID of the new track.
     */
    Track.Id startNewTrack();

    /**
     * Resumes the track identified by trackId.
     */
    void resumeTrack(Track.Id trackId);

    /**
     * Pauses the current recording track.
     */
    void pauseCurrentTrack();

    /**
     * Resumes the current recording track.
     */
    void resumeCurrentTrack();

    /**
     * Ends the current recording track.
     */
    void endCurrentTrack();

    /**
     * Returns true if currently recording a track.
     */
    boolean isRecording();

    /**
     * Returns true if the current recording track is paused. Returns true if not recording.
     */
    boolean isPaused();

    /**
     * Gets the current recording track ID. Returns null if not recording.
     */
    Track.Id getRecordingTrackId();

    /**
     * Gets the total time for the current recording track. Returns 0 if not recording.
     */
    //TODO milliseconds?
    long getTotalTime();

    /**
     * Inserts a waypoint in the current recording track.
     *
     * @return the ID of the inserted waypoint
     */
    Waypoint.Id insertWaypoint(String name, String category, String description, String photoUrl);

    /**
     * Gets the current sensor data. Returns null if there is no data.
     *
     * @return SensorDataSet object.
     */
    SensorDataSet getSensorData();

    @VisibleForTesting
    void setRemoteSensorManager(BluetoothRemoteSensorManager remoteSensorManager);

    /**
     * Inserts a track point in the current recording track.
     * This is used for inserting special track points or for testing.
     *
     * @param trackPoint           the track point object to be inserted.
     * @param recordingGpsAccuracy recording GPS accuracy.
     */
    @VisibleForTesting
    void newTrackPoint(TrackPoint trackPoint, int recordingGpsAccuracy);

    void addListener(TrackRecordingServiceCallback listener);

    GpsStatusValue getGpsStatus();
}
