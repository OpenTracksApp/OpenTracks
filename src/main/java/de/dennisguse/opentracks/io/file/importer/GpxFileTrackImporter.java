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

import androidx.annotation.VisibleForTesting;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import de.dennisguse.opentracks.content.data.TrackPoint;
import de.dennisguse.opentracks.content.provider.ContentProviderUtils;

/**
 * Imports a GPX file.
 * Uses https://www8.garmin.com/xmlschemas/TrackPointExtensionv2.xsd
 * <p>
 * {@link de.dennisguse.opentracks.io.file.exporter.GPXTrackExporter} does not export information if a segment was started automatic or manually.
 * Therefore, all segments starts are marked as SEGMENT_START_AUTOMATIC.
 * Thus, the {@link de.dennisguse.opentracks.stats.TrackStatistics} cannot be restored correctly.
 *
 * @author Jimmy Shih
 */
public class GpxFileTrackImporter extends AbstractFileTrackImporter {

    private static final String TAG_DESCRIPTION = "desc";
    private static final String TAG_COMMENT = "cmt";
    private static final String TAG_ELEVATION = "ele";
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

    private static final String TAG_EXTENSION_GAIN = "opentracks:gain";
    private static final String TAG_EXTENSION_LOSS = "opentracks:loss";
    private static final String TAG_EXTENSION_DISTANCE = "opentracks:distance";

    /**
     * Constructor.
     *
     * @param context the context
     */
    public GpxFileTrackImporter(Context context) {
        this(context, new ContentProviderUtils(context));
    }

    @VisibleForTesting
    GpxFileTrackImporter(Context context, ContentProviderUtils contentProviderUtils) {
        super(context, contentProviderUtils);
    }

    @Override
    public void startElement(String uri, String localName, String tag, Attributes attributes) throws SAXException {
        switch (tag) {
            case TAG_MARKER:
                onMarkerStart(attributes);
                break;
            case TAG_TRACK:
                onTrackStart();
                break;
            case TAG_TRACK_SEGMENT:
                onTrackSegmentStart();
                break;
            case TAG_TRACK_POINT:
                onTrackPointStart(attributes);
                break;
        }
    }

    @Override
    public void endElement(String uri, String localName, String tag) throws SAXException {
        switch (tag) {
            case TAG_GPX:
                onFileEnd();
                break;
            case TAG_MARKER:
                onMarkerEnd();
                break;
            case TAG_TRACK:
                onTrackEnd();
                break;
            case TAG_TRACK_SEGMENT:
                onTrackSegmentEnd();
                break;
            case TAG_TRACK_POINT:
                onTrackPointEnd();
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
            case TAG_ELEVATION:
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

        // Reset element content
        content = null;
    }

    @Override
    protected void onTrackStart() throws SAXException {
        super.onTrackStart();
        name = null;
        description = null;
        category = null;
    }

    /**
     * On track point start.
     *
     * @param attributes the attributes
     */
    private void onTrackPointStart(Attributes attributes) {
        latitude = attributes.getValue(ATTRIBUTE_LAT);
        longitude = attributes.getValue(ATTRIBUTE_LON);
        altitude = null;
        time = null;
        speed = null;
        gain = null;
        loss = null;
    }

    private void onTrackPointEnd() throws SAXException {
        boolean isFirstTrackPointInSegment = isFirstTrackPointInSegment();
        TrackPoint trackPoint = getTrackPoint();
        if (isFirstTrackPointInSegment) {
            trackPoint.setType(TrackPoint.Type.SEGMENT_START_AUTOMATIC);
        }
        insertTrackPoint(trackPoint);
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

    private void onMarkerEnd() throws SAXException {
        addMarker();
    }
}
