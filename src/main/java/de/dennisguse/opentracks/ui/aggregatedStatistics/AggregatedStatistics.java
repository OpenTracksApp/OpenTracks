package de.dennisguse.opentracks.ui.aggregatedStatistics;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.dennisguse.opentracks.data.models.Track;
import de.dennisguse.opentracks.stats.TrackStatistics;

public class AggregatedStatistics {

    private final Map<String, AggregatedStatistic> dataMap = new HashMap<>();

    private final List<AggregatedStatistic> dataList = new ArrayList<>();

    public AggregatedStatistics(@NonNull List<Track> tracks) {
        for (Track track : tracks) {
            aggregate(track);
        }

        dataList.addAll(dataMap.values());
        dataList.sort((o1, o2) -> {
            if (o1.getCountTracks() == o2.getCountTracks()) {
                return o1.getActivityType().compareTo(o2.getActivityType());
            }
            return (o1.getCountTracks() < o2.getCountTracks() ? 1 : -1);
        });
    }

    @VisibleForTesting
    public void aggregate(@NonNull Track track) {
        String activityType = track.getActivityType();
        if (dataMap.containsKey(activityType)) {
            dataMap.get(activityType).add(track.getTrackStatistics());
        } else {
            dataMap.put(activityType, new AggregatedStatistic(activityType, track.getTrackStatistics()));
        }
    }

    public int getCount() {
        return dataMap.size();
    }

    public AggregatedStatistic get(String activityType) {
        return dataMap.get(activityType);
    }

    public AggregatedStatistic getItem(int position) {
        return dataList.get(position);
    }

    public static class AggregatedStatistic {
        private final String activityType;
        private final TrackStatistics trackStatistics;
        private int countTracks = 1;

        public AggregatedStatistic(String activityType, TrackStatistics trackStatistics) {
            this.activityType = activityType;
            this.trackStatistics = trackStatistics;
        }

        public String getActivityType() {
            return activityType;
        }

        public TrackStatistics getTrackStatistics() {
            return trackStatistics;
        }

        public int getCountTracks() {
            return countTracks;
        }

        void add(TrackStatistics statistics) {
            trackStatistics.merge(statistics);
            countTracks++;
        }
    }
}
