package de.dennisguse.opentracks.viewmodels;

import android.content.Context;
import android.util.Pair;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.List;

import de.dennisguse.opentracks.content.data.Distance;
import de.dennisguse.opentracks.content.data.TestDataUtil;
import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.content.data.TrackPoint;
import de.dennisguse.opentracks.content.provider.ContentProviderUtils;
import de.dennisguse.opentracks.content.provider.TrackPointIterator;
import de.dennisguse.opentracks.stats.TrackStatistics;
import de.dennisguse.opentracks.stats.TrackStatisticsUpdater;
import de.dennisguse.opentracks.util.UnitConversions;

import static org.junit.Assert.assertEquals;

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
            trackStatisticsUpdater.addTrackPoint(tp, Distance.of(0));
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

    private void whenAndThen(int numberOfPoints, float distanceInterval) {
        IntervalStatistics intervalStatistics = new IntervalStatistics(Distance.of(distanceInterval), Distance.of(0));
        Pair<Track.Id, TrackStatistics> trackWithStats = TestDataUtil.buildTrackWithTrackPoints(contentProviderUtils, numberOfPoints);
        Track.Id trackId = trackWithStats.first;
        TrackStatistics trackStatistics = trackWithStats.second;
        try (TrackPointIterator trackPointIterator = contentProviderUtils.getTrackPointLocationIterator(trackId, null)) {
            assertEquals(trackPointIterator.getCount(), numberOfPoints);
            intervalStatistics.addTrackPoints(trackPointIterator);
        }
        List<IntervalStatistics.Interval> intervalList = intervalStatistics.getIntervalList();
        Distance totalDistance = Distance.of(0);
        float totalTime = 0L;
        float totalGain = 0f;
        for (IntervalStatistics.Interval i : intervalList) {
            totalDistance = totalDistance.plus(i.getDistance());
            totalTime += i.getDistance().toM() / i.getSpeed().toMPS();
            totalGain += i.getGain_m();
        }

        // then
        assertEquals(trackStatistics.getTotalDistance().toM(), totalDistance.toM(), 0.01);
        assertEquals(trackStatistics.getTotalTime().toMillis(), totalTime * UnitConversions.S_TO_MS, 1);
        assertEquals(intervalList.size(), (int) Math.ceil(trackStatistics.getTotalDistance().toM() / distanceInterval));
        assertEquals(totalGain, numberOfPoints * TestDataUtil.ALTITUDE_GAIN, 0.1);

        for (int i = 0; i < intervalList.size() - 1; i++) {
            assertEquals(intervalList.get(i).getDistance().toM(), distanceInterval, 0.001);
            totalDistance = totalDistance.minus(intervalList.get(i).getDistance());
        }
        assertEquals(intervalList.get(intervalList.size() - 1).getDistance().toM(), totalDistance.toM(), 0.01);
    }
}