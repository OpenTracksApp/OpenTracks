package de.dennisguse.opentracks.chart;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Duration;

import de.dennisguse.opentracks.data.models.Altitude;
import de.dennisguse.opentracks.data.models.Distance;
import de.dennisguse.opentracks.data.models.Speed;
import de.dennisguse.opentracks.data.models.TrackPoint;
import de.dennisguse.opentracks.settings.UnitSystem;
import de.dennisguse.opentracks.stats.TrackStatistics;

@RunWith(AndroidJUnit4.class)
public class ChartPointTest {

    @Test
    public void create_by_time() {
        // given
        TrackStatistics statistics = new TrackStatistics();
        statistics.setTotalTime(Duration.ofSeconds(1000));

        // when
        ChartPoint point = new ChartPoint(statistics, TrackStubUtils.createDefaultTrackPoint(), Speed.of(0), Altitude.EGM2008.of(0), false, UnitSystem.IMPERIAL);

        // then
        assertEquals(1000000, (long) point.getTimeOrDistance());
    }

    @Test
    public void create_by_distance() {
        // given
        TrackStatistics statistics = new TrackStatistics();
        statistics.setTotalDistance(Distance.of(1000));

        // when
        ChartPoint point = new ChartPoint(statistics, TrackStubUtils.createDefaultTrackPoint(), Speed.of(0), Altitude.EGM2008.of(0), true, UnitSystem.METRIC);

        // then
        assertEquals(1, (long) point.getTimeOrDistance());
    }

    @Test
    public void create_get_altitude_speed_and_pace() {
        // given
        TrackStatistics statistics = new TrackStatistics();

        // when
        ChartPoint point = new ChartPoint(statistics, TrackStubUtils.createDefaultTrackPoint(), Speed.of(10), Altitude.EGM2008.of(50), false, UnitSystem.METRIC);

        // then
        assertEquals(50, point.getAltitude(), 0.01);
        assertEquals(36, point.getSpeed(), 0.01);
        assertEquals(1.66, point.getPace(), 0.01);
    }

    @Test
    public void create_sensorNotAvailable() {
        // given
        TrackStatistics statistics = new TrackStatistics();

        // when
        ChartPoint point = new ChartPoint(statistics, TrackStubUtils.createDefaultTrackPoint(), Speed.of(10), Altitude.EGM2008.of(50), false, UnitSystem.METRIC);

        // then
        assertNull(point.getHeartRate());
        assertNull(point.getCadence());
        assertNull(point.getPower());
    }

    @Test
    public void create_sensorAvailable() {
        // given
        TrackPoint trackPoint = TrackStubUtils.createDefaultTrackPoint();
        trackPoint.setHeartRate(100f);
        trackPoint.setCadence(101f);
        trackPoint.setPower(102f);

        TrackStatistics statistics = new TrackStatistics();

        // when
        ChartPoint point = new ChartPoint(statistics, trackPoint, Speed.of(10), Altitude.EGM2008.of(50), false, UnitSystem.METRIC);

        // then
        assertEquals(100.0, point.getHeartRate(), 0.01);
        assertEquals(101.0, point.getCadence(), 0.01);
        assertEquals(102.0, point.getPower(), 0.01);
    }
}