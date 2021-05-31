/*
 * Copyright 2011 Google Inc.
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

package de.dennisguse.opentracks.io.file.exporter;

import android.database.Cursor;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.NumberFormat;
import java.util.Locale;

import de.dennisguse.opentracks.content.data.Marker;
import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.content.data.TrackPoint;
import de.dennisguse.opentracks.content.provider.ContentProviderUtils;
import de.dennisguse.opentracks.content.provider.TrackPointIterator;
import de.dennisguse.opentracks.util.StringUtils;

/**
 * Convert {@link Track} incl. {@link Marker} and {@link TrackPoint} to GPX.
 * NOTE:
 * * does not export {@link TrackPoint} without a latitude/longitude (not supported by GPX 1.1).
 * * cannot export multiple {@link Track}s - all {@link TrackPoint}s are exported as if they would belong to the first track.
 *
 * @author Sandor Dornbush
 * @author Rodrigo Damazio
 */
public class GPXTrackExporter implements TrackExporter {

    private static final String TAG = GPXTrackExporter.class.getSimpleName();

    private static final NumberFormat ALTITUDE_FORMAT = NumberFormat.getInstance(Locale.US);
    private static final NumberFormat COORDINATE_FORMAT = NumberFormat.getInstance(Locale.US);
    private static final NumberFormat SPEED_FORMAT = NumberFormat.getInstance(Locale.US);
    private static final NumberFormat DISTANCE_FORMAT = NumberFormat.getInstance(Locale.US);
    private static final NumberFormat HEARTRATE_FORMAT = NumberFormat.getInstance(Locale.US);
    private static final NumberFormat CADENCE_FORMAT = NumberFormat.getInstance(Locale.US);
    private static final NumberFormat POWER_FORMAT = NumberFormat.getInstance(Locale.US);

    static {
        /*
         * GPX readers expect to see fractional numbers with US-style punctuation.
         * That is, they want periods for decimal points, rather than commas.
         */
        ALTITUDE_FORMAT.setMaximumFractionDigits(1);
        ALTITUDE_FORMAT.setGroupingUsed(false);

        COORDINATE_FORMAT.setMaximumFractionDigits(6);
        COORDINATE_FORMAT.setMaximumIntegerDigits(3);
        COORDINATE_FORMAT.setGroupingUsed(false);

        SPEED_FORMAT.setMaximumFractionDigits(2);
        SPEED_FORMAT.setGroupingUsed(false);

        HEARTRATE_FORMAT.setMaximumFractionDigits(0);
        HEARTRATE_FORMAT.setGroupingUsed(false);

        CADENCE_FORMAT.setMaximumFractionDigits(0);
        CADENCE_FORMAT.setGroupingUsed(false);

        POWER_FORMAT.setMaximumFractionDigits(0);
        POWER_FORMAT.setGroupingUsed(false);
    }

    private final ContentProviderUtils contentProviderUtils;

    private final String creator;
    private PrintWriter printWriter;

    public GPXTrackExporter(ContentProviderUtils contentProviderUtils, String creator) {
        this.contentProviderUtils = contentProviderUtils;
        this.creator = creator;
    }

    @Override
    public boolean writeTrack(Track track, @NonNull OutputStream outputStream) {
        return writeTrack(new Track[]{track}, outputStream);
    }

    @Override
    public boolean writeTrack(Track[] tracks, @NonNull OutputStream outputStream) {
        try {
            prepare(outputStream);
            writeHeader();

            for (Track track : tracks) {
                writeMarkers(track);
            }

            for (Track track : tracks) {
                writeTrackPoints(track);
            }

            writeFooter();
            close();

            return true;
        } catch (InterruptedException e) {
            Log.e(TAG, "Thread interrupted", e);
            return false;
        }
    }

    private void writeTrackPoints(Track track) throws InterruptedException {
        boolean wroteTrack = false;
        boolean wroteSegment = false;

        try (TrackPointIterator trackPointIterator = contentProviderUtils.getTrackPointLocationIterator(track.getId(), null)) {
            while (trackPointIterator.hasNext()) {
                if (Thread.interrupted()) throw new InterruptedException();

                TrackPoint trackPoint = trackPointIterator.next();

                if (!wroteTrack) {
                    writeBeginTrack(track);
                    wroteTrack = true;
                }

                switch (trackPoint.getType()) {
                    case SEGMENT_START_MANUAL:
                        if (wroteSegment) writeCloseSegment();
                        writeOpenSegment();
                        wroteSegment = true;
                        Log.i(TAG, "Exporting " + TrackPoint.Type.SEGMENT_START_MANUAL.name() + " is not supported.");
                        break;
                    case SEGMENT_END_MANUAL:
                        writeCloseSegment();
                        wroteSegment = false;
                        Log.i(TAG, "Exporting " + TrackPoint.Type.SEGMENT_END_MANUAL.name() + " is not supported.");
                        break;
                    case SEGMENT_START_AUTOMATIC:
                        if (wroteSegment) writeCloseSegment();
                        writeOpenSegment();
                        wroteSegment = true;
                        writeTrackPoint(trackPoint);
                        break;
                    case TRACKPOINT:
                        if (!wroteSegment) {
                            // Might happen for older data (pre v3.15.0)
                            writeOpenSegment();
                            wroteSegment = true;
                        }
                        writeTrackPoint(trackPoint);
                        break;
                }
            }

            if (wroteSegment) {
                // Should not be necessary as tracks should end with SEGMENT_END_MANUAL.
                // Anyhow, make sure that the last segment is closed.
                writeCloseSegment();
            }

            if (!wroteTrack) {
                // Write an empty track
                writeBeginTrack(track);
            }

            writeEndTrack();
        }
    }

    public void prepare(OutputStream outputStream) {
        this.printWriter = new PrintWriter(outputStream);
    }

    public void close() {
        if (printWriter != null) {
            printWriter.flush();
            printWriter = null;
        }
    }


    public void writeHeader() {
        if (printWriter != null) {
            printWriter.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            printWriter.println("<gpx");
            printWriter.println("version=\"1.1\"");
            printWriter.println("creator=\"" + creator + "\"");
            printWriter.println("xmlns=\"http://www.topografix.com/GPX/1/1\"");
            printWriter.println("xmlns:topografix=\"http://www.topografix.com/GPX/Private/TopoGrafix/0/1\"");
            printWriter.println("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
            printWriter.println("xmlns:opentracks=\"http://opentracksapp.com/xmlschemas/v1\"");
            printWriter.println("xmlns:gpxtpx=\"http://www.garmin.com/xmlschemas/TrackPointExtension/v2\"");
            printWriter.println("xmlns:pwr=\"http://www.garmin.com/xmlschemas/PowerExtension/v1\"");
            printWriter.println("xsi:schemaLocation=" +
                    "\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd"
                    + " http://www.topografix.com/GPX/Private/TopoGrafix/0/1 http://www.topografix.com/GPX/Private/TopoGrafix/0/1/topografix.xsd"
                    + " http://www.garmin.com/xmlschemas/TrackPointExtension/v2 https://www8.garmin.com/xmlschemas/TrackPointExtensionv2.xsd"
                    + " http://www.garmin.com/xmlschemas/PowerExtension/v1 https://www8.garmin.com/xmlschemas/PowerExtensionv1.xsd"
                    + " http://opentracksapp.com/xmlschemas/v1 http://opentracksapp.com/xmlschemas/OpenTracks_v1.xsd\">");
        }
    }

    public void writeFooter() {
        if (printWriter != null) {
            printWriter.println("</gpx>");
        }
    }

    private void writeMarkers(Track track) throws InterruptedException {
        try (Cursor cursor = contentProviderUtils.getMarkerCursor(track.getId(), null, -1)) {
            if (cursor != null && cursor.moveToFirst()) {
                for (int i = 0; i < cursor.getCount(); i++) {
                    if (Thread.interrupted()) {
                        throw new InterruptedException();
                    }
                    Marker marker = contentProviderUtils.createMarker(cursor);
                    writeMarker(marker);

                    cursor.moveToNext();
                }
            }
        }
    }

    public void writeMarker(Marker marker) {
        if (printWriter != null) {
            printWriter.println("<wpt " + formatLocation(marker.getLatitude(), marker.getLongitude()) + ">");
            if (marker.hasAltitude()) {
                printWriter.println("<ele>" + ALTITUDE_FORMAT.format(marker.getAltitude().toM()) + "</ele>");
            }
            printWriter.println("<time>" + StringUtils.formatDateTimeIso8601(marker.getTime()) + "</time>");
            printWriter.println("<name>" + StringUtils.formatCData(marker.getName()) + "</name>");
            printWriter.println("<desc>" + StringUtils.formatCData(marker.getDescription()) + "</desc>");
            printWriter.println("<type>" + StringUtils.formatCData(marker.getCategory()) + "</type>");
            printWriter.println("</wpt>");
        }
    }

    public void writeBeginTrack(Track track) {
        if (printWriter != null) {
            printWriter.println("<trk>");
            printWriter.println("<name>" + StringUtils.formatCData(track.getName()) + "</name>");
            printWriter.println("<desc>" + StringUtils.formatCData(track.getDescription()) + "</desc>");
            printWriter.println("<type>" + StringUtils.formatCData(track.getCategory()) + "</type>");

            printWriter.println("<extensions>");
            printWriter.println("<topografix:color>c0c0c0</topografix:color>");
            printWriter.println("<opentracks:trackid>" + track.getUuid() + "</opentracks:trackid>");
            printWriter.println("</extensions>");
        }
    }

    public void writeEndTrack() {
        if (printWriter != null) {
            printWriter.println("</trk>");
        }
    }

    public void writeOpenSegment() {
        printWriter.println("<trkseg>");
    }

    public void writeCloseSegment() {
        printWriter.println("</trkseg>");
    }

    public void writeTrackPoint(TrackPoint trackPoint) {
        if (printWriter != null) {

            printWriter.println("<trkpt " + formatLocation(trackPoint.getLatitude(), trackPoint.getLongitude()) + ">");

            if (trackPoint.hasAltitude()) {
                printWriter.println("<ele>" + ALTITUDE_FORMAT.format(trackPoint.getAltitude().toM()) + "</ele>");
            }

            printWriter.println("<time>" + StringUtils.formatDateTimeIso8601(trackPoint.getTime()) + "</time>");

            if (trackPoint.hasSpeed() || trackPoint.hasHeartRate() || trackPoint.hasCyclingCadence() || trackPoint.hasAltitudeGain() || trackPoint.hasAltitudeLoss()) {
                printWriter.println("<extensions><gpxtpx:TrackPointExtension>");

                if (trackPoint.hasSpeed()) {
                    printWriter.println("<gpxtpx:speed>" + SPEED_FORMAT.format(trackPoint.getSpeed().toMPS()) + "</gpxtpx:speed>");
                }

                if (trackPoint.hasHeartRate()) {
                    printWriter.println("<gpxtpx:hr>" + HEARTRATE_FORMAT.format(trackPoint.getHeartRate_bpm()) + "</gpxtpx:hr>");
                }

                if (trackPoint.hasCyclingCadence()) {
                    printWriter.println("<gpxtpx:cad>" + CADENCE_FORMAT.format(trackPoint.getCyclingCadence_rpm()) + "</gpxtpx:cad>");
                }

                if (trackPoint.hasPower()) {
                    printWriter.println("<pwr:PowerInWatts>" + POWER_FORMAT.format(trackPoint.getPower()) + "</pwr:PowerInWatts>");
                }

                if (trackPoint.hasAltitudeGain()) {
                    printWriter.println("<opentracks:gain>" + ALTITUDE_FORMAT.format(trackPoint.getAltitudeGain()) + "</opentracks:gain>");
                }

                if (trackPoint.hasAltitudeLoss()) {
                    printWriter.println("<opentracks:loss>" + ALTITUDE_FORMAT.format(trackPoint.getAltitudeLoss()) + "</opentracks:loss>");
                }

                if (trackPoint.hasSensorDistance()) {
                    printWriter.println("<opentracks:distance>" + DISTANCE_FORMAT.format(trackPoint.getSensorDistance().toM()) + "</opentracks:distance>");
                }

                printWriter.println("</gpxtpx:TrackPointExtension></extensions>");
            }

            printWriter.println("</trkpt>");
        }
    }

    private String formatLocation(double latitude, double longitude) {
        return "lat=\"" + COORDINATE_FORMAT.format(latitude) + "\" lon=\"" + COORDINATE_FORMAT.format(longitude) + "\"";
    }
}
