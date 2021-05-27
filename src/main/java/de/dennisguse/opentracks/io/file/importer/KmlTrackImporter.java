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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import de.dennisguse.opentracks.content.data.Distance;
import de.dennisguse.opentracks.content.data.Marker;
import de.dennisguse.opentracks.content.data.Speed;
import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.content.data.TrackPoint;
import de.dennisguse.opentracks.io.file.exporter.KMLTrackExporter;
import de.dennisguse.opentracks.util.StringUtils;

/**
 * Imports a KML file.
 *
 * @author Jimmy Shih
 */
public class KmlTrackImporter extends DefaultHandler implements XMLImporter.TrackParser {

    private static final String TAG = KmlTrackImporter.class.getSimpleName();

    private static final String MARKER_STYLE = "#" + KMLTrackExporter.MARKER_STYLE;

    private static final String TAG_COORDINATES = "coordinates";
    private static final String TAG_DESCRIPTION = "description";
    private static final String TAG_ICON = "icon";
    private static final String TAG_GX_COORD = "gx:coord";
    private static final String TAG_GX_MULTI_TRACK = "gx:MultiTrack";
    private static final String TAG_GX_SIMPLE_ARRAY_DATA = "gx:SimpleArrayData";
    private static final String TAG_GX_TRACK = "gx:Track";
    private static final String TAG_GX_VALUE = "gx:value";
    private static final String TAG_HREF = "href";
    private static final String TAG_KML = "kml";
    private static final String TAG_NAME = "name";
    private static final String TAG_PHOTO_OVERLAY = "PhotoOverlay";
    private static final String TAG_PLACEMARK = "Placemark";
    private static final String TAG_STYLE_URL = "styleUrl";
    private static final String TAG_VALUE = "value";
    private static final String TAG_WHEN = "when";
    private static final String TAG_UUID = "opentracks:trackid";

    private static final String ATTRIBUTE_NAME = "name";

    private Locator locator;

    private final Context context;

    // Belongs to the current track
    private final ArrayList<Instant> whenList = new ArrayList<>();
    private final ArrayList<Location> locationList = new ArrayList<>();

    private String extendedDataType;
    private final ArrayList<Float> sensorSpeedList = new ArrayList<>();
    private final ArrayList<Float> sensorDistanceList = new ArrayList<>();
    private final ArrayList<Float> sensorCadenceList = new ArrayList<>();
    private final ArrayList<Float> sensorHeartRateList = new ArrayList<>();
    private final ArrayList<Float> sensorPowerList = new ArrayList<>();
    private final ArrayList<Float> altitudeGainList = new ArrayList<>();
    private final ArrayList<Float> altitudeLossList = new ArrayList<>();

    private final ArrayList<Marker> markers = new ArrayList<>();

    // The current element content
    private String content = "";

    private String icon;
    private String name;
    private String description;
    private String category;
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
            case TAG_PLACEMARK:
            case TAG_PHOTO_OVERLAY:
                // Note that a track is contained in a Placemark, calling onMarkerStart will clear various track variables like name, category, and description.
                onMarkerStart();
                break;
            case TAG_GX_MULTI_TRACK:
                trackImporter.newTrack();
                break;
            case TAG_GX_TRACK:
                if (trackImporter == null) {
                    throw new SAXException("No " + TAG_GX_MULTI_TRACK);
                }
                onTrackSegmentStart();
                break;
            case TAG_GX_SIMPLE_ARRAY_DATA:
                onExtendedDataStart(attributes);
                break;
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) {
        content += new String(ch, start, length);
    }

    @Override
    public void endElement(String uri, String localName, String tag) throws SAXException {
        switch (tag) {
            case TAG_KML:
                onFileEnd();
                break;
            case TAG_PLACEMARK:
            case TAG_PHOTO_OVERLAY:
                // Note that a track is contained in a Placemark, calling onMarkerEnd is save since markerType is not set for a track.
                onMarkerEnd();
                break;
            case TAG_COORDINATES:
                onMarkerLocationEnd();
                break;
            case TAG_GX_MULTI_TRACK:
                trackImporter.setTrack(context, name, uuid, description, category, icon);
                break;
            case TAG_GX_TRACK:
                onTrackSegmentEnd();
                break;
            case TAG_GX_COORD:
                onCoordEnded();
                break;
            case TAG_GX_VALUE:
                onExtendedDataValueEnd();
                break;
            case TAG_NAME:
                if (content != null) {
                    name = content.trim();
                }
                break;
            case TAG_UUID:
                if (content != null) {
                    uuid = content.trim();
                }
                break;
            case TAG_DESCRIPTION:
                if (content != null) {
                    description = content.trim();
                }
                break;
            case TAG_ICON:
                if (content != null) {
                    icon = content.trim();
                }
                break;
            case TAG_VALUE:
                if (content != null) {
                    category = content.trim();
                }
                break;
            case TAG_WHEN:
                if (content != null) {
                    whenList.add(StringUtils.parseTime(content.trim()));
                }

                break;
            case TAG_STYLE_URL:
                if (content != null) {
                    markerType = content.trim();
                }
                break;
            case TAG_HREF:
                if (content != null) {
                    photoUrl = content.trim();
                }
                break;
        }

        // Reset element content
        content = "";
    }

    private void onMarkerStart() {
        // Reset all Placemark variables
        name = null;
        icon = null;
        description = null;
        category = null;
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
        marker.setCategory(category != null ? category : "");
        marker.setPhotoUrl(photoUrl);
        markers.add(marker);

        name = null;
        description = null;
        category = null;
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
    }

    private void onTrackSegmentEnd() {
        if (locationList.size() != whenList.size()) {
            throw new ImportParserException("<coords> and <when> should have the same count.");
        }

        // Close a track segment by inserting the segment locations
        for (int i = 0; i < locationList.size(); i++) {
            Instant time = whenList.get(i);
            Location location = locationList.get(i);

            TrackPoint trackPoint;
            if (i == 0) {
                if (location == null) {
                    trackPoint = TrackPoint.createSegmentStartManualWithTime(time);
                } else {
                    trackPoint = new TrackPoint(TrackPoint.Type.SEGMENT_START_AUTOMATIC, location, time);
                }
            } else if (i == locationList.size() - 1 && location == null) {
                trackPoint = TrackPoint.createSegmentEndWithTime(time);
            } else {
                trackPoint = new TrackPoint(TrackPoint.Type.TRACKPOINT, location, time);
            }

            if (i < sensorSpeedList.size() && sensorSpeedList.get(i) != null) {
                trackPoint.setSpeed(Speed.of(sensorSpeedList.get(i)));
            }
            if (i < sensorDistanceList.size() && sensorDistanceList.get(i) != null) {
                trackPoint.setSensorDistance(Distance.of(sensorDistanceList.get(i)));
            }
            if (i < sensorHeartRateList.size()) {
                trackPoint.setHeartRate_bpm(sensorHeartRateList.get(i));
            }
            if (i < sensorCadenceList.size()) {
                trackPoint.setCyclingCadence_rpm(sensorCadenceList.get(i));
            }
            if (i < sensorPowerList.size()) {
                trackPoint.setPower(sensorPowerList.get(i));
            }
            if (i < altitudeGainList.size()) {
                trackPoint.setAltitudeGain(altitudeGainList.get(i));
            }
            if (i < altitudeLossList.size()) {
                trackPoint.setAltitudeLoss(altitudeLossList.get(i));
            }

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


    private void onExtendedDataStart(Attributes attributes) {
        extendedDataType = attributes.getValue(ATTRIBUTE_NAME);
    }

    private void onExtendedDataValueEnd() throws SAXException {
        Float value = null;
        if (content != null) {
            content = content.trim();
            if (!content.equals("")) {
                try {
                    value = Float.parseFloat(content);
                } catch (NumberFormatException e) {
                    throw new SAXException(createErrorMessage("Unable to parse gx:value:" + content), e);
                }
            }
        }
        switch (extendedDataType) {
            case KMLTrackExporter.EXTENDED_DATA_TYPE_SPEED:
                sensorSpeedList.add(value);
                break;
            case KMLTrackExporter.EXTENDED_DATA_TYPE_DISTANCE:
                sensorDistanceList.add(value);
                break;
            case KMLTrackExporter.EXTENDED_DATA_TYPE_POWER:
                sensorPowerList.add(value);
                break;
            case KMLTrackExporter.EXTENDED_DATA_TYPE_HEART_RATE:
                sensorHeartRateList.add(value);
                break;
            case KMLTrackExporter.EXTENDED_DATA_TYPE_CADENCE:
                sensorCadenceList.add(value);
                break;
            case KMLTrackExporter.EXTENDED_DATA_TYPE_ALTITUDE_GAIN:
                altitudeGainList.add(value);
                break;
            case KMLTrackExporter.EXTENDED_DATA_TYPE_ALTITUDE_LOSS:
                altitudeLossList.add(value);
                break;
            default:
                Log.w(TAG, "Data from extended data " + extendedDataType + " is not (yet) supported.");
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
