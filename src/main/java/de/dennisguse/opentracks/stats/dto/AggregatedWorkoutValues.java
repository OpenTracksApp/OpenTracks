package de.dennisguse.opentracks.stats.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import de.dennisguse.opentracks.content.data.Track;

public class AggregatedWorkoutValues {
    private double fastestAverage;
    private double greatestDistance;
    private long fastestAverageDate;
    private long greatestDistanceDate;
    private final ArrayList<DataPointAverageSpeed> averageSpeedValues;
    private final ArrayList<DataPointDistance> distanceValues;

    public AggregatedWorkoutValues(List<Track> tracks) {
        fastestAverage = greatestDistance = 0;
        averageSpeedValues = new ArrayList<>();
        distanceValues = new ArrayList<>();
        for (Track track : tracks) {

            if (fastestAverage < track.getTrackStatistics().getAverageMovingSpeed()) {
                fastestAverage = track.getTrackStatistics().getAverageMovingSpeed();
                fastestAverageDate = track.getTrackStatistics().getStartTime_ms();
            }

            if (greatestDistance < track.getTrackStatistics().getTotalDistance()) {
                greatestDistance = track.getTrackStatistics().getTotalDistance();
                greatestDistanceDate = track.getTrackStatistics().getStartTime_ms();
            }

            float timepoint = TimeUnit.MILLISECONDS.toHours(track.getTrackStatistics().getStopTime_ms());
            averageSpeedValues.add(new DataPointAverageSpeed(timepoint, track.getTrackStatistics().getAverageMovingSpeed()));
            distanceValues.add(new DataPointDistance(timepoint, track.getTrackStatistics().getTotalDistance()));
        }
    }

    public double getFastestAverage() {
        return fastestAverage;
    }

    public double getGreatestDistance() {
        return greatestDistance;
    }

    public long getFastestAverageDate() {
        return fastestAverageDate;
    }

    public long getGreatestDistanceDate() {
        return greatestDistanceDate;
    }

    public ArrayList<DataPointAverageSpeed> getAverageSpeedData() {
        return averageSpeedValues;
    }

    public ArrayList<DataPointDistance> getDistanceData() {
        return distanceValues;
    }
}
