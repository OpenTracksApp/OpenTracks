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
import android.util.Log;

import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.helpers.DefaultHandler;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import de.dennisguse.opentracks.content.data.Altitude;
import de.dennisguse.opentracks.content.data.Distance;
import de.dennisguse.opentracks.content.data.Marker;
import de.dennisguse.opentracks.content.data.Speed;
import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.content.data.TrackPoint;
import de.dennisguse.opentracks.util.StringUtils;

/**
 * Imports a GPX file.
 * Uses:
 * * https://www8.garmin.com/xmlschemas/TrackPointExtensionv2.xsd
 * * https://www8.garmin.com/xmlschemas/PowerExtensionv1.xsd
 * <p>
 * {@link de.dennisguse.opentracks.io.file.exporter.GPXTrackExporter} does not export information if a segment was started automatic or manually.
 * Therefore, all segments starts are marked as SEGMENT_START_AUTOMATIC.
 * Thus, the {@link de.dennisguse.opentracks.stats.TrackStatistics} cannot be restored correctly.
 *
 * @author Jimmy Shih
 */
public class GpxTrackImporter extends DefaultHandler implements XMLImporter.TrackParser {

    private static final String TAG = GpxTrackImporter.class.getSimpleName();

    private static final String TAG_DESCRIPTION = "desc";
    private static final String TAG_COMMENT = "cmt";
    private static final String TAG_ALTITUDE = "ele";
    private static final String TAG_GPX = "gpx";
    private static final String TAG_NAME = "name";
    private static final String TAG_TIME = "time";
    private static final String TAG_TRACK = "trk";
    private static final String TAG_TRACK_POINT = "trkpt";
    private static final String TAG_TRACK_SEGMENT = "trkseg";
    private static final String TAG_TYPE = "type";
    private static final String TAG_MARKER = "wpt";
    private static final String TAG_ID = "opentracks:trackid";

    private static final String ATTRIBUTE_LAT = "lat";
    private static final String ATTRIBUTE_LON = "lon";

    private static final String TAG_EXTENSION_SPEED = "gpxtpx:speed";
    private static final String TAG_EXTENSION_HEARTRATE = "gpxtpx:hr";
    private static final String TAG_EXTENSION_CADENCE = "gpxtpx:cad";
    private static final String TAG_EXTENSION_POWER = "pwr:PowerInWatts";

    private static final String TAG_EXTENSION_GAIN = "opentracks:gain";
    private static final String TAG_EXTENSION_LOSS = "opentracks:loss";
    private static final String TAG_EXTENSION_DISTANCE = "opentracks:distance";

    private Locator locator;

    private final Context context;

    // Belongs to the current track
    private final ArrayList<Marker> markers = new ArrayList<>();

    // The current element content
    private String content = "";

    private String name;
    private String description;
    private String category;
    private String latitude;
    private String longitude;
    private String altitude;
    private String time;
    private String speed;
    private String heartrate;
    private String cadence;
    private String power;
    protected String markerType;
    protected String photoUrl;
    protected String uuid;
    protected String gain;
    protected String loss;
    protected String distance;

    private final LinkedList<TrackPoint> currentSegment = new LinkedList<>();

    private final TrackImporter trackImporter;

    public GpxTrackImporter(Context context, TrackImporter trackImporter) {
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
            case TAG_GPX:
                onFileEnd();
                break;
            case TAG_MARKER:
                onMarkerEnd();
                break;
            case TAG_TRACK:
                trackImporter.setTrack(context, name, uuid, description, category, null);
                break;
            case TAG_TRACK_SEGMENT:
                onTrackSegmentEnd();
                break;
            case TAG_TRACK_POINT:
                currentSegment.add(createTrackPoint());
                break;
            case TAG_NAME:
                if (content != null) {
                    name = content.trim();
                }
                break;
            case TAG_DESCRIPTION:
                if (content != null) {
                    description = content.trim();
                }
                break;
            case TAG_TYPE:
                if (content != null) {
                    category = content.trim();
                }
                break;
            case TAG_TIME:
                if (content != null) {
                    time = content.trim();
                }
                break;
            case TAG_ALTITUDE:
                if (content != null) {
                    altitude = content.trim();
                }
                break;
            case TAG_COMMENT:
                if (content != null) {
                    markerType = content.trim();
                }
                break;
            case TAG_EXTENSION_SPEED:
                if (content != null) {
                    speed = content.trim();
                }
                break;
            case TAG_EXTENSION_HEARTRATE:
                if (content != null) {
                    heartrate = content.trim();
                }
                break;
            case TAG_EXTENSION_CADENCE:
                if (content != null) {
                    cadence = content.trim();
                }
                break;
            case TAG_EXTENSION_POWER:
                if (content != null) {
                    power = content.trim();
                }
                break;
            case TAG_ID:
                if (content != null) {
                    uuid = content.trim();
                }
                break;
            case TAG_EXTENSION_GAIN:
                if (content != null) {
                    gain = content.trim();
                }
                break;
            case TAG_EXTENSION_LOSS:
                if (content != null) {
                    loss = content.trim();
                }
                break;
            case TAG_EXTENSION_DISTANCE:
                if (content != null) {
                    distance = content.trim();
                }
                break;
        }

        content = "";
    }

    private void onTrackSegmentEnd() {
        if (currentSegment.isEmpty()) {
            Log.w(TAG, "No locations in current segment.");
            return;
        }

        TrackPoint first = currentSegment.getFirst();
        first.setType(TrackPoint.Type.SEGMENT_START_AUTOMATIC);

        trackImporter.addTrackPoints(currentSegment);
        currentSegment.clear();
    }


    private TrackPoint createTrackPoint() throws ParsingException {
        Instant parsedTime = null;
        try {
            parsedTime = StringUtils.parseTime(time);
        } catch (Exception e) {
            throw new ParsingException(createErrorMessage(String.format(Locale.US, "Unable to parse time: %s", time)), e);
        }

        TrackPoint trackPoint = new TrackPoint(TrackPoint.Type.TRACKPOINT, parsedTime);
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
                trackPoint.setAltitude(Altitude.WGS84.of(Double.parseDouble(altitude)));
            } catch (NumberFormatException e) {
                throw new ParsingException(createErrorMessage(String.format(Locale.US, "Unable to parse altitude: %s", altitude)), e);
            }
        }

        if (speed != null) {
            try {
                trackPoint.setSpeed(Speed.of(speed));
            } catch (NumberFormatException e) {
                throw new ParsingException(createErrorMessage(String.format(Locale.US, "Unable to parse speed: %s", speed)), e);
            }
        }
        if (heartrate != null) {
            try {
                trackPoint.setHeartRate_bpm(Float.parseFloat(heartrate));
            } catch (NumberFormatException e) {
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
        if (distance != null) {
            try {
                trackPoint.setSensorDistance(Distance.of(distance));
            } catch (NumberFormatException e) {
                throw new ParsingException(createErrorMessage(String.format(Locale.US, "Unable to parse distance: %s", distance)), e);
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
        power = null;
        gain = null;
        loss = null;
    }

    private void onMarkerStart(Attributes attributes) {
        name = null;
        description = null;
        category = null;
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
