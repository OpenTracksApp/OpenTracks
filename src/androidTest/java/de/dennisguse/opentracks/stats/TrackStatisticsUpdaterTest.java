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
        assertEquals(56.90, statistics.getTotalDistance(), 0.01);
        assertEquals(Duration.ofSeconds(9), statistics.getTotalTime());
        assertEquals(Duration.ofSeconds(4), statistics.getMovingTime());

        assertEquals(2.5, statistics.getMinElevation(), 0.01);
        assertEquals(27.5, statistics.getMaxElevation(), 0.01);
        assertEquals(18.0, statistics.getTotalElevationGain(), 0.01);
        assertEquals(18.0, statistics.getTotalElevationLoss(), 0.01);

        assertEquals(14.226, statistics.getMaxSpeed(), 0.01);
        assertEquals(14.226, statistics.getAverageMovingSpeed(), 0.01);
        assertEquals(6.322, statistics.getAverageSpeed(), 0.01);
    }
}