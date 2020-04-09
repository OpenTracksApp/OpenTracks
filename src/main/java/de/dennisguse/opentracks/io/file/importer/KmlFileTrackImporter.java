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
import de.dennisguse.opentracks.content.sensor.SensorDataSet;
import de.dennisguse.opentracks.io.file.exporter.KmlTrackWriter;

/**
 * Imports a KML file.
 *
 * @author Jimmy Shih
 */
public class KmlFileTrackImporter extends AbstractFileTrackImporter {

    private static final String TAG = KmlFileTrackImporter.class.getSimpleName();

    private static final String WAYPOINT_STYLE = "#" + KmlTrackWriter.WAYPOINT_STYLE;

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

    private static final String ATTRIBUTE_NAME = "name";

    private boolean trackStarted = false;
    private String sensorType;
    private ArrayList<TrackPoint> trackPoints;
    private ArrayList<Float> cadenceList;
    private ArrayList<Float> heartRateList;
    private ArrayList<Float> powerList;

    /**
     * Constructor.
     *
     * @param context       the context
     * @param importTrackId track id to import to. -1L to import to a new track.
     */
    public KmlFileTrackImporter(Context context, long importTrackId) {
        this(context, importTrackId, new ContentProviderUtils(context));
    }

    @VisibleForTesting
    KmlFileTrackImporter(Context context, long importTrackId, ContentProviderUtils contentProviderUtils) {
        super(context, importTrackId, contentProviderUtils);
    }

    @Override
    public void startElement(String uri, String localName, String tag, Attributes attributes)
            throws SAXException {
        switch (tag) {
            case TAG_PLACEMARK:
            case TAG_PHOTO_OVERLAY:
                // Note that a track is contained in a Placemark, calling onWaypointStart will clear various track variables like name, category, and description.
                onWaypointStart();
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
                onSensorDataStart(attributes);
                break;
        }
    }

    @Override
    public void endElement(String uri, String localName, String tag) throws SAXException {
        //TODO Check if order is relevant (uses localname and tag); and convert to switch statement
        if (tag.equals(TAG_KML)) {
            onFileEnd();
        } else if (tag.equals(TAG_PLACEMARK) || tag.equals(TAG_PHOTO_OVERLAY)) {
            // Note that a track is contained in a Placemark, calling onWaypointend is save since waypointType is not set for a track.
            onWaypointEnd();
        } else if (localName.equals(TAG_COORDINATES)) {
            onWaypointLocationEnd();
        } else if (tag.equals(TAG_GX_MULTI_TRACK)) {
            onTrackEnd();
        } else if (tag.equals(TAG_GX_TRACK)) {
            onTrackSegmentEnd();
        } else if (tag.equals(TAG_GX_COORD)) {
            onTrackPointEnd();
        } else if (tag.equals(TAG_GX_VALUE)) {
            onSensorValueEnd();
        } else if (tag.equals(TAG_NAME)) {
            if (content != null) {
                name = content.trim();
            }
        } else if (localName.equals(TAG_DESCRIPTION)) {
            if (content != null) {
                description = content.trim();
            }
        } else if (localName.equals(TAG_ICON)) {
            if (content != null) {
                icon = content.trim();
            }
        } else if (localName.equals(TAG_VALUE)) {
            if (content != null) {
                category = content.trim();
            }
        } else if (localName.equals(TAG_WHEN)) {
            if (content != null) {
                time = content.trim();
            }
        } else if (localName.equals(TAG_STYLE_URL)) {
            if (content != null) {
                waypointType = content.trim();
            }
        } else if (localName.equals(TAG_HREF)) {
            if (content != null) {
                photoUrl = content.trim();
            }
        }

        // Reset element content
        content = null;
    }

    /**
     * On waypoint start.
     */
    private void onWaypointStart() {
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
        waypointType = null;
    }

    /**
     * On waypoint end.
     */
    private void onWaypointEnd() throws SAXException {
        if (!WAYPOINT_STYLE.equals(waypointType)) {
            return;
        }

        // If there is photoUrl it has to be changed because that url in kml file is a relative path to the internal kmz file.
        photoUrl = getInternalPhotoUrl(photoUrl);

        addWaypoint();
    }

    /**
     * On waypoint location end.
     */
    private void onWaypointLocationEnd() {
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
        trackPoints = new ArrayList<>();
        heartRateList = new ArrayList<>();
        cadenceList = new ArrayList<>();
        powerList = new ArrayList<>();
    }

    /**
     * On track segment end.
     */
    private void onTrackSegmentEnd() {
        // Close a track segment by inserting the segment locations
        for (int i = 0; i < trackPoints.size(); i++) {
            TrackPoint trackPoint = trackPoints.get(i);

            boolean hasSensorData = false;
            float heartrate = SensorDataSet.DATA_UNAVAILABLE;
            float cadence = SensorDataSet.DATA_UNAVAILABLE;
            float power = SensorDataSet.DATA_UNAVAILABLE;

            if (i < heartRateList.size()) {
                heartrate = heartRateList.get(i);
                hasSensorData = true;
            }
            if (i < cadenceList.size()) {
                cadence = cadenceList.get(i);
                hasSensorData = true;
            }
            if (i < powerList.size()) {
                power = powerList.get(i);
                hasSensorData = true;
            }

            if (!hasSensorData) {
                insertTrackPoint(trackPoint);
            } else {
                TrackPoint sensorDataSetLocation = new TrackPoint(trackPoint, new SensorDataSet(heartrate, cadence, power, SensorDataSet.DATA_UNAVAILABLE, trackPoint.getTime()));
                insertTrackPoint(sensorDataSetLocation);
            }
        }
    }

    /**
     * On track point end. gx:coord end tag.
     */
    private void onTrackPointEnd() throws SAXException {
        // Add location to locationList
        if (content == null) {
            return;
        }
        String[] parts = content.trim().split(" ");
        if (parts.length != 2 && parts.length != 3) {
            return;
        }
        longitude = parts[0];
        latitude = parts[1];
        altitude = parts.length == 3 ? parts[2] : null;

        TrackPoint location = getTrackPoint();
        if (location == null) {
            return;
        }
        trackPoints.add(location);
        time = null;
    }

    /**
     * On sensor data start. gx:SimpleArrayData start tag.
     */
    private void onSensorDataStart(Attributes attributes) {
        sensorType = attributes.getValue(ATTRIBUTE_NAME);
    }

    /**
     * On sensor value end. gx:value end tag.
     */
    private void onSensorValueEnd() throws SAXException {
        if (content == null) {
            return;
        }
        content = content.trim();
        if (content.equals("")) {
            return;
        }
        float value;
        try {
            value = Float.parseFloat(content);
        } catch (NumberFormatException e) {
            throw new SAXException(createErrorMessage("Unable to parse gx:value:" + content), e);
        }
        switch (sensorType) {
            case KmlTrackWriter.SENSOR_TYPE_POWER:
                powerList.add(value);
                break;
            case KmlTrackWriter.SENSOR_TYPE_HEART_RATE:
                heartRateList.add(value);
                break;
            case KmlTrackWriter.SENSOR_TYPE_CADENCE:
                cadenceList.add(value);
                break;
            default:
                Log.w(TAG, "Data from sensor " + sensorType + " is not (yet) supported.");
        }
    }
}
