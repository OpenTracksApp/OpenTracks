/*
 * Copyright 2012 Google Inc.
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

package de.dennisguse.opentracks.io.file.importer;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.content.data.TrackPoint;
import de.dennisguse.opentracks.content.data.Waypoint;
import de.dennisguse.opentracks.content.provider.ContentProviderUtils;
import de.dennisguse.opentracks.content.provider.TrackPointIterator;
import de.dennisguse.opentracks.stats.TripStatisticsUpdater;
import de.dennisguse.opentracks.util.FileUtils;
import de.dennisguse.opentracks.util.LocationUtils;
import de.dennisguse.opentracks.util.PreferencesUtils;
import de.dennisguse.opentracks.util.StringUtils;
import de.dennisguse.opentracks.util.TrackIconUtils;
import de.dennisguse.opentracks.util.UnitConversions;

/**
 * Abstract class for file track importers.
 *
 * @author Jimmy Shih
 */
abstract class AbstractFileTrackImporter extends DefaultHandler implements TrackImporter {

    private static final String TAG = AbstractFileTrackImporter.class.getSimpleName();

    // The maximum number of buffered locations for bulk-insertion
    private static final int MAX_BUFFERED_LOCATIONS = 512;
    private final Context context;
    private final long importTrackId;
    private final ContentProviderUtils contentProviderUtils;
    private final int recordingDistanceInterval;

    private final List<Long> trackIds = new ArrayList<>();
    private final List<Waypoint> waypoints = new ArrayList<>();

    // The current element content
    //TODO Should be made private and getter be used by child classes.
    protected String content;
    protected String icon;
    protected String name;
    protected String description;
    protected String category;
    protected String latitude;
    protected String longitude;
    protected String altitude;
    protected String time;
    protected String waypointType;
    protected String photoUrl;

    // The current track data
    private TrackData trackData;

    // The SAX locator to get the current line information
    private Locator locator;

    /**
     * Constructor.
     *
     * @param context       the context
     * @param importTrackId the track id to import to. -1L to import to a new track.
     */
    AbstractFileTrackImporter(Context context, long importTrackId, ContentProviderUtils contentProviderUtils) {
        this.context = context;
        this.importTrackId = importTrackId;
        this.contentProviderUtils = contentProviderUtils;
        this.recordingDistanceInterval = PreferencesUtils.getRecordingDistanceInterval(context);
    }

    @Override
    public void setDocumentLocator(Locator locator) {
        this.locator = locator;
    }

    @Override
    public void characters(char[] ch, int start, int length) {
        String newContent = new String(ch, start, length);
        if (content == null) {
            content = newContent;
        } else {
            // In 99% of the cases, a single call to this method will be made for each sequence of characters we're interested in, so we'll rarely be concatenating strings, thus not justifying the use of a StringBuilder.
            content += newContent;
        }
    }

    @Override
    public long importFile(InputStream inputStream) {
        try {
            SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
            long start = System.currentTimeMillis();

            saxParser.parse(inputStream, this);
            Log.d(TAG, "Total import time: " + (System.currentTimeMillis() - start) + "ms");
            if (trackIds.size() != 1) {
                Log.d(TAG, trackIds.size() + " tracks imported");
                cleanImport();
                return -1L;
            }
            return trackIds.get(0);
        } catch (IOException | SAXException | ParserConfigurationException e) {
            Log.e(TAG, "Unable to import file", e);
            cleanImport();
            return -1L;
        }
    }

    /**
     * On file end.
     */
    protected void onFileEnd() {
        // Add waypoints to the last imported track
        int size = trackIds.size();
        if (size == 0) {
            return;
        }
        long trackId = trackIds.get(size - 1);
        Track track = contentProviderUtils.getTrack(trackId);
        if (track == null) {
            return;
        }

        int waypointPosition = -1;
        Waypoint waypoint = null;
        TrackPoint trackPoint = null;
        TripStatisticsUpdater trackTripStatisticstrackUpdater = new TripStatisticsUpdater(track.getTripStatistics().getStartTime());
        @Deprecated // TODO Should not be necessary anymore?
                TripStatisticsUpdater markerTripStatisticsUpdater = new TripStatisticsUpdater(track.getTripStatistics().getStartTime());

        try (TrackPointIterator trackPointIterator = contentProviderUtils.getTrackPointLocationIterator(track.getId(), -1L, false)) {

            while (true) {
                if (waypoint == null) {
                    waypointPosition++;
                    waypoint = waypointPosition < waypoints.size() ? waypoints.get(waypointPosition) : null;
                    if (waypoint == null) {
                        // No more waypoints
                        return;
                    }
                }

                if (trackPoint == null) {
                    if (!trackPointIterator.hasNext()) {
                        // No more track points. Ignore the rest of the waypoints.
                        return;
                    }
                    trackPoint = trackPointIterator.next();
                    trackTripStatisticstrackUpdater.addTrackPoint(trackPoint, recordingDistanceInterval);
                    markerTripStatisticsUpdater.addTrackPoint(trackPoint, recordingDistanceInterval);
                }

                if (waypoint.getLocation().getTime() > trackPoint.getTime()) {
                    trackPoint = null;
                } else if (waypoint.getLocation().getTime() < trackPoint.getTime()) {
                    Log.w(TAG, "Ignoring waypoint: current trackPoint was after waypoint.");
                    waypoint = null;
                } else {
                    // The waypoint trackPoint time matches the track point time
                    if (!LocationUtils.isValidLocation(trackPoint.getLocation())) {
                        // Invalid trackPoint, load the next trackPoint
                        trackPoint = null;
                        continue;
                    }

                    // Valid trackPoint
                    if (trackPoint.getLatitude() == waypoint.getLocation().getLatitude() && trackPoint.getLongitude() == waypoint.getLocation().getLongitude()) {
                        String waypointDescription = waypoint.getDescription();
                        String icon = context.getString(R.string.marker_waypoint_icon_url);
                        double length = trackTripStatisticstrackUpdater.getTripStatistics().getTotalDistance();
                        long duration = trackTripStatisticstrackUpdater.getTripStatistics().getTotalTime();

                        // Insert waypoint
                        Waypoint newWaypoint = new Waypoint(waypoint.getName(), waypointDescription, waypoint.getCategory(), icon, track.getId(), length, duration, trackPoint.getLocation(), waypoint.getPhotoUrl());
                        contentProviderUtils.insertWaypoint(newWaypoint);
                    }

                    // Load the next waypoint
                    waypoint = null;
                }
            }
        }
    }

    /**
     * On track start.
     */
    protected void onTrackStart() throws SAXException {
        trackData = new TrackData();
        long trackId;
        if (importTrackId == -1L) {
            Uri uri = contentProviderUtils.insertTrack(trackData.track);
            trackId = Long.parseLong(uri.getLastPathSegment());
        } else {
            if (trackIds.size() > 0) {
                throw new SAXException(createErrorMessage("Cannot import more than one track to an existing track " + importTrackId));
            }
            trackId = importTrackId;
            contentProviderUtils.clearTrack(context, trackId);
        }
        trackIds.add(trackId);
        trackData.track.setId(trackId);
    }

    /**
     * On track end.
     */
    protected void onTrackEnd() {
        flushLocations(trackData);
        if (name != null) {
            trackData.track.setName(name);
        }
        if (description != null) {
            trackData.track.setDescription(description);
        }
        if (category != null) {
            trackData.track.setCategory(category);
            //TODO remove when GPX and KML support reading this property.
            if (icon == null)
                trackData.track.setIcon(TrackIconUtils.getIconValue(context, category));
        }
        if (icon != null) {
            trackData.track.setIcon(icon);
        }
        if (trackData.tripStatisticsUpdater == null) {
            trackData.tripStatisticsUpdater = new TripStatisticsUpdater(trackData.importTime);
            trackData.tripStatisticsUpdater.updateTime(trackData.importTime);
        }
        trackData.track.setTripStatistics(trackData.tripStatisticsUpdater.getTripStatistics());
        contentProviderUtils.updateTrack(trackData.track);
    }

    /**
     * On track segment start.
     */
    protected void onTrackSegmentStart() {
        trackData.numberOfSegments++;

        //If not the first segment, add a pause separator if there is at least one location in the last segment.
        if (trackData.numberOfSegments > 1 && trackData.lastLocationInCurrentSegment != null) {
            insertLocation(TrackPoint.createPauseWithTime(trackData.lastLocationInCurrentSegment.getTime()));
        }
        trackData.lastLocationInCurrentSegment = null;
    }

    /**
     * Adds a waypoint.
     */
    protected void addWaypoint() throws SAXException {
        // Waypoint must have a time, else cannot match to the track points
        if (time == null) {
            return;
        }

        TrackPoint trackPoint = createTrackPoint();

        if (!LocationUtils.isValidLocation(trackPoint.getLocation())) {
            throw new SAXException(createErrorMessage("Invalid location detected: " + trackPoint));
        }
        Waypoint waypoint = new Waypoint(trackPoint.getLocation());

        if (name != null) {
            waypoint.setName(name);
        }
        if (description != null) {
            waypoint.setDescription(description);
        }
        if (category != null) {
            waypoint.setCategory(category);
        }

        if (photoUrl != null) {
            waypoint.setPhotoUrl(photoUrl);
        }
        waypoints.add(waypoint);
    }

    /**
     * Gets a track point.
     */
    protected TrackPoint getTrackPoint() throws SAXException {
        TrackPoint trackPoint = createTrackPoint();

        // Calculate derived attributes from the previous point
        if (trackData.lastLocationInCurrentSegment != null && trackData.lastLocationInCurrentSegment.getTime() != 0) {
            long timeDifference = trackPoint.getTime() - trackData.lastLocationInCurrentSegment.getTime();

            // Check for negative time change
            if (timeDifference <= 0) {
                Log.w(TAG, "Time difference not postive.");
            } else {

                /*
                 * We don't have a speed and bearing in GPX, make something up from the last two points.
                 * GPS points tend to have some inherent imprecision, speed and bearing will likely be off, so the statistics for things like max speed will also be off.
                 */
                double duration = timeDifference * UnitConversions.MS_TO_S;
                double speed = trackData.lastLocationInCurrentSegment.distanceTo(trackPoint) / duration;
                trackPoint.setSpeed((float) speed);
            }
            trackPoint.setBearing(trackData.lastLocationInCurrentSegment.bearingTo(trackPoint));
        }

        if (!LocationUtils.isValidLocation(trackPoint.getLocation())) {
            throw new SAXException(createErrorMessage("Invalid location detected: " + trackPoint));
        }

        if (trackData.numberOfSegments > 1 && trackData.lastLocationInCurrentSegment == null) {
            // If not the first segment, add a resume separator before adding the first location.
            insertLocation(TrackPoint.createResumeWithTime(trackPoint.getTime()));
        }
        trackData.lastLocationInCurrentSegment = trackPoint;
        return trackPoint;
    }

    /**
     * Inserts a track point.
     *
     * @param trackPoint the trackPoint
     */
    protected void insertTrackPoint(TrackPoint trackPoint) {
        insertLocation(trackPoint);
    }

    /**
     * Creates an error message.
     *
     * @param message the message
     */
    protected String createErrorMessage(String message) {
        return String.format(Locale.US, "Parsing error at line: %d column: %d. %s", locator.getLineNumber(), locator.getColumnNumber(), message);
    }

    /**
     * Gets the photo url for a file.
     *
     * @param fileName the file name
     */
    protected String getPhotoUrl(String fileName) {
        if (importTrackId == -1L) {
            return null;
        }
        File dir = FileUtils.getPhotoDir(context, importTrackId);
        File file = new File(dir, fileName);
        return Uri.fromFile(file).toString();
    }

    /**
     * Creates a location.
     */
    private TrackPoint createTrackPoint() throws SAXException {
        if (latitude == null || longitude == null) {
            return null;
        }
        double latitudeValue;
        double longitudeValue;
        try {
            latitudeValue = Double.parseDouble(latitude);
            longitudeValue = Double.parseDouble(longitude);
        } catch (NumberFormatException e) {
            throw new SAXException(createErrorMessage(String.format(Locale.US, "Unable to parse latitude longitude: %s %s", latitude, longitude)), e);
        }
        Double altitudeValue = null;
        if (altitude != null) {
            try {
                altitudeValue = Double.parseDouble(altitude);
            } catch (NumberFormatException e) {
                throw new SAXException(createErrorMessage(String.format(Locale.US, "Unable to parse altitude: %s", altitude)), e);
            }
        }

        long timeValue;
        if (time == null) {
            timeValue = trackData.importTime;
        } else {
            try {
                timeValue = StringUtils.parseTime(time);
            } catch (IllegalArgumentException e) {
                throw new SAXException(createErrorMessage(String.format(Locale.US, "Unable to parse time: %s", time)), e);
            }
        }
        return new TrackPoint(latitudeValue, longitudeValue, altitudeValue, timeValue);
    }

    /**
     * Inserts a trackPoint.
     *
     * @param trackPoint the trackPoint
     */
    private void insertLocation(TrackPoint trackPoint) {
        if (trackData.tripStatisticsUpdater == null) {
            trackData.tripStatisticsUpdater = new TripStatisticsUpdater(trackPoint.getTime() != -1L ? trackPoint.getTime() : trackData.importTime);
        }
        trackData.tripStatisticsUpdater.addTrackPoint(trackPoint, recordingDistanceInterval);

        trackData.bufferedTrackPoints[trackData.numBufferedLocations] = trackPoint;
        trackData.numBufferedLocations++;
        trackData.numberOfLocations++;

        if (trackData.numBufferedLocations >= MAX_BUFFERED_LOCATIONS) {
            flushLocations(trackData);
        }
    }

    /**
     * Flushes the locations to the database.
     *
     * @param data the track data
     */
    private void flushLocations(TrackData data) {
        if (data.numBufferedLocations <= 0) {
            return;
        }
        contentProviderUtils.bulkInsertTrackPoint(data.bufferedTrackPoints, data.numBufferedLocations, data.track.getId());
        data.numBufferedLocations = 0;
    }

    /**
     * Cleans up import.
     */
    private void cleanImport() {
        for (long trackId : trackIds) {
            contentProviderUtils.deleteTrack(context, trackId);
        }
    }

    /**
     * Data for the current track.
     *
     * @author Jimmy Shih
     */
    // TODO Why private inner class?
    private static class TrackData {
        // The current track
        final Track track = new Track();

        // The number of segments processed for the current track
        int numberOfSegments = 0;

        // The last location in the current segment; Null if the current segment doesn't have a last location
        TrackPoint lastLocationInCurrentSegment;

        // The number of locations processed for the current track
        int numberOfLocations = 0;

        // The trip statistics updater for the current track
        TripStatisticsUpdater tripStatisticsUpdater;

        // The import time of the track.
        final long importTime = System.currentTimeMillis();

        // The buffered locations
        final TrackPoint[] bufferedTrackPoints = new TrackPoint[MAX_BUFFERED_LOCATIONS];

        // The number of buffered locations
        int numBufferedLocations = 0;
    }
}
