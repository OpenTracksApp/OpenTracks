/*
 * Copyright 2010 Google Inc.
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

import com.google.android.apps.mytracks.content.Track;
import com.google.android.apps.mytracks.content.Waypoint;

import android.location.Location;

import java.io.OutputStream;

/**
 * Interface for writing tracks to a file. The expected sequence of calls is:
 * 
 * <pre>
 * {@link #prepare(OutputStream)}
 * {@link #writeHeader(Track[])}
 * For each track:
 *     {@link #writeBeginWaypoints(Track)}
 *     For each waypoint:
 *         {@link #writeWaypoint(Waypoint)}
 *     {@link #writeEndWaypoints()}
 * {@link #writeBeginTracks()}
 * For each track:
 *     {@link #writeBeginTrack(Track, Location)}
 *     For each segment:
 *         {@link #writeOpenSegment()}
 *         For each location in the segment:
 *             {@link #writeLocation(Location)}
 *         {@link #writeCloseSegment()}
 *     {@link #writeEndTrack(Track, Location)}
 * {@link #writeEndTracks()}
 * {@link #writeFooter()}
 * {@link #close()}
 * </pre>
 * 
 * @author Rodrigo Damazio
 */
public interface TrackWriter {

  /**
   * Gets the file extension (e.g, gpx, kml, ...).
   */
  public String getExtension();

  /**
   * Prepares the output stream.
   * 
   * @param outputStream the output stream
   */
  public void prepare(OutputStream outputStream);

  /**
   * Closes the output stream.
   */
  public void close();

  /**
   * Writes the header
   * 
   * @param tracks the tracks
   */
  public void writeHeader(Track[] tracks);

  /**
   * Writes the footer.
   */
  public void writeFooter();

  /**
   * Writes the beginning of the waypoints.
   * 
   * @param track the track
   */
  public void writeBeginWaypoints(Track track);

  /**
   * Writes the end of the waypoints.
   */
  public void writeEndWaypoints();

  /**
   * Writes a waypoint.
   * 
   * @param waypoint the waypoint
   */
  public void writeWaypoint(Waypoint waypoint);

  /**
   * Writes the beginning of the tracks.
   */
  public void writeBeginTracks();

  /**
   * Writes the end of the tracks,
   */
  public void writeEndTracks();

  /**
   * Writes the beginning of a track.
   * 
   * @param track the track
   * @param startLocation the start location
   */
  public void writeBeginTrack(Track track, Location startLocation);

  /**
   * Writes the end of a track.
   * 
   * @param track the track
   * @param endLocation the end location
   */
  public void writeEndTrack(Track track, Location endLocation);

  /**
   * Writes open segment.
   */
  public void writeOpenSegment();

  /**
   * Writes close segment.
   */
  public void writeCloseSegment();

  /**
   * Writes a location.
   * 
   * @param location the location
   */
  public void writeLocation(Location location);
}