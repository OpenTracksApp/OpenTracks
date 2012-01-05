/*
 * Copyright 2011 Google Inc.
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
package com.google.android.apps.mytracks;

import static com.google.android.apps.mytracks.Constants.TAG;

import com.google.android.apps.mytracks.content.MyTracksProviderUtils;
import com.google.android.apps.mytracks.content.Track;
import com.google.android.apps.mytracks.content.TracksColumns;
import com.google.android.apps.mytracks.content.Waypoint;
import com.google.android.apps.mytracks.content.WaypointsColumns;
import com.google.android.apps.mytracks.stats.TripStatistics;
import com.google.android.maps.mytracks.R;

import android.app.ListActivity;
import android.app.SearchManager;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * Activity to search for tracks or waypoints.
 *
 * @author Rodrigo Damazio
 */
public class SearchActivity extends ListActivity {

  private static final double TRACK_CATEGORY_PROMOTION = 2.0;

  private static final double TRACK_DESCRIPTION_PROMOTION = 8.0;

  private static final double TRACK_NAME_PROMOTION = 16.0;

  private static final double CURRENT_TRACK_WAYPOINT_PROMOTION = 2.0;

  private static final double CURRENT_TRACK_DEMOTION = 0.5;

  private static final String EXTRA_CURRENT_TRACK_ID = "trackId";

  /** Maximum number of waypoints which will be retrieved and scored. */
  private static final int MAX_SCORED_WAYPOINTS = 100;

  private static final Comparator<ScoredResult> SCORED_RESULT_COMPARATOR =
      new Comparator<SearchActivity.ScoredResult>() {
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

  private static final boolean LOG_SCORES = true;

  private MyTracksProviderUtils providerUtils;

  private LocationManager locationManager;

  private static class SearchQuery {
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

  private static class ScoredResult {
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

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    providerUtils = MyTracksProviderUtils.Factory.get(this);
    locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

    handleIntent(getIntent());
  }

  @Override
  protected void onNewIntent(Intent intent) {
    setIntent(intent);
    handleIntent(intent);
  }

  private void handleIntent(Intent intent) {
    if (!Intent.ACTION_SEARCH.equals(intent.getAction())) {
      Log.e(TAG, "Got bad search intent: " + intent);
      finish();
      return;
    }

    String textQuery = intent.getStringExtra(SearchManager.QUERY).toLowerCase();
    Location currentLocation = locationManager.getLastKnownLocation("gps");
    long currentTrackId = intent.getLongExtra(EXTRA_CURRENT_TRACK_ID, -1);
    long currentTimestamp = System.currentTimeMillis();

    final SearchQuery query =
        new SearchQuery(textQuery, currentLocation, currentTrackId, currentTimestamp);

    // Do the actual search in a separate thread.
    new Thread() {
      @Override
      public void run() {
        doSearch(query);
      }
    }.start();
  }

  private void doSearch(SearchQuery query) {
    ArrayList<Track> tracks = new ArrayList<Track>();
    ArrayList<Waypoint> waypoints = new ArrayList<Waypoint>();
    TreeSet<ScoredResult> scoredResults = new TreeSet<ScoredResult>(SCORED_RESULT_COMPARATOR);

    retrieveTracks(query, tracks);
    retrieveWaypoints(query, waypoints);

    scoreTrackResults(tracks, query, scoredResults);
    scoreWaypointResults(waypoints, query, scoredResults);

    final List<? extends Map<String, ?>> displayResults = prepareResultsforDisplay(scoredResults);

    if (LOG_SCORES) {
      Log.i(TAG, "Search scores: " + displayResults);
    }

    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        showSearchResults(displayResults);
      }
    });
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

  private List<Map<String, Object>> prepareResultsforDisplay(
      Collection<ScoredResult> scoredResults) {
    ArrayList<Map<String, Object>> output = new ArrayList<Map<String, Object>>(scoredResults.size());
    for (ScoredResult result : scoredResults) {
      Map<String, Object> resultMap = new HashMap<String, Object>();
      if (result.track != null) {
        prepareTrackForDisplay(result.track, resultMap);
      } else {
        prepareWaypointForDisplay(result.waypoint, resultMap);
      }

      resultMap.put("score", result.score);

      output.add(resultMap);
    }

    return output;
  }

  private void prepareWaypointForDisplay(Waypoint waypoint, Map<String, Object> resultMap) {
    // TODO: Yellow pushpin for statistics marker.
    resultMap.put("icon", R.drawable.blue_pushpin);
    resultMap.put("name", waypoint.getName());
    resultMap.put("description", waypoint.getDescription());
    resultMap.put("category", waypoint.getCategory());
    resultMap.put("time", String.format("%tc", waypoint.getLocation().getTime()));
    resultMap.put("trackId", waypoint.getTrackId());
    resultMap.put("waypointId", waypoint.getId());
  }

  private void prepareTrackForDisplay(Track track, Map<String, Object> resultMap) {
    resultMap.put("icon", R.drawable.track);
    resultMap.put("name", track.getName());
    resultMap.put("description", track.getDescription());
    resultMap.put("category", track.getCategory());
    resultMap.put("time", String.format("%tc", track.getStatistics().getStartTime()));
    resultMap.put("trackId", track.getId());
  }

  /**
   * Shows the given search results.
   * Must be run from the UI thread.
   * @param data
   *
   * @param scoredResults the results to show, properly ordered
   */
  private void showSearchResults(List<? extends Map<String, ?>> data) {
    SimpleAdapter adapter = new SimpleAdapter(this, data,
        // TODO: Custom view for search results.
        R.layout.mytracks_list_item,
        new String[] {
          "icon",
          "name",
          "description",
          "category",
          "time"
        },
        new int[] {
          R.id.trackdetails_item_icon,
          R.id.trackdetails_item_name,
          R.id.trackdetails_item_description,
          R.id.trackdetails_item_category,
          R.id.trackdetails_item_time
        });

    setListAdapter(adapter);
  }

  @Override
  protected void onListItemClick(ListView l, View v, int position, long id) {
    @SuppressWarnings("unchecked")
    Map<String, Object> clickedData = (Map<String, Object>) getListAdapter().getItem(position);

    Intent intent = new Intent(Intent.ACTION_VIEW);
    if (clickedData.containsKey("waypointId")) {
      long waypointId = (Long) clickedData.get("waypointId");
      Uri uri = ContentUris.withAppendedId(WaypointsColumns.CONTENT_URI, waypointId);
      intent.setDataAndType(uri, WaypointsColumns.CONTENT_ITEMTYPE);
    } else {
      long trackId = (Long) clickedData.get("trackId");
      Uri uri = ContentUris.withAppendedId(TracksColumns.CONTENT_URI, trackId);
      intent.setDataAndType(uri, TracksColumns.CONTENT_ITEMTYPE);
    }
    startActivity(intent);
  }

  private static double squash(double timeAgoHours) {
    return 1.0 / Math.log1p(timeAgoHours);
  }
}
