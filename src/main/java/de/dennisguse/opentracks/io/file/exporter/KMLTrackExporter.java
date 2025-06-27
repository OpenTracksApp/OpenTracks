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

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.data.ContentProviderUtils;
import de.dennisguse.opentracks.data.TrackPointIterator;
import de.dennisguse.opentracks.data.models.ActivityType;
import de.dennisguse.opentracks.data.models.Marker;
import de.dennisguse.opentracks.data.models.Position;
import de.dennisguse.opentracks.data.models.Track;
import de.dennisguse.opentracks.data.models.TrackPoint;
import de.dennisguse.opentracks.ui.markers.MarkerUtils;
import de.dennisguse.opentracks.util.StringUtils;

/**
 * Convert {@link Track} incl. {@link Marker} and {@link TrackPoint} to KML version 2.3.
 * <a href="https://docs.opengeospatial.org/is/12-007r2/12-007r2.html">...</a>
 *
 * @author Sandor Dornbush
 * @author Rodrigo Damazio
 * @author Leif Hendrik Wilden
 */
public class KMLTrackExporter implements TrackExporter {

    private static final String TAG = KMLTrackExporter.class.getSimpleName();

    public static final String MARKER_STYLE = "waypoint";
    private static final String TRACK_STYLE = "track";
    private static final String SCHEMA_ID = "schema";

    public static final String EXTENDED_DATA_TYPE_LOCALIZED = "type";
    public static final String EXTENDED_DATA_ACTIVITY_TYPE = "activityType";

    public static final String EXTENDED_DATA_TYPE_TRACKPOINT = "trackpoint_type";
    public static final String EXTENDED_DATA_TYPE_SPEED = "speed";
    public static final String EXTENDED_DATA_TYPE_DISTANCE = "distance";
    public static final String EXTENDED_DATA_TYPE_CADENCE = "cadence";
    public static final String EXTENDED_DATA_TYPE_HEARTRATE = "heartrate";
    public static final String EXTENDED_DATA_TYPE_POWER = "power";
    public static final String EXTENDED_DATA_TYPE_ALTITUDE_GAIN = "elevation_gain";
    public static final String EXTENDED_DATA_TYPE_ALTITUDE_LOSS = "elevation_loss";
    public static final String EXTENDED_DATA_TYPE_ACCURACY_HORIZONTAL = "accuracy_horizontal";
    public static final String EXTENDED_DATA_TYPE_ACCURACY_VERTICAL = "accuracy_vertical";

    private static final NumberFormat SENSOR_DATA_FORMAT = NumberFormat.getInstance(Locale.US);

    static {
        SENSOR_DATA_FORMAT.setMaximumFractionDigits(1);
        SENSOR_DATA_FORMAT.setGroupingUsed(false);
    }

    private final Context context;
    private final boolean exportPhotos;
    private final ContentProviderUtils contentProviderUtils;

    private PrintWriter printWriter;

    private final ArrayList<TrackPoint.Type> trackpointTypeList = new ArrayList<>();

    private final List<Float> speedList = new ArrayList<>();
    private final List<Float> distanceList = new ArrayList<>();
    private final List<Float> powerList = new ArrayList<>();
    private final List<Float> cadenceList = new ArrayList<>();
    private final List<Float> heartRateList = new ArrayList<>();
    private final List<Float> altitudeGainList = new ArrayList<>();
    private final List<Float> altitudeLossList = new ArrayList<>();
    private final List<Float> accuracyHorizontal = new ArrayList<>();
    private final List<Float> accuracyVertical = new ArrayList<>();

    public KMLTrackExporter(Context context, ContentProviderUtils contentProviderUtils, boolean exportPhotos) {
        this.context = context;
        this.exportPhotos = exportPhotos;
        this.contentProviderUtils = contentProviderUtils;
    }

    @Override
    public boolean writeTrack(List<Track> tracks, @NonNull OutputStream outputStream) {
        try {
            prepare(outputStream);
            writeHeader(tracks);
            for (Track track : tracks) {
                writeMarkers(track);
            }
            boolean hasMultipleTracks = tracks.size() > 1;
            if (hasMultipleTracks) {
                writeMultiTrackBegin();
            }
            for (Track track : tracks) {
                writeLocations(track);
            }
            if (hasMultipleTracks) {
                writeMultiTrackEnd();
            }
            writeFooter();
            close();

            return true;
        } catch (InterruptedException e) {
            Log.e(TAG, "Thread interrupted", e);
            return false;
        }
    }

    private void writeMarkers(Track track) throws InterruptedException {
        boolean hasMarkers = false;
        try (Cursor cursor = contentProviderUtils.getMarkerCursor(track.getId(), null, -1)) {
            if (cursor != null && cursor.moveToFirst()) {
                for (int i = 0; i < cursor.getCount(); i++) {
                    if (Thread.interrupted()) {
                        throw new InterruptedException();
                    }
                    if (!hasMarkers) {
                        writeBeginMarkers(track);
                        hasMarkers = true;
                    }
                    Marker marker = contentProviderUtils.createMarker(cursor);
                    writeMarker(marker, track.getZoneOffset());

                    cursor.moveToNext();
                }
            }
        }
        if (hasMarkers) {
            writeEndMarkers();
        }
    }

    private void writeLocations(Track track) throws InterruptedException {
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
                    case SEGMENT_START_MANUAL, SEGMENT_START_AUTOMATIC -> {
                        if (wroteSegment) writeCloseSegment();
                        writeOpenSegment();
                        writeTrackPoint(track.getZoneOffset(), trackPoint);
                        wroteSegment = true;
                    }
                    case SEGMENT_END_MANUAL -> {
                        if (!wroteSegment) writeOpenSegment();
                        writeTrackPoint(track.getZoneOffset(), trackPoint);
                        writeCloseSegment();
                        wroteSegment = false;
                    }
                    case TRACKPOINT, IDLE -> {
                        if (!wroteSegment) {
                            // Might happen for older data (pre v3.15.0)
                            writeOpenSegment();
                            wroteSegment = true;
                        }

                        writeTrackPoint(track.getZoneOffset(), trackPoint);
                    }
                    default ->
                            throw new RuntimeException("Exporting this TrackPoint type is not implemented: " + trackPoint.getType());
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

    @VisibleForTesting
    void prepare(OutputStream outputStream) {
        this.printWriter = new PrintWriter(outputStream);
    }

    @VisibleForTesting
    void close() {
        printWriter.flush();
        printWriter = null;
    }

    private void writeHeader(List<Track> tracks) {
        printWriter.println(
                """
                        <?xml version="1.0" encoding="UTF-8"?>
                        """);
        printWriter.println(
                """
                        <kml xmlns="http://www.opengis.net/kml/2.3"
                            xmlns:atom="http://www.w3.org/2005/Atom"
                            xmlns:opentracks="http://opentracksapp.com/xmlschemas/v1"
                            xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                            xsi:schemaLocation="http://www.opengis.net/kml/2.3 http://schemas.opengis.net/kml/2.3/ogckml23.xsd
                                                http://opentracksapp.com/xmlschemas/v1 http://opentracksapp.com/xmlschemas/OpenTracks_v1.xsd">
                        """); //TODO ADD xsi:schemaLocation for atom
        printWriter.println("<Document>");
        printWriter.println("<open>1</open>");
        printWriter.println("<visibility>1</visibility>");

        Track track = tracks.get(0);
        printWriter.println("<name>" + StringUtils.formatCData(track.getName()) + "</name>");
        printWriter.println("<atom:generator>" + StringUtils.formatCData(context.getString(R.string.app_name)) + "</atom:generator>");

        writeTrackStyle();
        writePlacemarkerStyle();
        printWriter.println("<Schema id=\"" + SCHEMA_ID + "\">");

        writeSimpleArrayStyle(EXTENDED_DATA_TYPE_SPEED, context.getString(R.string.description_speed_ms));
        writeSimpleArrayStyle(EXTENDED_DATA_TYPE_POWER, context.getString(R.string.description_sensor_power));
        writeSimpleArrayStyle(EXTENDED_DATA_TYPE_CADENCE, context.getString(R.string.description_sensor_cadence));
        writeSimpleArrayStyle(EXTENDED_DATA_TYPE_HEARTRATE, context.getString(R.string.description_sensor_heart_rate));

        printWriter.println("</Schema>");
    }

    private void writeFooter() {
        printWriter.println("</Document>");
        printWriter.println("</kml>");
    }

    private void writeBeginMarkers(Track track) {
        printWriter.println("<Folder>");
        printWriter.println("<name>" + StringUtils.formatCData(context.getString(R.string.track_markers, track.getName())) + "</name>");
        printWriter.println("<open>1</open>");
    }

    private void writeMarker(Marker marker, ZoneOffset zoneOffset) {
        boolean existsPhoto = MarkerUtils.buildInternalPhotoFile(context, marker.getTrackId(), marker.getPhotoUrl()) != null;
        if (marker.hasPhoto() && exportPhotos && existsPhoto) {
            writePhotoOverlay(marker, zoneOffset);
        } else {
            writePlacemark(marker.getName(), marker.getCategory(), marker.getDescription(), marker.getPosition(), marker.getTime(), zoneOffset);
        }
    }

    private void writeEndMarkers() {
        printWriter.println("</Folder>");
    }

    private void writeMultiTrackBegin() {
        printWriter.println("<Folder id=\"tracks\">");
        printWriter.println("<name>" + context.getString(R.string.generic_tracks) + "</name>");
        printWriter.println("<open>1</open>");
    }

    private void writeMultiTrackEnd() {
        printWriter.println("</Folder>");
    }

    private void writeBeginTrack(Track track) {
        printWriter.println("<Placemark>");

        printWriter.println("<name>" + StringUtils.formatCData(track.getName()) + "</name>");
        printWriter.println("<description>" + StringUtils.formatCData(track.getDescription()) + "</description>");
        printWriter.println("<opentracks:trackid>" + track.getUuid() + "</opentracks:trackid>");

        printWriter.println("<styleUrl>#" + TRACK_STYLE + "</styleUrl>");
        writeActivityType(track.getActivityType());
        writeTypeLocalized(track.getActivityTypeLocalized());
        printWriter.println("<MultiTrack>");
        printWriter.println("<altitudeMode>absolute</altitudeMode>");
        printWriter.println("<interpolate>1</interpolate>");
    }

    private void writeEndTrack() {
        printWriter.println("</MultiTrack>");
        printWriter.println("</Placemark>");
    }

    @VisibleForTesting
    void writeOpenSegment() {
        printWriter.println("<Track>");
        trackpointTypeList.clear();
        speedList.clear();
        distanceList.clear();
        powerList.clear();
        cadenceList.clear();
        heartRateList.clear();
        altitudeGainList.clear();
        altitudeLossList.clear();
        accuracyHorizontal.clear();
        accuracyVertical.clear();
    }

    @VisibleForTesting
    void writeCloseSegment() {
        printWriter.println("<ExtendedData>");
        printWriter.println("<SchemaData schemaUrl=\"#" + SCHEMA_ID + "\">");

        writeTrackPointType(trackpointTypeList);

        if (speedList.stream().anyMatch(Objects::nonNull)) {
            writeSimpleArraySensorData(speedList, EXTENDED_DATA_TYPE_SPEED);
        }
        if (distanceList.stream().anyMatch(Objects::nonNull)) {
            writeSimpleArraySensorData(distanceList, EXTENDED_DATA_TYPE_DISTANCE);
        }
        if (powerList.stream().anyMatch(Objects::nonNull)) {
            writeSimpleArraySensorData(powerList, EXTENDED_DATA_TYPE_POWER);
        }
        if (cadenceList.stream().anyMatch(Objects::nonNull)) {
            writeSimpleArraySensorData(cadenceList, EXTENDED_DATA_TYPE_CADENCE);
        }
        if (heartRateList.stream().anyMatch(Objects::nonNull)) {
            writeSimpleArraySensorData(heartRateList, EXTENDED_DATA_TYPE_HEARTRATE);
        }
        if (altitudeGainList.stream().anyMatch(Objects::nonNull)) {
            writeSimpleArraySensorData(altitudeGainList, EXTENDED_DATA_TYPE_ALTITUDE_GAIN);
        }
        if (altitudeLossList.stream().anyMatch(Objects::nonNull)) {
            writeSimpleArraySensorData(altitudeLossList, EXTENDED_DATA_TYPE_ALTITUDE_LOSS);
        }
        if (accuracyHorizontal.stream().anyMatch(Objects::nonNull)) {
            writeSimpleArraySensorData(accuracyHorizontal, EXTENDED_DATA_TYPE_ACCURACY_HORIZONTAL);
        }
        if (accuracyVertical.stream().anyMatch(Objects::nonNull)) {
            writeSimpleArraySensorData(accuracyVertical, EXTENDED_DATA_TYPE_ACCURACY_VERTICAL);
        }
        printWriter.println("</SchemaData>");
        printWriter.println("</ExtendedData>");
        printWriter.println("</Track>");
    }

    @VisibleForTesting
    void writeTrackPoint(ZoneOffset zoneOffset, TrackPoint trackPoint) {
        printWriter.println("<when>" + getTime(zoneOffset, trackPoint.getTime()) + "</when>");

        trackpointTypeList.add(trackPoint.getType());

        if (trackPoint.hasLocation()) {
            printWriter.println("<coord>" + getCoordinates(trackPoint.getPosition(), " ") + "</coord>");
        } else {
            printWriter.println("<coord/>");
        }
        speedList.add(trackPoint.hasSpeed() ? (float) trackPoint.getSpeed().toMPS() : null);

        distanceList.add(trackPoint.hasSensorDistance() ? (float) trackPoint.getSensorDistance().toM() : null);
        heartRateList.add(trackPoint.hasHeartRate() ? trackPoint.getHeartRate().getBPM() : null);
        cadenceList.add(trackPoint.hasCadence() ? trackPoint.getCadence().getRPM() : null);
        powerList.add(trackPoint.hasPower() ? trackPoint.getPower().getW() : null);

        altitudeGainList.add(trackPoint.hasAltitudeGain() ? trackPoint.getAltitudeGain() : null);
        altitudeLossList.add(trackPoint.hasAltitudeLoss() ? trackPoint.getAltitudeLoss() : null);
        accuracyHorizontal.add(trackPoint.hasHorizontalAccuracy() ? (float) trackPoint.getHorizontalAccuracy().toM() : null);
        accuracyVertical.add(trackPoint.hasVerticalAccuracy() ? (float) trackPoint.getVerticalAccuracy().toM() : null);
    }

    private void writeSimpleArraySensorData(List<Float> list, String name) {
        printWriter.println("<SimpleArrayData name=\"" + name + "\">");
        for (int i = 0; i < list.size(); i++) {
            Float value = list.get(i);
            if (value == null) {
                printWriter.println("<value />");
            } else {
                printWriter.println("<value>" + SENSOR_DATA_FORMAT.format(value) + "</value>");
            }
        }
        printWriter.println("</SimpleArrayData>");
    }

    private void writeTrackPointType(List<TrackPoint.Type> list) {
        printWriter.println("<SimpleArrayData name=\"" + EXTENDED_DATA_TYPE_TRACKPOINT + "\">");
        for (TrackPoint.Type value : list) {
            printWriter.println("<value>" + value.name() + "</value>");
        }
        printWriter.println("</SimpleArrayData>");
    }

    private void writePlacemark(String name, String activityType, String description, @Nullable Position position, Instant time, ZoneOffset zoneOffset) {
        if (position != null) {
            printWriter.println("<Placemark>");
            printWriter.println("<name>" + StringUtils.formatCData(name) + "</name>");
            printWriter.println("<description>" + StringUtils.formatCData(description) + "</description>");
            printWriter.println("<TimeStamp><when>" + getTime(zoneOffset, time) + "</when></TimeStamp>");
            printWriter.println("<styleUrl>#" + KMLTrackExporter.MARKER_STYLE + "</styleUrl>");
            writeTypeLocalized(activityType);
            printWriter.println("<Point>");
            printWriter.println("<coordinates>" + getCoordinates(position, ",") + "</coordinates>");
            printWriter.println("</Point>");
            printWriter.println("</Placemark>");
        }
    }

    private void writePhotoOverlay(Marker marker, ZoneOffset zoneOffset) {
        printWriter.println("<PhotoOverlay>");
        printWriter.println("<name>" + StringUtils.formatCData(marker.getName()) + "</name>");
        printWriter.println("<description>" + StringUtils.formatCData(marker.getDescription()) + "</description>");
        printWriter.print("<Camera>");
        printWriter.print("<longitude>" + marker.getPosition().longitude() + "</longitude>");
        printWriter.print("<latitude>" + marker.getPosition().latitude() + "</latitude>");
        printWriter.print("<altitude>20</altitude>");
        printWriter.print("<tilt>90</tilt>");
        printWriter.println("</Camera>");
        printWriter.println("<TimeStamp><when>" + getTime(zoneOffset, marker.getTime()) + "</when></TimeStamp>");
        printWriter.println("<styleUrl>#" + MARKER_STYLE + "</styleUrl>");
        writeTypeLocalized(marker.getCategory());

        if (exportPhotos) {
            printWriter.println("<Icon><href>" + KMZTrackExporter.buildKmzImageFilePath(marker) + "</href></Icon>");
        }

        printWriter.print("<ViewVolume>");
        printWriter.print("<near>10</near>");
        printWriter.print("<leftFov>-60</leftFov>");
        printWriter.print("<rightFov>60</rightFov>");
        printWriter.print("<bottomFov>-45</bottomFov>");
        printWriter.print("<topFov>45</topFov>");
        printWriter.println("</ViewVolume>");
        printWriter.println("<Point>");
        printWriter.println("<coordinates>" + getCoordinates(marker.getPosition(), ",") + "</coordinates>");
        printWriter.println("</Point>");
        printWriter.println("</PhotoOverlay>");
    }

    /**
     * Returns the formatted time of the location; either absolute or relative depending exportTrackDetail.
     */
    private String getTime(ZoneOffset zoneOffset, Instant instant) {
        return StringUtils.formatDateTimeIso8601(instant, zoneOffset);
    }

    private static String getCoordinates(Position position, String separator) {
        String result = position.longitude() + separator + position.latitude();
        if (position.hasAltitude()) {
            result += separator + position.altitude().toM();
        }
        return result;
    }

    private void writeTypeLocalized(String localizedValue) {
        if (localizedValue == null || localizedValue.isEmpty()) {
            return;
        }
        printWriter.println("<ExtendedData>");
        printWriter.println("<Data name=\"" + EXTENDED_DATA_TYPE_LOCALIZED + "\"><value>" + StringUtils.formatCData(localizedValue) + "</value></Data>");
        printWriter.println("</ExtendedData>");
    }

    private void writeActivityType(ActivityType value) {
        if (value == null) {
            return;
        }
        printWriter.println("<ExtendedData>");
        printWriter.println("<Data name=\"" + EXTENDED_DATA_ACTIVITY_TYPE + "\"><value>" + StringUtils.formatCData(value.getId()) + "</value></Data>");
        printWriter.println("</ExtendedData>");
    }

    private void writeTrackStyle() {
        printWriter.println("<Style id=\"" + TRACK_STYLE + "\">");
        printWriter.println("<LineStyle><color>7f0000ff</color><width>4</width></LineStyle>");
        printWriter.println("<IconStyle>");
        printWriter.println("<scale>1.3</scale>");
        printWriter.println("<Icon />");
        printWriter.println("</IconStyle>");
        printWriter.println("</Style>");
    }

    /**
     * Writes a placemarker style.
     */
    private void writePlacemarkerStyle() {
        printWriter.println("<Style id=\"" + KMLTrackExporter.MARKER_STYLE + "\"><IconStyle>");
        printWriter.println("<Icon />");
        printWriter.println("</IconStyle></Style>");
    }

    /**
     * Writes a simple array style.
     *
     * @param name             the name of the simple array.
     * @param extendedDataType the extended data display name
     */
    private void writeSimpleArrayStyle(String name, String extendedDataType) {
        printWriter.println("<SimpleArrayField name=\"" + name + "\" type=\"float\">");
        printWriter.println("<displayName>" + StringUtils.formatCData(extendedDataType) + "</displayName>");
        printWriter.println("</SimpleArrayField>");
    }
}
