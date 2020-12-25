package de.dennisguse.opentracks.util;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

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
    public void getAnnouncement() {
        TrackStatistics stats = new TrackStatistics();
        stats.setTotalDistance(20000);
        stats.setTotalTime(600000);
        stats.setMovingTime(300000);
        stats.setMaxSpeed(100);
        stats.setTotalElevationGain(6000);

        // when
        String announcement = AnnouncementUtils.getAnnouncement(context, stats, "airplane", null);

        // then
        assertEquals("OpenTracks total distance 20.00 kilometers in 5 minutes 0 seconds at 240.0 kilometers per hour", announcement);
    }

    @Test
    public void getAnnouncement_withInterval() {
        TrackStatistics stats = new TrackStatistics();
        stats.setTotalDistance(20000);
        stats.setTotalTime(600000);
        stats.setMovingTime(300000);
        stats.setMaxSpeed(100);
        stats.setTotalElevationGain(6000);

        List<TrackPoint> trackPoints = TestDataUtil.createTrack(new Track.Id(System.currentTimeMillis()), 10).second;
        IntervalStatistics intervalStatistics = new IntervalStatistics(trackPoints, 1000);
        IntervalStatistics.Interval lastInterval = intervalStatistics.getIntervalList().get(intervalStatistics.getIntervalList().size() - 1);

        int speedId = R.plurals.voiceSpeedKilometersPerHour;
        double kmPerHour = lastInterval.getSpeed_ms() * UnitConversions.MPS_TO_KMH;

        String firstPartMsg = "OpenTracks total distance 20.00 kilometers in 5 minutes 0 seconds at 240.0 kilometers per hour";
        String rateMsg = " Lap speed of " + context.getResources().getQuantityString(speedId, getQuantityCount(kmPerHour), kmPerHour);
        String msg = firstPartMsg + rateMsg;

        // when
        String announcement = AnnouncementUtils.getAnnouncement(context, stats, "airplane", lastInterval);

        // then
        assertEquals(msg, announcement);
    }

    private int getQuantityCount(double d) {
        if (d == 0) {
            return 0;
        } else if (d == 1) {
            return 1;
        } else if (d == 2) {
            return 2;
        } else {
            int count = (int) d;
            return Math.max(count, 3);
        }
    }
}