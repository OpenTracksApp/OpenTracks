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

import android.content.Context;
import android.database.Cursor;
import android.location.Location;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.OutputStream;

import de.dennisguse.opentracks.content.ContentProviderUtils;
import de.dennisguse.opentracks.content.LocationFactory;
import de.dennisguse.opentracks.content.LocationIterator;
import de.dennisguse.opentracks.content.TrackPoint;
import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.content.data.Waypoint;
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
    private final Track[] tracks;
    private final TrackWriter trackWriter;
    private final TrackExporterListener trackExporterListener;

    /**
     * Constructor.
     *
     * @param contentProviderUtils  the content provider utils
     * @param trackWriter           the track writer
     * @param tracks                the tracks
     * @param trackExporterListener the track export listener
     */
    public FileTrackExporter(ContentProviderUtils contentProviderUtils, TrackWriter trackWriter, Track[] tracks, TrackExporterListener trackExporterListener) {
        this.contentProviderUtils = contentProviderUtils;
        this.tracks = tracks;
        this.trackWriter = trackWriter;
        this.trackExporterListener = trackExporterListener;
    }

    @Override
    public boolean writeTrack(@NonNull Context context, @NonNull OutputStream outputStream) {
        try {
            trackWriter.prepare(outputStream);
            trackWriter.writeHeader(tracks);
            for (Track track1 : tracks) {
                writeWaypoints(track1);
            }
            trackWriter.writeBeginTracks();
            long startTime = tracks[0].getTripStatistics().getStartTime();
            for (Track track : tracks) {
                long offset = track.getTripStatistics().getStartTime() - startTime;
                writeLocations(track, offset);
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
         * TODO: Stream through the waypoints in chunks.
         *  I am leaving the number of waypoints very high which should not be a problem, because we don't try to load them into objects all at the same time.
         */
        boolean hasWaypoints = false;
        try (Cursor cursor = contentProviderUtils.getWaypointCursor(track.getId(), -1L, ContentProviderUtils.MAX_LOADED_WAYPOINTS_POINTS)) {
            if (cursor != null && cursor.moveToFirst()) {
                for (int i = 0; i < cursor.getCount(); i++) {
                    if (Thread.interrupted()) {
                        throw new InterruptedException();
                    }
                    if (!hasWaypoints) {
                        trackWriter.writeBeginWaypoints(track);
                        hasWaypoints = true;
                    }
                    Waypoint waypoint = contentProviderUtils.createWaypoint(cursor);
                    trackWriter.writeWaypoint(waypoint);

                    cursor.moveToNext();
                }
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

        try (LocationIterator locationIterator = contentProviderUtils.getTrackPointLocationIterator(track.getId(), -1L, false, locationFactory)) {

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
                //Close the last segment
                trackWriter.writeCloseSegment();
            }

            if (wroteTrack) {
                Location lastValidTrackPoint = contentProviderUtils.getLastValidTrackPoint(track.getId());
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
     * Sets a location time.
     *
     * @param location the location
     * @param offset   the time offset
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
    private class TrackWriterLocationFactory extends LocationFactory {
        Location currentLocation;
        Location lastLocation;

        @Override
        public Location createLocation() {
            if (currentLocation == null) {
                currentLocation = new TrackPoint("");
            }
            return currentLocation;
        }

        void swapLocations() {
            Location tempLocation = lastLocation;
            lastLocation = currentLocation;
            currentLocation = tempLocation;
            if (currentLocation != null) {
                currentLocation.reset();
            }
        }
    }
}
