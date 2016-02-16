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

package com.google.android.apps.mytracks.io.file.exporter;

import com.google.android.apps.mytracks.Constants;
import com.google.android.apps.mytracks.content.MyTracksLocation;
import com.google.android.apps.mytracks.content.MyTracksProviderUtils;
import com.google.android.apps.mytracks.content.MyTracksProviderUtils.LocationIterator;
import com.google.android.apps.mytracks.content.Track;
import com.google.android.apps.mytracks.content.Waypoint;
import com.google.android.apps.mytracks.util.LocationUtils;

import android.database.Cursor;
import android.location.Location;
import android.util.Log;

import java.io.OutputStream;

/**
 * Track Writer for writing tracks to an {@link OutputStream}.
 * 
 * @author Sandor Dornbush
 * @author Rodrigo Damazio
 */
public class FileTrackExporter implements TrackExporter {

  private static final String TAG = FileTrackExporter.class.getSimpleName();

  private final MyTracksProviderUtils myTracksProviderUtils;
  private final Track[] tracks;
  private final TrackWriter trackWriter;
  private final TrackExporterListener trackExporterListener;

  /**
   * Constructor.
   * 
   * @param myTracksProviderUtils the my tracks provider utils
   * @param tracks the tracks
   * @param trackWriter the track writer
   * @param trackExporterListener the track export listener
   */
  public FileTrackExporter(MyTracksProviderUtils myTracksProviderUtils, Track[] tracks,
      TrackWriter trackWriter, TrackExporterListener trackExporterListener) {
    this.myTracksProviderUtils = myTracksProviderUtils;
    this.tracks = tracks;
    this.trackWriter = trackWriter;
    this.trackExporterListener = trackExporterListener;
  }

  @Override
  public boolean writeTrack(OutputStream outputStream) {
    try {
      trackWriter.prepare(outputStream);
      trackWriter.writeHeader(tracks);
      for (int i = 0; i < tracks.length; i++) {
        writeWaypoints(tracks[i]);
      }
      trackWriter.writeBeginTracks();
      long startTime = tracks[0].getTripStatistics().getStartTime();
      for (int i = 0; i < tracks.length; i++) {
        long offset = tracks[i].getTripStatistics().getStartTime() - startTime;
        writeLocations(tracks[i], offset);
      }
      trackWriter.writeEndTracks();
      trackWriter.writeFooter();
      trackWriter.close();
      return true;
    } catch (InterruptedException e) {
      Log.e(TAG, "Thread interrupted", e);
      return false;
    }
  }

  /**
   * Writes the waypoints.
   */
  private void writeWaypoints(Track track) throws InterruptedException {
    /*
     * TODO: Stream through the waypoints in chunks. I am leaving the number of
     * waypoints very high which should not be a problem because we don't try to
     * load them into objects all at the same time.
     */
    boolean hasWaypoints = false;
    Cursor cursor = null;
    try {
      cursor = myTracksProviderUtils.getWaypointCursor(
          track.getId(), -1L, Constants.MAX_LOADED_WAYPOINTS_POINTS);
      if (cursor != null && cursor.moveToFirst()) {
        /*
         * Yes, this will skip the first waypoint and that is intentional as the
         * first waypoint holds the stats for the track.
         */
        while (cursor.moveToNext()) {
          if (Thread.interrupted()) {
            throw new InterruptedException();
          }
          if (!hasWaypoints) {
            trackWriter.writeBeginWaypoints(track);
            hasWaypoints = true;
          }
          Waypoint waypoint = myTracksProviderUtils.createWaypoint(cursor);
          trackWriter.writeWaypoint(waypoint);
        }
      }
    } finally {
      if (cursor != null) {
        cursor.close();
      }
    }
    if (hasWaypoints) {
      trackWriter.writeEndWaypoints();
    }
  }

  /**
   * Writes the locations.
   */
  private void writeLocations(Track track, long offset) throws InterruptedException {
    boolean wroteTrack = false;
    boolean wroteSegment = false;
    boolean isLastLocationValid = false;
    TrackWriterLocationFactory locationFactory = new TrackWriterLocationFactory();
    int locationNumber = 0;
    LocationIterator locationIterator = null;

    try {
      locationIterator = myTracksProviderUtils.getTrackPointLocationIterator(
          track.getId(), -1L, false, locationFactory);

      while (locationIterator.hasNext()) {
        if (Thread.interrupted()) {
          throw new InterruptedException();
        }
        Location location = locationIterator.next();

        setLocationTime(location, offset);
        locationNumber++;

        boolean isLocationValid = LocationUtils.isValidLocation(location);
        boolean isSegmentValid = isLocationValid && isLastLocationValid;
        if (!wroteTrack && isSegmentValid) {
          // Found the first two consecutive locations that are valid
          trackWriter.writeBeginTrack(track, locationFactory.lastLocation);
          wroteTrack = true;
        }

        if (isSegmentValid) {
          if (!wroteSegment) {
            // Start a segment
            trackWriter.writeOpenSegment();
            wroteSegment = true;

            // Write the previous location, which we had previously skipped
            trackWriter.writeLocation(locationFactory.lastLocation);
          }

          // Write the current location
          trackWriter.writeLocation(location);
          if (trackExporterListener != null) {
            trackExporterListener.onProgressUpdate(locationNumber, track.getNumberOfPoints());
          }
        } else {
          if (wroteSegment) {
            trackWriter.writeCloseSegment();
            wroteSegment = false;
          }
        }
        locationFactory.swapLocations();
        isLastLocationValid = isLocationValid;
      }

      if (wroteSegment) {
        trackWriter.writeCloseSegment();
        wroteSegment = false;
      }
      if (wroteTrack) {
        Location lastValidTrackPoint = myTracksProviderUtils.getLastValidTrackPoint(track.getId());
        setLocationTime(lastValidTrackPoint, offset);
        trackWriter.writeEndTrack(track, lastValidTrackPoint);
      } else {
        // Write an empty track
        trackWriter.writeBeginTrack(track, null);
        trackWriter.writeEndTrack(track, null);
      }
    } finally {
      if (locationIterator != null) {
        locationIterator.close();
      }
    }
  }

  /**
   * Sets a location time.
   * 
   * @param location the location
   * @param offset the time offset
   */
  private void setLocationTime(Location location, long offset) {
    if (location != null) {
      location.setTime(location.getTime() - offset);
    }
  }

  /**
   * Track writer location factory. Keeping the last two locations.
   * 
   * @author Jimmy Shih
   */
  private class TrackWriterLocationFactory implements MyTracksProviderUtils.LocationFactory {
    Location currentLocation;
    Location lastLocation;

    @Override
    public Location createLocation() {
      if (currentLocation == null) {
        currentLocation = new MyTracksLocation("");
      }
      return currentLocation;
    }

    public void swapLocations() {
      Location tempLocation = lastLocation;
      lastLocation = currentLocation;
      currentLocation = tempLocation;
      if (currentLocation != null) {
        currentLocation.reset();
      }
    }
  }
}
