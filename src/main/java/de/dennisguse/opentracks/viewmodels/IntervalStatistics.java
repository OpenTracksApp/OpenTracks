package de.dennisguse.opentracks.viewmodels;

import androidx.annotation.NonNull;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import de.dennisguse.opentracks.content.data.Distance;
import de.dennisguse.opentracks.content.data.Speed;
import de.dennisguse.opentracks.content.data.TrackPoint;

public class IntervalStatistics {
    private final List<Interval> intervalList = new ArrayList<>();
    private final Distance distanceInterval;

    public IntervalStatistics(@NonNull List<TrackPoint> trackPoints, Distance distanceInterval) {
        intervalList.clear();
        this.distanceInterval = distanceInterval;

        if (trackPoints.size() == 0) {
            return;
        }

        Interval interval = new Interval();
        interval.gain_m += trackPoints.get(0).hasAltitudeGain() ? trackPoints.get(0).getAltitudeGain() : 0;
        interval.loss_m += trackPoints.get(0).hasAltitudeLoss() ? trackPoints.get(0).getAltitudeLoss() : 0;
        for (int i = 1; i < trackPoints.size(); i++) {
            TrackPoint prevTrackPoint = trackPoints.get(i - 1);
            TrackPoint trackPoint = trackPoints.get(i);

            if (trackPoint.hasLocation() && prevTrackPoint.hasLocation()) {
                interval.distance = interval.distance.plus(prevTrackPoint.distanceToPrevious(trackPoint));
                interval.time = interval.time.plus(Duration.between(prevTrackPoint.getTime(), trackPoint.getTime()));
                interval.gain_m += trackPoint.hasAltitudeGain() ? trackPoint.getAltitudeGain() : 0;
                interval.loss_m += trackPoint.hasAltitudeLoss() ? trackPoint.getAltitudeLoss() : 0;

                if (interval.distance.greaterThan(distanceInterval)) {
                    Interval adjustedInterval = new Interval(interval, distanceInterval.dividedBy(interval.distance));

                    intervalList.add(adjustedInterval);

                    interval = new Interval(interval.distance.minus(adjustedInterval.distance), interval.time.minus(adjustedInterval.time));
                }
            }
        }

        if (interval.distance.greaterThan(Distance.of(1))) {
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
        if (intervalList.size() == 1 && intervalList.get(0).getDistance().lessThan(distanceInterval)) {
            return null;
        }

        for (int i = intervalList.size() - 1; i >= 0; i--) {
            if (intervalList.get(i).getDistance().greaterOrEqualThan(distanceInterval)) {
                return this.intervalList.get(i);
            }
        }

        return null;
    }

    public static class Interval {
        private Distance distance = Distance.of(0);
        private Duration time = Duration.ofSeconds(0);
        private float gain_m = 0f;
        private float loss_m = 0f;

        public Interval() {
        }

        public Interval(Distance distance, Duration time) {
            this.distance = distance;
            this.time = time;
        }

        public Interval(Interval i, double adjustFactor) {
            distance = i.distance.multipliedBy(adjustFactor);
            time = Duration.ofMillis((long) (i.time.toMillis() * adjustFactor));
            time = i.time;
            gain_m = i.gain_m;
            loss_m = i.loss_m;
        }

        public Distance getDistance() {
            return distance;
        }

        public Speed getSpeed() {
            return Speed.of(distance, time);
        }

        public float getGain_m() {
            return gain_m;
        }

        public float getLoss_m() {
            return loss_m;
        }
    }
}
