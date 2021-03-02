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

import androidx.annotation.NonNull;

import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import javax.xml.parsers.ParserConfigurationException;
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
            SAXParserFactory.newInstance().newSAXParser().parse(inputStream, this);
            return trackIds.get(0);
        } catch (IOException | SAXException | ParserConfigurationException | ParsingException e) {
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
        TrackStatisticsUpdater trackStatisticsUpdater = new TrackStatisticsUpdater();

        // TODO We are doing in memory processing for trackpoints; so we can do this in memory as well.
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
                }

                if ((marker.getTime()).isAfter(trackPoint.getTime())) {
                    trackPoint = null;
                } else if (marker.getTime().isBefore(trackPoint.getTime())) {
                    Log.w(TAG, "Ignoring marker: current trackPoint was after marker.");
                    marker = null;
                } else {
                    // The marker trackPoint time matches the track point time
                    if (!trackPoint.hasLocation()) {
                        // Invalid trackPoint, load the next trackPoint
                        trackPoint = null;
                        continue;
                    }

                    // Valid trackPoint
                    if (trackPoint.getLatitude() == marker.getLatitude() && trackPoint.getLongitude() == marker.getLongitude()) {
                        String markerDescription = marker.getDescription();
                        String icon = context.getString(R.string.marker_icon_url);
                        double length = trackStatisticsUpdater.getTrackStatistics().getTotalDistance();
                        long duration = trackStatisticsUpdater.getTrackStatistics().getTotalTime().toMillis();

                        // Insert marker
                        Marker newMarker = new Marker(marker.getName(), markerDescription, marker.getCategory(), icon, track.getId(), length, duration, trackPoint, marker.getPhotoUrl());
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

        TrackStatisticsUpdater statistics = new TrackStatisticsUpdater();
        //TODO I guess, we should not filter by recordingDistanceInterval on import; the data is already recorded, so we should not change it.
        for (TrackPoint trackPoint : trackData.bufferedTrackPoints) {
            statistics.addTrackPoint(trackPoint, recordingDistanceInterval);
        }
        if (!statistics.isTrackInitialized()) {
            throw new ImportParserException("Track did not contain any locations.");
        }
        trackData.track.setTrackStatistics(statistics.getTrackStatistics());

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

        flushTrackPoints();
    }

    protected void onTrackSegmentStart() {
        trackData.numberOfSegments++;

        //If not the first segment, add a pause separator if there is at least one TrackPoint in the last segment.
        if (trackData.numberOfSegments > 1
                && trackData.lastLocationInCurrentSegment != null
                && (trackData.lastLocationInCurrentSegment.getType().equals(TrackPoint.Type.SEGMENT_START_MANUAL))
        ) {
            insertTrackPoint(TrackPoint.createSegmentEndWithTime(trackData.lastLocationInCurrentSegment.getTime()));
        }
        trackData.lastLocationInCurrentSegment = null;
    }

    protected void onTrackSegmentEnd() {
        TrackPoint trackPoint = trackData.lastLocationInCurrentSegment;
        if (trackPoint == null) {
            return;
        }

        if (!trackPoint.hasLocation()) {
            trackPoint.setType(TrackPoint.Type.SEGMENT_END_MANUAL);
        }
    }

    protected void addMarker() throws ParsingException {
        // Markers must have a time, else cannot match to the track points
        if (time == null) {
            Log.w(TAG, "Marker without time ignored.");
            return;
        }

        TrackPoint trackPoint = createTrackPoint();

        if (!trackPoint.hasLocation()) {
            Log.w(TAG, "Marker with invalid coordinates ignored: " + trackPoint.getLocation());
            return;
        }
        Marker marker = new Marker(null, trackPoint);

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

    protected TrackPoint getTrackPoint() throws ParsingException {
        TrackPoint trackPoint = createTrackPoint();

        if (trackPoint.hasLocation()) {
            Instant time = trackPoint.getTime();
            if (trackPoint.getLatitude() == 100) {
                //TODO Remove by 31st December 2021.
                trackPoint = new TrackPoint(TrackPoint.Type.SEGMENT_END_MANUAL);
                trackPoint.setTime(time);
            } else if (trackPoint.getLatitude() == 200) {
                //TODO Remove by 31st December 2021.
                trackPoint = new TrackPoint(TrackPoint.Type.SEGMENT_START_MANUAL);
                trackPoint.setTime(time);

            } else if (!LocationUtils.isValidLocation(trackPoint.getLocation())) {
                throw new ParsingException(createErrorMessage("Invalid location detected: " + trackPoint));
            }
        }

        // Calculate derived attributes from the previous point
        if (trackData.lastLocationInCurrentSegment != null && trackData.lastLocationInCurrentSegment.getTime() == null) {
            if (!trackPoint.hasSpeed()) {
                Duration timeDifference = Duration.between(trackData.lastLocationInCurrentSegment.getTime(), trackPoint.getTime());

                // Check for negative time change
                if (timeDifference.isNegative()) {
                    Log.w(TAG, "Time difference not positive.");
                } else {

                    /*
                     * We don't have a speed and bearing in GPX, make something up from the last two points.
                     * GPS points tend to have some inherent imprecision, speed and bearing will likely be off, so the statistics for things like max speed will also be off.
                     */
                    if (trackPoint.hasLocation() && trackData.lastLocationInCurrentSegment.hasLocation()) {
                        float speed = trackData.lastLocationInCurrentSegment.distanceTo(trackPoint) / timeDifference.toMillis();
                        trackPoint.setSpeed(speed);
                    }
                }
            }
            if (trackPoint.hasLocation() && trackData.lastLocationInCurrentSegment.hasLocation()) {
                trackPoint.setBearing(trackData.lastLocationInCurrentSegment.bearingTo(trackPoint));

                long maxRecordingDistance = PreferencesUtils.getMaxRecordingDistance(context);
                double distanceToLastTrackLocation = trackPoint.distanceTo(trackData.lastLocationInCurrentSegment);
                if (distanceToLastTrackLocation > maxRecordingDistance) {
                    trackPoint.setType(TrackPoint.Type.SEGMENT_START_AUTOMATIC);
                }
            }
        }

        trackData.lastLocationInCurrentSegment = trackPoint;
        return trackPoint;
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

    private TrackPoint createTrackPoint() throws ParsingException {
        TrackPoint trackPoint = new TrackPoint(TrackPoint.Type.TRACKPOINT);

        try {
            trackPoint.setTime(StringUtils.parseTime(time));
        } catch (Exception e) {
            throw new ParsingException(createErrorMessage(String.format(Locale.US, "Unable to parse time: %s", time)), e);
        }

        if (latitude == null || longitude == null) {
            return trackPoint;
        }

        try {
            trackPoint.setLatitude(Double.parseDouble(latitude));
            trackPoint.setLongitude(Double.parseDouble(longitude));
        } catch (NumberFormatException e) {
            throw new ParsingException(createErrorMessage(String.format(Locale.US, "Unable to parse latitude longitude: %s %s", latitude, longitude)), e);
        }


        if (altitude != null) {
            try {
                trackPoint.setAltitude(Double.parseDouble(altitude));
            } catch (NumberFormatException e) {
                throw new ParsingException(createErrorMessage(String.format(Locale.US, "Unable to parse altitude: %s", altitude)), e);
            }
        }

        if (speed != null) {
            try {
                trackPoint.setSpeed(Float.parseFloat(speed));
            } catch (Exception e) {
                throw new ParsingException(createErrorMessage(String.format(Locale.US, "Unable to parse speed: %s", speed)), e);
            }
        }
        if (heartrate != null) {
            try {
                trackPoint.setHeartRate_bpm(Float.parseFloat(heartrate));
            } catch (Exception e) {
                throw new ParsingException(createErrorMessage(String.format(Locale.US, "Unable to parse heart rate: %s", heartrate)), e);
            }
        }

        if (cadence != null) {
            try {
                trackPoint.setCyclingCadence_rpm(Float.parseFloat(cadence));
            } catch (Exception e) {
                throw new ParsingException(createErrorMessage(String.format(Locale.US, "Unable to parse cadence: %s", cadence)), e);
            }
        }

        if (gain != null) {
            try {
                trackPoint.setElevationGain(Float.parseFloat(gain));
            } catch (Exception e) {
                throw new ParsingException(createErrorMessage(String.format(Locale.US, "Unable to parse elevation gain: %s", gain)), e);
            }
        }
        if (loss != null) {
            try {
                trackPoint.setElevationLoss(Float.parseFloat(loss));
            } catch (Exception e) {
                throw new ParsingException(createErrorMessage(String.format(Locale.US, "Unable to parse elevation loss: %s", loss)), e);
            }
        }

        return trackPoint;
    }

    /**
     * Inserts a trackPoint.
     *
     * @param trackPoint the trackPoint
     */
    protected void insertTrackPoint(TrackPoint trackPoint) {
        trackData.bufferedTrackPoints.add(trackPoint);
    }

    protected boolean isFirstTrackPointInSegment() {
        return trackData.lastLocationInCurrentSegment == null;
    }

    /**
     * Flushes the TrackPoints to the database.
     */
    private void flushTrackPoints() {
        if (trackData.bufferedTrackPoints.size() > 0) {
            contentProviderUtils.bulkInsertTrackPoint(trackData.bufferedTrackPoints, trackData.track.getId());
            trackData.bufferedTrackPoints.clear();
        }
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

        // The buffered locations
        final List<TrackPoint> bufferedTrackPoints = new ArrayList<>();
    }

    public static class ParsingException extends RuntimeException {

        private ParsingException(@NonNull String message) {
            super(message);
        }

        private ParsingException(@NonNull String message, Exception cause) {
            super(message, cause);
        }

        @NonNull
        @Override
        public String toString() {
            return "" + getMessage();
        }
    }
}
