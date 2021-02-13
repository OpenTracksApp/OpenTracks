package de.dennisguse.opentracks.util;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Duration;
import java.util.List;

import de.dennisguse.opentracks.R;
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
    public void getAnnouncement_metric() {
        PreferencesUtils.setString(context, R.string.stats_units_key, context.getString(R.string.stats_units_metric));

        TrackStatistics stats = new TrackStatistics();
        stats.setTotalDistance(20000);
        stats.setTotalTime(Duration.ofMillis(600000));
        stats.setMovingTime(Duration.ofMillis(300000));
        stats.setMaxSpeed(100);
        stats.setTotalElevationGain(6000f);

        // when
        String announcement = AnnouncementUtils.getAnnouncement(context, stats, "airplane", null);

        // then
        assertEquals("OpenTracks total distance 20.00 kilometers in 5 minutes 0 seconds at 240.0 kilometers per hour", announcement);
    }

    @Test
    public void getAnnouncement_withInterval_metric() {
        PreferencesUtils.setString(context, R.string.stats_units_key, context.getString(R.string.stats_units_metric));

        TrackStatistics stats = new TrackStatistics();
        stats.setTotalDistance(20000);
        stats.setTotalTime(Duration.ofMillis(600000));
        stats.setMovingTime(Duration.ofMillis(300000));
        stats.setMaxSpeed(100);
        stats.setTotalElevationGain(6000f);

        List<TrackPoint> trackPoints = TestDataUtil.createTrack(new Track.Id(System.currentTimeMillis()), 10).second;
        IntervalStatistics intervalStatistics = new IntervalStatistics(trackPoints, 1000);
        IntervalStatistics.Interval lastInterval = intervalStatistics.getIntervalList().get(intervalStatistics.getIntervalList().size() - 1);

        String expected = "OpenTracks total distance 20.00 kilometers in 5 minutes 0 seconds at 240.0 kilometers per hour Lap speed of 51.2 kilometers per hour";

        // when
        String announcement = AnnouncementUtils.getAnnouncement(context, stats, "airplane", lastInterval);

        // then
        assertEquals(expected, announcement);
    }
}