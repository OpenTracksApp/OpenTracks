package de.dennisguse.opentracks.util;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Duration;
import java.util.List;

import de.dennisguse.opentracks.content.data.Distance;
import de.dennisguse.opentracks.content.data.Speed;
import de.dennisguse.opentracks.content.data.TestDataUtil;
import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.content.data.TrackPoint;
import de.dennisguse.opentracks.stats.TrackStatistics;
import de.dennisguse.opentracks.viewmodels.IntervalStatistics;

import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class AnnouncementUtilsTest {

    private final Context context = ApplicationProvider.getApplicationContext();

    @Test
    public void getAnnouncement_metric_speed() {
        TrackStatistics stats = new TrackStatistics();
        stats.setTotalDistance(Distance.of(20000));
        stats.setTotalTime(Duration.ofHours(2).plusMinutes(5).plusSeconds(10));
        stats.setMovingTime(Duration.ofHours(1).plusMinutes(5).plusSeconds(10));
        stats.setMaxSpeed(Speed.of(100));
        stats.setTotalAltitudeGain(6000f);

        // when
        String announcement = AnnouncementUtils.getAnnouncement(context, stats, true, true, null);

        // then
        assertEquals("OpenTracks total distance 20.00 kilometers in 1 hour 5 minutes 10 seconds at 18.4 kilometers per hour", announcement);
    }

    @Test
    public void getAnnouncement_withInterval_metric_speed() {
        TrackStatistics stats = new TrackStatistics();
        stats.setTotalDistance(Distance.of(20000));
        stats.setTotalTime(Duration.ofHours(2).plusMinutes(5).plusSeconds(10));
        stats.setMovingTime(Duration.ofHours(1).plusMinutes(5).plusSeconds(10));
        stats.setMaxSpeed(Speed.of(100));
        stats.setTotalAltitudeGain(6000f);

        List<TrackPoint> trackPoints = TestDataUtil.createTrack(new Track.Id(System.currentTimeMillis()), 10).second;
        IntervalStatistics intervalStatistics = new IntervalStatistics(trackPoints, Distance.of(1000), Distance.of(0));
        IntervalStatistics.Interval lastInterval = intervalStatistics.getIntervalList().get(intervalStatistics.getIntervalList().size() - 1);

        // when
        String announcement = AnnouncementUtils.getAnnouncement(context, stats, true, true, lastInterval);

        // then
        assertEquals("OpenTracks total distance 20.00 kilometers in 1 hour 5 minutes 10 seconds at 18.4 kilometers per hour Lap speed of 51.2 kilometers per hour", announcement);
    }

    @Test
    public void getAnnouncement_metric_pace() {
        TrackStatistics stats = new TrackStatistics();
        stats.setTotalDistance(Distance.of(20000));
        stats.setTotalTime(Duration.ofHours(2).plusMinutes(5).plusSeconds(10));
        stats.setMovingTime(Duration.ofHours(1).plusMinutes(5).plusSeconds(10));
        stats.setMaxSpeed(Speed.of(100));
        stats.setTotalAltitudeGain(6000f);

        // when
        String announcement = AnnouncementUtils.getAnnouncement(context, stats, true, false, null);

        // then
        assertEquals("OpenTracks total distance 20.00 kilometers in 1 hour 5 minutes 10 seconds at 3 minutes 15 seconds per kilometer", announcement);
    }

    @Test
    public void getAnnouncement_withInterval_metric_pace() {
        TrackStatistics stats = new TrackStatistics();
        stats.setTotalDistance(Distance.of(20000));
        stats.setTotalTime(Duration.ofHours(2).plusMinutes(5).plusSeconds(10));
        stats.setMovingTime(Duration.ofHours(1).plusMinutes(5).plusSeconds(10));
        stats.setMaxSpeed(Speed.of(100));
        stats.setTotalAltitudeGain(6000f);

        List<TrackPoint> trackPoints = TestDataUtil.createTrack(new Track.Id(System.currentTimeMillis()), 10).second;
        IntervalStatistics intervalStatistics = new IntervalStatistics(trackPoints, Distance.of(1000));
        IntervalStatistics.Interval lastInterval = intervalStatistics.getIntervalList().get(intervalStatistics.getIntervalList().size() - 1);

        // when
        String announcement = AnnouncementUtils.getAnnouncement(context, stats, true, false, lastInterval);

        // then
        assertEquals("OpenTracks total distance 20.00 kilometers in 1 hour 5 minutes 10 seconds at 3 minutes 15 seconds per kilometer Lap time of 1 minute 10 seconds per kilometer", announcement);
    }

    @Test
    public void getAnnouncement_imperial_speed() {
        TrackStatistics stats = new TrackStatistics();
        stats.setTotalDistance(Distance.of(20000));
        stats.setTotalTime(Duration.ofHours(2).plusMinutes(5).plusSeconds(10));
        stats.setMovingTime(Duration.ofHours(1).plusMinutes(5).plusSeconds(10));
        stats.setMaxSpeed(Speed.of(100));
        stats.setTotalAltitudeGain(6000f);

        // when
        String announcement = AnnouncementUtils.getAnnouncement(context, stats, false, true, null);

        // then
        assertEquals("OpenTracks total distance 12.43 miles in 1 hour 5 minutes 10 seconds at 11.4 miles per hour", announcement);
    }

    @Test
    public void getAnnouncement_withInterval_imperial_speed() {
        TrackStatistics stats = new TrackStatistics();
        stats.setTotalDistance(Distance.of(20000));
        stats.setTotalTime(Duration.ofHours(2).plusMinutes(5).plusSeconds(10));
        stats.setMovingTime(Duration.ofHours(1).plusMinutes(5).plusSeconds(10));
        stats.setMaxSpeed(Speed.of(100));
        stats.setTotalAltitudeGain(6000f);

        List<TrackPoint> trackPoints = TestDataUtil.createTrack(new Track.Id(System.currentTimeMillis()), 10).second;
        IntervalStatistics intervalStatistics = new IntervalStatistics(trackPoints, Distance.of(1000));
        IntervalStatistics.Interval lastInterval = intervalStatistics.getIntervalList().get(intervalStatistics.getIntervalList().size() - 1);

        // when
        String announcement = AnnouncementUtils.getAnnouncement(context, stats, false, true, lastInterval);

        // then
        assertEquals("OpenTracks total distance 12.43 miles in 1 hour 5 minutes 10 seconds at 11.4 miles per hour Lap speed of 31.8 miles per hour", announcement);
    }

    @Test
    public void getAnnouncement_imperial_pace() {
        TrackStatistics stats = new TrackStatistics();
        stats.setTotalDistance(Distance.of(20000));
        stats.setTotalTime(Duration.ofHours(2).plusMinutes(5).plusSeconds(10));
        stats.setMovingTime(Duration.ofHours(1).plusMinutes(5).plusSeconds(10));
        stats.setMaxSpeed(Speed.of(100));
        stats.setTotalAltitudeGain(6000f);

        // when
        String announcement = AnnouncementUtils.getAnnouncement(context, stats, false, false, null);

        // then
        assertEquals("OpenTracks total distance 12.43 miles in 1 hour 5 minutes 10 seconds at 5 minutes 15 seconds per mile", announcement);
    }

    @Test
    public void getAnnouncement_withInterval_imperial_pace() {
        TrackStatistics stats = new TrackStatistics();
        stats.setTotalDistance(Distance.of(20000));
        stats.setTotalTime(Duration.ofHours(2).plusMinutes(5).plusSeconds(10));
        stats.setMovingTime(Duration.ofHours(1).plusMinutes(5).plusSeconds(10));
        stats.setMaxSpeed(Speed.of(100));
        stats.setTotalAltitudeGain(6000f);

        List<TrackPoint> trackPoints = TestDataUtil.createTrack(new Track.Id(System.currentTimeMillis()), 10).second;
        IntervalStatistics intervalStatistics = new IntervalStatistics(trackPoints, Distance.of(1000));
        IntervalStatistics.Interval lastInterval = intervalStatistics.getIntervalList().get(intervalStatistics.getIntervalList().size() - 1);

        // when
        String announcement = AnnouncementUtils.getAnnouncement(context, stats, false, false, lastInterval);

        // then
        assertEquals("OpenTracks total distance 12.43 miles in 1 hour 5 minutes 10 seconds at 5 minutes 15 seconds per mile Lap time of 1 minute 53 seconds per mile", announcement);
    }
}