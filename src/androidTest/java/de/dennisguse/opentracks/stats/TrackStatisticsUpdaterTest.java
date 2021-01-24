package de.dennisguse.opentracks.stats;

import org.junit.Test;

import java.time.Duration;

import de.dennisguse.opentracks.content.data.TestDataUtil;
import de.dennisguse.opentracks.content.data.Track;

import static org.junit.Assert.assertEquals;

public class TrackStatisticsUpdaterTest {

    @Test
    public void addTrackPoint() {
        // given
        TestDataUtil.TrackData data = TestDataUtil.createTestingTrack(new Track.Id(1));

        // when
        TrackStatisticsUpdater updater = new TrackStatisticsUpdater();
        data.trackPoints.forEach(it -> updater.addTrackPoint(it, 50));

        // then
        TrackStatistics statistics = updater.getTrackStatistics();
        assertEquals(85.35, statistics.getTotalDistance(), 0.01);
        assertEquals(Duration.ofMillis(13999), statistics.getTotalTime());
        assertEquals(Duration.ofSeconds(6), statistics.getMovingTime());

        assertEquals(2.5, statistics.getMinElevation(), 0.01);
        assertEquals(27.5, statistics.getMaxElevation(), 0.01);
        assertEquals(27, statistics.getTotalElevationGain(), 0.01);
        assertEquals(27.0, statistics.getTotalElevationLoss(), 0.01);

        assertEquals(14.226, statistics.getMaxSpeed(), 0.01);
        assertEquals(14.226, statistics.getAverageMovingSpeed(), 0.01);
        assertEquals(6.566, statistics.getAverageSpeed(), 0.01);
    }
}