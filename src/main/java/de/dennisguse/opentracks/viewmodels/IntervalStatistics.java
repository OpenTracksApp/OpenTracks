package de.dennisguse.opentracks.viewmodels;

import androidx.annotation.Nullable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import de.dennisguse.opentracks.content.data.Distance;
import de.dennisguse.opentracks.content.data.Speed;
import de.dennisguse.opentracks.content.data.TrackPoint;
import de.dennisguse.opentracks.content.provider.TrackPointIterator;
import de.dennisguse.opentracks.stats.TrackStatistics;
import de.dennisguse.opentracks.stats.TrackStatisticsUpdater;

public class IntervalStatistics {
    private TrackStatisticsUpdater trackStatisticsUpdater = new TrackStatisticsUpdater();
    private final List<Interval> intervalList;
    private final Distance distanceInterval;
    private final Distance minGPSDistance;
    private Interval interval, lastInterval;

    /**
     * @param distanceInterval distance of every interval.
     * @param minGPSDistance   the setting value for GPS distance.
     */
    public IntervalStatistics(Distance distanceInterval, Distance minGPSDistance) {
        this.distanceInterval = distanceInterval;
        this.minGPSDistance = minGPSDistance;

        interval = new Interval();
        lastInterval = new Interval();
        intervalList = new ArrayList<>();
        intervalList.add(lastInterval);
    }

    /**
     * Complete intervals with the tracks points from the iterator.
     *
     * @return the last track point's id used to compute the intervals.
     */
    public TrackPoint.Id addTrackPoints(TrackPointIterator trackPointIterator) {
        boolean newIntervalAdded = false;
        TrackPoint trackPoint = null;

        while (trackPointIterator.hasNext()) {
            trackPoint = trackPointIterator.next();
            trackStatisticsUpdater.addTrackPoint(trackPoint, minGPSDistance);

            if (trackStatisticsUpdater.getTrackStatistics().getTotalDistance().plus(interval.distance).greaterOrEqualThan(distanceInterval)) {
                interval.add(trackStatisticsUpdater.getTrackStatistics(), trackPoint);

                double adjustFactor = distanceInterval.dividedBy(interval.distance);
                Interval adjustedInterval = new Interval(interval);
                adjustedInterval.adjust(adjustFactor);

                intervalList.set(intervalList.size() - 1, adjustedInterval);

                interval = new Interval(interval.distance.minus(adjustedInterval.distance), interval.time.minus(adjustedInterval.time));
                trackStatisticsUpdater = new TrackStatisticsUpdater();
                trackStatisticsUpdater.addTrackPoint(trackPoint, minGPSDistance);

                lastInterval = new Interval(interval);
                intervalList.add(lastInterval);

                newIntervalAdded = true;
            }
        }

        if (newIntervalAdded) {
            lastInterval.add(trackStatisticsUpdater.getTrackStatistics(), null);
        } else {
            lastInterval.set(trackStatisticsUpdater.getTrackStatistics());
        }

        return trackPoint != null ? trackPoint.getId() : null;
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
        private Float gain_m;
        private Float loss_m;

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

        private void adjust(double adjustFactor) {
            distance = distance.multipliedBy(adjustFactor);
            time = Duration.ofMillis((long) (time.toMillis() * adjustFactor));
        }

        public Distance getDistance() {
            return distance;
        }

        public Speed getSpeed() {
            return Speed.of(distance, time);
        }

        public boolean hasGain() {
            return gain_m != null;
        }

        public Float getGain_m() {
            return gain_m;
        }

        public boolean hasLoss() {
            return loss_m != null;
        }

        public Float getLoss_m() {
            return loss_m;
        }

        private void add(TrackStatistics trackStatistics, @Nullable TrackPoint lastTrackPoint) {
            distance = distance.plus(trackStatistics.getTotalDistance());
            time = time.plus(trackStatistics.getTotalTime());
            gain_m = trackStatistics.hasTotalAltitudeGain() ? trackStatistics.getTotalAltitudeGain() : gain_m;
            loss_m = trackStatistics.hasTotalAltitudeLoss() ? trackStatistics.getTotalAltitudeLoss() : loss_m;
            if (hasGain() && lastTrackPoint != null && lastTrackPoint.hasAltitudeGain()) {
                gain_m = gain_m - lastTrackPoint.getAltitudeGain();
                loss_m = loss_m - lastTrackPoint.getAltitudeLoss();
            }
        }

        private void set(TrackStatistics trackStatistics) {
            distance = trackStatistics.getTotalDistance();
            time = trackStatistics.getTotalTime();
            gain_m = trackStatistics.hasTotalAltitudeGain() ? trackStatistics.getTotalAltitudeGain() : gain_m;
            loss_m = trackStatistics.hasTotalAltitudeLoss() ? trackStatistics.getTotalAltitudeLoss() : loss_m;
        }
    }
}
