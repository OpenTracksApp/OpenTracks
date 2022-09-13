package de.dennisguse.opentracks.io.file.exporter;

import static org.junit.Assert.assertEquals;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.ZoneOffset;

import de.dennisguse.opentracks.data.models.TrackPoint;
import de.dennisguse.opentracks.io.file.TrackFileFormat;

@RunWith(JUnit4.class)
public class KmlTrackExporterTest {

    private final Context context = ApplicationProvider.getApplicationContext();

    /**
     * Sensor data by type should only be created if present in at least on TrackPoint.
     */
    @Test
    public void writeCloseSegment_only_write_sensordata_if_present() {
        String expected = "<when>1970-01-01T00:00:00Z</when>\n" +
                "<coord/>\n" +
                "<when>1970-01-01T01:00:00+01:00</when>\n" +
                "<coord/>\n" +
                "<ExtendedData>\n" +
                "<SchemaData schemaUrl=\"#schema\">\n" +
                "</SchemaData>\n" +
                "</ExtendedData>\n" +
                "</Track>\n";

        // given
        TrackPoint trackPoint = new TrackPoint(TrackPoint.Type.SEGMENT_START_MANUAL, Instant.ofEpochSecond(0));

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        KMLTrackExporter kmlTrackWriter = (KMLTrackExporter) TrackFileFormat.KML_WITH_TRACKDETAIL_AND_SENSORDATA.createTrackExporter(context);
        kmlTrackWriter.prepare(outputStream);

        kmlTrackWriter.writeTrackPoint(ZoneOffset.UTC, trackPoint);
        kmlTrackWriter.writeTrackPoint(ZoneOffset.ofTotalSeconds(3600), trackPoint);

        // when
        kmlTrackWriter.writeCloseSegment();
        kmlTrackWriter.close();

        // then
        assertEquals(expected, outputStream.toString());
    }
}