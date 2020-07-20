package de.dennisguse.opentracks.viewmodels;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.stats.TrackStatistics;

public class AggregatedStatistics {

    private final Map<String, AggregatedStatistic> aggregatedStatistics = new HashMap<>();

    public void aggregate(List<Track> tracks) {
        for (Track track : tracks) {
            aggregate(track);
        }
    }

    public void aggregate(Track track) {
        String category = track.getCategory();
        if (aggregatedStatistics.containsKey(category)) {
            aggregatedStatistics.get(category).add(track.getTrackStatistics());
        } else {
            aggregatedStatistics.put(category, new AggregatedStatistic(track.getTrackStatistics()));
        }
    }

    public int getCount() {
        return aggregatedStatistics.size();
    }

    public String getSportName(int position) {
        return getKey(position);
    }

    public AggregatedStatistic get(String category) {
        return aggregatedStatistics.get(category);
    }

    private String getKey(int position) {
        return (new ArrayList<>(aggregatedStatistics.keySet())).get(position);
    }

    public AggregatedStatistic getItem(int position) {
        return (new ArrayList<>(aggregatedStatistics.values())).get(position);
    }

    public static class AggregatedStatistic {
        private final TrackStatistics trackStatistics;
        private int countTracks;

        public AggregatedStatistic(TrackStatistics trackStatistics) {
            this.trackStatistics = trackStatistics;
            this.countTracks = 1;
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
