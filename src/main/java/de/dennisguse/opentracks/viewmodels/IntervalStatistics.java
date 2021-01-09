package de.dennisguse.opentracks.viewmodels;

import androidx.annotation.NonNull;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import de.dennisguse.opentracks.content.data.TrackPoint;
import de.dennisguse.opentracks.util.UnitConversions;

public class IntervalStatistics {
    private final List<Interval> intervalList = new ArrayList<>();
    private final float distanceInterval_m;

    /**
     * @param trackPoints        the list of TrackPoint.
     * @param distanceInterval_m the meters of every interval.
     */
    public IntervalStatistics(@NonNull List<TrackPoint> trackPoints, float distanceInterval_m) {
        intervalList.clear();
        this.distanceInterval_m = distanceInterval_m;

        if (trackPoints.size() == 0) {
            return;
        }

        Interval interval = new Interval();
        interval.gain_m += trackPoints.get(0).hasElevationGain() ? trackPoints.get(0).getElevationGain() : 0;
        interval.loss_m += trackPoints.get(0).hasElevationLoss() ? trackPoints.get(0).getElevationLoss() : 0;
        for (int i = 1; i < trackPoints.size(); i++) {
            TrackPoint prevTrackPoint = trackPoints.get(i - 1);
            TrackPoint trackPoint = trackPoints.get(i);

            if (trackPoint.hasLocation() && prevTrackPoint.hasLocation()) {
                interval.distance_m += prevTrackPoint.distanceTo(trackPoint);
                interval.time = interval.time.plus(Duration.between(prevTrackPoint.getTime(), trackPoint.getTime()));
                interval.gain_m += trackPoint.hasElevationGain() ? trackPoint.getElevationGain() : 0;
                interval.loss_m += trackPoint.hasElevationLoss() ? trackPoint.getElevationLoss() : 0;

                if (interval.distance_m >= distanceInterval_m) {
                    float adjustFactor = distanceInterval_m / interval.distance_m;
                    Interval adjustedInterval = new Interval(interval);
                    adjustedInterval.adjust(adjustFactor);

                    intervalList.add(adjustedInterval);

                    interval = new Interval(interval.distance_m - adjustedInterval.distance_m, interval.time.minus(adjustedInterval.time));
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

    /**
     * Return the last completed interval.
     * An interval is complete if its distance is equal to distanceInterval_m.
     *
     * @return the interval object or null if any interval is completed.
     */
    public Interval getLastInterval() {
        if (intervalList.size() == 1 && intervalList.get(0).getDistance_m() < distanceInterval_m) {
            return null;
        }

        for (int i = intervalList.size() - 1; i >= 0; i--) {
            if (intervalList.get(i).getDistance_m() >= distanceInterval_m) {
                return this.intervalList.get(i);
            }
        }

        return null;
    }

    public static class Interval {
        private float distance_m = 0f;
        private Duration time = Duration.ofSeconds(0);
        private float gain_m = 0f;
        private float loss_m = 0f;

        public Interval() {
        }

        public Interval(float distance_m, Duration time) {
            this.distance_m = distance_m;
            this.time = time;
        }

        public Interval(Interval i) {
            distance_m = i.distance_m;
            time = i.time;
            gain_m = i.gain_m;
            loss_m = i.loss_m;
        }

        public float getDistance_m() {
            return distance_m;
        }

        public void adjust(float adjustFactor) {
            distance_m *= adjustFactor;
            time = Duration.ofMillis((long) (time.toMillis() * adjustFactor));
        }

        /**
         * @return speed of the interval in m/s.
         */
        public float getSpeed_ms() {
            if (distance_m == 0f) {
                return 0f;
            }
            return (distance_m / (time.toMillis() * (float) UnitConversions.MS_TO_S));
        }

        public float getGain_m() {
            return gain_m;
        }

        public float getLoss_m() {
            return loss_m;
        }
    }
}
