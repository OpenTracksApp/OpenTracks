package de.dennisguse.opentracks.ui.intervals;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.util.Pair;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.List;

import de.dennisguse.opentracks.content.data.TestDataUtil;
import de.dennisguse.opentracks.data.ContentProviderUtils;
import de.dennisguse.opentracks.data.TrackPointIterator;
import de.dennisguse.opentracks.data.models.Distance;
import de.dennisguse.opentracks.data.models.Track;
import de.dennisguse.opentracks.data.models.TrackPoint;
import de.dennisguse.opentracks.stats.TrackStatistics;
import de.dennisguse.opentracks.stats.TrackStatisticsUpdater;

@RunWith(JUnit4.class)
public class IntervalStatisticsTest {

    private static final String TAG = IntervalStatisticsTest.class.getSimpleName();

    private final Context context = ApplicationProvider.getApplicationContext();
    private ContentProviderUtils contentProviderUtils;

    @Before
    public void setUp() {
        contentProviderUtils = new ContentProviderUtils(context);
    }

    private TrackStatistics buildTrackStatistics(List<TrackPoint> trackPoints) {
        TrackStatisticsUpdater trackStatisticsUpdater = new TrackStatisticsUpdater();
        for (TrackPoint tp : trackPoints) {
            trackStatisticsUpdater.addTrackPoint(tp);
        }
        return trackStatisticsUpdater.getTrackStatistics();
    }

    /**
     * Tests that build method compute the distance correctly comparing the result with TrackStatisticsUpdater result.
     */
    @Test
    public void testBuild_1() {
        // With 50 points and interval distance of 1000m.

        // given
        float distanceInterval = 1000f;

        // when and then
        whenAndThen(50, distanceInterval);
    }

    /**
     * Tests that build method compute the distance correctly comparing the result with TrackStatisticsUpdater result.
     */
    @Test
    public void testBuild_2() {
        // With 200 points and interval distance of 1000m.

        // given
        float distanceInterval = 1000f;

        // when and then
        whenAndThen(200, distanceInterval);
    }

    /**
     * Tests that build method compute the distance correctly comparing the result with TrackStatisticsUpdater result.
     */
    @Test
    public void testBuild_3() {
        // With 200 points and interval distance of 3000m.

        // given
        float distanceInterval = 3000f;

        // when and then
        whenAndThen(3000, distanceInterval);
    }

    /**
     * Tests that build method compute the distance correctly comparing the result with TrackStatisticsUpdater result.
     */
    @Test
    public void testBuild_4() {
        // With 1000 points and interval distance of 3000m.

        // given
        float distanceInterval = 3000f;

        // when and then
        whenAndThen(1000, distanceInterval);
    }

    /**
     * Tests that build method compute the distance correctly comparing the result with TrackStatisticsUpdater result.
     */
    @Test
    public void testBuild_5() {
        // With 10000 points and interval distance of 1000m.

        // given
        float distanceInterval = 1000f;

        // when and then
        whenAndThen(10000, distanceInterval);
    }

    @Test
    public void testWithNoLossTrackPoints() {
        // TrackPoints with elevation gain but without elevation loss.

        // given
        float distanceInterval = 1000f;
        int numberOfPoints = 10000;
        Track dummyTrack = new Track();
        dummyTrack.setId(new Track.Id(System.currentTimeMillis()));
        dummyTrack.setName("Dummy Track Without Elevation Loss");
        contentProviderUtils.insertTrack(dummyTrack);
        TrackStatisticsUpdater trackStatisticsUpdater = new TrackStatisticsUpdater();
        for (int i = 0; i < numberOfPoints; i++) {
            TrackPoint tp = TestDataUtil.createTrackPoint(i);
            tp.setAltitudeLoss(null);
            contentProviderUtils.insertTrackPoint(tp, dummyTrack.getId());
            trackStatisticsUpdater.addTrackPoint(tp);
        }
        dummyTrack.setTrackStatistics(trackStatisticsUpdater.getTrackStatistics());
        contentProviderUtils.updateTrack(dummyTrack);
        Pair<Track.Id, TrackStatistics> trackWithStats = new Pair<>(dummyTrack.getId(), trackStatisticsUpdater.getTrackStatistics());

        // when and then
        whenAndThen(trackWithStats, numberOfPoints, distanceInterval);
    }

    private void whenAndThen(int numberOfPoints, float distanceInterval) {
        Pair<Track.Id, TrackStatistics> trackWithStats = TestDataUtil.buildTrackWithTrackPoints(contentProviderUtils, numberOfPoints);
        whenAndThen(trackWithStats, numberOfPoints, distanceInterval);

    }

    private void whenAndThen(Pair<Track.Id, TrackStatistics> trackWithStats, int numberOfPoints, float distanceInterval) {
        IntervalStatistics intervalStatistics = new IntervalStatistics(Distance.of(distanceInterval));
        Track.Id trackId = trackWithStats.first;
        TrackStatistics trackStatistics = trackWithStats.second;
        try (TrackPointIterator trackPointIterator = contentProviderUtils.getTrackPointLocationIterator(trackId, null)) {
            assertEquals(trackPointIterator.getCount(), numberOfPoints);
            intervalStatistics.addTrackPoints(trackPointIterator);
        }
        List<IntervalStatistics.Interval> intervalList = intervalStatistics.getIntervalList();
        Distance totalDistance = Distance.of(0);
        float totalTime = 0L;
        Float totalGain = null;
        Float totalLoss = null;
        for (IntervalStatistics.Interval i : intervalList) {
            totalDistance = totalDistance.plus(i.getDistance());
            totalTime += i.getDistance().toM() / i.getSpeed().toMPS();

            if (totalGain == null) {
                totalGain = i.getGain_m();
            } else if (i.getGain_m() != null) {
                totalGain += i.getGain_m();
            }

            if (totalLoss == null) {
                totalLoss = i.getLoss_m();
            } else if (i.getLoss_m() != null) {
                totalLoss += i.getLoss_m();
            }
        }

        // then
        assertEquals(trackStatistics.getTotalDistance().toM(), totalDistance.toM(), 0.01);
        assertEquals(trackStatistics.getTotalTime().toSeconds(), totalTime, 0.01);
        assertEquals(intervalList.size(), (int) Math.ceil(trackStatistics.getTotalDistance().toM() / distanceInterval));
        if (totalGain != null) {
            assertEquals(totalGain, numberOfPoints * TestDataUtil.ALTITUDE_GAIN, 0.1);
        } else {
            assertTrue(intervalStatistics.getIntervalList().stream().noneMatch(IntervalStatistics.Interval::hasGain));
        }
        if (totalLoss != null) {
            assertEquals(totalLoss, numberOfPoints * TestDataUtil.ALTITUDE_LOSS, 0.1);
        } else {
            assertTrue(intervalStatistics.getIntervalList().stream().noneMatch(IntervalStatistics.Interval::hasLoss));
        }

        for (int i = 0; i < intervalList.size() - 1; i++) {
            assertEquals(intervalList.get(i).getDistance().toM(), distanceInterval, 0.001);
            totalDistance = totalDistance.minus(intervalList.get(i).getDistance());
        }
        assertEquals(intervalList.get(intervalList.size() - 1).getDistance().toM(), totalDistance.toM(), 0.01);
    }
}