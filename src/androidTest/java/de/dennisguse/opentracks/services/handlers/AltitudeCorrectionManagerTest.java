package de.dennisguse.opentracks.services.handlers;

import static org.junit.Assert.assertEquals;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;
import java.time.Instant;

import de.dennisguse.opentracks.data.models.TrackPoint;

/**
 * Expectation is EGM2008 from  <a href="https://geographiclib.sourceforge.io/">GeographicLib</a>] EGM2008 5minute undulation data.
 */
@RunWith(JUnit4.class)
public class AltitudeCorrectionManagerTest {

    private static final double MAX_ERROR = 0.4;

    private final Context context = ApplicationProvider.getApplicationContext();

    @Test
    public void data_Northpole() {
        // given
        TrackPoint trackPoint = new TrackPoint(TrackPoint.Type.TRACKPOINT, Instant.ofEpochMilli(0));
        trackPoint.setLatitude(90);
        trackPoint.setLongitude(0.1);
        trackPoint.setAltitude(0);

        // when
        new AltitudeCorrectionManager().correctAltitude(context, trackPoint);

        // then
        assertEquals(-14.8980, trackPoint.getAltitude().toM(), 2 * MAX_ERROR); //TODO For some reason, the Northpole is more off.
    }

    @Test
    public void data_Southpole() {
        // given
        TrackPoint trackPoint = new TrackPoint(TrackPoint.Type.TRACKPOINT, Instant.ofEpochMilli(0));
        trackPoint.setLatitude(-90);
        trackPoint.setLongitude(0);
        trackPoint.setAltitude(0);

        // when
        new AltitudeCorrectionManager().correctAltitude(context, trackPoint);

        // then
        assertEquals(30.15, trackPoint.getAltitude().toM(), MAX_ERROR);
    }

    @Test
    public void data_Southpole2() {
        // given
        TrackPoint trackPoint = new TrackPoint(TrackPoint.Type.TRACKPOINT, Instant.ofEpochMilli(0));
        trackPoint.setLatitude(-90);
        trackPoint.setLongitude(180);
        trackPoint.setAltitude(0);

        // when
        new AltitudeCorrectionManager().correctAltitude(context, trackPoint);

        // then
        assertEquals(30.15, trackPoint.getAltitude().toM(), MAX_ERROR);
    }

    @Test
    public void data_Southpole3() {
        // given
        TrackPoint trackPoint = new TrackPoint(TrackPoint.Type.TRACKPOINT, Instant.ofEpochMilli(0));
        trackPoint.setLatitude(-90);
        trackPoint.setLongitude(-180);
        trackPoint.setAltitude(0);

        // when
        new AltitudeCorrectionManager().correctAltitude(context, trackPoint);

        // then
        assertEquals(30.15, trackPoint.getAltitude().toM(), MAX_ERROR);
    }

    @Test
    public void data_0() throws IOException {
        // given
        TrackPoint trackPoint = new TrackPoint(TrackPoint.Type.TRACKPOINT, Instant.ofEpochMilli(0));
        trackPoint.setLatitude(0);
        trackPoint.setLongitude(0);
        trackPoint.setAltitude(0);

        // when
        new AltitudeCorrectionManager().correctAltitude(context, trackPoint);

        // then
        assertEquals(-17.2260, trackPoint.getAltitude().toM(), MAX_ERROR);
    }

    @Test
    public void data_Berlin_Germany_() {
        // given
        TrackPoint trackPoint = new TrackPoint(TrackPoint.Type.TRACKPOINT, Instant.ofEpochMilli(0));
        trackPoint.setLatitude(52.530644);
        trackPoint.setLongitude(13.383068);
        trackPoint.setAltitude(0);

        // when
        new AltitudeCorrectionManager().correctAltitude(context, trackPoint);

        // then
        assertEquals(-39.4865, trackPoint.getAltitude().toM(), MAX_ERROR);
    }

    @Test
    public void data_Seattle_USA() {
        // given
        TrackPoint trackPoint = new TrackPoint(TrackPoint.Type.TRACKPOINT, Instant.ofEpochMilli(0));

        trackPoint.setLatitude(47.63153);
        trackPoint.setLongitude(-122.30938);
        trackPoint.setAltitude(0);

        // when
        new AltitudeCorrectionManager().correctAltitude(context, trackPoint);

        // then
        assertEquals(22.99, trackPoint.getAltitude().toM(), MAX_ERROR);
    }

    @Test
    public void data_MaxUndulation() throws IOException {
        // given
        TrackPoint trackPoint = new TrackPoint(TrackPoint.Type.TRACKPOINT, Instant.ofEpochMilli(0));
        trackPoint.setLatitude(-8.417);
        trackPoint.setLongitude(147.367);
        trackPoint.setAltitude(0);

        // when
        new AltitudeCorrectionManager().correctAltitude(context, trackPoint);

        // then
        assertEquals(-85.824, trackPoint.getAltitude().toM(), MAX_ERROR);
    }
}
