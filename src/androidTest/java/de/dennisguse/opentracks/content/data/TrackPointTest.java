package de.dennisguse.opentracks.content.data;

import org.junit.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

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
}