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
package com.google.android.apps.mytracks.content;

import com.google.android.apps.mytracks.stats.TripStatistics;

import android.content.Context;
import android.database.Cursor;
import android.location.Location;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;

public class SearchEngine {

  private static final double TRACK_CATEGORY_PROMOTION = 2.0;

  private static final double TRACK_DESCRIPTION_PROMOTION = 8.0;

  private static final double TRACK_NAME_PROMOTION = 16.0;

  private static final double CURRENT_TRACK_WAYPOINT_PROMOTION = 2.0;

  private static final double CURRENT_TRACK_DEMOTION = 0.5;

  /** Maximum number of waypoints which will be retrieved and scored. */
  private static final int MAX_SCORED_WAYPOINTS = 100;

  public static class SearchQuery {
    public SearchQuery(String textQuery, Location currentLocation, long currentTrackId,
        long currentTimestamp) {
      this.textQuery = textQuery;
      this.currentLocation = currentLocation;
      this.currentTrackId = currentTrackId;
      this.currentTimestamp = currentTimestamp;
    }

    final String textQuery;
    final Location currentLocation;
    final long currentTrackId;
    final long currentTimestamp;
  }

  public static class ScoredResult {
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

    final Track track;
    final Waypoint waypoint;
    final double score;
  }

  private static final Comparator<ScoredResult> SCORED_RESULT_COMPARATOR =
      new Comparator<ScoredResult>() {
        @Override
        public int compare(ScoredResult r1, ScoredResult r2) {
          // Score ordering.
          int scoreDiff = (int) (r2.score - r1.score);
          if (scoreDiff != 0) {
            return scoreDiff;
          }

          // Arbitrary ordering, by ID.
          long id1 = r1.track != null ? r1.track.getId() : r1.waypoint.getId();
          long id2 = r2.track != null ? r2.track.getId() : r2.waypoint.getId();
          return (int) (id2 - id1);
        }
      };

  private final MyTracksProviderUtils providerUtils;

  public SearchEngine(Context ctx) {
    providerUtils = MyTracksProviderUtils.Factory.get(ctx);
  }

  public SortedSet<ScoredResult> doSearch(SearchQuery query) {
    ArrayList<Track> tracks = new ArrayList<Track>();
    ArrayList<Waypoint> waypoints = new ArrayList<Waypoint>();
    TreeSet<ScoredResult> scoredResults = new TreeSet<ScoredResult>(SCORED_RESULT_COMPARATOR);

    retrieveTracks(query, tracks);
    retrieveWaypoints(query, waypoints);

    scoreTrackResults(tracks, query, scoredResults);
    scoreWaypointResults(waypoints, query, scoredResults);

    return scoredResults;
  }

  private void retrieveTracks(SearchQuery query, ArrayList<Track> tracks) {
    // TODO: Use a different sort order as a ranking function.
    String queryLikeSelection = "%" + query.textQuery + "%";
    String trackSelection =
        TracksColumns.NAME + " LIKE ? OR " +
        TracksColumns.DESCRIPTION + " LIKE ? OR " +
        TracksColumns.CATEGORY + " LIKE ?";
    String[] trackSelectionArgs = new String[] {
        queryLikeSelection,
        queryLikeSelection,
        queryLikeSelection };

    // TODO: Limit number of results to be scored.
    Cursor tracksCursor = providerUtils.getTracksCursor(trackSelection, trackSelectionArgs);
    if (tracksCursor != null) {
      try {
        tracks.ensureCapacity(tracksCursor.getCount());

        while (tracksCursor.moveToNext()) {
          tracks.add(providerUtils.createTrack(tracksCursor));
        }
      } finally {
        tracksCursor.close();
      }
    }
  }

  private void retrieveWaypoints(SearchQuery query, ArrayList<Waypoint> waypoints) {
    // TODO: Use a different sort order as a ranking function.
    String queryLikeSelection2 = "%" + query.textQuery + "%";
    String waypointSelection =
        WaypointsColumns.NAME + " LIKE ? OR " +
        WaypointsColumns.DESCRIPTION + " LIKE ? OR " +
        WaypointsColumns.CATEGORY + " LIKE ?";
    String[] waypointSelectionArgs = new String[] {
        queryLikeSelection2,
        queryLikeSelection2,
        queryLikeSelection2 };
    Cursor waypointsCursor = providerUtils.getWaypointsCursor(waypointSelection, waypointSelectionArgs, MAX_SCORED_WAYPOINTS);
    if (waypointsCursor != null) {
      try {
        waypoints.ensureCapacity(waypointsCursor.getCount());

        while (waypointsCursor.moveToNext()) {
          waypoints.add(providerUtils.createWaypoint(waypointsCursor));
        }
      } finally {
        waypointsCursor.close();
      }
    }
  }

  private void scoreTrackResults(Collection<Track> tracks, SearchQuery query, Collection<ScoredResult> output) {
    for (Track track : tracks) {
      // Calculate the score.
      double score = scoreTrackResult(query, track);

      // Add to the output.
      output.add(new ScoredResult(track, score));
    }
  }

  private double scoreTrackResult(SearchQuery query, Track track) {
    double score = 1.0;

    score *= getTitleBoost(query, track.getName(), track.getDescription(), track.getCategory());

    TripStatistics statistics = track.getStatistics();
    double meanLatitude = (statistics.getTop() + statistics.getBottom()) / 2000000.0;
    double meanLongitude = (statistics.getRight() + statistics.getLeft()) / 2000000.0;
    score *= getDistanceBoost(query, meanLatitude, meanLongitude);

    long meanTimestamp = (statistics.getStartTime() + statistics.getStopTime()) / 2L;
    score *= getTimeBoost(query, meanTimestamp);

    // Score the currently-selected track lower (user is already there, wouldn't be searching for it).
    if (track.getId() == query.currentTrackId) {
      score *= CURRENT_TRACK_DEMOTION;
    }

    return score;
  }

  private void scoreWaypointResults(Collection<Waypoint> waypoints, SearchQuery query, Collection<ScoredResult> output) {
    for (Waypoint waypoint : waypoints) {
      // Calculate the score.
      double score = scoreWaypointResult(query, waypoint);

      // Add to the output.
      output.add(new ScoredResult(waypoint, score));
    }
  }

  private double scoreWaypointResult(SearchQuery query, Waypoint waypoint) {
    double score = 1.0;

    Location location = waypoint.getLocation();
    score *= getTitleBoost(query, waypoint.getName(), waypoint.getDescription(), waypoint.getCategory());
    score *= getDistanceBoost(query, location.getLatitude(), location.getLongitude());
    score *= getTimeBoost(query, location.getTime());

    // Score waypoints in the currently-selected track higher (searching inside the current track).
    if (waypoint.getTrackId() == query.currentTrackId) {
      score *= CURRENT_TRACK_WAYPOINT_PROMOTION;
    }

    return score;
  }

  private double getTitleBoost(SearchQuery query,
      String name, String description, String category) {
    // Title boost: track name > description > category.
    double boost = 1.0;
    if (name.toLowerCase().contains(query.textQuery)) {
      boost *= TRACK_NAME_PROMOTION;
    }
    if (description.toLowerCase().contains(query.textQuery)) {
      boost *= TRACK_DESCRIPTION_PROMOTION;
    }
    if (category.toLowerCase().contains(query.textQuery)) {
      boost *= TRACK_CATEGORY_PROMOTION;
    }
    return boost;
  }

  private double getTimeBoost(SearchQuery query, long timestamp) {
    // Score recent tracks higher.
    long timeAgoHours = (query.currentTimestamp - timestamp) / (60L * 60L);
    if (timeAgoHours > 0L) {
      return squash(timeAgoHours);
    } else {
      // Should rarely happen (track recorded in the last hour).
      return Double.POSITIVE_INFINITY;
    }
  }

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
    double distanceKm = distanceResults[0] / 1000.0;
    if (distanceKm > 0.0) {
      // Use the inverse of the amortized distance.
      return squash(distanceKm);
    } else {
      // Should rarely happen (distance is exactly 0).
      return Double.POSITIVE_INFINITY;
    }
  }

  private static double squash(double timeAgoHours) {
    return 1.0 / Math.log1p(timeAgoHours);
  }
}
