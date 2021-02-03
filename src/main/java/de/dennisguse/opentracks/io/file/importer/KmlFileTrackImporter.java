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
import android.util.Log;

import androidx.annotation.VisibleForTesting;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import java.util.ArrayList;

import de.dennisguse.opentracks.content.data.TrackPoint;
import de.dennisguse.opentracks.content.provider.ContentProviderUtils;
import de.dennisguse.opentracks.io.file.exporter.KMLTrackExporter;

/**
 * Imports a KML file.
 *
 * @author Jimmy Shih
 */
public class KmlFileTrackImporter extends AbstractFileTrackImporter {

    private static final String TAG = KmlFileTrackImporter.class.getSimpleName();

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

    private boolean trackStarted = false;
    private String extendedDataType;
    private final ArrayList<TrackPoint> trackPoints = new ArrayList<>();
    private final ArrayList<Float> speedList = new ArrayList<>();
    private final ArrayList<Float> distanceList = new ArrayList<>();
    private final ArrayList<Float> cadenceList = new ArrayList<>();
    private final ArrayList<Float> heartRateList = new ArrayList<>();
    private final ArrayList<Float> powerList = new ArrayList<>();
    private final ArrayList<Float> elevationGainList = new ArrayList<>();
    private final ArrayList<Float> elevationLossList = new ArrayList<>();

    public KmlFileTrackImporter(Context context) {
        this(context, new ContentProviderUtils(context));
    }

    @VisibleForTesting
    KmlFileTrackImporter(Context context, ContentProviderUtils contentProviderUtils) {
        super(context, contentProviderUtils);
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
                trackStarted = true;
                onTrackStart();
                break;
            case TAG_GX_TRACK:
                if (!trackStarted) {
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
                onTrackEnd();
                break;
            case TAG_GX_TRACK:
                onTrackSegmentEnd();
                break;
            case TAG_GX_COORD:
                onTrackPointEnd();
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
                    time = content.trim();
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
        content = null;
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
        time = null;
        markerType = null;
    }

    private void onMarkerEnd() throws SAXException {
        if (!MARKER_STYLE.equals(markerType)) {
            return;
        }
        addMarker();
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

    @Override
    protected void onTrackSegmentStart() {
        super.onTrackSegmentStart();
        trackPoints.clear();
        speedList.clear();
        distanceList.clear();
        heartRateList.clear();
        cadenceList.clear();
        powerList.clear();
        elevationGainList.clear();
        elevationLossList.clear();
    }

    protected void onTrackSegmentEnd() {
        super.onTrackSegmentEnd();
        // Close a track segment by inserting the segment locations
        for (int i = 0; i < trackPoints.size(); i++) {
            TrackPoint trackPoint = trackPoints.get(i);

            if (i < speedList.size()) {
                trackPoint.setSpeed(speedList.get(i));
            }
            if (i < distanceList.size()) {
                trackPoint.setSensorDistance(distanceList.get(i));
            }
            if (i < heartRateList.size()) {
                trackPoint.setHeartRate_bpm(heartRateList.get(i));
            }
            if (i < cadenceList.size()) {
                trackPoint.setCyclingCadence_rpm(cadenceList.get(i));
            }
            if (i < powerList.size()) {
                trackPoint.setPower(powerList.get(i));
            }
            if (i < elevationGainList.size()) {
                trackPoint.setElevationGain(elevationGainList.get(i));
            }
            if (i < elevationLossList.size()) {
                trackPoint.setElevationLoss(elevationLossList.get(i));
            }

            insertTrackPoint(trackPoint);
        }
    }

    /**
     * On track point end. gx:coord end tag.
     */
    private void onTrackPointEnd() throws SAXException {
        // Add trackPoint to trackPoints
        if (content == null) {
            return;
        }
        String[] parts = content.trim().split(" ");
        if (parts.length == 2 || parts.length == 3) {
            longitude = parts[0];
            latitude = parts[1];
            altitude = parts.length == 3 ? parts[2] : null;
        }

        // Similar to GPX
        boolean isFirstTrackPointInSegment = isFirstTrackPointInSegment();
        TrackPoint trackPoint = getTrackPoint();
        if (isFirstTrackPointInSegment) {
            TrackPoint.Type type = !trackPoint.hasLocation() ? TrackPoint.Type.SEGMENT_START_MANUAL : TrackPoint.Type.SEGMENT_START_AUTOMATIC;

            trackPoint.setType(type);
        }
        trackPoints.add(trackPoint);

        // Reset variables for next trackpoint (which might not have such data).
        time = null;
        longitude = null;
        latitude = null;
        altitude = null;
    }

    /**
     * On extended data start. gx:SimpleArrayData start tag.
     */
    private void onExtendedDataStart(Attributes attributes) {
        extendedDataType = attributes.getValue(ATTRIBUTE_NAME);
    }

    /**
     * On extended data value end. gx:value end tag.
     */
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
                speedList.add(value);
                break;
            case KMLTrackExporter.EXTENDED_DATA_TYPE_DISTANCE:
                distanceList.add(value);
                break;
            case KMLTrackExporter.EXTENDED_DATA_TYPE_POWER:
                powerList.add(value);
                break;
            case KMLTrackExporter.EXTENDED_DATA_TYPE_HEART_RATE:
                heartRateList.add(value);
                break;
            case KMLTrackExporter.EXTENDED_DATA_TYPE_CADENCE:
                cadenceList.add(value);
                break;
            case KMLTrackExporter.EXTENDED_DATA_TYPE_ELEVATION_GAIN:
                elevationGainList.add(value);
                break;
            case KMLTrackExporter.EXTENDED_DATA_TYPE_ELEVATION_LOSS:
                elevationLossList.add(value);
                break;
            default:
                Log.w(TAG, "Data from extended data " + extendedDataType + " is not (yet) supported.");
        }
    }
}
