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
package de.dennisguse.opentracks.io.file.exporter;

import java.io.OutputStream;

import de.dennisguse.opentracks.content.data.Marker;
import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.content.data.TrackPoint;

/**
 * Interface for writing tracks to a file. The expected sequence of calls is:
 *
 * <pre>
 * {@link #prepare(OutputStream)}
 * {@link #writeHeader(Track[])}
 * For each track:
 *     {@link #writeBeginWaypoints(Track)}
 *     For each waypoint:
 *         {@link #writeWaypoint(Marker)}
 *     {@link #writeEndWaypoints()}
 * {@link #writeMultiTrackBegin()}
 * For each track:
 *     {@link #writeBeginTrack(Track, TrackPoint)}
 *     For each segment:
 *         {@link #writeOpenSegment()}
 *         For each trackPoint in the segment:
 *             {@link #writeTrackPoint(TrackPoint)}
 *         {@link #writeCloseSegment()}
 *     {@link #writeEndTrack(Track, TrackPoint)}
 * {@link #writeMultiTrackEnd()}
 * {@link #writeFooter()}
 * {@link #close()}
 * </pre>
 *
 * @author Rodrigo Damazio
 */
public interface TrackWriter {

    /**
     * Prepares the output stream.
     *
     * @param outputStream the output stream
     */
    void prepare(OutputStream outputStream);

    /**
     * Closes the output stream.
     */
    void close();

    /**
     * Writes the header
     *
     * @param tracks the tracks
     */
    void writeHeader(Track[] tracks);

    /**
     * Writes the footer.
     */
    void writeFooter();

    /**
     * Writes the beginning of the waypoints.
     *
     * @param track the track
     */
    void writeBeginWaypoints(Track track);

    /**
     * Writes the end of the waypoints.
     */
    void writeEndWaypoints();

    /**
     * Writes a waypoint.
     *
     * @param waypoint the waypoint
     */
    void writeWaypoint(Marker waypoint);

    /**
     * Writes the beginning of the tracks.
     */
    void writeMultiTrackBegin();

    /**
     * Writes the end of the tracks,
     */
    void writeMultiTrackEnd();

    /**
     * Writes the beginning of a track.
     *
     * @param track           the track
     * @param startTrackPoint the start location
     */
    void writeBeginTrack(Track track, TrackPoint startTrackPoint);

    /**
     * Writes the end of a track.
     *
     * @param track         the track
     * @param endTrackPoint the end location
     */
    void writeEndTrack(Track track, TrackPoint endTrackPoint);

    /**
     * Writes open segment.
     */
    void writeOpenSegment();

    /**
     * Writes close segment.
     */
    void writeCloseSegment();

    /**
     * Writes a trackPoint.
     *
     * @param trackPoint the trackPoint
     */
    void writeTrackPoint(TrackPoint trackPoint);
}