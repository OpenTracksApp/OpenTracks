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

import android.util.Log;

import androidx.annotation.NonNull;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

import de.dennisguse.opentracks.data.ContentProviderUtils;
import de.dennisguse.opentracks.data.TrackPointIterator;
import de.dennisguse.opentracks.data.models.Marker;
import de.dennisguse.opentracks.data.models.Track;
import de.dennisguse.opentracks.data.models.TrackPoint;
import de.dennisguse.opentracks.util.StringUtils;

/**
 * Exports the {@link TrackPoint} into a CSV.
 * * decimal separator: .
 * * column separator: ,
 * <p>
 * NOTE:
 * * {@link Track} data is not exported.
 * * {@link Marker} data is not exported.
 */
public class CSVTrackExporter implements TrackExporter {

    private static final String TAG = CSVTrackExporter.class.getSimpleName();

    private static final NumberFormat ALTITUDE_FORMAT = NumberFormat.getInstance(Locale.US);
    private static final NumberFormat COORDINATE_FORMAT = NumberFormat.getInstance(Locale.US);
    private static final NumberFormat SPEED_FORMAT = NumberFormat.getInstance(Locale.US);
    private static final NumberFormat DISTANCE_FORMAT = NumberFormat.getInstance(Locale.US);
    private static final NumberFormat HEARTRATE_FORMAT = NumberFormat.getInstance(Locale.US);
    private static final NumberFormat CADENCE_FORMAT = NumberFormat.getInstance(Locale.US);
    private static final NumberFormat POWER_FORMAT = NumberFormat.getInstance(Locale.US);

    static {
        ALTITUDE_FORMAT.setMaximumFractionDigits(1);
        ALTITUDE_FORMAT.setGroupingUsed(false);

        COORDINATE_FORMAT.setMaximumFractionDigits(6);
        COORDINATE_FORMAT.setGroupingUsed(false);

        SPEED_FORMAT.setMaximumFractionDigits(2);
        SPEED_FORMAT.setGroupingUsed(false);

        DISTANCE_FORMAT.setMaximumFractionDigits(0);
        DISTANCE_FORMAT.setGroupingUsed(false);

        HEARTRATE_FORMAT.setMaximumFractionDigits(0);
        HEARTRATE_FORMAT.setGroupingUsed(false);

        CADENCE_FORMAT.setMaximumFractionDigits(0);
        CADENCE_FORMAT.setGroupingUsed(false);

        POWER_FORMAT.setMaximumFractionDigits(0);
        POWER_FORMAT.setGroupingUsed(false);
    }

    private final ContentProviderUtils contentProviderUtils;

    private PrintWriter printWriter;

    public CSVTrackExporter(ContentProviderUtils contentProviderUtils) {
        this.contentProviderUtils = contentProviderUtils;
    }

    @Override
    public boolean writeTrack(@NonNull List<Track> tracks, @NonNull OutputStream outputStream) {
        List<Column> columns = List.of(
                new Column("time", null),
                new Column("trackpoint_type", t -> quote(t.getType().name())),
                new Column("latitude", t -> t.hasLocation() ? COORDINATE_FORMAT.format(t.getPosition().latitude()) : ""),
                new Column("longitude", t -> t.hasLocation() ? COORDINATE_FORMAT.format(t.getPosition().longitude()) : ""),
                new Column("altitude", t -> t.hasAltitude() ? ALTITUDE_FORMAT.format(t.getAltitude().toM()) : ""),
                new Column("accuracy_horizontal", t -> t.hasHorizontalAccuracy() ? DISTANCE_FORMAT.format(t.getHorizontalAccuracy().toM()) : ""),
                new Column("accuracy_vertical", t -> t.hasVerticalAccuracy() ? DISTANCE_FORMAT.format(t.getVerticalAccuracy().toM()) : ""),

                new Column("speed", t -> t.hasSpeed() ? SPEED_FORMAT.format(t.getSpeed().toKMH()) : ""),
                new Column("altitude_gain", t -> t.hasAltitudeGain() ? ALTITUDE_FORMAT.format(t.getAltitudeGain()) : ""),
                new Column("altitude_loss", t -> t.hasAltitudeLoss() ? ALTITUDE_FORMAT.format(t.getAltitudeLoss()) : ""),
                new Column("sensor_distance", t -> t.hasSensorDistance() ? DISTANCE_FORMAT.format(t.getSensorDistance().toM()) : ""),
                new Column("heartrate", t -> t.hasHeartRate() ? HEARTRATE_FORMAT.format(t.getHeartRate().getBPM()) : ""),
                new Column("cadence", t -> t.hasCadence() ? CADENCE_FORMAT.format(t.getCadence().getRPM()) : ""),
                new Column("power", t -> t.hasPower() ? POWER_FORMAT.format(t.getPower().getW()) : ""));

        try {
            prepare(outputStream);

            boolean headerWritten = false;

            for (Track track : tracks) {
                columns.get(0).extractor = t -> quote(StringUtils.formatDateTimeIso8601(t.getTime(), track.getZoneOffset()));

                if (!headerWritten) {
                    writeHeader(columns);
                    headerWritten = true;
                }

                writeTrackPoints(columns, track);
            }

            close();

            return true;
        } catch (InterruptedException e) {
            Log.e(TAG, "Thread interrupted", e);
            return false;
        }
    }

    private void writeTrackPoints(List<Column> columns, Track track) throws InterruptedException {
        try (TrackPointIterator trackPointIterator = contentProviderUtils.getTrackPointLocationIterator(track.getId(), null)) {
            while (trackPointIterator.hasNext()) {
                if (Thread.interrupted()) throw new InterruptedException();

                writeTrackPoint(columns, trackPointIterator.next());
            }
        }
    }

    public void prepare(OutputStream outputStream) {
        this.printWriter = new PrintWriter(outputStream);
    }

    public void close() {
        printWriter.flush();
        printWriter = null;
    }

    public void writeHeader(List<Column> columns) {
        String columnNames = columns.stream().map(c -> c.columnName).reduce((s, s2) -> s + "," + s2).orElseThrow(() -> new RuntimeException("No columns defined"));
        printWriter.println("#" + columnNames);
    }

    public void writeTrackPoint(List<Column> columns, TrackPoint trackPoint) {
        String columnNames = columns.stream().map(c -> c.extractor.apply(trackPoint)).reduce((s, s2) -> s + "," + s2).orElseThrow(() -> new RuntimeException("No columns defined"));
        printWriter.println(columnNames);
    }

    private static class Column {
        final String columnName;
        Function<TrackPoint, String> extractor;

        Column(String columnName, Function<TrackPoint, String> extractor) {
            this.columnName = columnName;
            this.extractor = extractor;
        }
    }

    private static String quote(String content) {
        return '"' + content + '"';
    }
}
