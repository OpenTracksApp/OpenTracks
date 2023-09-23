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
import android.location.Location;
import android.util.Log;

import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import de.dennisguse.opentracks.data.models.Distance;
import de.dennisguse.opentracks.data.models.Marker;
import de.dennisguse.opentracks.data.models.Speed;
import de.dennisguse.opentracks.data.models.Track;
import de.dennisguse.opentracks.data.models.TrackPoint;
import de.dennisguse.opentracks.io.file.exporter.KMLTrackExporter;
import de.dennisguse.opentracks.util.StringUtils;

/**
 * Imports a KML file; preferred version: KML2.3, but also supports KML2.2.
 *
 * @author Jimmy Shih
 */
public class KmlTrackImporter extends DefaultHandler implements XMLImporter.TrackParser {

    private static final String TAG = KmlTrackImporter.class.getSimpleName();

    private static final String MARKER_STYLE = "#" + KMLTrackExporter.MARKER_STYLE;

    private static final String TAG_COORDINATES = "coordinates";
    private static final String TAG_DESCRIPTION = "description";
    private static final String TAG_ICON = "icon";

    private static final String TAG_COORD = "coord";
    private static final String TAG_KML22_COORD = "gx:coord";

    private static final String TAG_MULTI_TRACK = "MultiTrack";
    private static final String TAG_KML22_MULTI_TRACK = "gx:MultiTrack";

    private static final String TAG_DATA_ACTIVITYTYPE = "Data";

    private static final String TAG_SIMPLE_ARRAY_DATA = "SimpleArrayData";
    private static final String TAG_KML22_SIMPLE_ARRAY_DATA = "gx:SimpleArrayData";

    private static final String TAG_TRACK = "Track";
    private static final String TAG_KML22_TRACK = "gx:Track";

    private static final String TAG_VALUE = "value";
    private static final String TAG_KML22_VALUE = "gx:value";

    private static final String TAG_HREF = "href";
    private static final String TAG_KML = "kml";
    private static final String TAG_NAME = "name";
    private static final String TAG_PHOTO_OVERLAY = "PhotoOverlay";
    private static final String TAG_PLACEMARK = "Placemark";
    private static final String TAG_STYLE_URL = "styleUrl";
    //    private static final String TAG_VALUE = "value"; TODO
    private static final String TAG_WHEN = "when";
    private static final String TAG_UUID = "opentracks:trackid";

    private static final String ATTRIBUTE_NAME = "name";

    private Locator locator;

    private final Context context;

    // Belongs to the current track
    private ZoneOffset zoneOffset;

    private final ArrayList<Instant> whenList = new ArrayList<>();
    private final ArrayList<Location> locationList = new ArrayList<>();

    private String dataType;
    private final ArrayList<Float> sensorSpeedList = new ArrayList<>();
    private final ArrayList<Float> sensorDistanceList = new ArrayList<>();
    private final ArrayList<Float> sensorCadenceList = new ArrayList<>();
    private final ArrayList<Float> sensorHeartRateList = new ArrayList<>();
    private final ArrayList<Float> sensorPowerList = new ArrayList<>();
    private final ArrayList<Float> altitudeGainList = new ArrayList<>();
    private final ArrayList<Float> altitudeLossList = new ArrayList<>();
    private final ArrayList<Float> accuracyHorizontal = new ArrayList<>();
    private final ArrayList<Float> accuracyVertical = new ArrayList<>();

    private final ArrayList<Marker> markers = new ArrayList<>();

    // The current element content
    private String content = "";

    private String icon;
    private String name;
    private String description;
    private String activityType;
    private String latitude;
    private String longitude;
    private String altitude;
    private String markerType;
    private String photoUrl;
    private String uuid;

    private final TrackImporter trackImporter;

    public KmlTrackImporter(Context context, TrackImporter trackImporter) {
        this.context = context;
        this.trackImporter = trackImporter;
    }

    @Override
    public void setDocumentLocator(Locator locator) {
        this.locator = locator;
    }

    @Override
    public void startElement(String uri, String localName, String tag, Attributes attributes) throws SAXException {
        switch (tag) {
            case TAG_PLACEMARK, TAG_PHOTO_OVERLAY ->
                // Note that a track is contained in a Placemark, calling onMarkerStart will clear various track variables like name, category, and description.
                    onMarkerStart();
            case TAG_MULTI_TRACK, TAG_KML22_MULTI_TRACK -> trackImporter.newTrack();
            case TAG_TRACK, TAG_KML22_TRACK -> {
                if (trackImporter == null) {
                    throw new SAXException("Missing " + TAG_MULTI_TRACK);
                }
                onTrackSegmentStart();
            }
            case TAG_DATA_ACTIVITYTYPE, TAG_SIMPLE_ARRAY_DATA, TAG_KML22_SIMPLE_ARRAY_DATA ->
                    dataType = attributes.getValue(ATTRIBUTE_NAME);
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) {
        content += new String(ch, start, length);
    }

    @Override
    public void endElement(String uri, String localName, String tag) throws SAXException {
        switch (tag) {
            case TAG_KML -> onFileEnd();
            case TAG_PLACEMARK, TAG_PHOTO_OVERLAY ->
                // Note that a track is contained in a Placemark, calling onMarkerEnd is save since markerType is not set for a track.
                    onMarkerEnd();
            case TAG_COORDINATES -> onMarkerLocationEnd();
            case TAG_MULTI_TRACK, TAG_KML22_MULTI_TRACK -> {
                trackImporter.setTrack(context, name, uuid, description, activityType, icon, zoneOffset);
                zoneOffset = null;
            }
            case TAG_TRACK, TAG_KML22_TRACK -> onTrackSegmentEnd();
            case TAG_COORD, TAG_KML22_COORD -> onCoordEnded();
            case TAG_VALUE, TAG_KML22_VALUE -> {
                if (KMLTrackExporter.EXTENDED_DATA_TYPE_ACTIVITYTYPE.equals(dataType)) {
                    if (content != null) {
                        activityType = content.trim();
                    }
                } else {
                    onExtendedDataValueEnd();
                }
            }
            case TAG_NAME -> {
                if (content != null) {
                    name = content.trim();
                }
            }
            case TAG_UUID -> {
                if (content != null) {
                    uuid = content.trim();
                }
            }
            case TAG_DESCRIPTION -> {
                if (content != null) {
                    description = content.trim();
                }
            }
            case TAG_ICON -> {
                if (content != null) {
                    icon = content.trim();
                }
            }
            case TAG_WHEN -> {
                if (content != null) {
                    try {
                        OffsetDateTime time = StringUtils.parseTime(content.trim());
                        if (zoneOffset == null) {
                            zoneOffset = time.getOffset();
                        }
                        whenList.add(time.toInstant());
                    } catch (Exception e) {
                        throw new ParsingException(createErrorMessage(String.format(Locale.US, "Unable to parse time: %s", content.trim())), e);
                    }
                }
            }
            case TAG_STYLE_URL -> {
                if (content != null) {
                    markerType = content.trim();
                }
            }
            case TAG_HREF -> {
                if (content != null) {
                    photoUrl = content.trim();
                }
            }
        }

        // Reset element content
        content = "";
    }

    private void onMarkerStart() {
        // Reset all Placemark variables
        name = null;
        icon = null;
        description = null;
        activityType = null;
        photoUrl = null;
        latitude = null;
        longitude = null;
        altitude = null;
        markerType = null;
    }

    private void onMarkerEnd() {
        if (!MARKER_STYLE.equals(markerType)) {
            return;
        }

        if (whenList.size() != 1) {
            Log.w(TAG, "Marker without time ignored.");
            return;
        }

        Location location = createLocation(longitude, latitude, altitude);
        if (location == null) {
            Log.w(TAG, "Marker with invalid coordinates ignored: " + location);
            return;
        }

        Marker marker = new Marker(null, new TrackPoint(TrackPoint.Type.TRACKPOINT, location, whenList.get(0))); //TODO Creating marker without need
        marker.setName(name != null ? name : "");
        marker.setDescription(description != null ? description : "");
        marker.setCategory(activityType != null ? activityType : "");
        marker.setPhotoUrl(photoUrl);
        markers.add(marker);

        name = null;
        description = null;
        activityType = null;
        photoUrl = null;
        whenList.clear();
    }

    private void onMarkerLocationEnd() {
        if (content != null) {
            String[] parts = content.trim().split(",");
            if (parts.length != 2 && parts.length != 3) {
                return;
            }
            longitude = parts[0];
            latitude = parts[1];
            altitude = parts.length == 3 ? parts[2] : null;
        }
    }

    private void onTrackSegmentStart() {
        locationList.clear();
        whenList.clear();

        sensorSpeedList.clear();
        sensorDistanceList.clear();
        sensorHeartRateList.clear();
        sensorCadenceList.clear();
        sensorPowerList.clear();
        altitudeGainList.clear();
        altitudeLossList.clear();
        accuracyHorizontal.clear();
        accuracyVertical.clear();
    }

    private void onTrackSegmentEnd() {
        if (locationList.size() != whenList.size()) {
            throw new ImportParserException("<coords> and <when> should have the same count.");
        }

        // Close a track segment by inserting the segment locations
        for (int i = 0; i < locationList.size(); i++) {
            Instant time = whenList.get(i);
            Location location = locationList.get(i);

            TrackPoint trackPoint = new TrackPoint(TrackPoint.Type.TRACKPOINT, time);
            if (location != null) {
                trackPoint.setLocation(location);
            }

            if (i < sensorSpeedList.size() && sensorSpeedList.get(i) != null) {
                trackPoint.setSpeed(Speed.of(sensorSpeedList.get(i)));

                if (TrackPoint.IDLE_SPEED.greaterOrEqualThan(trackPoint.getSpeed())) {
                    trackPoint.setType(TrackPoint.Type.IDLE);
                }
            }
            if (i < sensorDistanceList.size() && sensorDistanceList.get(i) != null) {
                trackPoint.setSensorDistance(Distance.of(sensorDistanceList.get(i)));
            }
            if (i < sensorHeartRateList.size() && sensorHeartRateList.get(i) != null) {
                trackPoint.setHeartRate(sensorHeartRateList.get(i));
            }
            if (i < sensorCadenceList.size() && sensorCadenceList.get(i) != null) {
                trackPoint.setCadence(sensorCadenceList.get(i));
            }
            if (i < sensorPowerList.size() && sensorPowerList.get(i) != null) {
                trackPoint.setPower(sensorPowerList.get(i));
            }
            if (i < altitudeGainList.size()) {
                trackPoint.setAltitudeGain(altitudeGainList.get(i));
            }
            if (i < altitudeLossList.size()) {
                trackPoint.setAltitudeLoss(altitudeLossList.get(i));
            }
            if (i < accuracyHorizontal.size() && accuracyHorizontal.get(i) != null) {
                trackPoint.setHorizontalAccuracy(Distance.of(accuracyHorizontal.get(i)));
            }
            if (i < accuracyVertical.size() && accuracyVertical.get(i) != null) {
                trackPoint.setVerticalAccuracy(Distance.of(accuracyVertical.get(i)));
            }

            // Update TrackPoint type for START / STOP.
            TrackPoint.Type type = trackPoint.getType();
            if (i == 0) {
                //first
                if (!trackPoint.wasCreatedManually()) {
                    type = TrackPoint.Type.SEGMENT_START_MANUAL;
                } else {
                    type = TrackPoint.Type.SEGMENT_START_AUTOMATIC;
                }
            } else if (i == locationList.size() - 1 && !trackPoint.wasCreatedManually()) {
                //last
                type = TrackPoint.Type.SEGMENT_END_MANUAL;
            }
            trackPoint.setType(type);

            trackImporter.addTrackPoint(trackPoint);
        }
    }

    private void onCoordEnded() {
        String[] parts = content.trim().split(" ");
        if (parts.length == 2 || parts.length == 3) {
            longitude = parts[0];
            latitude = parts[1];
            altitude = parts.length == 3 ? parts[2] : null;
        }

        locationList.add(createLocation(longitude, latitude, altitude));

        longitude = null;
        latitude = null;
        altitude = null;
    }

    private Location createLocation(String longitude, String latitude, String altitude) {
        Location location = null;
        if (longitude != null || latitude != null) {
            location = new Location("import");

            try {
                location.setLatitude(Double.parseDouble(latitude));
                location.setLongitude(Double.parseDouble(longitude));
            } catch (NumberFormatException e) {
                throw new ParsingException(createErrorMessage(String.format(Locale.US, "Unable to parse latitude longitude: %s %s", latitude, longitude)), e);
            }

            if (altitude != null) {
                try {
                    location.setAltitude(Double.parseDouble(altitude));
                } catch (NumberFormatException e) {
                    throw new ParsingException(createErrorMessage(String.format(Locale.US, "Unable to parse altitude: %s", altitude)), e);
                }
            }
        }
        return location;
    }

    private void onExtendedDataValueEnd() throws SAXException {
        Float value = null;
        if (content != null) {
            content = content.trim();
            if (!content.equals("")) {
                try {
                    value = Float.parseFloat(content);
                } catch (NumberFormatException e) {
                    throw new SAXException(createErrorMessage("Unable to parse value:" + content), e);
                }
            }
        }
        switch (dataType) {
            case KMLTrackExporter.EXTENDED_DATA_TYPE_SPEED -> sensorSpeedList.add(value);
            case KMLTrackExporter.EXTENDED_DATA_TYPE_DISTANCE -> sensorDistanceList.add(value);
            case KMLTrackExporter.EXTENDED_DATA_TYPE_POWER -> sensorPowerList.add(value);
            case KMLTrackExporter.EXTENDED_DATA_TYPE_HEART_RATE -> sensorHeartRateList.add(value);
            case KMLTrackExporter.EXTENDED_DATA_TYPE_CADENCE -> sensorCadenceList.add(value);
            case KMLTrackExporter.EXTENDED_DATA_TYPE_ALTITUDE_GAIN -> altitudeGainList.add(value);
            case KMLTrackExporter.EXTENDED_DATA_TYPE_ALTITUDE_LOSS -> altitudeLossList.add(value);
            case KMLTrackExporter.EXTENDED_DATA_TYPE_ACCURACY_HORIZONTAL ->
                    accuracyHorizontal.add(value);
            case KMLTrackExporter.EXTENDED_DATA_TYPE_ACCURACY_VERTICAL ->
                    accuracyVertical.add(value);
            default ->
                    Log.w(TAG, "Data from extended data " + dataType + " is not (yet) supported.");
        }
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
