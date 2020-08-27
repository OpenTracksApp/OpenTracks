package de.dennisguse.opentracks.viewmodels;

import java.util.ArrayList;
import java.util.List;

import de.dennisguse.opentracks.content.data.TrackPoint;
import de.dennisguse.opentracks.util.LocationUtils;
import de.dennisguse.opentracks.util.UnitConversions;

public class IntervalStatistics {
    List<Interval> intervalList = new ArrayList<>();

    /**
     * @param trackPointList     the list of TrackPoint.
     * @param distanceInterval_m the meters of every interval.
     */
    public void build(List<TrackPoint> trackPointList, float distanceInterval_m) {
        intervalList.clear();

        if (trackPointList == null || trackPointList.size() == 0) {
            return;
        }

        Interval interval = new Interval();
        for (int i = 1; i < trackPointList.size(); i++) {
            TrackPoint prevTrackPoint = trackPointList.get(i - 1);
            TrackPoint trackPoint = trackPointList.get(i);

            if (LocationUtils.isValidLocation(trackPoint.getLocation()) && LocationUtils.isValidLocation(prevTrackPoint.getLocation())) {
                interval.distance_m += prevTrackPoint.distanceTo(trackPoint);
                interval.time_ms += trackPoint.getTime() - prevTrackPoint.getTime();

                if (interval.distance_m >= distanceInterval_m) {
                    float adjustFactor = distanceInterval_m / interval.distance_m;
                    Interval adjustedInterval = new Interval(interval);
                    adjustedInterval.adjust(adjustFactor);

                    intervalList.add(adjustedInterval);

                    interval = new Interval(interval.distance_m - adjustedInterval.distance_m, interval.time_ms - adjustedInterval.time_ms);
                }
            }
        }

        if (interval.distance_m > 1f) {
            intervalList.add(interval);
        }
    }

    public List<Interval> getIntervalList() {
        return intervalList;
    }

    public static class Interval {
        private float distance_m = 0f;
        private float time_ms = 0f;

        public Interval() {}

        public Interval(float distance_m, float time_ms) {
            this.distance_m = distance_m;
            this.time_ms = time_ms;
        }

        public Interval(Interval i) {
            distance_m = i.distance_m;
            time_ms = i.time_ms;
        }

        public float getDistance_m() {
            return distance_m;
        }

        public void adjust(float adjustFactor) {
            distance_m *= adjustFactor;
            time_ms *= adjustFactor;
        }

        /**
         * @return speed of the interval in m/s.
         */
        public float getSpeed_ms() {
            if (distance_m == 0f) {
                return 0f;
            }
            return distance_m / (float) (time_ms * UnitConversions.MS_TO_S);
        }
    }
}
