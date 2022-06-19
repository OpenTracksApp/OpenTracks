package de.dennisguse.opentracks.services.announcement;

import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.util.Pair;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Duration;
import java.util.Locale;

import de.dennisguse.opentracks.LocaleRule;
import de.dennisguse.opentracks.content.data.TestDataUtil;
import de.dennisguse.opentracks.data.ContentProviderUtils;
import de.dennisguse.opentracks.data.TrackPointIterator;
import de.dennisguse.opentracks.data.models.Distance;
import de.dennisguse.opentracks.data.models.HeartRate;
import de.dennisguse.opentracks.data.models.Speed;
import de.dennisguse.opentracks.data.models.Track;
import de.dennisguse.opentracks.settings.PreferencesUtils;
import de.dennisguse.opentracks.settings.UnitSystem;
import de.dennisguse.opentracks.stats.SensorStatistics;
import de.dennisguse.opentracks.stats.TrackStatistics;
import de.dennisguse.opentracks.ui.intervals.IntervalStatistics;

@RunWith(AndroidJUnit4.class)
public class VoiceAnnouncementUtilsTest {

    @Rule
    public final LocaleRule mLocaleRule = new LocaleRule(Locale.ENGLISH);

    private final Context context = ApplicationProvider.getApplicationContext();
    private ContentProviderUtils contentProviderUtils;

    @Before
    public void setUp() {
        contentProviderUtils = new ContentProviderUtils(context);

        PreferencesUtils.setVoiceAnnounceLapHeartRate(false);
        PreferencesUtils.setVoiceAnnounceAverageHeartRate(false);
        PreferencesUtils.setVoiceAnnounceTotalDistance(true);
        PreferencesUtils.setVoiceAnnounceMovingTime(true);
        PreferencesUtils.setVoiceAnnounceAverageSpeedPace(true);
        PreferencesUtils.setVoiceAnnounceLapSpeedPace(true);
    }

    @Test
    public void getAnnouncement_metric_speed() {
        TrackStatistics stats = new TrackStatistics();
        stats.setTotalDistance(Distance.of(20000));
        stats.setTotalTime(Duration.ofHours(2).plusMinutes(5).plusSeconds(10));
        stats.setMovingTime(Duration.ofHours(1).plusMinutes(5).plusSeconds(10));
        stats.setMaxSpeed(Speed.of(100));
        stats.setTotalAltitudeGain(6000f);

        // when
        String announcement = VoiceAnnouncementUtils.getAnnouncement(context, stats, UnitSystem.METRIC, true, null, null).toString();

        // then
        assertEquals("Total distance 20.00 kilometers. 1 hour 5 minutes 10 seconds. Speed 18.4 kilometers per hour.", announcement);
    }

    @Test
    public void getAnnouncement_metric_speed_rounding_check() {
        TrackStatistics stats = new TrackStatistics();
        stats.setTotalDistance(Distance.of(20000));
        stats.setTotalTime(Duration.ofHours(1).plusMinutes(5).plusSeconds(10));
        stats.setMovingTime(Duration.ofHours(1).plusSeconds(1));
        stats.setMaxSpeed(Speed.of(100));
        stats.setTotalAltitudeGain(6000f);

        // when
        String announcement = VoiceAnnouncementUtils.getAnnouncement(context, stats, UnitSystem.METRIC, true, null, null).toString();

        // then
        assertEquals("Total distance 20.00 kilometers. 1 hour 1 second. Speed 20.0 kilometers per hour.", announcement);
    }

    @Test
    public void getAnnouncement_metric_distance_rounding_check() {
        TrackStatistics stats = new TrackStatistics();
        stats.setTotalDistance(Distance.of(19999));
        stats.setTotalTime(Duration.ofHours(1).plusMinutes(5).plusSeconds(10));
        stats.setMovingTime(Duration.ofHours(1));
        stats.setMaxSpeed(Speed.of(100));
        stats.setTotalAltitudeGain(6000f);

        // when
        String announcement = VoiceAnnouncementUtils.getAnnouncement(context, stats, UnitSystem.METRIC, true, null, null).toString();

        // then
        assertEquals("Total distance 20.00 kilometers. 1 hour. Speed 20.0 kilometers per hour.", announcement);
    }

    @Test
    public void getAnnouncement_metric_distance_rounding_check_two() {
        TrackStatistics stats = new TrackStatistics();
        stats.setTotalDistance(Distance.of(19990));
        stats.setTotalTime(Duration.ofHours(1).plusMinutes(5).plusSeconds(10));
        stats.setMovingTime(Duration.ofHours(1));
        stats.setMaxSpeed(Speed.of(100));
        stats.setTotalAltitudeGain(6000f);

        // when
        String announcement = VoiceAnnouncementUtils.getAnnouncement(context, stats, UnitSystem.METRIC, true, null, null).toString();

        // then
        assertEquals("Total distance 19.99 kilometers. 1 hour. Speed 20.0 kilometers per hour.", announcement);
    }

    @Test
    public void getAnnouncement_withInterval_metric_speed() {
        // given
        int numberOfPoints = 1000;
        Pair<Track.Id, TrackStatistics> trackWithStats = TestDataUtil.buildTrackWithTrackPoints(contentProviderUtils, numberOfPoints);
        Track.Id trackId = trackWithStats.first;
        TrackStatistics stats = trackWithStats.second;
        IntervalStatistics.Interval lastInterval;
        try (TrackPointIterator trackPointIterator = contentProviderUtils.getTrackPointLocationIterator(trackId, null)) {
            assertEquals(trackPointIterator.getCount(), numberOfPoints);
            IntervalStatistics intervalStatistics = new IntervalStatistics(Distance.of(1000));
            intervalStatistics.addTrackPoints(trackPointIterator);
            lastInterval = intervalStatistics.getIntervalList().get(intervalStatistics.getIntervalList().size() - 1);
        }

        // when
        String announcement = VoiceAnnouncementUtils.getAnnouncement(context, stats, UnitSystem.METRIC, true, lastInterval, null).toString();

        // then
        assertEquals("Total distance 14.21 kilometers. 16 minutes 39 seconds. Speed 51.2 kilometers per hour. Lap speed 51.2 kilometers per hour.", announcement);
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
        String announcement = VoiceAnnouncementUtils.getAnnouncement(context, stats, UnitSystem.METRIC, false, null, null).toString();

        // then
        assertEquals("Total distance 20.00 kilometers. 1 hour 5 minutes 10 seconds. Pace 3 minutes 15 seconds per kilometer.", announcement);
    }

    @Test
    public void getAnnouncement_withInterval_metric_pace() {
        // given
        int numberOfPoints = 1000;
        Pair<Track.Id, TrackStatistics> trackWithStats = TestDataUtil.buildTrackWithTrackPoints(contentProviderUtils, numberOfPoints);
        Track.Id trackId = trackWithStats.first;
        TrackStatistics stats = trackWithStats.second;
        IntervalStatistics.Interval lastInterval;
        try (TrackPointIterator trackPointIterator = contentProviderUtils.getTrackPointLocationIterator(trackId, null)) {
            assertEquals(trackPointIterator.getCount(), numberOfPoints);
            IntervalStatistics intervalStatistics = new IntervalStatistics(Distance.of(1000));
            intervalStatistics.addTrackPoints(trackPointIterator);
            lastInterval = intervalStatistics.getIntervalList().get(intervalStatistics.getIntervalList().size() - 1);
        }

        // when
        String announcement = VoiceAnnouncementUtils.getAnnouncement(context, stats, UnitSystem.METRIC, false, lastInterval, null).toString();

        // then
        assertEquals("Total distance 14.21 kilometers. 16 minutes 39 seconds. Pace 1 minute 10 seconds per kilometer. Lap time 1 minute 10 seconds per kilometer.", announcement);
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
        String announcement = VoiceAnnouncementUtils.getAnnouncement(context, stats, UnitSystem.IMPERIAL, true, null, null).toString();

        // then
        assertEquals("Total distance 12.43 miles. 1 hour 5 minutes 10 seconds. Speed 11.4 miles per hour.", announcement);
    }

    @Test
    public void getAnnouncement_withInterval_imperial_speed() {
        // given
        int numberOfPoints = 1000;
        Pair<Track.Id, TrackStatistics> trackWithStats = TestDataUtil.buildTrackWithTrackPoints(contentProviderUtils, numberOfPoints);
        Track.Id trackId = trackWithStats.first;
        TrackStatistics stats = trackWithStats.second;
        IntervalStatistics.Interval lastInterval;
        try (TrackPointIterator trackPointIterator = contentProviderUtils.getTrackPointLocationIterator(trackId, null)) {
            assertEquals(trackPointIterator.getCount(), numberOfPoints);
            IntervalStatistics intervalStatistics = new IntervalStatistics(Distance.of(1000));
            intervalStatistics.addTrackPoints(trackPointIterator);
            lastInterval = intervalStatistics.getIntervalList().get(intervalStatistics.getIntervalList().size() - 1);
        }

        // when
        String announcement = VoiceAnnouncementUtils.getAnnouncement(context, stats, UnitSystem.IMPERIAL, true, lastInterval, null).toString();

        // then
        assertEquals("Total distance 8.83 miles. 16 minutes 39 seconds. Speed 31.8 miles per hour. Lap speed 31.8 miles per hour.", announcement);
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
        String announcement = VoiceAnnouncementUtils.getAnnouncement(context, stats, UnitSystem.IMPERIAL, false, null, null).toString();

        // then
        assertEquals("Total distance 12.43 miles. 1 hour 5 minutes 10 seconds. Pace 5 minutes 15 seconds per mile.", announcement);
    }

    @Test
    public void getAnnouncement_withInterval_imperial_pace() {
        // given
        int numberOfPoints = 1000;
        Pair<Track.Id, TrackStatistics> trackWithStats = TestDataUtil.buildTrackWithTrackPoints(contentProviderUtils, numberOfPoints);
        Track.Id trackId = trackWithStats.first;
        TrackStatistics stats = trackWithStats.second;
        IntervalStatistics.Interval lastInterval;
        try (TrackPointIterator trackPointIterator = contentProviderUtils.getTrackPointLocationIterator(trackId, null)) {
            assertEquals(trackPointIterator.getCount(), numberOfPoints);
            IntervalStatistics intervalStatistics = new IntervalStatistics(Distance.of(1000));
            intervalStatistics.addTrackPoints(trackPointIterator);
            lastInterval = intervalStatistics.getIntervalList().get(intervalStatistics.getIntervalList().size() - 1);
        }

        // when
        String announcement = VoiceAnnouncementUtils.getAnnouncement(context, stats, UnitSystem.IMPERIAL, false, lastInterval, null).toString();

        // then
        assertEquals("Total distance 8.83 miles. 16 minutes 39 seconds. Pace 1 minute 53 seconds per mile. Lap time 1 minute 53 seconds per mile.", announcement);
    }

    @Test
    public void getAnnouncement_heart_rate_and_sensor_statistics() {
        PreferencesUtils.setVoiceAnnounceAverageHeartRate(true);
        PreferencesUtils.setVoiceAnnounceLapHeartRate(true);

        int numberOfPoints = 1000;
        Pair<Track.Id, TrackStatistics> trackWithStats = TestDataUtil.buildTrackWithTrackPoints(contentProviderUtils, numberOfPoints);
        Track.Id trackId = trackWithStats.first;
        TrackStatistics stats = trackWithStats.second;
        IntervalStatistics.Interval lastInterval;
        try (TrackPointIterator trackPointIterator = contentProviderUtils.getTrackPointLocationIterator(trackId, null)) {
            assertEquals(trackPointIterator.getCount(), numberOfPoints);
            IntervalStatistics intervalStatistics = new IntervalStatistics(Distance.of(1000));
            intervalStatistics.addTrackPoints(trackPointIterator);
            lastInterval = intervalStatistics.getIntervalList().get(intervalStatistics.getIntervalList().size() - 1);
        }

        SensorStatistics sensorStatistics = new SensorStatistics(HeartRate.of(180f), HeartRate.of(180f), null, null, null);

        // when
        String announcement = VoiceAnnouncementUtils.getAnnouncement(context, stats, UnitSystem.METRIC, true, lastInterval, sensorStatistics).toString();

        // then
        assertEquals("Total distance 14.21 kilometers. 16 minutes 39 seconds. Speed 51.2 kilometers per hour. Lap speed 51.2 kilometers per hour. Average heart rate 180 bpm. Current heart rate 133 bpm.", announcement);
    }

    @Test
    public void getAnnouncement_only_lap_heart_rate() {
        PreferencesUtils.setVoiceAnnounceLapHeartRate(true);
        PreferencesUtils.setVoiceAnnounceAverageHeartRate(false);
        PreferencesUtils.setVoiceAnnounceTotalDistance(false);
        PreferencesUtils.setVoiceAnnounceMovingTime(false);
        PreferencesUtils.setVoiceAnnounceAverageSpeedPace(false);
        PreferencesUtils.setVoiceAnnounceLapSpeedPace(false);

        int numberOfPoints = 1000;
        Pair<Track.Id, TrackStatistics> trackWithStats = TestDataUtil.buildTrackWithTrackPoints(contentProviderUtils, numberOfPoints);
        Track.Id trackId = trackWithStats.first;
        TrackStatistics stats = trackWithStats.second;
        IntervalStatistics.Interval lastInterval;
        try (TrackPointIterator trackPointIterator = contentProviderUtils.getTrackPointLocationIterator(trackId, null)) {
            assertEquals(trackPointIterator.getCount(), numberOfPoints);
            IntervalStatistics intervalStatistics = new IntervalStatistics(Distance.of(1000));
            intervalStatistics.addTrackPoints(trackPointIterator);
            lastInterval = intervalStatistics.getIntervalList().get(intervalStatistics.getIntervalList().size() - 1);
        }

        SensorStatistics sensorStatistics = new SensorStatistics(HeartRate.of(180f), HeartRate.of(180f), null, null, null);

        // when
        String announcement = VoiceAnnouncementUtils.getAnnouncement(context, stats, UnitSystem.METRIC, true, lastInterval, sensorStatistics).toString();

        // then
        assertEquals(" Current heart rate 133 bpm.", announcement);
    }
}
