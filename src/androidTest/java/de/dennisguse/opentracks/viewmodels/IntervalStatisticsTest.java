package de.dennisguse.opentracks.viewmodels;

import android.util.Pair;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Arrays;
import java.util.List;

import de.dennisguse.opentracks.content.data.TestDataUtil;
import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.content.data.TrackPoint;
import de.dennisguse.opentracks.stats.TrackStatistics;
import de.dennisguse.opentracks.stats.TrackStatisticsUpdater;
import de.dennisguse.opentracks.util.UnitConversions;

import static org.junit.Assert.assertEquals;

@RunWith(JUnit4.class)
public class IntervalStatisticsTest {

	private static final String TAG = IntervalStatisticsTest.class.getSimpleName();

	private List<TrackPoint> buildTrackPoints(int numberOfTrackPoints) {
		Pair<Track, TrackPoint[]> pair = TestDataUtil.createTrack(new Track.Id(System.currentTimeMillis()), numberOfTrackPoints);
		return Arrays.asList(pair.second);
	}

	private TrackStatistics buildTrackStatistics(List<TrackPoint> trackPoints) {
		TrackStatisticsUpdater trackStatisticsUpdater = new TrackStatisticsUpdater(trackPoints.get(0).getTime());
		for (TrackPoint tp : trackPoints) {
			trackStatisticsUpdater.addTrackPoint(tp, 0);
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
		List<TrackPoint> trackPoints = buildTrackPoints(50);
		TrackStatistics trackStatistics = buildTrackStatistics(trackPoints);
		float distanceInterval = 1000f;
		IntervalStatistics intervalStatistics = new IntervalStatistics();

		// when and then
		whenAndThen(trackPoints, trackStatistics, distanceInterval, intervalStatistics);
	}

	/**
	 * Tests that build method compute the distance correctly comparing the result with TrackStatisticsUpdater result.
	 */
	@Test
	public void testBuild_2() {
		// With 200 points and interval distance of 1000m.

		// given
		List<TrackPoint> trackPoints = buildTrackPoints(200);
		TrackStatistics trackStatistics = buildTrackStatistics(trackPoints);
		float distanceInterval = 1000f;
		IntervalStatistics intervalStatistics = new IntervalStatistics();

		// when and then
		whenAndThen(trackPoints, trackStatistics, distanceInterval, intervalStatistics);
	}

	/**
	 * Tests that build method compute the distance correctly comparing the result with TrackStatisticsUpdater result.
	 */
	@Test
	public void testBuild_3() {
		// With 200 points and interval distance of 3000m.

		// given
		List<TrackPoint> trackPoints = buildTrackPoints(200);
		TrackStatistics trackStatistics = buildTrackStatistics(trackPoints);
		float distanceInterval = 3000f;
		IntervalStatistics intervalStatistics = new IntervalStatistics();

		// when and then
		whenAndThen(trackPoints, trackStatistics, distanceInterval, intervalStatistics);
	}

	/**
	 * Tests that build method compute the distance correctly comparing the result with TrackStatisticsUpdater result.
	 */
	@Test
	public void testBuild_4() {
		// With 1000 points and interval distance of 3000m.

		// given
		List<TrackPoint> trackPoints = buildTrackPoints(1000);
		TrackStatistics trackStatistics = buildTrackStatistics(trackPoints);
		float distanceInterval = 3000f;
		IntervalStatistics intervalStatistics = new IntervalStatistics();

		// when and then
		whenAndThen(trackPoints, trackStatistics, distanceInterval, intervalStatistics);
	}

	/**
	 * Tests that build method compute the distance correctly comparing the result with TrackStatisticsUpdater result.
	 */
	@Test
	public void testBuild_5() {
		// With 10000 points and interval distance of 1000m.

		// given
		List<TrackPoint> trackPoints = buildTrackPoints(10000);
		TrackStatistics trackStatistics = buildTrackStatistics(trackPoints);
		float distanceInterval = 1000f;
		IntervalStatistics intervalStatistics = new IntervalStatistics();

		// when and then
		whenAndThen(trackPoints, trackStatistics, distanceInterval, intervalStatistics);
	}

	private void whenAndThen(List<TrackPoint> trackPoints, TrackStatistics trackStatistics, float distanceInterval, IntervalStatistics intervalStatistics) {
		intervalStatistics.build(trackPoints, distanceInterval);
		List<IntervalStatistics.Interval> intervalList = intervalStatistics.getIntervalList();
		double totalDistance = 0d;
		long totalTime = 0L;
		float totalGain = 0f;
		for (IntervalStatistics.Interval i : intervalList) {
			totalDistance += i.getDistance_m();
			totalTime += ((i.getDistance_m() / i.getSpeed_ms()) * UnitConversions.S_TO_MS);
			totalGain += i.getGain_m();
		}

		// then
		assertEquals(trackStatistics.getTotalDistance(), totalDistance, 0.01);
		assertEquals(trackStatistics.getTotalTime() * UnitConversions.MS_TO_S, totalTime * UnitConversions.MS_TO_S, 0.1);
		assertEquals(intervalList.size(), (int) Math.ceil(trackStatistics.getTotalDistance() / distanceInterval));
		assertEquals(totalGain, trackPoints.size() * TestDataUtil.ELEVATION_GAIN, 0.1);
		for (int i = 0; i < intervalList.size() - 1; i++) {
			assertEquals(intervalList.get(i).getDistance_m(), distanceInterval, 0.001);
			totalDistance -= intervalList.get(i).getDistance_m();
		}
		assertEquals(intervalList.get(intervalList.size() - 1).getDistance_m(), totalDistance, 0.01);
	}
}