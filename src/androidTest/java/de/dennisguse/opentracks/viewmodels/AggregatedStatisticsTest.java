package de.dennisguse.opentracks.viewmodels;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.stats.TrackStatistics;
import de.dennisguse.opentracks.util.TrackIconUtils;

@RunWith(JUnit4.class)
public class AggregatedStatisticsTest {

    private final Context context = ApplicationProvider.getApplicationContext();

    /**
     * Create a TrackStatistics object.
     *
     * @param totalDistance distance in meters.
     * @param totalTime     total time in milliseconds.
     * @return TrackStatistics object.
     */
    private static Track createTrack(Context context, long totalDistance, long totalTime, String category) {
        TrackStatistics statistics = new TrackStatistics();
        statistics.setStartTime_ms(1000L);  // Resulting start time
        statistics.setStopTime_ms(1000L + totalTime);
        statistics.setTotalTime(totalTime);
        statistics.setMovingTime(totalTime);
        statistics.setTotalDistance(totalDistance);
        statistics.setTotalElevationGain(50.0);
        statistics.setMaxSpeed(50.0);  // Resulting max speed
        statistics.setMaxElevation(1250.0);
        statistics.setMinElevation(1200.0);  // Resulting min elevation

        Track track = new Track();
        track.setIcon(TrackIconUtils.getIconValue(context, category));
        track.setCategory(category);
        track.setTrackStatistics(statistics);
        return track;
    }

    @Test
    public void testAggregate() {
        // given
        // 10km in 40 minutes.
        long totalDistance = 10000;
        long totalTime = 2400000;
        String biking = context.getString(R.string.activity_type_biking);
        Track track = createTrack(context, totalDistance, totalTime, biking);

        // when
        AggregatedStatistics aggregatedStatistics = new AggregatedStatistics();
        aggregatedStatistics.aggregate(track);

        // then
        Assert.assertEquals(1, aggregatedStatistics.getCount());
        Assert.assertNotNull(aggregatedStatistics.get(biking));
        Assert.assertEquals(1, aggregatedStatistics.get(biking).getCountTracks());

        TrackStatistics statistics2 = aggregatedStatistics.get(biking).getTrackStatistics();
        Assert.assertEquals(totalDistance, statistics2.getTotalDistance(), 0);
        Assert.assertEquals(totalTime, statistics2.getMovingTime());
    }

    @Test
    public void testAggregate_mountainBiking() {
        // given
        // 10km in 40 minutes.
        long totalDistance = 10000;
        long totalTime = 2400000;
        String mountainBiking = context.getString(R.string.activity_type_mountain_biking);
        Track track = createTrack(context, totalDistance, totalTime, mountainBiking);

        // when
        AggregatedStatistics aggregatedStatistics = new AggregatedStatistics();
        aggregatedStatistics.aggregate(track);

        // then
        Assert.assertNotNull(aggregatedStatistics.get(mountainBiking));
    }

    @Test
    public void testAggregate_trailRunning() {
        // given
        // 10km in 40 minutes.
        long totalDistance = 10000;
        long totalTime = 2400000;
        String trailRunning = context.getString(R.string.activity_type_trail_running);
        Track track = createTrack(context, totalDistance, totalTime, trailRunning);

        // when
        AggregatedStatistics aggregatedStatistics = new AggregatedStatistics();
        aggregatedStatistics.aggregate(track);

        // then
        Assert.assertNotNull(aggregatedStatistics.get(trailRunning));
    }

    @Test
    public void testAggregate_twoBikingTracks() {
        // given
        // 10km in 40 minutes.
        long totalDistance = 10000;
        long totalTime = 2400000;
        String biking = context.getString(R.string.activity_type_biking);
        Track[] tracks = new Track[2];
        for (int i = 0; i < 2; i++) {
            tracks[i] = createTrack(context, totalDistance, totalTime, biking);
        }

        // when
        AggregatedStatistics aggregatedStatistics = new AggregatedStatistics();
        aggregatedStatistics.aggregate(tracks[0]); // biking activity 1.
        aggregatedStatistics.aggregate(tracks[1]); // biking activity 2.

        // then
        Assert.assertEquals(1, aggregatedStatistics.getCount());
        Assert.assertNotNull(aggregatedStatistics.get(biking));
        Assert.assertEquals(2, aggregatedStatistics.get(biking).getCountTracks());

        TrackStatistics statistics2 = aggregatedStatistics.get(biking).getTrackStatistics();
        Assert.assertEquals(totalDistance * 2, statistics2.getTotalDistance(), 0);
        Assert.assertEquals(totalTime * 2L, statistics2.getMovingTime());
    }

    @Test
    public void testAggregate_threeDifferentTracks() {
        // given
        // 10km in 40 minutes.
        long totalDistance = 10000;

        String biking = context.getString(R.string.activity_type_biking);
        String running = context.getString(R.string.activity_type_running);
        String walking = context.getString(R.string.activity_type_walking);
        long totalTime = 2400000;
        Track[] tracks = new Track[]{
                createTrack(context, totalDistance, totalTime, biking),
                createTrack(context, totalDistance, totalTime, running),
                createTrack(context, totalDistance, totalTime, walking)
        };

        // when
        AggregatedStatistics aggregatedStatistics = new AggregatedStatistics();
        aggregatedStatistics.aggregate(tracks[0]); // biking activity 1.
        aggregatedStatistics.aggregate(tracks[1]); // biking activity 2.
        aggregatedStatistics.aggregate(tracks[2]); // biking activity 3.

        // then
        Assert.assertEquals(3, aggregatedStatistics.getCount());
        Assert.assertNotNull(aggregatedStatistics.get(biking));
        Assert.assertNotNull(aggregatedStatistics.get(running));
        Assert.assertNotNull(aggregatedStatistics.get(walking));
        Assert.assertEquals(1, aggregatedStatistics.get(biking).getCountTracks());
        Assert.assertEquals(1, aggregatedStatistics.get(running).getCountTracks());
        Assert.assertEquals(1, aggregatedStatistics.get(walking).getCountTracks());

        {
            TrackStatistics statistics2 = aggregatedStatistics.get(biking).getTrackStatistics();
            Assert.assertEquals(totalDistance, statistics2.getTotalDistance(), 0);
            Assert.assertEquals(totalTime, statistics2.getMovingTime());
        }

        {
            TrackStatistics statistics2 = aggregatedStatistics.get(running).getTrackStatistics();
            Assert.assertEquals(totalDistance, statistics2.getTotalDistance(), 0);
            Assert.assertEquals(totalTime, statistics2.getMovingTime());
        }

        {
            TrackStatistics statistics2 = aggregatedStatistics.get(walking).getTrackStatistics();
            Assert.assertEquals(totalDistance, statistics2.getTotalDistance(), 0);
            Assert.assertEquals(totalTime, statistics2.getMovingTime());
        }
    }

    @Test
    public void testAggregate_severalTracksWithSeveralActivities() {
        // given
        // 10km in 40 minutes.
        long totalDistance = 10000;
        long totalTime = 2400000;
        String biking = context.getString(R.string.activity_type_biking);
        String running = context.getString(R.string.activity_type_running);
        String walking = context.getString(R.string.activity_type_walking);
        String driving = context.getString(R.string.activity_type_driving);
        Track[] tracks = new Track[]{
                createTrack(context, totalDistance, totalTime, biking),
                createTrack(context, totalDistance, totalTime, running),
                createTrack(context, totalDistance, totalTime, walking),
                createTrack(context, totalDistance, totalTime, biking),
                createTrack(context, totalDistance, totalTime, running),
                createTrack(context, totalDistance, totalTime, walking),
                createTrack(context, totalDistance, totalTime, biking),
                createTrack(context, totalDistance, totalTime, biking),
                createTrack(context, totalDistance, totalTime, biking),
                createTrack(context, totalDistance, totalTime, driving),
        };


        // when
        AggregatedStatistics aggregatedStatistics = new AggregatedStatistics();
        for (Track track : tracks) {
            aggregatedStatistics.aggregate(track);

        }

        // then
        // 4 sports.
        Assert.assertEquals(4, aggregatedStatistics.getCount());

        // There is a map for every sport.
        Assert.assertNotNull(aggregatedStatistics.get(biking));
        Assert.assertNotNull(aggregatedStatistics.get(running));
        Assert.assertNotNull(aggregatedStatistics.get(walking));
        Assert.assertNotNull(aggregatedStatistics.get(driving));

        // Number of tracks by sport.
        Assert.assertEquals(5, aggregatedStatistics.get(biking).getCountTracks()); // Biking.
        Assert.assertEquals(2, aggregatedStatistics.get(running).getCountTracks()); // Running.
        Assert.assertEquals(2, aggregatedStatistics.get(walking).getCountTracks()); // Walking.
        Assert.assertEquals(1, aggregatedStatistics.get(driving).getCountTracks()); // Driving.

        // Biking.
        {
            TrackStatistics statistics2 = aggregatedStatistics.get(biking).getTrackStatistics();
            Assert.assertEquals(totalDistance * 5, statistics2.getTotalDistance(), 0);
            Assert.assertEquals(totalTime * 5, statistics2.getMovingTime());
        }

        // Running.
        {
            TrackStatistics statistics2 = aggregatedStatistics.get(running).getTrackStatistics();
            Assert.assertEquals(totalDistance * 2, statistics2.getTotalDistance(), 0);
            Assert.assertEquals(totalTime * 2, statistics2.getMovingTime());
        }

        // Walking.
        {
            TrackStatistics statistics2 = aggregatedStatistics.get(walking).getTrackStatistics();
            Assert.assertEquals(totalDistance * 2, statistics2.getTotalDistance(), 0);
            Assert.assertEquals(totalTime * 2, statistics2.getMovingTime());
        }

        // Driving.
        {
            TrackStatistics statistics2 = aggregatedStatistics.get(driving).getTrackStatistics();
            Assert.assertEquals(totalDistance, statistics2.getTotalDistance(), 0);
            Assert.assertEquals(totalTime, statistics2.getMovingTime());
        }
    }
}
