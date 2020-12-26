/*
 * Copyright 2008 Google Inc.
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

import android.location.Location;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.NumberFormat;
import java.util.Locale;

import de.dennisguse.opentracks.content.data.Marker;
import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.content.data.TrackPoint;
import de.dennisguse.opentracks.util.StringUtils;

/**
 * Write track as GPX to a file.
 *
 * @author Sandor Dornbush
 */
//TODO Export markers
public class GpxTrackWriter implements TrackWriter {

    private static final NumberFormat ELEVATION_FORMAT = NumberFormat.getInstance(Locale.US);
    private static final NumberFormat COORDINATE_FORMAT = NumberFormat.getInstance(Locale.US);
    private static final NumberFormat SPEED_FORMAT = NumberFormat.getInstance(Locale.US);
    private static final NumberFormat HEARTRATE_FORMAT = NumberFormat.getInstance(Locale.US);
    private static final NumberFormat CADENCE_FORMAT = NumberFormat.getInstance(Locale.US);

    static {
        /*
         * GPX readers expect to see fractional numbers with US-style punctuation.
         * That is, they want periods for decimal points, rather than commas.
         */
        ELEVATION_FORMAT.setMaximumFractionDigits(1);
        ELEVATION_FORMAT.setGroupingUsed(false);

        COORDINATE_FORMAT.setMaximumFractionDigits(6);
        COORDINATE_FORMAT.setMaximumIntegerDigits(3);
        COORDINATE_FORMAT.setGroupingUsed(false);

        SPEED_FORMAT.setMaximumFractionDigits(2);
        SPEED_FORMAT.setGroupingUsed(false);

        HEARTRATE_FORMAT.setMaximumFractionDigits(0);
        HEARTRATE_FORMAT.setGroupingUsed(false);

        CADENCE_FORMAT.setMaximumFractionDigits(0);
        CADENCE_FORMAT.setGroupingUsed(false);
    }

    private final String creator;
    private PrintWriter printWriter;

    public GpxTrackWriter(String creator) {
        this.creator = creator;
    }

    @Override
    public void prepare(OutputStream outputStream) {
        this.printWriter = new PrintWriter(outputStream);
    }

    @Override
    public void close() {
        if (printWriter != null) {
            printWriter.flush();
            printWriter = null;
        }
    }

    @Override
    public void writeHeader(Track[] tracks) {
        if (printWriter != null) {
            printWriter.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            printWriter.println("<gpx");
            printWriter.println("version=\"1.1\"");
            printWriter.println("creator=\"" + creator + "\"");
            printWriter.println("xmlns=\"http://www.topografix.com/GPX/1/1\"");
            printWriter.println("xmlns:topografix=\"http://www.topografix.com/GPX/Private/TopoGrafix/0/1\"");
            printWriter.println("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
            printWriter.println("xmlns:atom=\"http://www.w3.org/2005/Atom\"");
            printWriter.println("xmlns:opentracks=\"http://opentracksapp.com/xmlschemas/v1\"");
            printWriter.println("xmlns:gpxtpx=\"http://www.garmin.com/xmlschemes/TrackPointExtension/v2\"");
            printWriter.println("xsi:schemaLocation=" +
                    "\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd"
                    + " http://www.topografix.com/GPX/Private/TopoGrafix/0/1 http://www.topografix.com/GPX/Private/TopoGrafix/0/1/topografix.xsd"
                    + " http://www.garmin.com/xmlschemas/TrackPointExtension/v2 https://www8.garmin.com/xmlschemas/TrackPointExtensionv2.xsd"
                    + " http://opentracksapp.com/xmlschemas/v1 http://opentracksapp.com/xmlschemas/OpenTracks_v1.xsd\">");

            printWriter.println("<metadata>");

            Track track = tracks[0];
            printWriter.println("<name>" + StringUtils.formatCData(track.getName()) + "</name>");
            printWriter.println("<desc>" + StringUtils.formatCData(track.getDescription()) + "</desc>");
            printWriter.println("</metadata>");
        }
    }

    @Override
    public void writeFooter() {
        if (printWriter != null) {
            printWriter.println("</gpx>");
        }
    }

    @Override
    public void writeBeginMarkers(Track track) {
        // Do nothing
    }

    @Override
    public void writeEndMarkers() {
        // Do nothing
    }

    @Override
    public void writeMarker(Marker marker) {
        if (printWriter != null) {
            Location location = marker.getLocation();
            printWriter.println("<wpt " + formatLocation(location) + ">");
            if (location.hasAltitude()) {
                printWriter.println("<ele>" + ELEVATION_FORMAT.format(location.getAltitude()) + "</ele>");
            }
            printWriter.println("<time>" + StringUtils.formatDateTimeIso8601(location.getTime()) + "</time>");
            printWriter.println("<name>" + StringUtils.formatCData(marker.getName()) + "</name>");
            printWriter.println("<desc>" + StringUtils.formatCData(marker.getDescription()) + "</desc>");
            printWriter.println("<type>" + StringUtils.formatCData(marker.getCategory()) + "</type>");
            printWriter.println("</wpt>");
        }
    }

    @Override
    public void writeMultiTrackBegin() {
        // Do nothing
    }

    @Override
    public void writeMultiTrackEnd() {
        // Do nothing
    }

    @Override
    public void writeBeginTrack(Track track, TrackPoint startTrackPoint) {
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

    @Override
    public void writeEndTrack(Track track, TrackPoint endTrackPoint) {
        if (printWriter != null) {
            printWriter.println("</trk>");
        }
    }

    @Override
    public void writeOpenSegment() {
        printWriter.println("<trkseg>");
    }

    @Override
    public void writeCloseSegment() {
        printWriter.println("</trkseg>");
    }

    @Override
    public void writeTrackPoint(TrackPoint trackPoint) {
        if (printWriter != null) {
            printWriter.println("<trkpt " + formatLocation(trackPoint.getLocation()) + ">");
            if (trackPoint.hasAltitude()) {
                printWriter.println("<ele>" + ELEVATION_FORMAT.format(trackPoint.getAltitude()) + "</ele>");
            }

            printWriter.println("<time>" + StringUtils.formatDateTimeIso8601(trackPoint.getTime()) + "</time>");

            if (trackPoint.hasSpeed() || trackPoint.hasHeartRate() || trackPoint.hasCyclingCadence() || trackPoint.hasElevationGain() || trackPoint.hasElevationLoss()) {
                printWriter.println("<extensions><gpxtpx:TrackPointExtension>");

                if (trackPoint.hasSpeed()) {
                    printWriter.println("<gpxtpx:speed>" + SPEED_FORMAT.format(trackPoint.getSpeed()) + "</gpxtpx:speed>");
                }

                if (trackPoint.hasHeartRate()) {
                    printWriter.println("<gpxtpx:hr>" + HEARTRATE_FORMAT.format(trackPoint.getHeartRate_bpm()) + "</gpxtpx:hr>");
                }

                if (trackPoint.hasCyclingCadence()) {
                    printWriter.println("<gpxtpx:cad>" + HEARTRATE_FORMAT.format(trackPoint.getCyclingCadence_rpm()) + "</gpxtpx:cad>");
                }

                if (trackPoint.hasElevationGain()) {
                    printWriter.println("<opentracks:gain>" + ELEVATION_FORMAT.format(trackPoint.getElevationGain()) + "</opentracks:gain>");
                }

                if (trackPoint.hasElevationLoss()) {
                    printWriter.println("<opentracks:loss>" + ELEVATION_FORMAT.format(trackPoint.getElevationLoss()) + "</opentracks:loss>");
                }

                printWriter.println("</gpxtpx:TrackPointExtension></extensions>");
            }

            printWriter.println("</trkpt>");
        }
    }

    /**
     * Formats a location with latitude and longitude coordinates.
     *
     * @param location the location
     */
    private String formatLocation(Location location) {
        return "lat=\"" + COORDINATE_FORMAT.format(location.getLatitude()) + "\" lon=\"" + COORDINATE_FORMAT.format(location.getLongitude()) + "\"";
    }
}
