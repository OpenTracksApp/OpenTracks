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
import android.location.Location;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.content.DescriptionGenerator;
import de.dennisguse.opentracks.content.data.Marker;
import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.content.data.TrackPoint;
import de.dennisguse.opentracks.content.provider.ContentProviderUtils;
import de.dennisguse.opentracks.content.provider.TrackPointIterator;
import de.dennisguse.opentracks.util.FileUtils;
import de.dennisguse.opentracks.util.StringUtils;

/**
 * Convert {@link Track} incl. {@link Marker} and {@link TrackPoint} to KML.
 *
 * @author Sandor Dornbush
 * @author Rodrigo Damazio
 * @author Leif Hendrik Wilden
 */
public class KMLTrackExporter implements TrackExporter {

    private static final String TAG = KMLTrackExporter.class.getSimpleName();

    public static final String MARKER_STYLE = "waypoint";
    private static final String START_STYLE = "start";
    private static final String END_STYLE = "end";
    private static final String TRACK_STYLE = "track";
    private static final String SCHEMA_ID = "schema";

    public static final String EXTENDED_DATA_TYPE_SPEED = "speed";
    public static final String EXTENDED_DATA_TYPE_DISTANCE = "distance";
    public static final String EXTENDED_DATA_TYPE_CADENCE = "cadence";
    public static final String EXTENDED_DATA_TYPE_HEART_RATE = "heart_rate";
    public static final String EXTENDED_DATA_TYPE_POWER = "power";
    public static final String EXTENDED_DATA_TYPE_ELEVATION_GAIN = "elevation_gain";
    public static final String EXTENDED_DATA_TYPE_ELEVATION_LOSS = "elevation_loss";

    private static final String MARKER_ICON = "http://maps.google.com/mapfiles/kml/pushpin/blue-pushpin.png";
    private static final String START_ICON = "http://maps.google.com/mapfiles/kml/paddle/grn-circle.png";
    private static final String END_ICON = "http://maps.google.com/mapfiles/kml/paddle/red-circle.png";
    private static final String TRACK_ICON = "http://earth.google.com/images/kml-icons/track-directional/track-0.png";

    private final Context context;
    private final boolean exportPhotos;
    private final boolean exportTrackDetail;
    private final boolean exportSensorData;
    private final DescriptionGenerator descriptionGenerator;
    private final ContentProviderUtils contentProviderUtils;

    private PrintWriter printWriter;
    private final List<Float> speedList = new ArrayList<>();
    private final List<Float> distanceList = new ArrayList<>();
    private final List<Float> powerList = new ArrayList<>();
    private final List<Float> cadenceList = new ArrayList<>();
    private final List<Float> heartRateList = new ArrayList<>();
    private final List<Float> elevationGainList = new ArrayList<>();
    private final List<Float> elevationLossList = new ArrayList<>();

    private TrackPoint startTrackPoint;

    public KMLTrackExporter(Context context, boolean exportTrackDetail, boolean exportSensorData, boolean exportPhotos) {
        this.context = context;
        this.exportTrackDetail = exportTrackDetail;
        this.exportSensorData = exportSensorData;
        this.exportPhotos = exportPhotos;
        this.descriptionGenerator = new DescriptionGenerator(context);
        this.contentProviderUtils = new ContentProviderUtils(context);
    }

    public boolean writeTrack(Track track, @NonNull OutputStream outputStream) {
        return writeTrack(new Track[]{track}, outputStream);
    }

    public boolean writeTrack(Track[] tracks, @NonNull OutputStream outputStream) {
        try {
            prepare(outputStream);
            writeHeader(tracks);
            for (Track track : tracks) {
                writeMarkers(track);
            }
            boolean hasMultipleTracks = tracks.length > 1;
            if (hasMultipleTracks) {
                writeMultiTrackBegin();
            }
            //TODO Why use startTime of first track for the others?
            Instant startTime = tracks[0].getTrackStatistics().getStartTime();
            for (Track track : tracks) {
                Duration offset = Duration.between(track.getTrackStatistics().getStartTime(), startTime);
                writeLocations(track, offset);
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
                    writeMarker(marker);

                    cursor.moveToNext();
                }
            }
        }
        if (hasMarkers) {
            writeEndMarkers();
        }
    }

    private void writeLocations(Track track, Duration offset) throws InterruptedException {
        boolean wroteTrack = false;
        boolean wroteSegment = false;

        try (TrackPointIterator trackPointIterator = contentProviderUtils.getTrackPointLocationIterator(track.getId(), null)) {
            while (trackPointIterator.hasNext()) {
                if (Thread.interrupted()) throw new InterruptedException();

                TrackPoint trackPoint = trackPointIterator.next();
                setLocationTime(trackPoint, offset);

                if (!wroteTrack) {
                    writeBeginTrack(track, trackPoint);
                    wroteTrack = true;
                }

                switch (trackPoint.getType()) {
                    case SEGMENT_START_MANUAL:
                    case SEGMENT_START_AUTOMATIC:
                        if (wroteSegment) writeCloseSegment();
                        writeOpenSegment();
                        writeTrackPoint(trackPoint);
                        wroteSegment = true;
                        break;
                    case SEGMENT_END_MANUAL:
                        if (!wroteSegment) writeOpenSegment();
                        writeTrackPoint(trackPoint);
                        writeCloseSegment();
                        wroteSegment = false;
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

            if (wroteTrack) {
                TrackPoint lastValidTrackPoint = contentProviderUtils.getLastValidTrackPoint(track.getId());
                setLocationTime(lastValidTrackPoint, offset);
                writeEndTrack(track, lastValidTrackPoint);
            } else {
                // Write an empty track
                writeBeginTrack(track, null);
                writeEndTrack(track, null);
            }
        }
    }

    @VisibleForTesting
    void prepare(OutputStream outputStream) {
        this.printWriter = new PrintWriter(outputStream);
    }

    @VisibleForTesting
    void close() {
        if (printWriter != null) {
            printWriter.flush();
            printWriter = null;
        }
    }

    private void writeHeader(Track[] tracks) {
        if (printWriter != null) {
            printWriter.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            printWriter.println("<kml xmlns=\"http://www.opengis.net/kml/2.2\"");
            printWriter.println("xmlns:gx=\"http://www.google.com/kml/ext/2.2\"");
            printWriter.println("xmlns:atom=\"http://www.w3.org/2005/Atom\"");
            printWriter.println("xmlns:opentracks=\"http://opentracksapp.com/xmlschemas/v1\">");
            //TODO ADD xsi:schemaLocation here!

            printWriter.println("<Document>");
            printWriter.println("<open>1</open>");
            printWriter.println("<visibility>1</visibility>");

            if (exportTrackDetail) {
                Track track = tracks[0];
                printWriter.println("<name>" + StringUtils.formatCData(track.getName()) + "</name>");
                printWriter.println("<atom:generator>" + StringUtils.formatCData(context.getString(R.string.app_name)) + "</atom:generator>");
            }

            writeTrackStyle();
            writePlacemarkerStyle(START_STYLE, START_ICON, 32, 1);
            writePlacemarkerStyle(END_STYLE, END_ICON, 32, 1);
            writePlacemarkerStyle(MARKER_STYLE, MARKER_ICON, 20, 2);
            printWriter.println("<Schema id=\"" + SCHEMA_ID + "\">");

            writeSimpleArrayStyle(EXTENDED_DATA_TYPE_SPEED, context.getString(R.string.description_speed_ms));

            if (exportSensorData) {
                writeSimpleArrayStyle(EXTENDED_DATA_TYPE_POWER, context.getString(R.string.description_sensor_power));
                writeSimpleArrayStyle(EXTENDED_DATA_TYPE_CADENCE, context.getString(R.string.description_sensor_cadence));
                writeSimpleArrayStyle(EXTENDED_DATA_TYPE_HEART_RATE, context.getString(R.string.description_sensor_heart_rate));
            }
            printWriter.println("</Schema>");
        }
    }

    private void writeFooter() {
        if (printWriter != null) {
            printWriter.println("</Document>");
            printWriter.println("</kml>");
        }
    }

    private void writeBeginMarkers(Track track) {
        if (printWriter != null) {
            printWriter.println("<Folder>");
            if (exportTrackDetail) {
                printWriter.println("<name>" + StringUtils.formatCData(context.getString(R.string.track_markers, track.getName())) + "</name>");
            }
            printWriter.println("<open>1</open>");
        }
    }

    private void writeMarker(Marker marker) {
        if (printWriter != null && exportTrackDetail) {
            boolean existsPhoto = FileUtils.buildInternalPhotoFile(context, marker.getTrackId(), marker.getPhotoURI()) != null;
            if (marker.hasPhoto() && exportPhotos && existsPhoto) {
                float heading = getHeading(marker.getTrackId(), marker.getLocation());
                writePhotoOverlay(marker, heading);
            } else {
                writePlacemark(marker.getName(), marker.getCategory(), marker.getDescription(), MARKER_STYLE, marker.getLocation());
            }
        }
    }

    private void writeEndMarkers() {
        if (printWriter != null) {
            printWriter.println("</Folder>");
        }
    }

    private void writeMultiTrackBegin() {
        if (printWriter != null) {
            printWriter.println("<Folder id=tour>");
            printWriter.println("<name>" + context.getString(R.string.generic_tracks) + "</name>");
            printWriter.println("<open>1</open>");
        }
    }

    private void writeMultiTrackEnd() {
        if (printWriter != null) {
            printWriter.println("</Folder>");
        }
    }

    private void writeBeginTrack(Track track, TrackPoint startTrackPoint) {
        this.startTrackPoint = startTrackPoint;
        if (printWriter != null) {
            String name = context.getString(R.string.marker_label_start, track.getName());
            Location location = startTrackPoint != null ? startTrackPoint.getLocation() : null;
            writePlacemark(name, "", "", START_STYLE, location);
            printWriter.println("<Placemark>");

            if (exportTrackDetail) {
                printWriter.println("<name>" + StringUtils.formatCData(track.getName()) + "</name>");
                printWriter.println("<description>" + StringUtils.formatCData(track.getDescription()) + "</description>");
                printWriter.println("<icon>" + StringUtils.formatCData(track.getIcon()) + "</icon>");
                printWriter.println("<opentracks:trackid>" + track.getUuid() + "</opentracks:trackid>");
            }

            printWriter.println("<styleUrl>#" + TRACK_STYLE + "</styleUrl>");
            writeCategory(track.getCategory());
            printWriter.println("<gx:MultiTrack>");
            printWriter.println("<altitudeMode>absolute</altitudeMode>");
            printWriter.println("<gx:interpolate>1</gx:interpolate>");
        }
    }


    private void writeEndTrack(Track track, TrackPoint endTrackPoint) {
        if (printWriter != null) {
            printWriter.println("</gx:MultiTrack>");
            printWriter.println("</Placemark>");

            if (exportTrackDetail) {
                String name = context.getString(R.string.marker_label_end, track.getName());
                String description = descriptionGenerator.generateTrackDescription(track, false);
                Location location = endTrackPoint != null ? endTrackPoint.getLocation() : null;
                writePlacemark(name, "", description, END_STYLE, location);
            }
        }
    }

    @VisibleForTesting
    void writeOpenSegment() {
        if (printWriter != null) {
            printWriter.println("<gx:Track>");
            speedList.clear();
            distanceList.clear();
            powerList.clear();
            cadenceList.clear();
            heartRateList.clear();
            elevationGainList.clear();
            elevationLossList.clear();
        }
    }

    @VisibleForTesting
    void writeCloseSegment() {
        if (printWriter != null) {
            printWriter.println("<ExtendedData>");
            printWriter.println("<SchemaData schemaUrl=\"#" + SCHEMA_ID + "\">");
            if (speedList.stream().anyMatch(Objects::nonNull)) {
                writeSimpleArrayData(speedList, EXTENDED_DATA_TYPE_SPEED);
            }
            if (exportSensorData) {
                if (distanceList.stream().anyMatch(Objects::nonNull)) {
                    writeSimpleArrayData(distanceList, EXTENDED_DATA_TYPE_DISTANCE);
                }
                if (powerList.stream().anyMatch(Objects::nonNull)) {
                    writeSimpleArrayData(powerList, EXTENDED_DATA_TYPE_POWER);
                }
                if (cadenceList.stream().anyMatch(Objects::nonNull)) {
                    writeSimpleArrayData(cadenceList, EXTENDED_DATA_TYPE_CADENCE);
                }
                if (heartRateList.stream().anyMatch(Objects::nonNull)) {
                    writeSimpleArrayData(heartRateList, EXTENDED_DATA_TYPE_HEART_RATE);
                }
                if (elevationGainList.stream().anyMatch(Objects::nonNull)) {
                    writeSimpleArrayData(elevationGainList, EXTENDED_DATA_TYPE_ELEVATION_GAIN);
                }
                if (elevationLossList.stream().anyMatch(Objects::nonNull)) {
                    writeSimpleArrayData(elevationLossList, EXTENDED_DATA_TYPE_ELEVATION_LOSS);
                }
            }
            printWriter.println("</SchemaData>");
            printWriter.println("</ExtendedData>");
            printWriter.println("</gx:Track>");
        }
    }

    @VisibleForTesting
    void writeTrackPoint(TrackPoint trackPoint) {
        if (printWriter != null) {
            if (exportTrackDetail) {
                printWriter.println("<when>" + getTime(trackPoint.getLocation()) + "</when>");
            }

            if (trackPoint.hasLocation()) {
                printWriter.println("<gx:coord>" + (trackPoint.hasLocation() ? getCoordinates(trackPoint.getLocation(), " ") : "") + "</gx:coord>");
            } else {
                printWriter.println("<gx:coord/>");
            }
            speedList.add(trackPoint.hasSpeed() ? trackPoint.getSpeed() : null);

            if (exportSensorData) {
                distanceList.add(trackPoint.hasSensorDistance() ? trackPoint.getSensorDistance() : null);
                heartRateList.add(trackPoint.hasHeartRate() ? trackPoint.getHeartRate_bpm() : null);
                cadenceList.add(trackPoint.hasCyclingCadence() ? trackPoint.getCyclingCadence_rpm() : null);
                powerList.add(trackPoint.hasPower() ? trackPoint.getPower() : null);

                elevationGainList.add(trackPoint.hasElevationGain() ? trackPoint.getElevationGain() : null);
                elevationLossList.add(trackPoint.hasElevationLoss() ? trackPoint.getElevationLoss() : null);
            }
        }
    }

    /**
     * Writes the simple array data.
     *
     * @param list a list of simple array data
     * @param name the name of the simple array data
     */
    private void writeSimpleArrayData(List<Float> list, String name) {
        printWriter.println("<gx:SimpleArrayData name=\"" + name + "\">");
        for (int i = 0; i < list.size(); i++) {
            Float value = list.get(i);
            if (value == null) {
                printWriter.println("<gx:value />");
            } else {
                printWriter.println("<gx:value>" + list.get(i) + "</gx:value>");
            }
        }
        printWriter.println("</gx:SimpleArrayData>");
    }

    /**
     * Writes a placemark.
     *
     * @param name        the name
     * @param category    the category
     * @param description the description
     * @param styleName   the style name
     * @param location    the location
     */
    private void writePlacemark(String name, String category, String description, String styleName, Location location) {
        if (location != null && exportTrackDetail) {
            printWriter.println("<Placemark>");
            printWriter.println("<name>" + StringUtils.formatCData(name) + "</name>");
            printWriter.println("<description>" + StringUtils.formatCData(description) + "</description>");
            printWriter.println("<TimeStamp><when>" + getTime(location) + "</when></TimeStamp>");
            printWriter.println("<styleUrl>#" + styleName + "</styleUrl>");
            writeCategory(category);
            printWriter.println("<Point>");
            printWriter.println("<coordinates>" + getCoordinates(location, ",") + "</coordinates>");
            printWriter.println("</Point>");
            printWriter.println("</Placemark>");
        }
    }

    private void writePhotoOverlay(Marker marker, float heading) {
        if (exportTrackDetail) {
            printWriter.println("<PhotoOverlay>");
            printWriter.println("<name>" + StringUtils.formatCData(marker.getName()) + "</name>");
            printWriter.println("<description>" + StringUtils.formatCData(marker.getDescription()) + "</description>");
            printWriter.print("<Camera>");
            printWriter.print("<longitude>" + marker.getLongitude() + "</longitude>");
            printWriter.print("<latitude>" + marker.getLatitude() + "</latitude>");
            printWriter.print("<altitude>20</altitude>");
            printWriter.print("<heading>" + heading + "</heading>");
            printWriter.print("<tilt>90</tilt>");
            printWriter.println("</Camera>");
            printWriter.println("<TimeStamp><when>" + getTime(marker.getLocation()) + "</when></TimeStamp>");
            printWriter.println("<styleUrl>#" + MARKER_STYLE + "</styleUrl>");
            writeCategory(marker.getCategory());

            if (exportPhotos) {
                printWriter.println("<Icon><href>" + KmzTrackExporter.buildKmzImageFilePath(marker) + "</href></Icon>");
            }

            printWriter.print("<ViewVolume>");
            printWriter.print("<near>10</near>");
            printWriter.print("<leftFov>-60</leftFov>");
            printWriter.print("<rightFov>60</rightFov>");
            printWriter.print("<bottomFov>-45</bottomFov>");
            printWriter.print("<topFov>45</topFov>");
            printWriter.println("</ViewVolume>");
            printWriter.println("<Point>");
            printWriter.println("<coordinates>" + getCoordinates(marker.getLocation(), ",") + "</coordinates>");
            printWriter.println("</Point>");
            printWriter.println("</PhotoOverlay>");
        }
    }

    /**
     * Returns the formatted time of the location; either absolute or relative depending exportTrackDetail.
     *
     * @param location the location
     */
    private String getTime(Location location) {
        if (exportTrackDetail) {
            return StringUtils.formatDateTimeIso8601(Instant.ofEpochMilli(location.getTime()));
        } else {
            return StringUtils.formatDateTimeIso8601(Instant.ofEpochMilli(location.getTime() - startTrackPoint.getTime().toEpochMilli()));
        }
    }

    /**
     * Gets the heading to a location.
     *
     * @param trackId  the track id containing the location
     * @param location the location
     */
    private float getHeading(Track.Id trackId, Location location) {
        TrackPoint.Id trackPointId = contentProviderUtils.getTrackPointId(trackId, location);
        if (trackPointId == null) {
            return location.getBearing();
        }
        TrackPoint viewLocation = contentProviderUtils.getLastValidTrackPoint(trackId);
        if (viewLocation != null) {
            return viewLocation.bearingTo(location);
        }

        return location.getBearing();
    }

    private String getCoordinates(Location location, String separator) {
        StringBuilder builder = new StringBuilder();
        builder.append(location.getLongitude()).append(separator).append(location.getLatitude());
        if (location.hasAltitude()) {
            builder.append(separator).append(location.getAltitude());
        }
        return builder.toString();
    }

    /**
     * Writes the category.
     *
     * @param category the category
     */
    private void writeCategory(String category) {
        if (category == null || category.equals("")) {
            return;
        }
        printWriter.println("<ExtendedData>");
        printWriter.println("<Data name=\"type\"><value>" + StringUtils.formatCData(category) + "</value></Data>");
        printWriter.println("</ExtendedData>");
    }

    /**
     * Writes the track style.
     */
    private void writeTrackStyle() {
        printWriter.println("<Style id=\"" + TRACK_STYLE + "\">");
        printWriter.println("<LineStyle><color>7f0000ff</color><width>4</width></LineStyle>");
        printWriter.println("<IconStyle>");
        printWriter.println("<scale>1.3</scale>");
        printWriter.println("<Icon><href>" + TRACK_ICON + "</href></Icon>");
        printWriter.println("</IconStyle>");
        printWriter.println("</Style>");
    }

    /**
     * Writes a placemarker style.
     *
     * @param name the name of the style
     * @param url  the url of the style icon
     * @param x    the x position of the hotspot
     * @param y    the y position of the hotspot
     */
    private void writePlacemarkerStyle(String name, String url, int x, int y) {
        printWriter.println("<Style id=\"" + name + "\"><IconStyle>");
        printWriter.println("<scale>1.3</scale>");
        printWriter.println("<Icon><href>" + url + "</href></Icon>");
        printWriter.println("<hotSpot x=\"" + x + "\" y=\"" + y + "\" xunits=\"pixels\" yunits=\"pixels\"/>");
        printWriter.println("</IconStyle></Style>");
    }

    /**
     * Writes a simple array style.
     *
     * @param name             the name of the simple array.
     * @param extendedDataType the extended data display name
     */
    private void writeSimpleArrayStyle(String name, String extendedDataType) {
        printWriter.println("<gx:SimpleArrayField name=\"" + name + "\" type=\"float\">");
        printWriter.println("<displayName>" + StringUtils.formatCData(extendedDataType) + "</displayName>");
        printWriter.println("</gx:SimpleArrayField>");
    }

    /**
     * Sets a trackPoint time.
     *
     * @param trackPoint the trackPoint
     * @param offset     the time offset
     */
    //TODO Why?
    private void setLocationTime(TrackPoint trackPoint, Duration offset) {
        if (trackPoint != null) {
            trackPoint.setTime(trackPoint.getTime().minus(offset));
        }
    }
}
