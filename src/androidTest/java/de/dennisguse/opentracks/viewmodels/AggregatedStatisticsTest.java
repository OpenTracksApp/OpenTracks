package de.dennisguse.opentracks.viewmodels;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.List;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.stats.TrackStatistics;
import de.dennisguse.opentracks.util.TrackIconUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
        statistics.setTotalElevationGain(50.0f);
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
        AggregatedStatistics aggregatedStatistics = new AggregatedStatistics(List.of(track));

        // then
        assertEquals(1, aggregatedStatistics.getCount());
        assertNotNull(aggregatedStatistics.get(biking));
        assertEquals(1, aggregatedStatistics.get(biking).getCountTracks());

        TrackStatistics statistics2 = aggregatedStatistics.get(biking).getTrackStatistics();
        assertEquals(totalDistance, statistics2.getTotalDistance(), 0);
        assertEquals(totalTime, statistics2.getMovingTime());
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
        AggregatedStatistics aggregatedStatistics = new AggregatedStatistics(List.of(track));

        // then
        assertNotNull(aggregatedStatistics.get(mountainBiking));
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
        AggregatedStatistics aggregatedStatistics = new AggregatedStatistics(List.of(track));

        // then
        assertNotNull(aggregatedStatistics.get(trailRunning));
    }

    @Test
    public void testAggregate_twoBikingTracks() {
        // given
        // 10km in 40 minutes.
        long totalDistance = 10000;
        long totalTime = 2400000;
        String biking = context.getString(R.string.activity_type_biking);
        List<Track> tracks = List.of(createTrack(context, totalDistance, totalTime, biking), createTrack(context, totalDistance, totalTime, biking));

        // when
        AggregatedStatistics aggregatedStatistics = new AggregatedStatistics(tracks);

        // then
        assertEquals(1, aggregatedStatistics.getCount());
        assertNotNull(aggregatedStatistics.get(biking));
        assertEquals(2, aggregatedStatistics.get(biking).getCountTracks());

        TrackStatistics statistics2 = aggregatedStatistics.get(biking).getTrackStatistics();
        assertEquals(totalDistance * 2, statistics2.getTotalDistance(), 0);
        assertEquals(totalTime * 2L, statistics2.getMovingTime());
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
        List<Track> tracks = List.of(
                createTrack(context, totalDistance, totalTime, biking),
                createTrack(context, totalDistance, totalTime, running),
                createTrack(context, totalDistance, totalTime, walking)
        );

        // when
        AggregatedStatistics aggregatedStatistics = new AggregatedStatistics(tracks);

        // then
        assertEquals(3, aggregatedStatistics.getCount());
        assertNotNull(aggregatedStatistics.get(biking));
        assertNotNull(aggregatedStatistics.get(running));
        assertNotNull(aggregatedStatistics.get(walking));
        assertEquals(1, aggregatedStatistics.get(biking).getCountTracks());
        assertEquals(1, aggregatedStatistics.get(running).getCountTracks());
        assertEquals(1, aggregatedStatistics.get(walking).getCountTracks());

        {
            TrackStatistics statistics2 = aggregatedStatistics.get(biking).getTrackStatistics();
            assertEquals(totalDistance, statistics2.getTotalDistance(), 0);
            assertEquals(totalTime, statistics2.getMovingTime());
        }

        {
            TrackStatistics statistics2 = aggregatedStatistics.get(running).getTrackStatistics();
            assertEquals(totalDistance, statistics2.getTotalDistance(), 0);
            assertEquals(totalTime, statistics2.getMovingTime());
        }

        {
            TrackStatistics statistics2 = aggregatedStatistics.get(walking).getTrackStatistics();
            assertEquals(totalDistance, statistics2.getTotalDistance(), 0);
            assertEquals(totalTime, statistics2.getMovingTime());
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
        List<Track> tracks = List.of(
                createTrack(context, totalDistance, totalTime, biking),
                createTrack(context, totalDistance, totalTime, running),
                createTrack(context, totalDistance, totalTime, walking),
                createTrack(context, totalDistance, totalTime, biking),
                createTrack(context, totalDistance, totalTime, running),
                createTrack(context, totalDistance, totalTime, walking),
                createTrack(context, totalDistance, totalTime, biking),
                createTrack(context, totalDistance, totalTime, biking),
                createTrack(context, totalDistance, totalTime, biking),
                createTrack(context, totalDistance, totalTime, driving)
        );


        // when
        AggregatedStatistics aggregatedStatistics = new AggregatedStatistics(tracks);

        // then
        // 4 sports.
        assertEquals(4, aggregatedStatistics.getCount());

        // There is a map for every sport.
        assertNotNull(aggregatedStatistics.get(biking));
        assertNotNull(aggregatedStatistics.get(running));
        assertNotNull(aggregatedStatistics.get(walking));
        assertNotNull(aggregatedStatistics.get(driving));

        // Number of tracks by sport.
        assertEquals(5, aggregatedStatistics.get(biking).getCountTracks()); // Biking.
        assertEquals(2, aggregatedStatistics.get(running).getCountTracks()); // Running.
        assertEquals(2, aggregatedStatistics.get(walking).getCountTracks()); // Walking.
        assertEquals(1, aggregatedStatistics.get(driving).getCountTracks()); // Driving.

        // Biking.
        {
            TrackStatistics statistics2 = aggregatedStatistics.get(biking).getTrackStatistics();
            assertEquals(totalDistance * 5, statistics2.getTotalDistance(), 0);
            assertEquals(totalTime * 5, statistics2.getMovingTime());
        }

        // Running.
        {
            TrackStatistics statistics2 = aggregatedStatistics.get(running).getTrackStatistics();
            assertEquals(totalDistance * 2, statistics2.getTotalDistance(), 0);
            assertEquals(totalTime * 2, statistics2.getMovingTime());
        }

        // Walking.
        {
            TrackStatistics statistics2 = aggregatedStatistics.get(walking).getTrackStatistics();
            assertEquals(totalDistance * 2, statistics2.getTotalDistance(), 0);
            assertEquals(totalTime * 2, statistics2.getMovingTime());
        }

        // Driving.
        {
            TrackStatistics statistics2 = aggregatedStatistics.get(driving).getTrackStatistics();
            assertEquals(totalDistance, statistics2.getTotalDistance(), 0);
            assertEquals(totalTime, statistics2.getMovingTime());
        }

        // Check order

        {
            assertEquals(biking, aggregatedStatistics.getItem(0).getCategory());
            assertEquals(driving, aggregatedStatistics.getItem(3).getCategory());
        }
    }
}
