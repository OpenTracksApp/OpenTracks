/*
 * Copyright 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package de.dennisguse.opentracks.content;

import android.database.Cursor;
import android.location.Location;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.SortedSet;
import java.util.TreeSet;

import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.content.data.TracksColumns;
import de.dennisguse.opentracks.content.data.Waypoint;
import de.dennisguse.opentracks.content.data.WaypointsColumns;
import de.dennisguse.opentracks.content.provider.ContentProviderUtils;
import de.dennisguse.opentracks.stats.TrackStatistics;
import de.dennisguse.opentracks.util.LocationUtils;
import de.dennisguse.opentracks.util.UnitConversions;

/**
 * Engine for searching for tracks and waypoints by text.
 *
 * @author Rodrigo Damazio
 */
public class SearchEngine {

    /**
     * WHERE query to get tracks by name.
     */
    private static final String TRACK_SELECTION_QUERY =
            TracksColumns.NAME + " LIKE ? OR " +
                    TracksColumns.DESCRIPTION + " LIKE ? OR " +
                    TracksColumns.CATEGORY + " LIKE ?";

    /**
     * WHERE query to get waypoints by name.
     */
    private static final String WAYPOINT_SELECTION_QUERY =
            WaypointsColumns.NAME + " LIKE ? OR " +
                    WaypointsColumns.DESCRIPTION + " LIKE ? OR " +
                    WaypointsColumns.CATEGORY + " LIKE ?";

    /**
     * Order of track results.
     */
    private static final String TRACK_SELECTION_ORDER = TracksColumns._ID + " DESC LIMIT 1000";

    /**
     * Order of waypoint results.
     */
    private static final String WAYPOINT_SELECTION_ORDER = WaypointsColumns._ID + " DESC";

    /**
     * How much we promote a match in the track category.
     */
    private static final double TRACK_CATEGORY_PROMOTION = 2.0;

    /**
     * How much we promote a match in the track description.
     */
    private static final double TRACK_DESCRIPTION_PROMOTION = 8.0;

    /**
     * How much we promote a match in the track name.
     */
    private static final double TRACK_NAME_PROMOTION = 16.0;

    /**
     * How much we promote a waypoint result if it's in the currently-selected track.
     */
    private static final double CURRENT_TRACK_WAYPOINT_PROMOTION = 2.0;

    /**
     * How much we promote a track result if it's the currently-selected track.
     */
    private static final double CURRENT_TRACK_DEMOTION = 0.5;

    /**
     * Maximum number of waypoints which will be retrieved and scored.
     */
    private static final int MAX_SCORED_WAYPOINTS = 100;

    /**
     * Oldest timestamp for which we rank based on time (2000-01-01 00:00:00.000)
     */
    public static final long OLDEST_ALLOWED_TIMESTAMP = 946692000000L;

    /**
     * Comparator for scored results.
     */
    private static final Comparator<ScoredResult> SCORED_RESULT_COMPARATOR = (r1, r2) -> {
        // Score ordering.
        int scoreDiff = Double.compare(r2.score, r1.score);
        if (scoreDiff != 0) {
            return scoreDiff;
        }

        // Make tracks come before waypoints.
        if (r1.waypoint != null && r2.track != null) {
            return 1;
        } else if (r1.track != null && r2.waypoint != null) {
            return -1;
        }

        // Finally, use arbitrary ordering, by ID.
        long id1 = r1.track != null ? r1.track.getId().getId() : r1.waypoint.getId().getId();
        long id2 = r2.track != null ? r2.track.getId().getId() : r2.waypoint.getId().getId();
        long idDiff = id2 - id1;
        return Long.signum(idDiff);
    };

    private final ContentProviderUtils providerUtils;

    public SearchEngine(ContentProviderUtils providerUtils) {
        this.providerUtils = providerUtils;
    }

    /**
     * Squashes a number by calculating 1 / log (1 + x).
     */
    //TODO Why is this done?
    private static double squash(double x) {
        return 1.0 / Math.log1p(x);
    }

    /**
     * Executes a searchable query and returns a set of sorted results.
     *
     * @param query the query to execute
     * @return a set of results, sorted according to their score
     */
    public SortedSet<ScoredResult> search(SearchQuery query) {
        TreeSet<ScoredResult> scoredResults = new TreeSet<>(SCORED_RESULT_COMPARATOR);

        scoreTrackResults(retrieveTracks(query), query, scoredResults);
        scoreWaypointResults(retrieveWaypoints(query), query, scoredResults);

        return scoredResults;
    }

    /**
     * Retrieves tracks matching the given query from the database.
     *
     * @param query the query to retrieve for
     */
    private List<Track> retrieveTracks(SearchQuery query) {
        ArrayList<Track> tracks = new ArrayList<>();

        String queryLikeSelection = "%" + query.textQuery + "%";
        String[] trackSelectionArgs = new String[]{
                queryLikeSelection,
                queryLikeSelection,
                queryLikeSelection
        };

        try (Cursor cursor = providerUtils.getTrackCursor(TRACK_SELECTION_QUERY, trackSelectionArgs, TRACK_SELECTION_ORDER)) {
            if (cursor != null) {
                tracks.ensureCapacity(cursor.getCount());
                while (cursor.moveToNext()) {
                    tracks.add(providerUtils.createTrack(cursor));
                }
            }
        }

        return tracks;
    }

    /**
     * Retrieves waypoints matching the given query from the database.
     *
     * @param query the query to retrieve for
     */
    private List<Waypoint> retrieveWaypoints(SearchQuery query) {
        ArrayList<Waypoint> waypoints = new ArrayList<>();

        String queryLikeSelection2 = "%" + query.textQuery + "%";
        String[] waypointSelectionArgs = new String[]{
                queryLikeSelection2,
                queryLikeSelection2,
                queryLikeSelection2
        };

        try (Cursor cursor = providerUtils.getWaypointCursor(WAYPOINT_SELECTION_QUERY, waypointSelectionArgs, WAYPOINT_SELECTION_ORDER, MAX_SCORED_WAYPOINTS)) {
            if (cursor != null) {
                waypoints.ensureCapacity(cursor.getCount());
                while (cursor.moveToNext()) {
                    Waypoint waypoint = providerUtils.createWaypoint(cursor);
                    if (LocationUtils.isValidLocation(waypoint.getLocation())) {
                        waypoints.add(waypoint);
                    }
                }
            }
        }

        return waypoints;
    }

    /**
     * Scores a collection of track results.
     *
     * @param tracks the results to score
     * @param query  the query to score for
     * @param output the collection to fill with scored results
     */
    private void scoreTrackResults(Collection<Track> tracks, SearchQuery query, Collection<ScoredResult> output) {
        for (Track track : tracks) {
            // Calculate the score.
            double score = scoreTrackResult(query, track);

            // Add to the output.
            output.add(new ScoredResult(track, score));
        }
    }

    /**
     * Scores a single track result.
     *
     * @param query the query to score for
     * @param track the results to score
     * @return the score for the track
     */
    private double scoreTrackResult(SearchQuery query, Track track) {
        double score = 1.0;

        score *= getTitleBoost(query, track.getName(), track.getDescription(), track.getCategory());

        TrackStatistics statistics = track.getTrackStatistics();

        long meanTimestamp = (statistics.getStartTime_ms() + statistics.getStopTime_ms()) / 2L;
        score *= getTimeBoost(query, meanTimestamp);

        // Score the currently-selected track lower (user is already there, wouldn't be searching for it).
        if (track.getId() == query.currentTrackId) {
            score *= CURRENT_TRACK_DEMOTION;
        }

        return score;
    }

    /**
     * Scores a collection of waypoint results.
     *
     * @param waypoints the results to score
     * @param query     the query to score for
     * @param output    the collection to fill with scored results
     */
    private void scoreWaypointResults(Collection<Waypoint> waypoints, SearchQuery query, Collection<ScoredResult> output) {
        for (Waypoint waypoint : waypoints) {
            // Calculate the score.
            double score = scoreWaypointResult(query, waypoint);

            // Add to the output.
            output.add(new ScoredResult(waypoint, score));
        }
    }

    /**
     * Scores a single waypoint result.
     *
     * @param query    the query to score for
     * @param waypoint the results to score
     * @return the score for the waypoint
     */
    private double scoreWaypointResult(SearchQuery query, Waypoint waypoint) {
        double score = 1.0;

        Location location = waypoint.getLocation();
        score *= getTitleBoost(query, waypoint.getName(), waypoint.getDescription(), waypoint.getCategory());
        score *= getDistanceBoost(query, location.getLatitude(), location.getLongitude());
        score *= getTimeBoost(query, location.getTime());

        // Score waypoints in the currently-selected track higher (searching inside the current track).
        if (waypoint.getTrackId() != null && waypoint.getTrackId().equals(query.currentTrackId)) {
            score *= CURRENT_TRACK_WAYPOINT_PROMOTION;
        }

        return score;
    }

    /**
     * Calculates the boosting of the score due to the field(s) in which the match occured.
     *
     * @param query       the query to boost for
     * @param name        the name of the track or waypoint
     * @param description the description of the track or waypoint
     * @param category    the category of the track or waypoint
     * @return the total boost to be applied to the result
     */
    private double getTitleBoost(SearchQuery query, String name, String description, String category) {
        // Title boost: track name > description > category.
        double boost = 1.0;
        if (name.toLowerCase(Locale.getDefault()).contains(query.textQuery)) {
            boost *= TRACK_NAME_PROMOTION;
        }
        if (description.toLowerCase(Locale.getDefault()).contains(query.textQuery)) {
            boost *= TRACK_DESCRIPTION_PROMOTION;
        }
        if (category.toLowerCase(Locale.getDefault()).contains(query.textQuery)) {
            boost *= TRACK_CATEGORY_PROMOTION;
        }
        return boost;
    }

    /**
     * Calculates the boosting of the score due to the recency of the matched entity.
     *
     * @param query     the query to boost for
     * @param timestamp the timestamp to calculate the boost for
     * @return the total boost to be applied to the result
     */
    private double getTimeBoost(SearchQuery query, long timestamp) {
        if (timestamp < OLDEST_ALLOWED_TIMESTAMP) {
            // Safety: if timestamp is too old or invalid, don't rank based on time.
            return 1.0;
        }

        // Score recent tracks higher.
        long timeAgoHours = (long) ((query.currentTimestamp - timestamp) * UnitConversions.MS_TO_S * UnitConversions.S_TO_MIN * UnitConversions.MIN_TO_HR);
        if (timeAgoHours > 0L) {
            return squash(timeAgoHours);
        } else {
            // Should rarely happen (track recorded in the last hour).
            return Double.POSITIVE_INFINITY;
        }
    }

    /**
     * Calculates the boosting of the score due to proximity to a location.
     *
     * @param query     the query to boost for
     * @param latitude  the latitude to calculate the boost for
     * @param longitude the longitude to calculate the boost for
     * @return the total boost to be applied to the result
     */
    private double getDistanceBoost(SearchQuery query, double latitude, double longitude) {
        if (query.currentLocation == null) {
            return 1.0;
        }

        float[] distanceResults = new float[1];

        Location.distanceBetween(
                latitude, longitude,
                query.currentLocation.getLatitude(), query.currentLocation.getLongitude(),
                distanceResults);

        // Score tracks close to the current location higher.
        double distanceKm = distanceResults[0] * UnitConversions.M_TO_KM;

        if (distanceKm > 0.0) {
            // Use the inverse of the amortized distance.
            return squash(distanceKm);
        } else {
            // Should rarely happen (distance is exactly 0).
            return Double.POSITIVE_INFINITY;
        }
    }

    /**
     * Description of a searchable query, along with all contextual data needed to execute it.
     */
    public static class SearchQuery {
        final String textQuery;
        final Location currentLocation;
        final Track.Id currentTrackId;
        final long currentTimestamp;

        public SearchQuery(String textQuery, Location currentLocation, Track.Id currentTrackId, long currentTimestamp) {
            this.textQuery = textQuery.toLowerCase(Locale.getDefault());
            this.currentLocation = currentLocation;
            this.currentTrackId = currentTrackId;
            this.currentTimestamp = currentTimestamp;
        }
    }

    /**
     * Description of a searchable result which has been retrieved and scored.
     */
    public static class ScoredResult {
        public final Track track;
        public final Waypoint waypoint;
        final double score;

        ScoredResult(Track track, double score) {
            this.track = track;
            this.waypoint = null;
            this.score = score;
        }

        ScoredResult(Waypoint waypoint, double score) {
            this.track = null;
            this.waypoint = waypoint;
            this.score = score;
        }

        @NonNull
        @Override
        public String toString() {
            return "ScoredResult ["
                    + (track != null ? ("trackId=" + track.getId().getId() + ", ") : "")
                    + (waypoint != null ? ("wptId=" + waypoint.getId().getId() + ", ") : "")
                    + "score=" + score + "]";
        }
    }
}
