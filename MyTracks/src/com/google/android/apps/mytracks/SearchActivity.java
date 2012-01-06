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

import com.google.android.apps.mytracks.content.SearchEngine;
import com.google.android.apps.mytracks.content.SearchEngine.ScoredResult;
import com.google.android.apps.mytracks.content.SearchEngine.SearchQuery;
import com.google.android.apps.mytracks.content.SearchEngineProvider;
import com.google.android.apps.mytracks.content.Track;
import com.google.android.apps.mytracks.content.TracksColumns;
import com.google.android.apps.mytracks.content.Waypoint;
import com.google.android.apps.mytracks.content.WaypointsColumns;
import com.google.android.maps.mytracks.R;

import android.app.ListActivity;
import android.app.SearchManager;
import android.content.ContentUris;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.SearchRecentSuggestions;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

/**
 * Activity to search for tracks or waypoints.
 *
 * @author Rodrigo Damazio
 */
public class SearchActivity extends ListActivity {

  private static final String EXTRA_CURRENT_TRACK_ID = "trackId";

  private static final boolean LOG_SCORES = true;

  private SearchEngine engine;
  private LocationManager locationManager;

  private SearchRecentSuggestions suggestions;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    engine = new SearchEngine(this);
    suggestions = SearchEngineProvider.newHelper(this);
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
    SortedSet<ScoredResult> scoredResults = engine.doSearch(query);

    final List<? extends Map<String, ?>> displayResults = prepareResultsforDisplay(scoredResults);

    if (LOG_SCORES) {
      Log.i(TAG, "Search scores: " + displayResults);
    }

    // Then go back to the UI thread to display them.
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        showSearchResults(displayResults);
      }
    });

    // Save the query as a suggestion for the future.
    suggestions.saveRecentQuery(query.textQuery, null);
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
}
