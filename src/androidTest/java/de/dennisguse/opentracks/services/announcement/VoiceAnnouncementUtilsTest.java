package de.dennisguse.opentracks.services.announcement;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import android.content.Context;
import android.icu.text.MessageFormat;
import android.os.Build;
import android.util.Pair;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;

import de.dennisguse.opentracks.LocaleRule;
import de.dennisguse.opentracks.content.data.TestDataUtil;
import de.dennisguse.opentracks.data.ContentProviderUtils;
import de.dennisguse.opentracks.data.TrackPointIterator;
import de.dennisguse.opentracks.data.models.Distance;
import de.dennisguse.opentracks.data.models.HeartRate;
import de.dennisguse.opentracks.data.models.Speed;
import de.dennisguse.opentracks.data.models.Track;
import de.dennisguse.opentracks.sensors.sensorData.SensorDataSet;
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

        PreferencesUtils.setVoiceAnnounceHeartRateCurrent(false);
        PreferencesUtils.setVoiceAnnounceLapHeartRate(false);
        PreferencesUtils.setVoiceAnnounceAverageHeartRate(false);
        PreferencesUtils.setVoiceAnnounceTotalDistance(true);
        PreferencesUtils.setVoiceAnnounceMovingTime(true);
        PreferencesUtils.setVoiceAnnounceAverageSpeedPace(true);
        PreferencesUtils.setVoiceAnnounceLapSpeedPace(true);
    }

    @Test
    public void sdkRequirement() {
        assertFalse("Due to DateTimeFormatter using NBSP, these tests only work in SDK34+", Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE);
    }

    @Test
    public void getAnnouncement_metric_speed() {
        TrackStatistics stats = new TrackStatistics();
        stats.setStartTime(Instant.EPOCH);
        stats.setTotalDistance(Distance.of(20000));
        stats.setTotalTime(Duration.ofHours(2).plusMinutes(5).plusSeconds(10));
        stats.setMovingTime(Duration.ofHours(1).plusMinutes(5).plusSeconds(10));
        stats.setMaxSpeed(Speed.of(100));
        stats.setTotalAltitudeGain(6000f);

        Track track = new Track();
        track.setTrackStatistics(stats);

        SensorDataSet dataSet = Mockito.mock(SensorDataSet.class);
        Mockito.when(dataSet.getHeartRate()).thenReturn(new Pair<>(HeartRate.of(60), "unused"));

        // when
        String announcement = VoiceAnnouncementUtils.createStatistics(context, track, dataSet, UnitSystem.METRIC, true, null, null).toString();

        // then
        assertEquals("12:00 AM. Total distance 20.0 kilometers. 1 hour 5 minutes 10 seconds. Average moving speed 18.4 kilometers per hour.", announcement);
    }

    @Test
    public void getAnnouncement_metric_speed_rounding_check() {
        TrackStatistics stats = new TrackStatistics();
        stats.setStartTime(Instant.EPOCH);
        stats.setTotalDistance(Distance.of(20000));
        stats.setTotalTime(Duration.ofHours(1).plusMinutes(5).plusSeconds(10));
        stats.setMovingTime(Duration.ofHours(1).plusSeconds(1));
        stats.setMaxSpeed(Speed.of(100));
        stats.setTotalAltitudeGain(6000f);

        Track track = new Track();
        track.setTrackStatistics(stats);

        SensorDataSet dataSet = Mockito.mock(SensorDataSet.class);
        Mockito.when(dataSet.getHeartRate()).thenReturn(new Pair<>(HeartRate.of(60), "unused"));

        // when
        String announcement = VoiceAnnouncementUtils.createStatistics(context, track, dataSet, UnitSystem.METRIC, true, null, null).toString();

        // then
        assertEquals("12:00 AM. Total distance 20.0 kilometers. 1 hour 1 second. Average moving speed 20.0 kilometers per hour.", announcement);
    }

    @Test
    public void getAnnouncement_metric_distance_rounding_check() {
        TrackStatistics stats = new TrackStatistics();
        stats.setStartTime(Instant.EPOCH);
        stats.setTotalDistance(Distance.of(19999));
        stats.setTotalTime(Duration.ofHours(1).plusMinutes(5).plusSeconds(10));
        stats.setMovingTime(Duration.ofHours(1));
        stats.setMaxSpeed(Speed.of(100));
        stats.setTotalAltitudeGain(6000f);

        Track track = new Track();
        track.setTrackStatistics(stats);

        SensorDataSet dataSet = Mockito.mock(SensorDataSet.class);
        Mockito.when(dataSet.getHeartRate()).thenReturn(new Pair<>(HeartRate.of(60), "unused"));

        // when
        String announcement = VoiceAnnouncementUtils.createStatistics(context, track, dataSet, UnitSystem.METRIC, true, null, null).toString();

        // then
        assertEquals("12:00 AM. Total distance 20.0 kilometers. 1 hour. Average moving speed 20.0 kilometers per hour.", announcement);
    }

    @Test
    public void getAnnouncement_metric_distance_rounding_check_two() {
        TrackStatistics stats = new TrackStatistics();
        stats.setStartTime(Instant.EPOCH);
        stats.setTotalDistance(Distance.of(19900));
        stats.setTotalTime(Duration.ofHours(1).plusMinutes(5).plusSeconds(10));
        stats.setMovingTime(Duration.ofHours(1));
        stats.setMaxSpeed(Speed.of(100));
        stats.setTotalAltitudeGain(6000f);

        Track track = new Track();
        track.setTrackStatistics(stats);

        SensorDataSet dataSet = Mockito.mock(SensorDataSet.class);
        Mockito.when(dataSet.getHeartRate()).thenReturn(new Pair<>(HeartRate.of(60), "unused"));

        // when
        String announcement = VoiceAnnouncementUtils.createStatistics(context, track, dataSet, UnitSystem.METRIC, true, null, null).toString();

        // then
        assertEquals("12:00 AM. Total distance 19.9 kilometers. 1 hour. Average moving speed 19.9 kilometers per hour.", announcement);
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

        SensorDataSet dataSet = Mockito.mock(SensorDataSet.class);
        Mockito.when(dataSet.getHeartRate()).thenReturn(new Pair<>(HeartRate.of(60), "unused"));

        Track track = new Track();
        track.setTrackStatistics(stats);

        // when
        String announcement = VoiceAnnouncementUtils.createStatistics(context, track, dataSet, UnitSystem.METRIC, true, lastInterval, null).toString();

        // then
        assertEquals("12:16 AM. Total distance 14.2 kilometers. 16 minutes 39 seconds. Average moving speed 51.2 kilometers per hour. Lap speed 51.2 kilometers per hour.", announcement);
    }

    @Test
    public void getAnnouncement_metric_pace() {
        TrackStatistics stats = new TrackStatistics();
        stats.setStartTime(Instant.EPOCH);
        stats.setTotalDistance(Distance.of(20000));
        stats.setTotalTime(Duration.ofHours(2).plusMinutes(5).plusSeconds(10));
        stats.setMovingTime(Duration.ofHours(1).plusMinutes(5).plusSeconds(10));
        stats.setMaxSpeed(Speed.of(100));
        stats.setTotalAltitudeGain(6000f);

        Track track = new Track();
        track.setTrackStatistics(stats);

        SensorDataSet dataSet = Mockito.mock(SensorDataSet.class);
        Mockito.when(dataSet.getHeartRate()).thenReturn(new Pair<>(HeartRate.of(60), "unused"));

        // when
        String announcement = VoiceAnnouncementUtils.createStatistics(context, track, dataSet, UnitSystem.METRIC, false, null, null).toString();

        // then
        assertEquals("12:00 AM. Total distance 20.0 kilometers. 1 hour 5 minutes 10 seconds. Pace 3 minutes 15 seconds per kilometer.", announcement);
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

        Track track = new Track();
        track.setTrackStatistics(stats);

        SensorDataSet dataSet = Mockito.mock(SensorDataSet.class);
        Mockito.when(dataSet.getHeartRate()).thenReturn(new Pair<>(HeartRate.of(60), "unused"));

        // when
        String announcement = VoiceAnnouncementUtils.createStatistics(context, track, dataSet, UnitSystem.METRIC, false, lastInterval, null).toString();

        // then
        assertEquals("12:16 AM. Total distance 14.2 kilometers. 16 minutes 39 seconds. Pace 1 minute 10 seconds per kilometer. Lap time 1 minute 10 seconds per kilometer.", announcement);
    }

    @Test
    public void getAnnouncement_imperial_speed() {
        TrackStatistics stats = new TrackStatistics();
        stats.setStartTime(Instant.EPOCH);
        stats.setTotalDistance(Distance.of(20000));
        stats.setTotalTime(Duration.ofHours(2).plusMinutes(5).plusSeconds(10));
        stats.setMovingTime(Duration.ofHours(1).plusMinutes(5).plusSeconds(10));
        stats.setMaxSpeed(Speed.of(100));
        stats.setTotalAltitudeGain(6000f);

        Track track = new Track();
        track.setTrackStatistics(stats);

        SensorDataSet dataSet = Mockito.mock(SensorDataSet.class);
        Mockito.when(dataSet.getHeartRate()).thenReturn(new Pair<>(HeartRate.of(60), "unused"));

        // when
        String announcement = VoiceAnnouncementUtils.createStatistics(context, track, dataSet, UnitSystem.IMPERIAL_FEET, true, null, null).toString();

        // then
        assertEquals("12:00 AM. Total distance 12.4 miles. 1 hour 5 minutes 10 seconds. Average moving speed 11.4 miles per hour.", announcement);
    }

    @Test
    public void getAnnouncement_imperial_speed_1() {
        TrackStatistics stats = new TrackStatistics();
        stats.setStartTime(Instant.EPOCH);
        stats.setTotalDistance(Distance.ofMile(1.1));
        stats.setTotalTime(Duration.ofHours(2).plusMinutes(5).plusSeconds(10));
        stats.setMovingTime(Duration.ofHours(1));

        Track track = new Track();
        track.setTrackStatistics(stats);

        SensorDataSet dataSet = Mockito.mock(SensorDataSet.class);
        Mockito.when(dataSet.getHeartRate()).thenReturn(new Pair<>(HeartRate.of(60), "unused"));

        // when
        String announcement = VoiceAnnouncementUtils.createStatistics(context, track, dataSet, UnitSystem.IMPERIAL_FEET, true, null, null).toString();

        // then
        assertEquals("12:00 AM. Total distance 1.1 miles. 1 hour. Average moving speed 1.1 miles per hour.", announcement);
    }

    @Test
    public void getAnnouncement_imperial_meter_speed_1() {
        TrackStatistics stats = new TrackStatistics();
        stats.setStartTime(Instant.EPOCH);
        stats.setTotalDistance(Distance.ofMile(1.1));
        stats.setTotalTime(Duration.ofHours(2).plusMinutes(5).plusSeconds(10));
        stats.setMovingTime(Duration.ofHours(1));

        Track track = new Track();
        track.setTrackStatistics(stats);

        SensorDataSet dataSet = Mockito.mock(SensorDataSet.class);
        Mockito.when(dataSet.getHeartRate()).thenReturn(new Pair<>(HeartRate.of(60), "unused"));

        // when
        String announcement = VoiceAnnouncementUtils.createStatistics(context, track, dataSet, UnitSystem.IMPERIAL_METER, true, null, null).toString();

        // then
        assertEquals("12:00 AM. Total distance 1.1 miles. 1 hour. Average moving speed 1.1 miles per hour.", announcement);
    }

    @Test
    public void getAnnouncement_metric_speed_1() {
        TrackStatistics stats = new TrackStatistics();
        stats.setStartTime(Instant.EPOCH);
        stats.setTotalDistance(Distance.ofKilometer(1.1));
        stats.setTotalTime(Duration.ofHours(2).plusMinutes(5).plusSeconds(10));
        stats.setMovingTime(Duration.ofHours(1));

        Track track = new Track();
        track.setTrackStatistics(stats);

        SensorDataSet dataSet = Mockito.mock(SensorDataSet.class);
        Mockito.when(dataSet.getHeartRate()).thenReturn(new Pair<>(HeartRate.of(60), "unused"));

        // when
        String announcement = VoiceAnnouncementUtils.createStatistics(context, track, dataSet, UnitSystem.METRIC, true, null, null).toString();

        // then
        assertEquals("12:00 AM. Total distance 1.1 kilometers. 1 hour. Average moving speed 1.1 kilometers per hour.", announcement);
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

        Track track = new Track();
        track.setTrackStatistics(stats);

        SensorDataSet dataSet = Mockito.mock(SensorDataSet.class);
        Mockito.when(dataSet.getHeartRate()).thenReturn(new Pair<>(HeartRate.of(60), "unused"));

        // when
        String announcement = VoiceAnnouncementUtils.createStatistics(context, track, dataSet, UnitSystem.IMPERIAL_FEET, true, lastInterval, null).toString();

        // then
        assertEquals("12:16 AM. Total distance 8.8 miles. 16 minutes 39 seconds. Average moving speed 31.8 miles per hour. Lap speed 31.8 miles per hour.", announcement);
    }

    @Test
    public void getAnnouncement_imperial_pace() {
        TrackStatistics stats = new TrackStatistics();
        stats.setStartTime(Instant.EPOCH);
        stats.setTotalDistance(Distance.of(20000));
        stats.setTotalTime(Duration.ofHours(2).plusMinutes(5).plusSeconds(10));
        stats.setMovingTime(Duration.ofHours(1).plusMinutes(5).plusSeconds(10));
        stats.setMaxSpeed(Speed.of(100));
        stats.setTotalAltitudeGain(6000f);

        Track track = new Track();
        track.setTrackStatistics(stats);

        SensorDataSet dataSet = Mockito.mock(SensorDataSet.class);
        Mockito.when(dataSet.getHeartRate()).thenReturn(new Pair<>(HeartRate.of(60), "unused"));

        // when
        String announcement = VoiceAnnouncementUtils.createStatistics(context, track, dataSet, UnitSystem.IMPERIAL_FEET, false, null, null).toString();

        // then
        assertEquals("12:00 AM. Total distance 12.4 miles. 1 hour 5 minutes 10 seconds. Pace 5 minutes 15 seconds per mile.", announcement);
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

        Track track = new Track();
        track.setTrackStatistics(stats);

        SensorDataSet dataSet = Mockito.mock(SensorDataSet.class);
        Mockito.when(dataSet.getHeartRate()).thenReturn(new Pair<>(HeartRate.of(60), "unused"));

        // when
        String announcement = VoiceAnnouncementUtils.createStatistics(context, track, dataSet, UnitSystem.IMPERIAL_FEET, false, lastInterval, null).toString();

        // then
        assertEquals("12:16 AM. Total distance 8.8 miles. 16 minutes 39 seconds. Pace 1 minute 53 seconds per mile. Lap time 1 minute 53 seconds per mile.", announcement);
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

        SensorStatistics sensorStatistics = new SensorStatistics(HeartRate.of(180f), HeartRate.of(180f), null, null, null, null);

        Track track = new Track();
        track.setTrackStatistics(stats);

        SensorDataSet dataSet = Mockito.mock(SensorDataSet.class);
        Mockito.when(dataSet.getHeartRate()).thenReturn(new Pair<>(HeartRate.of(60), "unused"));

        // when
        String announcement = VoiceAnnouncementUtils.createStatistics(context, track, dataSet, UnitSystem.METRIC, true, lastInterval, sensorStatistics).toString();

        // then
        assertEquals("12:16 AM. Total distance 14.2 kilometers. 16 minutes 39 seconds. Average moving speed 51.2 kilometers per hour. Lap speed 51.2 kilometers per hour. Average heart rate 180 bpm. Lap heart rate 133 bpm.", announcement);
    }

    @Test
    public void getAnnouncement_heart_rate() {
        PreferencesUtils.setVoiceAnnounceHeartRateCurrent(true);
        PreferencesUtils.setVoiceAnnounceLapHeartRate(true);
        PreferencesUtils.setVoiceAnnounceAverageHeartRate(true);

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

        SensorStatistics sensorStatistics = new SensorStatistics(HeartRate.of(180f), HeartRate.of(180f), null, null, null, null);

        Track track = new Track();
        track.setTrackStatistics(stats);

        SensorDataSet dataSet = Mockito.mock(SensorDataSet.class);
        Mockito.when(dataSet.getHeartRate()).thenReturn(new Pair<>(HeartRate.of(60), "unused"));

        // when
        String announcement = VoiceAnnouncementUtils.createStatistics(context, track, dataSet, UnitSystem.METRIC, true, lastInterval, sensorStatistics).toString();

        // then
        assertEquals("12:16 AM.  Current heart rate 60 bpm. Average heart rate 180 bpm. Lap heart rate 133 bpm.", announcement);
    }

    @Test
    public void ICUMessageDemo() {
        // Android 7's ICU MessageFormat; working
        String template = """
                {n, plural,
                one {1 mile}
                other {{n,number,#.#} miles}
                }""";

        assertEquals("1.1 miles", MessageFormat.format(template, Map.of("n", 1.1)));
        assertEquals("1 mile", MessageFormat.format(template, Map.of("n", 1)));
        assertEquals("1.1 miles", MessageFormat.format(template, Map.of("n", 1.11)));
        assertEquals("1.2 miles", MessageFormat.format(template, Map.of("n", 1.18)));
    }
}
