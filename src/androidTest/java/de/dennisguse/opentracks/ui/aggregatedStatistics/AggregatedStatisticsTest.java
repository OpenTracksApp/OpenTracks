package de.dennisguse.opentracks.ui.aggregatedStatistics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.data.models.ActivityType;
import de.dennisguse.opentracks.data.models.Distance;
import de.dennisguse.opentracks.data.models.Speed;
import de.dennisguse.opentracks.data.models.Track;
import de.dennisguse.opentracks.stats.TrackStatistics;

@RunWith(JUnit4.class)
public class AggregatedStatisticsTest {

    private final Context context = ApplicationProvider.getApplicationContext();

    private static Track createTrack(Context context, Distance totalDistance, Duration totalTime, String activityTypeLocalized) {
        TrackStatistics statistics = new TrackStatistics();
        statistics.setStartTime(Instant.ofEpochMilli(1000L));  // Resulting start time
        statistics.setStopTime(statistics.getStartTime().plus(totalTime));
        statistics.setTotalTime(totalTime);
        statistics.setMovingTime(totalTime);
        statistics.setTotalDistance(totalDistance);
        statistics.setTotalAltitudeGain(50.0f);
        statistics.setMaxSpeed(Speed.of(50.0));  // Resulting max speed
        statistics.setMaxAltitude(1250.0);
        statistics.setMinAltitude(1200.0);  // Resulting min altitude

        Track track = new Track();
        track.setActivityType(ActivityType.findByLocalizedString(context, activityTypeLocalized));
        track.setActivityTypeLocalized(activityTypeLocalized);
        track.setTrackStatistics(statistics);
        return track;
    }

    @Test
    public void testAggregate() {
        // given
        // 10km in 40 minutes.
        Distance totalDistance = Distance.of(10000);
        Duration totalTime = Duration.ofMillis(2400000);
        String biking = context.getString(R.string.activity_type_biking);
        Track track = createTrack(context, totalDistance, totalTime, biking);

        // when
        AggregatedStatistics aggregatedStatistics = new AggregatedStatistics(List.of(track));

        // then
        assertEquals(1, aggregatedStatistics.getCount());
        assertNotNull(aggregatedStatistics.get(biking));
        assertEquals(1, aggregatedStatistics.get(biking).getCountTracks());

        TrackStatistics statistics2 = aggregatedStatistics.get(biking).getTrackStatistics();
        assertEquals(totalDistance, statistics2.getTotalDistance());
        assertEquals(totalTime, statistics2.getMovingTime());
    }

    @Test
    public void testAggregate_mountainBiking() {
        // given
        // 10km in 40 minutes.
        Distance totalDistance = Distance.of(10000);
        Duration totalTime = Duration.ofMillis(2400000);
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
        Distance totalDistance = Distance.of(10000);
        Duration totalTime = Duration.ofMillis(2400000);
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
        Distance totalDistance = Distance.of(10000);
        Duration totalTime = Duration.ofMillis(2400000);
        String biking = context.getString(R.string.activity_type_biking);
        List<Track> tracks = List.of(createTrack(context, totalDistance, totalTime, biking), createTrack(context, totalDistance, totalTime, biking));

        // when
        AggregatedStatistics aggregatedStatistics = new AggregatedStatistics(tracks);

        // then
        assertEquals(1, aggregatedStatistics.getCount());
        assertNotNull(aggregatedStatistics.get(biking));
        assertEquals(2, aggregatedStatistics.get(biking).getCountTracks());

        TrackStatistics statistics2 = aggregatedStatistics.get(biking).getTrackStatistics();
        assertEquals(totalDistance.multipliedBy(2), statistics2.getTotalDistance());
        assertEquals(totalTime.multipliedBy(2), statistics2.getMovingTime());
    }

    @Test
    public void testAggregate_threeDifferentTracks() {
        // given
        // 10km in 40 minutes.
        Distance totalDistance = Distance.of(10000);

        String biking = context.getString(R.string.activity_type_biking);
        String running = context.getString(R.string.activity_type_running);
        String walking = context.getString(R.string.activity_type_walking);
        Duration totalTime = Duration.ofMillis(2400000);
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
            assertEquals(totalDistance, statistics2.getTotalDistance());
            assertEquals(totalTime, statistics2.getMovingTime());
        }

        {
            TrackStatistics statistics2 = aggregatedStatistics.get(running).getTrackStatistics();
            assertEquals(totalDistance, statistics2.getTotalDistance());
            assertEquals(totalTime, statistics2.getMovingTime());
        }

        {
            TrackStatistics statistics2 = aggregatedStatistics.get(walking).getTrackStatistics();
            assertEquals(totalDistance, statistics2.getTotalDistance());
            assertEquals(totalTime, statistics2.getMovingTime());
        }
    }

    @Test
    public void testAggregate_severalTracksWithSeveralActivities() {
        // given
        // 10km in 40 minutes.
        Distance totalDistance = Distance.of(10000);
        Duration totalTime = Duration.ofMillis(2400000);
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
            assertEquals(totalDistance.multipliedBy(5), statistics2.getTotalDistance());
            assertEquals(totalTime.multipliedBy(5), statistics2.getMovingTime());
        }

        // Running.
        {
            TrackStatistics statistics2 = aggregatedStatistics.get(running).getTrackStatistics();
            assertEquals(totalDistance.multipliedBy(2), statistics2.getTotalDistance());
            assertEquals(totalTime.multipliedBy(2), statistics2.getMovingTime());
        }

        // Walking.
        {
            TrackStatistics statistics2 = aggregatedStatistics.get(walking).getTrackStatistics();
            assertEquals(totalDistance.multipliedBy(2), statistics2.getTotalDistance());
            assertEquals(totalTime.multipliedBy(2), statistics2.getMovingTime());
        }

        // Driving.
        {
            TrackStatistics statistics2 = aggregatedStatistics.get(driving).getTrackStatistics();
            assertEquals(totalDistance, statistics2.getTotalDistance());
            assertEquals(totalTime, statistics2.getMovingTime());
        }

        // Check order

        {
            assertEquals(biking, aggregatedStatistics.getItem(0).getActivityTypeLocalized());
            assertEquals(driving, aggregatedStatistics.getItem(3).getActivityTypeLocalized());
        }
    }
}
