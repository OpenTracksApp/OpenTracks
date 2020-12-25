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
import android.database.sqlite.SQLiteConstraintException;
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
import java.util.UUID;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.content.data.Marker;
import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.content.data.TrackPoint;
import de.dennisguse.opentracks.content.provider.ContentProviderUtils;
import de.dennisguse.opentracks.content.provider.TrackPointIterator;
import de.dennisguse.opentracks.stats.TrackStatisticsUpdater;
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

    private final Context context;
    private final ContentProviderUtils contentProviderUtils;
    private final int recordingDistanceInterval;

    private final List<Track.Id> trackIds = new ArrayList<>();
    private final List<Marker> markers = new ArrayList<>();

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
    protected String speed;
    protected String heartrate;
    protected String cadence;
    protected String power;
    protected String markerType;
    protected String photoUrl;
    protected String uuid;
    protected String gain;
    protected String loss;

    // The current track data
    private TrackData trackData;

    // The SAX locator to get the current line information
    private Locator locator;

    AbstractFileTrackImporter(Context context, ContentProviderUtils contentProviderUtils) {
        this.context = context;
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
    public Track.Id importFile(InputStream inputStream) {
        try {
            SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
            long start = System.currentTimeMillis();

            saxParser.parse(inputStream, this);
            Log.d(TAG, "Total import time: " + (System.currentTimeMillis() - start) + "ms");
            return trackIds.get(0);
        } catch (IOException | SAXException | ParserConfigurationException e) {
            Log.e(TAG, "Unable to import file", e);
            if (trackIds.size() > 0) {
                cleanImport();
            }
            throw new ImportParserException(e);
        } catch (SQLiteConstraintException e) {
            Log.e(TAG, "Unable to import file", e);
            throw new ImportAlreadyExistsException(e);
        }
    }

    /**
     * On file end.
     */
    protected void onFileEnd() {
        // Add markers to the last imported track
        int size = trackIds.size();
        if (size == 0) {
            return;
        }
        Track.Id trackId = trackIds.get(size - 1);
        Track track = contentProviderUtils.getTrack(trackId);
        if (track == null) {
            return;
        }

        int markerPosition = -1;
        Marker marker = null;
        TrackPoint trackPoint = null;
        TrackStatisticsUpdater trackStatisticsUpdater = new TrackStatisticsUpdater(track.getTrackStatistics().getStartTime_ms());
        // TODO Should not be necessary anymore?
        TrackStatisticsUpdater markerTrackStatisticsUpdater = new TrackStatisticsUpdater(track.getTrackStatistics().getStartTime_ms());

        try (TrackPointIterator trackPointIterator = contentProviderUtils.getTrackPointLocationIterator(track.getId(), null)) {

            while (true) {
                if (marker == null) {
                    markerPosition++;
                    marker = markerPosition < markers.size() ? markers.get(markerPosition) : null;
                    if (marker == null) {
                        // No more markers
                        return;
                    }
                    // If marker had photo it must be translated to internal photo url (depend on track id)
                    if (marker.hasPhoto()) {
                        marker.setPhotoUrl(getInternalPhotoUrl(marker.getPhotoUrl()));
                    }
                }

                if (trackPoint == null) {
                    if (!trackPointIterator.hasNext()) {
                        // No more track points. Ignore the rest of the markers.
                        return;
                    }
                    trackPoint = trackPointIterator.next();
                    trackStatisticsUpdater.addTrackPoint(trackPoint, recordingDistanceInterval);
                    markerTrackStatisticsUpdater.addTrackPoint(trackPoint, recordingDistanceInterval);
                }

                if (marker.getLocation().getTime() > trackPoint.getTime()) {
                    trackPoint = null;
                } else if (marker.getLocation().getTime() < trackPoint.getTime()) {
                    Log.w(TAG, "Ignoring marker: current trackPoint was after marker.");
                    marker = null;
                } else {
                    // The marker trackPoint time matches the track point time
                    if (!LocationUtils.isValidLocation(trackPoint.getLocation())) {
                        // Invalid trackPoint, load the next trackPoint
                        trackPoint = null;
                        continue;
                    }

                    // Valid trackPoint
                    if (trackPoint.getLatitude() == marker.getLocation().getLatitude() && trackPoint.getLongitude() == marker.getLocation().getLongitude()) {
                        String markerDescription = marker.getDescription();
                        String icon = context.getString(R.string.marker_icon_url);
                        double length = trackStatisticsUpdater.getTrackStatistics().getTotalDistance();
                        long duration = trackStatisticsUpdater.getTrackStatistics().getTotalTime();

                        // Insert marker
                        Marker newMarker = new Marker(marker.getName(), markerDescription, marker.getCategory(), icon, track.getId(), length, duration, trackPoint.getLocation(), marker.getPhotoUrl());
                        contentProviderUtils.insertMarker(newMarker);
                    }

                    // Load the next marker
                    marker = null;
                }
            }
        }
    }

    /**
     * On track start.
     */
    protected void onTrackStart() throws SAXException {
        trackData = new TrackData();
    }

    /**
     * On track end.
     */
    protected void onTrackEnd() {
        if (name != null) {
            trackData.track.setName(name);
        }

        try {
            trackData.track.setUuid(UUID.fromString(uuid));
        } catch (IllegalArgumentException | NullPointerException e) {
            Log.w(TAG, "could not parse Track UUID, generating a new one.");
            trackData.track.setUuid(UUID.randomUUID());
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
        if (trackData.trackStatisticsUpdater == null) {
            trackData.trackStatisticsUpdater = new TrackStatisticsUpdater(trackData.importTime);
            trackData.trackStatisticsUpdater.updateTime(trackData.importTime);
        }
        trackData.track.setTrackStatistics(trackData.trackStatisticsUpdater.getTrackStatistics());

        Track track = contentProviderUtils.getTrack(trackData.track.getUuid());
        if (track != null) {
            if (PreferencesUtils.getPreventReimportTracks(context)) {
                throw new ImportAlreadyExistsException(context.getString(R.string.import_prevent_reimport));
            }

            //TODO This is a workaround until we have proper UI.
            trackData.track.setUuid(UUID.randomUUID());
        }

        if (trackIds.size() > 0) {
            // TODO Multi track is not supported yet.
            cleanImport();
            throw new ImportParserException("Multi track not supported");
        }
        Uri uri = contentProviderUtils.insertTrack(trackData.track);
        Track.Id trackId = new Track.Id(Long.parseLong(uri.getLastPathSegment()));
        trackIds.add(trackId);
        trackData.track.setId(trackId);

        flushLocations(trackData);
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

    protected void addMarker() throws SAXException {
        // Markers must have a time, else cannot match to the track points
        if (time == null) {
            return;
        }

        TrackPoint trackPoint = createTrackPoint();

        if (!LocationUtils.isValidLocation(trackPoint.getLocation())) {
            Log.w(TAG, "Marker with invalid coordinates ignored: " + trackPoint.getLocation());
            return;
        }
        Marker marker = new Marker(trackPoint.getLocation());

        if (name != null) {
            marker.setName(name);
        }
        if (description != null) {
            marker.setDescription(description);
        }
        if (category != null) {
            marker.setCategory(category);
        }

        if (photoUrl != null) {
            marker.setPhotoUrl(photoUrl);
        }
        markers.add(marker);
    }

    /**
     * Gets a track point.
     */
    protected TrackPoint getTrackPoint() throws SAXException {
        TrackPoint trackPoint = createTrackPoint();
        if (trackPoint == null) {
            throw new SAXException(createErrorMessage("Invalid location detected: " + trackPoint));
        }

        // Calculate derived attributes from the previous point
        if (trackData.lastLocationInCurrentSegment != null && trackData.lastLocationInCurrentSegment.getTime() != 0) {
            if (!trackPoint.hasSpeed()) {
                long timeDifference = trackPoint.getTime() - trackData.lastLocationInCurrentSegment.getTime();

                // Check for negative time change
                if (timeDifference <= 0) {
                    Log.w(TAG, "Time difference not positive.");
                } else {

                    /*
                     * We don't have a speed and bearing in GPX, make something up from the last two points.
                     * GPS points tend to have some inherent imprecision, speed and bearing will likely be off, so the statistics for things like max speed will also be off.
                     */
                    double duration = timeDifference * UnitConversions.MS_TO_S;
                    double speed = trackData.lastLocationInCurrentSegment.distanceTo(trackPoint) / duration;
                    trackPoint.setSpeed((float) speed);
                }
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
     * @param externalPhotoUrl the file name
     */
    protected String getInternalPhotoUrl(String externalPhotoUrl) {
        if (trackData.track.getId() == null) {
            Log.e(TAG, "Track id is invalid.");
            return null;
        }

        if (externalPhotoUrl == null) {
            Log.i(TAG, "External photo url is null.");
            return null;
        }

        String importFileName = KmzTrackImporter.importNameForFilename(externalPhotoUrl);
        File file = FileUtils.buildInternalPhotoFile(context, trackData.track.getId(), Uri.parse(importFileName));
        if (file != null) {
            Uri photoUri = FileUtils.getUriForFile(context, file);
            return "" + photoUri;
        }

        return null;
    }

    /**
     * Creates a location.
     */
    private TrackPoint createTrackPoint() throws SAXException {
        if (latitude == null || longitude == null) {
            return null;
        }

        TrackPoint trackPoint = new TrackPoint();

        try {
            trackPoint.setLatitude(Double.parseDouble(latitude));
            trackPoint.setLongitude(Double.parseDouble(longitude));
        } catch (NumberFormatException e) {
            throw new SAXException(createErrorMessage(String.format(Locale.US, "Unable to parse latitude longitude: %s %s", latitude, longitude)), e);
        }

        if (time == null) {
            trackPoint.setTime(trackData.importTime);
        } else {
            try {
                trackPoint.setTime(StringUtils.parseTime(time));
            } catch (Exception e) {
                throw new SAXException(createErrorMessage(String.format(Locale.US, "Unable to parse time: %s", time)), e);
            }
        }

        if (altitude != null) {
            try {
                trackPoint.setAltitude(Double.parseDouble(altitude));
            } catch (NumberFormatException e) {
                throw new SAXException(createErrorMessage(String.format(Locale.US, "Unable to parse altitude: %s", altitude)), e);
            }
        }

        if (speed != null) {
            try {
                trackPoint.setSpeed(Float.parseFloat(speed));
            } catch (Exception e) {
                throw new SAXException(createErrorMessage(String.format(Locale.US, "Unable to parse speed: %s", speed)), e);
            }
        }
        if (heartrate != null) {
            try {
                trackPoint.setHeartRate_bpm(Float.parseFloat(heartrate));
            } catch (Exception e) {
                throw new SAXException(createErrorMessage(String.format(Locale.US, "Unable to parse heart rate: %s", heartrate)), e);
            }
        }

        if (cadence != null) {
            try {
                trackPoint.setCyclingCadence_rpm(Float.parseFloat(cadence));
            } catch (Exception e) {
                throw new SAXException(createErrorMessage(String.format(Locale.US, "Unable to parse cadence: %s", cadence)), e);
            }
        }

        if (gain != null) {
            try {
                trackPoint.setElevationGain(Float.parseFloat(gain));
            } catch (Exception e) {
                throw new SAXException(createErrorMessage(String.format(Locale.US, "Unable to parse elevation gain: %s", gain)), e);
            }
        }
        if (loss != null) {
            try {
                trackPoint.setElevationLoss(Float.parseFloat(loss));
            } catch (Exception e) {
                throw new SAXException(createErrorMessage(String.format(Locale.US, "Unable to parse elevation loss: %s", loss)), e);
            }
        }

        return trackPoint;
    }

    /**
     * Inserts a trackPoint.
     *
     * @param trackPoint the trackPoint
     */
    private void insertLocation(TrackPoint trackPoint) {
        if (trackData.trackStatisticsUpdater == null) {
            trackData.trackStatisticsUpdater = new TrackStatisticsUpdater(trackPoint.getTime() != -1L ? trackPoint.getTime() : trackData.importTime);
        }
        trackData.trackStatisticsUpdater.addTrackPoint(trackPoint, recordingDistanceInterval);

        trackData.bufferedTrackPoints.add(trackPoint);
        trackData.numBufferedTrackPoints++;
        trackData.numberOfLocations++;
    }

    /**
     * Flushes the locations to the database.
     *
     * @param data the track data
     */
    private void flushLocations(TrackData data) {
        if (data.numBufferedTrackPoints <= 0) {
            return;
        }
        contentProviderUtils.bulkInsertTrackPoint(data.bufferedTrackPoints, data.track.getId());
        data.numBufferedTrackPoints = 0;
    }

    /**
     * Cleans up import.
     */
    private void cleanImport() {
        contentProviderUtils.deleteTracks(context, trackIds);
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

        // The TrackStatisticsUpdater for the current track
        TrackStatisticsUpdater trackStatisticsUpdater;

        // The import time of the track.
        final long importTime = System.currentTimeMillis();

        // The buffered locations
        final List<TrackPoint> bufferedTrackPoints = new ArrayList<>();

        // The number of buffered locations
        int numBufferedTrackPoints = 0;
    }
}
