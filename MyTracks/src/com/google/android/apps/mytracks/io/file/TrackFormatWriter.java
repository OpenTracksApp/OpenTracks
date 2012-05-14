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
package com.google.android.apps.mytracks.io.file;

import com.google.android.apps.mytracks.content.Track;
import com.google.android.apps.mytracks.content.Waypoint;

import android.location.Location;

import java.io.OutputStream;

/**
 * Interface for writing a track to file.
 *
 * The expected sequence of calls is:
 * <ol>
 *   <li>{@link #prepare}
 *   <li>{@link #writeHeader}
 *   <li>{@link #writeBeginWaypoints}
 *   <li>For each waypoint: {@link #writeWaypoint}
 *   <li>{@link #writeEndWaypoints}
 *   <li>{@link #writeBeginTrack}
 *   <li>For each segment:
 *   <ol>
 *     <li>{@link #writeOpenSegment}
 *     <li>For each location in the segment: {@link #writeLocation}
 *     <li>{@link #writeCloseSegment}
 *   </ol>
 *   <li>{@link #writeEndTrack}
 *   <li>{@link #writeFooter}
 *   <li>{@link #close}
 * </ol>
 *
 * @author Rodrigo Damazio
 */
public interface TrackFormatWriter {

  /**
   * Gets the file extension (i.e. gpx, kml, ...)
   */
  public String getExtension();

  /**
   * Sets up the writer to write the given track.
   *
   * @param track the track to write
   * @param outputStream the output stream to write the track to
   */
  public void prepare(Track track, OutputStream outputStream);

  /**
   * Closes the underlying file handler.
   */
  public void close();

  /**
   * Writes the header.
   */
  public void writeHeader();

  /**
   * Writes the footer.
   */
  public void writeFooter();

  /**
   * Writes the beginning of the waypoints.
   */
  public void writeBeginWaypoints();
  
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
   * Writes the beginning of the track.
   * 
   * @param firstLocation the first location
   */
  public void writeBeginTrack(Location firstLocation);

  /**
   * Writes the end of the track.
   * 
   * @param lastLocation the last location
   */
  public void writeEndTrack(Location lastLocation);

  /**
   * Writes the statements necessary to open a new segment.
   */
  public void writeOpenSegment();

  /**
   * Writes the statements necessary to close a segment.
   */
  public void writeCloseSegment();

  /**
   * Writes a location.
   *
   * @param location the location
   */
  public void writeLocation(Location location);
}