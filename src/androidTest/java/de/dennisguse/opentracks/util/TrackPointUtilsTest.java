package de.dennisguse.opentracks.util;

import org.junit.Test;

import de.dennisguse.opentracks.content.data.TrackPoint;

import static org.junit.Assert.assertEquals;

public class TrackPointUtilsTest {

    @Test
    public void fixTime_none() {
        // given
        long time = System.currentTimeMillis();
        TrackPoint trackPoint = new TrackPoint();
        trackPoint.setTime(time);

        // when
        TrackPointUtils.fixTime(trackPoint);

        // then
        assertEquals(time, trackPoint.getTime());
    }

    @Test
    public void fixTime_0() {
        // given
        long time = System.currentTimeMillis();
        TrackPoint trackPoint = new TrackPoint();
        trackPoint.setTime(0);

        // when
        TrackPointUtils.fixTime(trackPoint);

        // then
        assertEquals(time, trackPoint.getTime(), 1000);
    }

    @Test
    public void fixTime_gpsWeekRollover() {
        // given
        long time = System.currentTimeMillis();
        TrackPoint trackPoint = new TrackPoint();
        trackPoint.setTime(time - 1024 * UnitConversions.ONE_WEEK_MS);

        // when
        TrackPointUtils.fixTime(trackPoint);

        // then
        assertEquals(time, trackPoint.getTime());
    }

}