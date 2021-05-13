package de.dennisguse.opentracks.content.data;

import android.location.Location;

import org.junit.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TrackPointTest {

    @Test
    public void isRecent_true() {
        TrackPoint tp = new TrackPoint(TrackPoint.Type.SEGMENT_END_MANUAL);
        tp.setTime(Instant.now());

        assertTrue(tp.isRecent());
    }

    @Test
    public void isRecent_false() {
        TrackPoint tp = new TrackPoint(TrackPoint.Type.SEGMENT_END_MANUAL);
        tp.setTime(Instant.now().minus(2, ChronoUnit.MINUTES));

        assertFalse(tp.isRecent());
    }

    @Test
    public void distanceToPrevious() {
        Location l1 = new Location("test");
        l1.setLatitude(0);
        l1.setLongitude(0.0001);
        TrackPoint tp1 = new TrackPoint(l1);

        Location l2 = new Location("test");
        l2.setLatitude(0);
        l2.setLongitude(0.0002);
        TrackPoint tp2 = new TrackPoint(l2);

        // without sensor distance
        assertEquals(11.13, tp2.distanceToPrevious(tp1).toM(), 0.01);

        // tp1 has sensor distance
        tp1.setSensorDistance(Distance.of(5));
        tp1.setSensorDistance(null);
        assertEquals(11.13, tp2.distanceToPrevious(tp1).toM(), 0.01);

        // tp2 has sensor distance
        tp1.setSensorDistance(null);
        tp2.setSensorDistance(Distance.of(5));
        assertEquals(5, tp2.distanceToPrevious(tp1).toM(), 0.01);

        // tp1 and tp2 have sensor distance
        tp1.setSensorDistance(Distance.of(10));
        tp2.setSensorDistance(Distance.of(5));
        assertEquals(5, tp2.distanceToPrevious(tp1).toM(), 0.01);
    }
}