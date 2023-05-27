package de.dennisguse.opentracks.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import de.dennisguse.opentracks.data.models.TrackPoint;

@RunWith(JUnit4.class)
public class EGM2008UtilsTest {

    private static final double MAX_BILINEAR_ERROR = 0.478;

    private final Context context = ApplicationProvider.getApplicationContext();

    @Test
    public void fileVerification() throws IOException {
        // given
        int expectedLength = 18671444;
        int expectedHeaderLength = 404;

        String expectedHeader = """
                P5
                # Geoid file in PGM format for the GeographicLib::Geoid class
                # Description WGS84 EGM2008, 5-minute grid
                # URL http://earth-info.nga.mil/GandG/wgs84/gravitymod/egm2008
                # DateTime 2009-08-29 18:45:00
                # MaxBilinearError 0.478
                # RMSBilinearError 0.012
                # MaxCubicError 0.294
                # RMSCubicError 0.005
                # Offset -108
                # Scale 0.003
                # Origin 90N 0E
                # AREA_OR_POINT Point
                # Vertical_Datum WGS84
                4320    2161
                65535
                """;

        // when
        try (InputStream inputStream = context.getResources().openRawResource(EGM2008Utils.EGM2008_5_DATA)) {
            assertEquals(expectedLength, inputStream.available());

            InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.US_ASCII);
            char[] data = new char[expectedHeaderLength];
            int length = reader.read(data);

            // then
            assertEquals(expectedHeaderLength, length);
            assertEquals(expectedHeader, new String(data));
        }
    }

    @Test
    public void data_Northpole() throws IOException {
        // given
        TrackPoint trackPoint = new TrackPoint(TrackPoint.Type.TRACKPOINT, Instant.ofEpochMilli(0));
        trackPoint.setLatitude(90);
        trackPoint.setLongitude(0.1);
        trackPoint.setAltitude(0);

        // when
        EGM2008Utils.EGM2008Correction altitude_egm2008 = EGM2008Utils.createCorrection(context, trackPoint.getLocation());

        // then
        assertEquals(-14.8980, altitude_egm2008.correctAltitude(trackPoint.getLocation()).toM(), MAX_BILINEAR_ERROR);
    }

    @Test
    public void data_Southpole() throws IOException {
        // given
        TrackPoint trackPoint = new TrackPoint(TrackPoint.Type.TRACKPOINT, Instant.ofEpochMilli(0));
        trackPoint.setLatitude(-90);
        trackPoint.setLongitude(0);
        trackPoint.setAltitude(0);

        // when
        EGM2008Utils.EGM2008Correction altitude_egm2008 = EGM2008Utils.createCorrection(context, trackPoint.getLocation());

        // then
        assertEquals(30.15, altitude_egm2008.correctAltitude(trackPoint.getLocation()).toM(), MAX_BILINEAR_ERROR);
    }

    @Test
    public void data_Southpole2() throws IOException {
        // given
        TrackPoint trackPoint = new TrackPoint(TrackPoint.Type.TRACKPOINT, Instant.ofEpochMilli(0));
        trackPoint.setLatitude(-90);
        trackPoint.setLongitude(180);
        trackPoint.setAltitude(0);

        // when
        EGM2008Utils.EGM2008Correction altitude_egm2008 = EGM2008Utils.createCorrection(context, trackPoint.getLocation());

        // then
        assertEquals(30.15, altitude_egm2008.correctAltitude(trackPoint.getLocation()).toM(), MAX_BILINEAR_ERROR);
    }

    @Test
    public void data_Southpole3() throws IOException {
        // given
        TrackPoint trackPoint = new TrackPoint(TrackPoint.Type.TRACKPOINT, Instant.ofEpochMilli(0));
        trackPoint.setLatitude(-90);
        trackPoint.setLongitude(-180);
        trackPoint.setAltitude(0);

        // when
        EGM2008Utils.EGM2008Correction altitude_egm2008 = EGM2008Utils.createCorrection(context, trackPoint.getLocation());

        // then
        assertEquals(30.15, altitude_egm2008.correctAltitude(trackPoint.getLocation()).toM(), MAX_BILINEAR_ERROR);
    }

    @Test
    public void data_0() throws IOException {
        // given
        TrackPoint trackPoint = new TrackPoint(TrackPoint.Type.TRACKPOINT, Instant.ofEpochMilli(0));
        trackPoint.setLatitude(0);
        trackPoint.setLongitude(0);
        trackPoint.setAltitude(0);

        // when
        EGM2008Utils.EGM2008Correction altitude_egm2008 = EGM2008Utils.createCorrection(context, trackPoint.getLocation());

        // then
        assertEquals(-17.2260, altitude_egm2008.correctAltitude(trackPoint.getLocation()).toM(), MAX_BILINEAR_ERROR);
    }

    @Test
    public void data_Berlin_Germany_() throws IOException {
        // given
        TrackPoint trackPoint = new TrackPoint(TrackPoint.Type.TRACKPOINT, Instant.ofEpochMilli(0));
        trackPoint.setLatitude(52.530644);
        trackPoint.setLongitude(13.383068);
        trackPoint.setAltitude(0);

        // when
        EGM2008Utils.EGM2008Correction altitude_egm2008 = EGM2008Utils.createCorrection(context, trackPoint.getLocation());

        // then
        assertEquals(-39.4865, altitude_egm2008.correctAltitude(trackPoint.getLocation()).toM(), MAX_BILINEAR_ERROR);
    }

    @Test
    public void data_Berlin_Germany_Caching() throws IOException {
        // given
        TrackPoint trackPoint1 = new TrackPoint(TrackPoint.Type.TRACKPOINT, Instant.ofEpochMilli(0));
        trackPoint1.setLatitude(52.530644);
        trackPoint1.setLongitude(13.383068);
        trackPoint1.setAltitude(0);

        TrackPoint trackPoint2 = new TrackPoint(TrackPoint.Type.TRACKPOINT, Instant.ofEpochMilli(0));
        trackPoint2.setLatitude(52.530000);
        trackPoint2.setLongitude(13.380000);
        trackPoint2.setAltitude(0);

        // when
        EGM2008Utils.EGM2008Correction altitude_egm2008 = EGM2008Utils.createCorrection(context, trackPoint1.getLocation());

        // then
        assertNotEquals(altitude_egm2008.correctAltitude(trackPoint1.getLocation()).toM(), altitude_egm2008.correctAltitude(trackPoint2.getLocation()).toM(), 0.0001);
    }

    @Test
    public void data_Seattle_USA() throws IOException {
        // given
        TrackPoint trackPoint = new TrackPoint(TrackPoint.Type.TRACKPOINT, Instant.ofEpochMilli(0));

        trackPoint.setLatitude(47.63153);
        trackPoint.setLongitude(-122.30938);
        trackPoint.setAltitude(0);

        // when
        EGM2008Utils.EGM2008Correction altitude_egm2008 = EGM2008Utils.createCorrection(context, trackPoint.getLocation());

        // then
        assertEquals(22.99, altitude_egm2008.correctAltitude(trackPoint.getLocation()).toM(), MAX_BILINEAR_ERROR);
    }

    @Test
    public void data_MaxUndulation() throws IOException {
        // given
        TrackPoint trackPoint = new TrackPoint(TrackPoint.Type.TRACKPOINT, Instant.ofEpochMilli(0));
        trackPoint.setLatitude(-8.417);
        trackPoint.setLongitude(147.367);
        trackPoint.setAltitude(0);

        // when
        EGM2008Utils.EGM2008Correction altitude_egm2008 = EGM2008Utils.createCorrection(context, trackPoint.getLocation());

        // then
        assertEquals(-85.824, altitude_egm2008.correctAltitude(trackPoint.getLocation()).toM(), MAX_BILINEAR_ERROR);
    }


    @Test
    public void getIndices() {
        TrackPoint trackPoint = new TrackPoint(TrackPoint.Type.TRACKPOINT, Instant.ofEpochMilli(0));
        trackPoint.setLatitude(90);
        trackPoint.setLongitude(0);
        assertEquals(new EGM2008Utils.Indices(0, 0), EGM2008Utils.getIndices(trackPoint.getLocation()));

        trackPoint.setLatitude(0);
        trackPoint.setLongitude(0);
        assertEquals(new EGM2008Utils.Indices(1080, 0), EGM2008Utils.getIndices(trackPoint.getLocation()));

        trackPoint.setLatitude(-90);
        trackPoint.setLongitude(180);
        assertEquals(new EGM2008Utils.Indices(2160, 4320 / 2), EGM2008Utils.getIndices(trackPoint.getLocation()));

        trackPoint.setLatitude(-90);
        trackPoint.setLongitude(-180);
        assertEquals(new EGM2008Utils.Indices(2160, 2160), EGM2008Utils.getIndices(trackPoint.getLocation()));
    }

    @Test
    public void getUndulationRaw_ok() throws IOException {
        try (DataInputStream dataInputStream = new DataInputStream(context.getResources().openRawResource(EGM2008Utils.EGM2008_5_DATA))) {
            assertEquals(40966, EGM2008Utils.getUndulationRaw(dataInputStream, new EGM2008Utils.Indices(0, 0)));
            assertEquals(41742, EGM2008Utils.getUndulationRaw(dataInputStream, new EGM2008Utils.Indices(1081, 0)));
            assertEquals(25950, EGM2008Utils.getUndulationRaw(dataInputStream, new EGM2008Utils.Indices(2160, 4319)));
        }
    }

    @Test(expected = EOFException.class)
    public void getUndulationRaw_error() throws IOException {
        try (DataInputStream dataInputStream = new DataInputStream(context.getResources().openRawResource(EGM2008Utils.EGM2008_5_DATA))) {
            assertEquals(0, EGM2008Utils.getUndulationRaw(dataInputStream, new EGM2008Utils.Indices(2161, 4320)), 0.01);
        }
    }
}
