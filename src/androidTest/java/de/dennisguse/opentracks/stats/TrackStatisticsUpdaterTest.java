package de.dennisguse.opentracks.stats;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Duration;
import java.time.Instant;

import de.dennisguse.opentracks.content.data.Altitude;
import de.dennisguse.opentracks.content.data.Distance;
import de.dennisguse.opentracks.content.data.Speed;
import de.dennisguse.opentracks.content.data.TestDataUtil;
import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.content.data.TrackPoint;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@RunWith(AndroidJUnit4.class)
public class TrackStatisticsUpdaterTest {

    private static final Distance GPS_DISTANCE = Distance.of(50);

    @Test
    public void empty() {
        // when
        TrackStatisticsUpdater subject = new TrackStatisticsUpdater();

        // then
        TrackStatistics statistics = subject.getTrackStatistics();
        assertNull(statistics.getStartTime());
        assertNull(statistics.getStopTime());
        assertEquals(Duration.ZERO, statistics.getTotalTime());
        assertEquals(Duration.ZERO, statistics.getMovingTime());

        assertEquals(Speed.of(0), statistics.getAverageSpeed());
        assertEquals(Speed.of(0), statistics.getAverageMovingSpeed());
        assertEquals(Speed.of(0), statistics.getMaxSpeed());

        assertNull(statistics.getTotalAltitudeGain());
        assertNull(statistics.getTotalAltitudeLoss());
    }

    @Test
    public void startTime() {
        // given
        Instant startTime = Instant.parse("2021-10-24T23:00:00.000Z");
        TrackPoint tp = new TrackPoint(TrackPoint.Type.SEGMENT_START_MANUAL, startTime);

        // when
        TrackStatisticsUpdater subject = new TrackStatisticsUpdater();

        subject.addTrackPoint(tp, GPS_DISTANCE);

        // then
        TrackStatistics statistics = subject.getTrackStatistics();
        assertEquals(startTime, statistics.getStartTime());
        assertEquals(startTime, statistics.getStopTime());
        assertEquals(Duration.ZERO, statistics.getTotalTime());
        assertEquals(Duration.ZERO, statistics.getMovingTime());

        assertEquals(Speed.of(0), statistics.getAverageSpeed());
        assertEquals(Speed.of(0), statistics.getAverageMovingSpeed());
        assertEquals(Speed.of(0), statistics.getMaxSpeed());

        assertNull(statistics.getTotalAltitudeGain());
        assertNull(statistics.getTotalAltitudeLoss());
    }

    @Test
    public void addTrackPoint_TestingTrack() {
        // given
        TestDataUtil.TrackData data = TestDataUtil.createTestingTrack(new Track.Id(1));

        // when
        TrackStatisticsUpdater subject = new TrackStatisticsUpdater();
        data.trackPoints.forEach(it -> subject.addTrackPoint(it, GPS_DISTANCE));

        // then
        TrackStatistics statistics = subject.getTrackStatistics();
        assertEquals(99.58, statistics.getTotalDistance().toM(), 0.01);
        assertEquals(Duration.ofSeconds(14), statistics.getTotalTime());
        assertEquals(Duration.ofSeconds(7), statistics.getMovingTime());

        assertEquals(2.5, statistics.getMinAltitude(), 0.01);
        assertEquals(27.5, statistics.getMaxAltitude(), 0.01);
        assertEquals(36, statistics.getTotalAltitudeGain(), 0.01);
        assertEquals(36, statistics.getTotalAltitudeLoss(), 0.01);

        assertEquals(14.226, statistics.getMaxSpeed().toMPS(), 0.01);
        assertEquals(14.226, statistics.getAverageMovingSpeed().toMPS(), 0.01);
        assertEquals(7.11, statistics.getAverageSpeed().toMPS(), 0.01);
    }

    @Test
    public void addTrackPoint_distance_from_GPS_not_moving() {
        // given
        TrackStatisticsUpdater subject = new TrackStatisticsUpdater();

        TrackPoint tp1 = new TrackPoint(TrackPoint.Type.SEGMENT_START_MANUAL, Instant.ofEpochMilli(1000));
        TrackPoint tp2 = new TrackPoint(0, 0, Altitude.WGS84.of(5.0), Instant.ofEpochMilli(2000));
        TrackPoint tp3 = new TrackPoint(0.00001, 0, Altitude.WGS84.of(5.0), Instant.ofEpochMilli(3000));

        // when
        subject.addTrackPoint(tp1, GPS_DISTANCE);
        subject.addTrackPoint(tp2, GPS_DISTANCE);
        subject.addTrackPoint(tp3, GPS_DISTANCE);

        // then
        assertEquals(0, subject.getTrackStatistics().getTotalDistance().toM(), 0.01);
    }

    @Test
    public void addTrackPoint_distance_from_GPS_moving() {
        // given
        TrackStatisticsUpdater subject = new TrackStatisticsUpdater();

        TrackPoint tp1 = new TrackPoint(TrackPoint.Type.SEGMENT_START_MANUAL, Instant.ofEpochMilli(1000));
        TrackPoint tp2 = new TrackPoint(0, 0, Altitude.WGS84.of(5.0), Instant.ofEpochMilli(2000));
        TrackPoint tp3 = new TrackPoint(0.00001, 0, Altitude.WGS84.of(5.0), Instant.ofEpochMilli(3000));
        tp3.setSpeed(Speed.of(5f));

        // when
        subject.addTrackPoint(tp1, GPS_DISTANCE);
        subject.addTrackPoint(tp2, GPS_DISTANCE);
        subject.addTrackPoint(tp3, GPS_DISTANCE);

        // then
        assertEquals(1.10, subject.getTrackStatistics().getTotalDistance().toM(), 0.01);
    }

    @Test
    public void addTrackPoint_distance_from_GPS_moving_and_sensor_moving() {
        // given
        TrackStatisticsUpdater subject = new TrackStatisticsUpdater();

        TrackPoint tp1 = new TrackPoint(TrackPoint.Type.SEGMENT_START_MANUAL, Instant.ofEpochMilli(1000));
        TrackPoint tp2 = new TrackPoint(0, 0, Altitude.WGS84.of(5.0), Instant.ofEpochMilli(2000));
        tp2.setSpeed(Speed.of(5f));
        TrackPoint tp3 = new TrackPoint(0.001, 0, Altitude.WGS84.of(5.0), Instant.ofEpochMilli(3000));
        tp2.setSpeed(Speed.of(5f));
        TrackPoint tp4 = new TrackPoint(0.001, 0, Altitude.WGS84.of(5.0), Instant.ofEpochMilli(4000));
        tp2.setSpeed(Speed.of(5f));
        tp4.setSensorDistance(Distance.of(5f));
        TrackPoint tp5 = new TrackPoint(TrackPoint.Type.SEGMENT_END_MANUAL, Instant.ofEpochMilli(5000));
        tp5.setSensorDistance(Distance.of(10f));

        // when
        subject.addTrackPoint(tp1, GPS_DISTANCE);
        subject.addTrackPoint(tp2, GPS_DISTANCE);
        subject.addTrackPoint(tp3, GPS_DISTANCE);

        // then
        assertEquals(110.57, subject.getTrackStatistics().getTotalDistance().toM(), 0.01);

        // when
        subject.addTrackPoint(tp4, GPS_DISTANCE);
        subject.addTrackPoint(tp5, GPS_DISTANCE);

        // then
        assertEquals(125.57, subject.getTrackStatistics().getTotalDistance().toM(), 0.01);
    }

    @Test
    public void addTrackPoint_distance_from_GPS_not_moving_and_sensor_moving() {
        // given
        TrackStatisticsUpdater subject = new TrackStatisticsUpdater();

        TrackPoint tp1 = new TrackPoint(TrackPoint.Type.SEGMENT_START_MANUAL, Instant.ofEpochMilli(1000));
        TrackPoint tp2 = new TrackPoint(0, 0, Altitude.WGS84.of(5.0), Instant.ofEpochMilli(2000));
        TrackPoint tp3 = new TrackPoint(0.00001, 0, Altitude.WGS84.of(5.0), Instant.ofEpochMilli(3000));
        TrackPoint tp4 = new TrackPoint(0.00001, 0, Altitude.WGS84.of(5.0), Instant.ofEpochMilli(4000));
        tp4.setSensorDistance(Distance.of(5f));
        TrackPoint tp5 = new TrackPoint(TrackPoint.Type.SEGMENT_END_MANUAL, Instant.ofEpochMilli(5000));
        tp5.setSensorDistance(Distance.of(10f));

        // when
        subject.addTrackPoint(tp1, GPS_DISTANCE);
        subject.addTrackPoint(tp2, GPS_DISTANCE);
        subject.addTrackPoint(tp3, GPS_DISTANCE);

        // then
        assertEquals(0, subject.getTrackStatistics().getTotalDistance().toM(), 0.01);

        // when
        subject.addTrackPoint(tp4, GPS_DISTANCE);
        subject.addTrackPoint(tp5, GPS_DISTANCE);

        // then
        assertEquals(15, subject.getTrackStatistics().getTotalDistance().toM(), 0.01);
    }

    @Test
    public void addTrackPoint_distance_from_GPS_moving_and_sensor_disconnecting() {
        // given
        TrackStatisticsUpdater subject = new TrackStatisticsUpdater();

        TrackPoint tp1 = new TrackPoint(TrackPoint.Type.SEGMENT_START_MANUAL, Instant.ofEpochMilli(1000));
        TrackPoint tp2 = new TrackPoint(0, 0, Altitude.WGS84.of(5.0), Instant.ofEpochMilli(2000));
        TrackPoint tp3 = new TrackPoint(0.00001, 0, Altitude.WGS84.of(5.0), Instant.ofEpochMilli(3000));
        tp3.setSensorDistance(Distance.of(5f));
        TrackPoint tp4 = new TrackPoint(0.0005, 0, Altitude.WGS84.of(5.0), Instant.ofEpochMilli(4000));
        TrackPoint tp5 = new TrackPoint(TrackPoint.Type.SEGMENT_END_MANUAL, Instant.ofEpochMilli(5000));

        // when
        subject.addTrackPoint(tp1, GPS_DISTANCE);
        subject.addTrackPoint(tp2, GPS_DISTANCE);
        subject.addTrackPoint(tp3, GPS_DISTANCE);

        // then
        assertEquals(5, subject.getTrackStatistics().getTotalDistance().toM(), 0.01);

        // when
        subject.addTrackPoint(tp4, GPS_DISTANCE);
        subject.addTrackPoint(tp5, GPS_DISTANCE);

        // then
        assertEquals(59.18, subject.getTrackStatistics().getTotalDistance().toM(), 0.01);
    }

    @Ignore("TODO: create a concept ont to compute speed from GPS and sensor")
    @Test
    public void addTrackPoint_speed_from_GPS_not_moving() {
    }

    @Ignore("TODO: create a concept ont to compute speed from GPS and sensor")
    @Test
    public void addTrackPoint_speed_from_GPS_moving() {
    }

    @Ignore("TODO: create a concept ont to compute speed from GPS and sensor")
    @Test
    public void addTrackPoint_speed_from_GPS_not_moving_and_sensor_speed() {
    }

    @Ignore("TODO: create a concept ont to compute speed from GPS and sensor")
    @Test
    public void addTrackPoint_speed_from_GPS_moving_and_sensor_speed() {
    }

    @Test
    public void copy_constructor() {
        // given
        TrackStatisticsUpdater subject = new TrackStatisticsUpdater();

        TrackPoint tp1 = new TrackPoint(TrackPoint.Type.SEGMENT_START_MANUAL, Instant.ofEpochMilli(1000));
        TrackPoint tp2 = new TrackPoint(0, 0, Altitude.WGS84.of(5.0), Instant.ofEpochMilli(2000));
        TrackPoint tp3 = new TrackPoint(0.00001, 0, Altitude.WGS84.of(5.0), Instant.ofEpochMilli(3000));
        TrackPoint tp4 = new TrackPoint(0.0005, 0, Altitude.WGS84.of(5.0), Instant.ofEpochMilli(4000));
        TrackPoint tp5 = new TrackPoint(TrackPoint.Type.SEGMENT_END_MANUAL, Instant.ofEpochMilli(5000));

        subject.addTrackPoint(tp1, GPS_DISTANCE);
        subject.addTrackPoint(tp2, GPS_DISTANCE);
        subject.addTrackPoint(tp3, GPS_DISTANCE);
        subject.addTrackPoint(tp4, GPS_DISTANCE);

        // when
        TrackStatisticsUpdater copy = new TrackStatisticsUpdater(subject);
        subject.addTrackPoint(tp5, GPS_DISTANCE);
        copy.addTrackPoint(tp5, GPS_DISTANCE);


        // then
        assertEquals(55.287, subject.getTrackStatistics().getTotalDistance().toM(), 0.01);
        assertEquals(55.287, copy.getTrackStatistics().getTotalDistance().toM(), 0.01);
    }
}