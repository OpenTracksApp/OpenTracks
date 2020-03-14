package de.dennisguse.opentracks.util;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import de.dennisguse.opentracks.stats.TripStatistics;

@RunWith(AndroidJUnit4.class)
public class AnnouncementUtilsTest {

    private Context context = ApplicationProvider.getApplicationContext();

    @Test
    public void getAnnouncement() {
        TripStatistics stats = new TripStatistics();
        stats.setTotalDistance(20000);
        stats.setTotalTime(600000);
        stats.setMovingTime(300000);
        stats.setMaxSpeed(100);
        stats.setTotalElevationGain(6000);

        // when
        String announcement = AnnouncementUtils.getAnnouncement(context, stats);

        // then
        Assert.assertEquals("OpenTracks total distance 20.00 kilometers in 5 minutes 0 seconds at 240.0 kilometers per hour", announcement);
    }
}