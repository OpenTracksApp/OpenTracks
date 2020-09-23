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
 *     {@link #writeBeginMarkers(Track)}
 *     For each marker:
 *         {@link #writeMarker(Marker)}
 *     {@link #writeEndMarkers()}
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

    void prepare(OutputStream outputStream);

    /**
     * Closes the output stream.
     */
    void close();

    void writeHeader(Track[] tracks);

    void writeFooter();

    void writeBeginMarkers(Track track);

    void writeEndMarkers();

    void writeMarker(Marker marker);

    void writeMultiTrackBegin();

    void writeMultiTrackEnd();

    void writeBeginTrack(Track track, TrackPoint startTrackPoint);

    void writeEndTrack(Track track, TrackPoint endTrackPoint);

    void writeOpenSegment();

    void writeCloseSegment();

    void writeTrackPoint(TrackPoint trackPoint);
}