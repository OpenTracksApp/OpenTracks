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

package de.dennisguse.opentracks.io.file.exporter;

import android.database.Cursor;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.OutputStream;

import de.dennisguse.opentracks.content.data.Marker;
import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.content.data.TrackPoint;
import de.dennisguse.opentracks.content.provider.ContentProviderUtils;
import de.dennisguse.opentracks.content.provider.TrackPointIterator;
import de.dennisguse.opentracks.util.LocationUtils;

/**
 * Track Writer for writing tracks to an {@link OutputStream}.
 *
 * @author Sandor Dornbush
 * @author Rodrigo Damazio
 */
public class FileTrackExporter implements TrackExporter {

    private static final String TAG = FileTrackExporter.class.getSimpleName();

    private final ContentProviderUtils contentProviderUtils;
    private final TrackWriter trackWriter;

    /**
     * Constructor.
     *
     * @param contentProviderUtils  the content provider utils
     * @param trackWriter           the track writer
     */
    public FileTrackExporter(ContentProviderUtils contentProviderUtils, TrackWriter trackWriter) {
        this.contentProviderUtils = contentProviderUtils;
        this.trackWriter = trackWriter;
    }

    @Override
    public boolean writeTrack(Track track, @NonNull OutputStream outputStream) {
        return writeTrack(new Track[]{track}, outputStream);
    }

    @Override
    public boolean writeTrack(Track[] tracks, @NonNull OutputStream outputStream) {
        try {
            trackWriter.prepare(outputStream);
            trackWriter.writeHeader(tracks);
            for (Track track : tracks) {
                writeMarkers(track);
            }
            boolean hasMultipleTracks = tracks.length > 1;
            if (hasMultipleTracks) {
                trackWriter.writeMultiTrackBegin();
            }
            long startTime = tracks[0].getTrackStatistics().getStartTime_ms();
            for (Track track : tracks) {
                long offset = track.getTrackStatistics().getStartTime_ms() - startTime;
                writeLocations(track, offset);
            }
            if (hasMultipleTracks) {
                trackWriter.writeMultiTrackEnd();
            }
            trackWriter.writeFooter();
            trackWriter.close();

            return true;
        } catch (InterruptedException e) {
            Log.e(TAG, "Thread interrupted", e);
            return false;
        }
    }

    private void writeMarkers(Track track) throws InterruptedException {
        /*
         * TODO: Stream through the markers in chunks.
         *  I am leaving the number of markers very high which should not be a problem, because we don't try to load them into objects all at the same time.
         */
        boolean hasMarkers = false;
        try (Cursor cursor = contentProviderUtils.getMarkerCursor(track.getId(), null, ContentProviderUtils.MAX_LOADED_MARKERS)) {
            if (cursor != null && cursor.moveToFirst()) {
                for (int i = 0; i < cursor.getCount(); i++) {
                    if (Thread.interrupted()) {
                        throw new InterruptedException();
                    }
                    if (!hasMarkers) {
                        trackWriter.writeBeginMarkers(track);
                        hasMarkers = true;
                    }
                    Marker marker = contentProviderUtils.createMarker(cursor);
                    trackWriter.writeMarker(marker);

                    cursor.moveToNext();
                }
            }
        }
        if (hasMarkers) {
            trackWriter.writeEndMarkers();
        }
    }

    /**
     * Writes the locations.
     */
    private void writeLocations(Track track, long offset) throws InterruptedException {
        boolean wroteTrack = false;
        boolean wroteSegment = false;
        boolean isLastLocationValid = false;
        TrackPoint lastTrackPoint = null;

        try (TrackPointIterator trackPointIterator = contentProviderUtils.getTrackPointLocationIterator(track.getId(), null)) {

            while (trackPointIterator.hasNext()) {
                if (Thread.interrupted()) {
                    throw new InterruptedException();
                }
                TrackPoint trackPoint = trackPointIterator.next();

                setLocationTime(trackPoint, offset);

                boolean isLocationValid = LocationUtils.isValidLocation(trackPoint.getLocation());
                boolean isSegmentValid = isLocationValid && isLastLocationValid;
                if (!wroteTrack && isSegmentValid) {
                    // Found the first two consecutive locations that are valid
                    trackWriter.writeBeginTrack(track, lastTrackPoint);
                    wroteTrack = true;
                }

                if (isSegmentValid) {
                    if (!wroteSegment) {
                        // Start a segment
                        trackWriter.writeOpenSegment();
                        wroteSegment = true;

                        // Write the previous trackPoint, which we had previously skipped
                        trackWriter.writeTrackPoint(lastTrackPoint);
                    }

                    // Write the current trackPoint
                    trackWriter.writeTrackPoint(trackPoint);
                } else {
                    if (wroteSegment) {
                        trackWriter.writeCloseSegment();
                        wroteSegment = false;
                    }
                }
                lastTrackPoint = trackPoint;
                isLastLocationValid = isLocationValid;
            }

            if (wroteSegment) {
                //Close the last segment
                trackWriter.writeCloseSegment();
            }

            if (wroteTrack) {
                TrackPoint lastValidTrackPoint = contentProviderUtils.getLastValidTrackPoint(track.getId());
                setLocationTime(lastValidTrackPoint, offset);
                trackWriter.writeEndTrack(track, lastValidTrackPoint);
            } else {
                // Write an empty track
                trackWriter.writeBeginTrack(track, null);
                trackWriter.writeEndTrack(track, null);
            }
        }
    }

    /**
     * Sets a trackPoint time.
     *
     * @param trackPoint the trackPoint
     * @param offset     the time offset
     */
    private void setLocationTime(TrackPoint trackPoint, long offset) {
        if (trackPoint != null) {
            trackPoint.setTime(trackPoint.getTime() - offset);
        }
    }
}
