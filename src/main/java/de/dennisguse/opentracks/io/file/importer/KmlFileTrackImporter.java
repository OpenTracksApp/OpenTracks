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
import android.net.Uri;

import androidx.annotation.VisibleForTesting;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import java.util.ArrayList;

import de.dennisguse.opentracks.content.ContentProviderUtils;
import de.dennisguse.opentracks.content.SensorDataSetLocation;
import de.dennisguse.opentracks.content.Waypoint.WaypointType;
import de.dennisguse.opentracks.content.sensor.SensorDataSet;

/**
 * Imports a KML file.
 *
 * @author Jimmy Shih
 */
public class KmlFileTrackImporter extends AbstractFileTrackImporter {

    private static final String CADENCE = "cadence";
    private static final String HEART_RATE = "heart_rate";
    private static final String POWER = "power";

    private static final String STATISTICS_STYLE = "#statistics";
    private static final String WAYPOINT_STYLE = "#waypoint";

    private static final String TAG_COORDINATES = "coordinates";
    private static final String TAG_DESCRIPTION = "description";
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
    private String sensorName;
    private ArrayList<Location> locationList;
    private ArrayList<Integer> cadenceList;
    private ArrayList<Integer> heartRateList;
    private ArrayList<Integer> powerList;

    /**
     * Constructor.
     *
     * @param context       the context
     * @param importTrackId track id to import to. -1L to import to a new track.
     */
    public KmlFileTrackImporter(Context context, long importTrackId) {
        this(context, importTrackId, ContentProviderUtils.Factory.get(context));
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
        // Add a waypoint if the waypointType matches
        WaypointType type;
        switch (waypointType) {
            case WAYPOINT_STYLE:
                type = WaypointType.WAYPOINT;
                break;
            case STATISTICS_STYLE:
                type = WaypointType.STATISTICS;
                break;
            default:
                return;
        }

        if (photoUrl != null) {
            Uri uri = Uri.parse(photoUrl);
            photoUrl = getPhotoUrl(uri.getLastPathSegment());
        }

        addWaypoint(type);
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
        locationList = new ArrayList<>();
        heartRateList = new ArrayList<>();
        cadenceList = new ArrayList<>();
        powerList = new ArrayList<>();
    }

    /**
     * On track segment end.
     */
    private void onTrackSegmentEnd() {
        // Close a track segment by inserting the segment locations
        boolean hasHeartRate = heartRateList.size() == locationList.size();
        boolean hasCadence = cadenceList.size() == locationList.size();
        boolean hasPower = powerList.size() == locationList.size();

        for (int i = 0; i < locationList.size(); i++) {
            Location location = locationList.get(i);

            if (!hasPower && !hasCadence && !hasHeartRate) {
                insertTrackPoint(location);
            } else {
                float heartrate = hasHeartRate ? heartRateList.get(i) : SensorDataSet.DATA_UNAVAILABLE;
                float cadence = hasHeartRate ? cadenceList.get(i) : SensorDataSet.DATA_UNAVAILABLE;
                float power = hasHeartRate ? powerList.get(i) : SensorDataSet.DATA_UNAVAILABLE;

                SensorDataSetLocation sensorDataSetLocation = new SensorDataSetLocation(location, new SensorDataSet(heartrate, cadence, power, SensorDataSet.DATA_UNAVAILABLE, location.getTime()));
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

        Location location = getTrackPoint();
        if (location == null) {
            return;
        }
        locationList.add(location);
        time = null;
    }

    /**
     * On sensor data start. gx:SimpleArrayData start tag.
     */
    private void onSensorDataStart(Attributes attributes) {
        sensorName = attributes.getValue(ATTRIBUTE_NAME);
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
        int value;
        try {
            value = Integer.parseInt(content);
        } catch (NumberFormatException e) {
            throw new SAXException(createErrorMessage("Unable to parse gx:value:" + content), e);
        }
        if (POWER.equals(sensorName)) {
            powerList.add(value);
        } else if (HEART_RATE.equals(sensorName)) {
            heartRateList.add(value);
        } else if (CADENCE.equals(sensorName)) {
            cadenceList.add(value);
        }
    }
}
