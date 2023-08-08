package de.dennisguse.opentracks.stats;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import de.dennisguse.opentracks.content.data.TestDataUtil;
import de.dennisguse.opentracks.data.models.Altitude;
import de.dennisguse.opentracks.data.models.Distance;
import de.dennisguse.opentracks.data.models.Speed;
import de.dennisguse.opentracks.data.models.Track;
import de.dennisguse.opentracks.data.models.TrackPoint;

@RunWith(AndroidJUnit4.class)
public class TrackStatisticsUpdaterTest {

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
        assertNull(statistics.getAverageHeartRate());
    }

    @Test
    public void startTime() {
        // given
        Instant startTime = Instant.parse("2021-10-24T23:00:00.000Z");
        TrackPoint tp = new TrackPoint(TrackPoint.Type.SEGMENT_START_MANUAL, startTime);

        // when
        TrackStatisticsUpdater subject = new TrackStatisticsUpdater();

        subject.addTrackPoint(tp);

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
        assertNull(statistics.getAverageHeartRate());
    }

    @Test
    public void addTrackPoint_TestingTrack() {
        // given
        TestDataUtil.TrackData data = TestDataUtil.createTestingTrack(new Track.Id(1));

        // when
        TrackStatisticsUpdater subject = new TrackStatisticsUpdater();
        data.trackPoints().forEach(subject::addTrackPoint);

        // then
        TrackStatistics statistics = subject.getTrackStatistics();
        assertEquals(142.26, statistics.getTotalDistance().toM(), 0.01);
        assertEquals(Duration.ofSeconds(12), statistics.getTotalTime());
        assertEquals(Duration.ofSeconds(12), statistics.getMovingTime());

        assertEquals(2.5, statistics.getMinAltitude(), 0.01);
        assertEquals(32.5, statistics.getMaxAltitude(), 0.01);
        assertEquals(36, statistics.getTotalAltitudeGain(), 0.01);
        assertEquals(36, statistics.getTotalAltitudeLoss(), 0.01);

        assertEquals(11.85, statistics.getMaxSpeed().toMPS(), 0.01);
        assertEquals(11.85, statistics.getAverageMovingSpeed().toMPS(), 0.01);
        assertEquals(11.85, statistics.getAverageSpeed().toMPS(), 0.01);
        assertEquals(106.834f, statistics.getAverageHeartRate().getBPM(), 0.01);
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
        subject.addTrackPoint(tp1);
        subject.addTrackPoint(tp2);
        subject.addTrackPoint(tp3);

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
        tp3.setSpeed(Speed.of(5f));
        TrackPoint tp4 = new TrackPoint(0.001, 0, Altitude.WGS84.of(5.0), Instant.ofEpochMilli(4000));
        tp4.setSpeed(Speed.of(5f));
        tp4.setSensorDistance(Distance.of(5f));
        TrackPoint tp5 = new TrackPoint(TrackPoint.Type.SEGMENT_END_MANUAL, Instant.ofEpochMilli(5000));
        tp5.setSensorDistance(Distance.of(10f));

        // when
        subject.addTrackPoint(tp1);
        subject.addTrackPoint(tp2);
        subject.addTrackPoint(tp3);

        // then
        assertEquals(110.57, subject.getTrackStatistics().getTotalDistance().toM(), 0.01);

        // when
        subject.addTrackPoint(tp4);
        subject.addTrackPoint(tp5);

        // then
        assertEquals(125.57, subject.getTrackStatistics().getTotalDistance().toM(), 0.01);
    }

    @Test
    public void addTrackPoint_distance_from_GPS_moving_and_sensor_disconnecting() {
        // given
        TrackStatisticsUpdater subject = new TrackStatisticsUpdater();

        TrackPoint tp1 = new TrackPoint(TrackPoint.Type.SEGMENT_START_MANUAL, Instant.ofEpochMilli(1000));
        TrackPoint tp2 = new TrackPoint(0, 0, Altitude.WGS84.of(5.0), Instant.ofEpochMilli(2000));
        tp2.setSpeed(Speed.of(5f));
        TrackPoint tp3 = new TrackPoint(0.00001, 0, Altitude.WGS84.of(5.0), Instant.ofEpochMilli(3000));
        tp3.setSensorDistance(Distance.of(5f));
        tp3.setSpeed(Speed.of(5f));
        TrackPoint tp4 = new TrackPoint(0.0005, 0, Altitude.WGS84.of(5.0), Instant.ofEpochMilli(4000));
        tp4.setSpeed(Speed.of(5f));
        TrackPoint tp5 = new TrackPoint(TrackPoint.Type.SEGMENT_END_MANUAL, Instant.ofEpochMilli(5000));

        // when
        subject.addTrackPoint(tp1);
        subject.addTrackPoint(tp2);
        subject.addTrackPoint(tp3);

        // then
        assertEquals(5, subject.getTrackStatistics().getTotalDistance().toM(), 0.01);

        // when
        subject.addTrackPoint(tp4);
        subject.addTrackPoint(tp5);

        // then
        assertEquals(59.18, subject.getTrackStatistics().getTotalDistance().toM(), 0.01);
    }

    @Test
    public void addTrackPoint_maxSpeed_multiple_segments() {
        TrackStatisticsUpdater subject = new TrackStatisticsUpdater();
        assertEquals(Speed.of(0f), subject.getTrackStatistics().getMaxSpeed());

        subject.addTrackPoints(List.of(
                new TrackPoint(TrackPoint.Type.SEGMENT_START_MANUAL, Instant.ofEpochSecond(0)),
                new TrackPoint(0, 0, Altitude.WGS84.of(0), Instant.ofEpochSecond(1))
                        .setSpeed(Speed.of(2f)),
                new TrackPoint(0, 0, Altitude.WGS84.of(0), Instant.ofEpochSecond(2))
                        .setSpeed(Speed.of(2f)),
                new TrackPoint(TrackPoint.Type.SEGMENT_END_MANUAL, Instant.ofEpochSecond(4))
        ));
        assertEquals(Speed.of(2f), subject.getTrackStatistics().getMaxSpeed());

        // when
        subject.addTrackPoints(List.of(
                new TrackPoint(TrackPoint.Type.SEGMENT_START_MANUAL, Instant.ofEpochSecond(5)),
                new TrackPoint(0, 0, Altitude.WGS84.of(0), Instant.ofEpochSecond(6))
                        .setSpeed(Speed.of(1f)),
                new TrackPoint(0, 0, Altitude.WGS84.of(0), Instant.ofEpochSecond(7))
                        .setSpeed(Speed.of(1f)),
                new TrackPoint(TrackPoint.Type.SEGMENT_END_MANUAL, Instant.ofEpochSecond(8))
        ));

        // then
        assertEquals(Speed.of(2f), subject.getTrackStatistics().getMaxSpeed());
    }

    @Test
    public void addTrackPoint_idle_withoutDistance() {
        TrackStatisticsUpdater subject = new TrackStatisticsUpdater();

        // when
        subject.addTrackPoints(List.of(
                new TrackPoint(TrackPoint.Type.SEGMENT_START_MANUAL, Instant.ofEpochSecond(0)),
                new TrackPoint(0, 0, Altitude.WGS84.of(0), Instant.ofEpochSecond(1))
                        .setSpeed(Speed.of(2f)),
                new TrackPoint(0, 0, Altitude.WGS84.of(0), Instant.ofEpochSecond(2))
                        .setSpeed(Speed.of(2f)),

                new TrackPoint(TrackPoint.Type.IDLE, Instant.ofEpochSecond(30)),
                new TrackPoint(TrackPoint.Type.TRACKPOINT, Instant.ofEpochSecond(40))
                        .setHeartRate(50),
                new TrackPoint(TrackPoint.Type.TRACKPOINT, Instant.ofEpochSecond(45))
                        .setHeartRate(50),
                new TrackPoint(0, 1, Altitude.WGS84.of(0), Instant.ofEpochSecond(50)),
                new TrackPoint(0, 2, Altitude.WGS84.of(0), Instant.ofEpochSecond(55)),

                new TrackPoint(TrackPoint.Type.SEGMENT_END_MANUAL, Instant.ofEpochSecond(60))
        ));

        // then
        assertEquals(Duration.ofSeconds(40), subject.getTrackStatistics().getMovingTime());
    }

    @Test
    public void addTrackPoint_idle_withDistance() {
        TrackStatisticsUpdater subject = new TrackStatisticsUpdater();

        // when
        subject.addTrackPoints(List.of(
                new TrackPoint(TrackPoint.Type.SEGMENT_START_MANUAL, Instant.ofEpochSecond(0)),
                new TrackPoint(0, 0, Altitude.WGS84.of(0), Instant.ofEpochSecond(1))
                        .setSensorDistance(Distance.of(10)),
                new TrackPoint(0, 0, Altitude.WGS84.of(0), Instant.ofEpochSecond(2))
                        .setSensorDistance(Distance.of(10)),

                new TrackPoint(TrackPoint.Type.IDLE, Instant.ofEpochSecond(30))
                        .setSensorDistance(Distance.ofKilometer(1)),
                new TrackPoint(TrackPoint.Type.TRACKPOINT, Instant.ofEpochSecond(40))
                        .setHeartRate(50),
                new TrackPoint(TrackPoint.Type.TRACKPOINT, Instant.ofEpochSecond(45))
                        .setHeartRate(50),
                new TrackPoint(0, 0, Altitude.WGS84.of(0), Instant.ofEpochSecond(50))
                        .setSensorDistance(Distance.of(10)),
                new TrackPoint(0, 0, Altitude.WGS84.of(0), Instant.ofEpochSecond(55))
                        .setSensorDistance(Distance.of(10)),

                new TrackPoint(TrackPoint.Type.SEGMENT_END_MANUAL, Instant.ofEpochSecond(60))
        ));

        // then
        assertEquals(Duration.ofSeconds(45), subject.getTrackStatistics().getMovingTime());
        assertEquals(Distance.of(1040), subject.getTrackStatistics().getTotalDistance());
    }

    @Test
    public void copy_constructor() {
        // given
        TrackStatisticsUpdater subject = new TrackStatisticsUpdater();

        TrackPoint tp1 = new TrackPoint(TrackPoint.Type.SEGMENT_START_MANUAL, Instant.ofEpochMilli(1000));
        TrackPoint tp2 = new TrackPoint(0, 0, Altitude.WGS84.of(5.0), Instant.ofEpochMilli(2000));
        tp2.setSpeed(Speed.of(5f));
        TrackPoint tp3 = new TrackPoint(0.00001, 0, Altitude.WGS84.of(5.0), Instant.ofEpochMilli(3000));
        tp3.setSpeed(Speed.of(5f));
        TrackPoint tp4 = new TrackPoint(0.0005, 0, Altitude.WGS84.of(5.0), Instant.ofEpochMilli(4000));
        tp4.setSpeed(Speed.of(5f));
        TrackPoint tp5 = new TrackPoint(TrackPoint.Type.SEGMENT_END_MANUAL, Instant.ofEpochMilli(5000));

        subject.addTrackPoint(tp1);
        subject.addTrackPoint(tp2);
        subject.addTrackPoint(tp3);
        subject.addTrackPoint(tp4);

        // when
        TrackStatisticsUpdater copy = new TrackStatisticsUpdater(subject);
        subject.addTrackPoint(tp5);
        copy.addTrackPoint(tp5);

        // then
        assertEquals(55.287, subject.getTrackStatistics().getTotalDistance().toM(), 0.01);
        assertEquals(55.287, copy.getTrackStatistics().getTotalDistance().toM(), 0.01);
    }
}