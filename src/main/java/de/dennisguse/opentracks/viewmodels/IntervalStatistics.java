package de.dennisguse.opentracks.viewmodels;

import java.util.ArrayList;
import java.util.List;

import de.dennisguse.opentracks.content.data.TrackPoint;
import de.dennisguse.opentracks.util.LocationUtils;
import de.dennisguse.opentracks.util.UnitConversions;

public class IntervalStatistics {
    List<Interval> intervalList;

    public IntervalStatistics() {
        intervalList = new ArrayList<>();
    }

    public void build(List<TrackPoint> trackPointList, float distanceInterval) {
        intervalList = new ArrayList<>();

        if (trackPointList == null || trackPointList.size() == 0) {
            return;
        }

        Interval interval = new Interval();
        for (int i = 1; i < trackPointList.size(); i++) {
            TrackPoint prevTrackPoint = trackPointList.get(i - 1);
            TrackPoint trackPoint = trackPointList.get(i);

            if (LocationUtils.isValidLocation(trackPoint.getLocation()) && LocationUtils.isValidLocation(prevTrackPoint.getLocation())) {
                interval.distance += prevTrackPoint.distanceTo(trackPoint);
                interval.time += trackPoint.getTime() - prevTrackPoint.getTime();

                if (interval.distance >= distanceInterval) {
                    intervalList.add(interval);
                    interval = new Interval();
                }
            }
        }

        if (interval.distance > 1f) {
            intervalList.add(interval);
        }
    }

    public List<Interval> getIntervalList() {
        return intervalList;
    }

    public static class Interval {
        float distance = 0f;
        float time = 0f;

        public float getDistance() {
            return distance;
        }

        /**
         * @return speed of the interval in m/s.
         */
        public float getSpeed() {
            if (distance == 0f) {
                return 0f;
            }
            return distance / (float) (time * UnitConversions.MS_TO_S);
        }
    }
}
