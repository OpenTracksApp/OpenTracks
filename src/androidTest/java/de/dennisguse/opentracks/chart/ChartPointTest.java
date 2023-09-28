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
        ChartPoint point = ChartPoint.create(statistics, TrackStubUtils.createDefaultTrackPoint(), Speed.of(0), false, UnitSystem.IMPERIAL_FEET);

        // then
        assertEquals(1000000, (long) point.timeOrDistance());
    }

    @Test
    public void create_by_distance() {
        // given
        TrackStatistics statistics = new TrackStatistics();
        statistics.setTotalDistance(Distance.of(1000));

        // when
        ChartPoint point = ChartPoint.create(statistics, TrackStubUtils.createDefaultTrackPoint(), Speed.of(0), true, UnitSystem.METRIC);

        // then
        assertEquals(1, (long) point.timeOrDistance());
    }

    @Test
    public void create_get_altitude_speed_and_pace() {
        // given
        TrackStatistics statistics = new TrackStatistics();
        TrackPoint trackPoint = TrackStubUtils.createDefaultTrackPoint()
                .setAltitude(Altitude.EGM2008.of(50));

        // when
        ChartPoint point = ChartPoint.create(statistics, trackPoint, Speed.of(10), false, UnitSystem.METRIC);

        // then
        assertEquals(50, point.altitude(), 0.01);
        assertEquals(36, point.speed(), 0.01);
        assertEquals(1.66, point.pace(), 0.01);
    }

    @Test
    public void create_sensorNotAvailable() {
        // given
        TrackStatistics statistics = new TrackStatistics();
        TrackPoint trackPoint = TrackStubUtils.createDefaultTrackPoint()
                .setAltitude(Altitude.EGM2008.of(50));
        // when
        ChartPoint point = ChartPoint.create(statistics, trackPoint, Speed.of(10), false, UnitSystem.METRIC);

        // then
        assertNull(point.heartRate());
        assertNull(point.cadence());
        assertNull(point.power());
    }

    @Test
    public void create_sensorAvailable() {
        // given
        TrackPoint trackPoint = TrackStubUtils.createDefaultTrackPoint()
                .setAltitude(Altitude.EGM2008.of(50))
                .setHeartRate(100f)
                .setCadence(101f)
                .setPower(102f);

        TrackStatistics statistics = new TrackStatistics();

        // when
        ChartPoint point = ChartPoint.create(statistics, trackPoint, Speed.of(10), false, UnitSystem.METRIC);

        // then
        assertEquals(100.0, point.heartRate(), 0.01);
        assertEquals(101.0, point.cadence(), 0.01);
        assertEquals(102.0, point.power(), 0.01);
    }
}