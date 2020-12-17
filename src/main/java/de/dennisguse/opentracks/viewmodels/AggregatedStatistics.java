package de.dennisguse.opentracks.viewmodels;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.stats.TrackStatistics;

public class AggregatedStatistics {

    private final Map<String, AggregatedStatistic> dataMap = new HashMap<>();

    private final List<AggregatedStatistic> dataList = new ArrayList<>();

    public AggregatedStatistics(@NonNull List<Track> tracks) {
        for (Track track : tracks) {
            aggregate(track);
        }

        dataList.addAll(dataMap.values());
        Collections.sort(dataList, (o1, o2) -> {
            if (o1.getCountTracks() == o2.getCountTracks()) {
                return o1.getCategory().compareTo(o2.getCategory());
            }
            return (o1.getCountTracks() < o2.getCountTracks() ? 1 : -1);
        });
    }

    @VisibleForTesting
    public void aggregate(@NonNull Track track) {
        String category = track.getCategory();
        if (dataMap.containsKey(category)) {
            dataMap.get(category).add(track.getTrackStatistics());
        } else {
            dataMap.put(category, new AggregatedStatistic(category, track.getTrackStatistics()));
        }
    }

    public int getCount() {
        return dataMap.size();
    }

    public AggregatedStatistic get(String category) {
        return dataMap.get(category);
    }

    public AggregatedStatistic getItem(int position) {
        return dataList.get(position);
    }

    public static class AggregatedStatistic {
        private final String category;
        private final TrackStatistics trackStatistics;
        private int countTracks = 1;

        public AggregatedStatistic(String category, TrackStatistics trackStatistics) {
            this.category = category;
            this.trackStatistics = trackStatistics;
        }

        public String getCategory() {
            return category;
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
