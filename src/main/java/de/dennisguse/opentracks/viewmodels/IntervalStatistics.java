package de.dennisguse.opentracks.viewmodels;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import de.dennisguse.opentracks.content.data.Distance;
import de.dennisguse.opentracks.content.data.Speed;
import de.dennisguse.opentracks.content.data.TrackPoint;
import de.dennisguse.opentracks.stats.TrackStatistics;
import de.dennisguse.opentracks.stats.TrackStatisticsUpdater;

public class IntervalStatistics {
    private final List<Interval> intervalList = new ArrayList<>();
    private final Distance distanceInterval;

    /**
     * @param trackPoints      the list of TrackPoint.
     * @param distanceInterval distance of every interval.
     * @param minGPSDistance   the setting value for GPS distance.
     */
    public IntervalStatistics(@NonNull List<TrackPoint> trackPoints, Distance distanceInterval, Distance minGPSDistance) {
        intervalList.clear();
        this.distanceInterval = distanceInterval;

        if (trackPoints.size() == 0) {
            return;
        }

        TrackStatisticsUpdater trackStatisticsUpdater = new TrackStatisticsUpdater();
        Interval interval = new Interval();

        for (TrackPoint trackPoint : trackPoints) {
            trackStatisticsUpdater.addTrackPoint(trackPoint, minGPSDistance);

            if (trackStatisticsUpdater.getTrackStatistics().getTotalDistance().plus(interval.distance).greaterOrEqualThan(distanceInterval)) {
                interval.update(trackStatisticsUpdater.getTrackStatistics(), trackPoint);

                double adjustFactor = distanceInterval.dividedBy(interval.distance);
                Interval adjustedInterval = new Interval(interval);
                adjustedInterval.adjust(adjustFactor);

                intervalList.add(adjustedInterval);

                interval = new Interval(interval.distance.minus(adjustedInterval.distance), interval.time.minus(adjustedInterval.time));
                trackStatisticsUpdater = new TrackStatisticsUpdater();
                trackStatisticsUpdater.addTrackPoint(trackPoint, minGPSDistance);
            }
        }

        if (trackStatisticsUpdater.getTrackStatistics().getTotalDistance().toM() >= 1d) {
            interval.update(trackStatisticsUpdater.getTrackStatistics(), null);
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
        private float gain_m;
        private float loss_m;

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

        public Interval(Interval i) {
            distance = i.distance;
            time = i.time;
            gain_m = i.gain_m;
            loss_m = i.loss_m;
        }

        public void adjust(double adjustFactor) {
            distance = distance.multipliedBy(adjustFactor);
            time = Duration.ofMillis((long) (time.toMillis() * adjustFactor));
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

        public void update(TrackStatistics trackStatistics, @Nullable TrackPoint lastTrackPoint) {
            distance = distance.plus(trackStatistics.getTotalDistance());
            time = time.plus(trackStatistics.getTotalTime());
            gain_m = trackStatistics.hasTotalAltitudeGain() ? trackStatistics.getTotalAltitudeGain() : gain_m;
            loss_m = trackStatistics.hasTotalAltitudeLoss() ? trackStatistics.getTotalAltitudeLoss() : loss_m;
            if (lastTrackPoint != null) {
                gain_m = lastTrackPoint.hasAltitudeGain() ? gain_m - lastTrackPoint.getAltitudeGain() : gain_m;
                loss_m = lastTrackPoint.hasAltitudeLoss() ? loss_m - lastTrackPoint.getAltitudeLoss() : loss_m;
            }
        }
    }
}
