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

package de.dennisguse.opentracks.io.file.importer;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.helpers.DefaultHandler;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import de.dennisguse.opentracks.data.models.Altitude;
import de.dennisguse.opentracks.data.models.Distance;
import de.dennisguse.opentracks.data.models.Marker;
import de.dennisguse.opentracks.data.models.Position;
import de.dennisguse.opentracks.data.models.Speed;
import de.dennisguse.opentracks.data.models.Track;
import de.dennisguse.opentracks.data.models.TrackPoint;
import de.dennisguse.opentracks.util.StringUtils;

/**
 * Imports a GPX file.
 * Uses:
 * * <a href="https://www8.garmin.com/xmlschemas/TrackPointExtensionv2.xsd">...</a>
 * * <a href="https://www8.garmin.com/xmlschemas/PowerExtensionv1.xsd">...</a>
 * <p>
 * {@link de.dennisguse.opentracks.io.file.exporter.GPXTrackExporter} does not export information if a segment was started automatic or manually.
 * Therefore, all segments starts are marked as SEGMENT_START_AUTOMATIC.
 * Thus, the {@link de.dennisguse.opentracks.stats.TrackStatistics} cannot be restored correctly.
 *
 * @author Jimmy Shih
 */
public class GPXTrackImporter extends DefaultHandler implements XMLImporter.TrackParser {

    private static final String TAG = GPXTrackImporter.class.getSimpleName();

    private static final String TAG_DESCRIPTION = "desc";
    private static final String TAG_ALTITUDE = "ele";
    private static final String TAG_GPX = "gpx";
    private static final String TAG_NAME = "name";
    private static final String TAG_TIME = "time";
    private static final String TAG_TRACK = "trk";
    private static final String TAG_TRACK_POINT = "trkpt";
    private static final String TAG_TRACK_SEGMENT = "trkseg";
    private static final String TAG_TYPE = "type";
    private static final String TAG_TYPE_LOCALIZED = "opentracks:typeTranslated";
    private static final String TAG_MARKER = "wpt";
    private static final String TAG_ID = "opentracks:trackid";

    private static final String ATTRIBUTE_LAT = "lat";
    private static final String ATTRIBUTE_LON = "lon";

    private static final String TAG_EXTENSION_SPEED = "gpxtpx:speed";
    /**
     * Often speed is exported without the proper namespace.
     */
    private static final String TAG_EXTENSION_SPEED_COMPAT = "speed";
    private static final String TAG_EXTENSION_HEARTRATE = "gpxtpx:hr";
    private static final String TAG_EXTENSION_CADENCE = "gpxtpx:cad";
    private static final String TAG_EXTENSION_POWER = "pwr:PowerInWatts";

    private static final String TAG_EXTENSION_GAIN = "opentracks:gain";
    private static final String TAG_EXTENSION_LOSS = "opentracks:loss";
    private static final String TAG_EXTENSION_DISTANCE = "opentracks:distance";
    private static final String TAG_EXTENSION_ACCURACY_HORIZONTAL = "opentracks:accuracy_horizontal";
    private static final String TAG_EXTENSION_ACCURACY_VERTICAL = "opentracks:accuracy_vertical";
    private Locator locator;

    private final Context context;

    private ZoneOffset zoneOffset;

    // Belongs to the current track
    private final ArrayList<Marker> markers = new ArrayList<>();

    // The current element content
    private String content = "";

    private String name;
    private String description;
    private String activityType;
    private String activityTypeLocalized;
    private String latitude;
    private String longitude;
    private String altitude;
    private String time;
    private String speed;
    private String heartrate;
    private String cadence;
    private String power;
    private String markerType;
    private Uri photoUrl;
    private String uuid;
    private String gain;
    private String loss;
    private String sensorDistance;
    private String accuracyHorizontal;
    private String accuracyVertical;

    private final LinkedList<TrackPoint> currentSegment = new LinkedList<>();

    private final TrackImporter trackImporter;

    public GPXTrackImporter(Context context, TrackImporter trackImporter) {
        this.context = context;
        this.trackImporter = trackImporter;
    }

    @Override
    public void setDocumentLocator(Locator locator) {
        this.locator = locator;
    }

    @Override
    public void startElement(String uri, String localName, String tag, Attributes attributes) {
        switch (tag) {
            case TAG_MARKER:
                onMarkerStart(attributes);
                break;
            case TAG_TRACK:
                trackImporter.newTrack();
                break;
            case TAG_TRACK_SEGMENT:
                //Nothing to do here.
                break;
            case TAG_TRACK_POINT:
                onTrackPointStart(attributes);
                break;
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) {
        content += new String(ch, start, length);
    }

    @Override
    public void endElement(String uri, String localName, String tag) {
        switch (tag) {
            case TAG_GPX -> onFileEnd();
            case TAG_MARKER -> onMarkerEnd();
            case TAG_TRACK -> {
                if (activityTypeLocalized == null) {
                    // Backward compatibility: up v4.9.1 as <type> contained localized content.
                    activityTypeLocalized = activityType;
                }
                trackImporter.setTrack(context, name, uuid, description, activityTypeLocalized, activityType, zoneOffset);
                zoneOffset = null;
            }
            case TAG_TRACK_SEGMENT -> onTrackSegmentEnd();
            case TAG_TRACK_POINT -> currentSegment.add(createTrackPoint());
            case TAG_NAME -> {
                if (content != null) {
                    name = content.trim();
                }
            }
            case TAG_DESCRIPTION -> {
                if (content != null) {
                    description = content.trim();
                }
            }
            case TAG_TYPE -> { //Track or Marker/WPT
                if (content != null) {
                    // In older  version this might be localized content.
                    activityType = content.trim();
                    markerType = content.trim();
                }
            }
            case TAG_TYPE_LOCALIZED -> {
                if (content != null) {
                    activityTypeLocalized = content.trim();
                }
            }
            case TAG_TIME -> {
                if (content != null) {
                    time = content.trim();
                }
            }
            case TAG_ALTITUDE -> {
                if (content != null) {
                    altitude = content.trim();
                }
            }
            case TAG_EXTENSION_SPEED, TAG_EXTENSION_SPEED_COMPAT -> {
                if (content != null) {
                    speed = content.trim();
                }
            }
            case TAG_EXTENSION_HEARTRATE -> {
                if (content != null) {
                    heartrate = content.trim();
                }
            }
            case TAG_EXTENSION_CADENCE -> {
                if (content != null) {
                    cadence = content.trim();
                }
            }
            case TAG_EXTENSION_POWER -> {
                if (content != null) {
                    power = content.trim();
                }
            }
            case TAG_ID -> {
                if (content != null) {
                    uuid = content.trim();
                }
            }
            case TAG_EXTENSION_GAIN -> {
                if (content != null) {
                    gain = content.trim();
                }
            }
            case TAG_EXTENSION_LOSS -> {
                if (content != null) {
                    loss = content.trim();
                }
            }
            case TAG_EXTENSION_DISTANCE -> {
                if (content != null) {
                    sensorDistance = content.trim();
                }
            }
            case TAG_EXTENSION_ACCURACY_HORIZONTAL -> {
                if (content != null) {
                    accuracyHorizontal = content.trim();
                }
            }
            case TAG_EXTENSION_ACCURACY_VERTICAL -> {
                if (content != null) {
                    accuracyVertical = content.trim();
                }
            }
        }

        content = "";
    }

    private void onTrackSegmentEnd() {
        if (currentSegment.isEmpty()) {
            Log.w(TAG, "No TrackPoints in current segment.");
            return;
        }

        TrackPoint first = currentSegment.getFirst();
        first.setType(TrackPoint.Type.SEGMENT_START_AUTOMATIC);

        trackImporter.addTrackPoints(currentSegment);
        currentSegment.clear();
    }


    private TrackPoint createTrackPoint() throws ParsingException {
        OffsetDateTime parsedTime;
        try {
            parsedTime = StringUtils.parseTime(time);
            if (zoneOffset == null) {
                zoneOffset = parsedTime.getOffset();
            }
        } catch (Exception e) {
            throw new ParsingException(createErrorMessage(String.format(Locale.US, "Unable to parse time: %s", time)), e);
        }

        if (latitude == null || longitude == null) {
            return new TrackPoint(TrackPoint.Type.TRACKPOINT, parsedTime.toInstant());
        }

        double latitudeParsed;
        double longitudeParsed;
        Distance accuracyHorizontalParsed = null;
        Altitude.WGS84 altitudeParsed = null;
        Distance accuracyVerticalParsed = null;
        Speed speedParsed = null;

        try {
            latitudeParsed = Double.parseDouble(latitude);
            longitudeParsed = Double.parseDouble(longitude);
        } catch (NumberFormatException e) {
            throw new ParsingException(createErrorMessage(String.format(Locale.US, "Unable to parse latitude longitude: %s %s", latitude, longitude)), e);
        }
        if (accuracyHorizontal != null) {
            try {
                accuracyHorizontalParsed = Distance.of(accuracyHorizontal);
            } catch (NumberFormatException e) {
                throw new ParsingException(createErrorMessage(String.format(Locale.US, "Unable to parse accuracy_horizontal: %s", sensorDistance)), e);
            }
        }

        if (altitude != null) {
            try {
                altitudeParsed = Altitude.WGS84.of(Double.parseDouble(altitude));
            } catch (NumberFormatException e) {
                throw new ParsingException(createErrorMessage(String.format(Locale.US, "Unable to parse altitude: %s", altitude)), e);
            }
        }
        if (accuracyVertical != null) {
            try {
                accuracyVerticalParsed = Distance.of(accuracyVertical);
            } catch (NumberFormatException e) {
                throw new ParsingException(createErrorMessage(String.format(Locale.US, "Unable to parse accuracy_vertical: %s", accuracyVertical)), e);
            }
        }

        if (speed != null) {
            try {
                speedParsed = Speed.of(speed);
            } catch (NumberFormatException e) {
                throw new ParsingException(createErrorMessage(String.format(Locale.US, "Unable to parse speed: %s", speed)), e);
            }
        }

        TrackPoint trackPoint = new TrackPoint(
                TrackPoint.Type.TRACKPOINT,
                new Position(
                parsedTime.toInstant(),
                latitudeParsed,
                longitudeParsed,
                accuracyHorizontalParsed,
                altitudeParsed,
                accuracyVerticalParsed,
                null,
                speedParsed
        ));

        if (heartrate != null) {
            try {
                trackPoint.setHeartRate(Float.parseFloat(heartrate));
            } catch (NumberFormatException e) {
                throw new ParsingException(createErrorMessage(String.format(Locale.US, "Unable to parse heart rate: %s", heartrate)), e);
            }
        }

        if (cadence != null) {
            try {
                trackPoint.setCadence(Float.parseFloat(cadence));
            } catch (Exception e) {
                throw new ParsingException(createErrorMessage(String.format(Locale.US, "Unable to parse cadence: %s", cadence)), e);
            }
        }

        if (power != null) {
            try {
                trackPoint.setPower(Float.parseFloat(power));
            } catch (NumberFormatException e) {
                throw new ParsingException(createErrorMessage(String.format(Locale.US, "Unable to parse power: %s", power)), e);
            }
        }

        if (gain != null) {
            try {
                trackPoint.setAltitudeGain(Float.parseFloat(gain));
            } catch (NumberFormatException e) {
                throw new ParsingException(createErrorMessage(String.format(Locale.US, "Unable to parse altitude gain: %s", gain)), e);
            }
        }
        if (loss != null) {
            try {
                trackPoint.setAltitudeLoss(Float.parseFloat(loss));
            } catch (NumberFormatException e) {
                throw new ParsingException(createErrorMessage(String.format(Locale.US, "Unable to parse altitude loss: %s", loss)), e);
            }
        }
        if (sensorDistance != null) {
            try {
                trackPoint.setSensorDistance(Distance.of(sensorDistance));
            } catch (NumberFormatException e) {
                throw new ParsingException(createErrorMessage(String.format(Locale.US, "Unable to parse distance: %s", sensorDistance)), e);
            }
        }

        return trackPoint;
    }

    private void onTrackPointStart(Attributes attributes) {
        latitude = attributes.getValue(ATTRIBUTE_LAT);
        longitude = attributes.getValue(ATTRIBUTE_LON);
        altitude = null;
        time = null;
        speed = null;

        gain = null;
        loss = null;

        sensorDistance = null;
        accuracyHorizontal = null;
        accuracyVertical = null;
        power = null;
        heartrate = null;
        cadence = null;
    }

    private void onMarkerStart(Attributes attributes) {
        name = null;
        description = null;
        photoUrl = null;
        latitude = attributes.getValue(ATTRIBUTE_LAT);
        longitude = attributes.getValue(ATTRIBUTE_LON);
        altitude = null;
        time = null;
        markerType = null;
    }

    private void onMarkerEnd() {
        // Markers must have a time, else cannot match to the track points
        if (time == null) {
            Log.w(TAG, "Marker without time; ignored.");
            return;
        }

        TrackPoint trackPoint = createTrackPoint();

        if (!trackPoint.hasLocation()) {
            Log.w(TAG, "Marker with invalid coordinates ignored: " + trackPoint.getPosition());
            return;
        }
        Marker marker = new Marker(null, trackPoint);

        if (name != null) {
            marker.setName(name);
        }
        if (description != null) {
            marker.setDescription(description);
        }
        if (markerType != null) {
            marker.setCategory(markerType);
        }

        if (photoUrl != null) {
            marker.setPhotoUrl(photoUrl);
        }
        markers.add(marker);
    }

    private String createErrorMessage(String message) {
        return String.format(Locale.US, "Parsing error at line: %d column: %d. %s", locator.getLineNumber(), locator.getColumnNumber(), message);
    }

    private void onFileEnd() {
        trackImporter.addMarkers(markers);
        trackImporter.finish();
    }

    @Override
    public DefaultHandler getHandler() {
        return this;
    }

    @Override
    public List<Track.Id> getImportTrackIds() {
        return trackImporter.getTrackIds();
    }

    @Override
    public void cleanImport() {
        trackImporter.cleanImport();
    }
}
