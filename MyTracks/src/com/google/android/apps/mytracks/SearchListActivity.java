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

import com.google.android.apps.mytracks.content.MyTracksProviderUtils;
import com.google.android.apps.mytracks.content.SearchEngine;
import com.google.android.apps.mytracks.content.SearchEngine.ScoredResult;
import com.google.android.apps.mytracks.content.SearchEngine.SearchQuery;
import com.google.android.apps.mytracks.content.SearchEngineProvider;
import com.google.android.apps.mytracks.content.Track;
import com.google.android.apps.mytracks.content.Waypoint;
import com.google.android.apps.mytracks.fragments.DeleteOneMarkerDialogFragment;
import com.google.android.apps.mytracks.fragments.DeleteOneTrackDialogFragment;
import com.google.android.apps.mytracks.stats.TripStatistics;
import com.google.android.apps.mytracks.util.ApiAdapterFactory;
import com.google.android.apps.mytracks.util.IntentUtils;
import com.google.android.apps.mytracks.util.ListItemUtils;
import com.google.android.apps.mytracks.util.PreferencesUtils;
import com.google.android.apps.mytracks.util.StringUtils;
import com.google.android.maps.mytracks.R;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.SearchRecentSuggestions;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

/**
 * An activity to display a list of search results.
 * 
 * @author Rodrigo Damazio
 */
public class SearchListActivity extends AbstractMyTracksActivity {

  private static final String TAG = SearchListActivity.class.getSimpleName();

  private static final String NAME_FIELD = "name";
  private static final String ICON_FIELD = "icon";
  private static final String CATEGORY_FIELD = "category";
  private static final String TOTAL_TIME_FIELD = "totalTime";
  private static final String TOTAL_DISTANCE_FIELD = "totalDistance";
  private static final String START_TIME_FIELD = "startTime";
  private static final String DESCRIPTION_FIELD = "description";
  private static final String TRACK_ID_FIELD = "trackId";
  private static final String MARKER_ID_FIELD = "markerId";

  private final OnSharedPreferenceChangeListener
      sharedPreferenceChangeListener = new OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences preferences, String key) {
          if (PreferencesUtils.getKey(SearchListActivity.this, R.string.recording_track_id_key).equals(key)) {
            recordingTrackId = PreferencesUtils.getLong(SearchListActivity.this, R.string.recording_track_id_key);
            arrayAdapter.notifyDataSetChanged();
          }
        }
      };

  // Callback when an item is selected in the contextual action mode
  private ContextualActionModeCallback
      contextualActionModeCallback = new ContextualActionModeCallback() {
        @Override
        public boolean onClick(int itemId, int position, long id) {
          return handleContextItem(itemId, position);
        }
      };

  private MyTracksProviderUtils myTracksProviderUtils;
  private SearchEngine searchEngine;
  private SearchRecentSuggestions searchRecentSuggestions;
  private LocationManager locationManager;
  private long recordingTrackId;
  private boolean metricUnits;
  private ArrayAdapter<Map<String, Object>> arrayAdapter;

  // UI elements
  private ListView listView;
  private MenuItem searchMenuItem;
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL);
    setContentView(R.layout.search_list);

    myTracksProviderUtils = MyTracksProviderUtils.Factory.get(this);
    searchEngine = new SearchEngine(myTracksProviderUtils);
    searchRecentSuggestions = SearchEngineProvider.newHelper(this);
    locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
    SharedPreferences sharedPreferences = getSharedPreferences(
        Constants.SETTINGS_NAME, Context.MODE_PRIVATE);
    sharedPreferences.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
    recordingTrackId = PreferencesUtils.getLong(this, R.string.recording_track_id_key);

    listView = (ListView) findViewById(R.id.search_list);
    listView.setEmptyView(findViewById(R.id.search_list_empty));
    listView.setOnItemClickListener(new OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Map<String, Object> item = arrayAdapter.getItem(position);
        Long trackId = (Long) item.get(TRACK_ID_FIELD);
        Long markerId = (Long) item.get(MARKER_ID_FIELD);
        Intent intent;
        if (markerId != null) {
          intent = IntentUtils.newIntent(SearchListActivity.this, TrackDetailActivity.class)
              .putExtra(TrackDetailActivity.EXTRA_MARKER_ID, markerId);
        } else {
          intent = IntentUtils.newIntent(SearchListActivity.this, TrackDetailActivity.class)
              .putExtra(TrackDetailActivity.EXTRA_TRACK_ID, trackId);
        }
        startActivity(intent);
      }
    });
    arrayAdapter = new ArrayAdapter<Map<String, Object>>(
        this, R.layout.list_item, R.id.list_item_name) {
      @Override
      public View getView(int position, View convertView, android.view.ViewGroup parent) {
        View view;
        if (convertView == null) {
          view = getLayoutInflater().inflate(R.layout.list_item, parent, false);
        } else {
          view = convertView;
        }
        Map<String, Object> resultMap = getItem(position);
        String name = (String) resultMap.get(NAME_FIELD);
        int iconId = (Integer) resultMap.get(ICON_FIELD);
        String category = (String) resultMap.get(CATEGORY_FIELD);
        String totalTime = (String) resultMap.get(TOTAL_TIME_FIELD);
        String totalDistance = (String) resultMap.get(TOTAL_DISTANCE_FIELD);
        String startTime = (String) resultMap.get(START_TIME_FIELD);
        String description = (String) resultMap.get(DESCRIPTION_FIELD);
        ListItemUtils.setListItem(view,
            name,
            iconId,
            category,
            totalTime,
            totalDistance,
            startTime,
            description);
        return view;
      }
    };
    listView.setAdapter(arrayAdapter);
    ApiAdapterFactory.getApiAdapter().configureListViewContextualMenu(this, listView,
        R.menu.list_context_menu, R.id.list_item_name, contextualActionModeCallback);
    handleIntent(getIntent());
  }

  @Override
  protected void onResume() {
    super.onResume();
    metricUnits = PreferencesUtils.getBoolean(this, R.string.metric_units_key, true);
  }

  @Override
  public void onNewIntent(Intent intent) {
    setIntent(intent);
    handleIntent(intent);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.search_list, menu);
    searchMenuItem = menu.findItem(R.id.search_list_search);
    ApiAdapterFactory.getApiAdapter().configureSearchWidget(this, searchMenuItem);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.search_list_search:
        return ApiAdapterFactory.getApiAdapter().handleSearchMenuSelection(this);
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  @Override
  public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
    super.onCreateContextMenu(menu, v, menuInfo);
    getMenuInflater().inflate(R.menu.list_context_menu, menu);
  }

  @Override
  public boolean onContextItemSelected(MenuItem item) {
    AdapterContextMenuInfo adapterContextMenuInfo = (AdapterContextMenuInfo) item.getMenuInfo();
    if (handleContextItem(item.getItemId(), adapterContextMenuInfo.position)) {
      return true;
     }
    return super.onContextItemSelected(item);
  }

  @Override
  public boolean onKeyUp(int keyCode, KeyEvent event) {
    if (keyCode == KeyEvent.KEYCODE_SEARCH) {
      if (ApiAdapterFactory.getApiAdapter().handleSearchKey(searchMenuItem)) {
        return true;
      }
    }
    return super.onKeyUp(keyCode, event);
  }

  /**
   * Handles a context item selection.
   * 
   * @param itemId the menu item id
   * @param position the position of the selected row
   * @return true if handled.
   */
  private boolean handleContextItem(int itemId, int position) {
    Map<String, Object> item = arrayAdapter.getItem(position);
    Long trackId = (Long) item.get(TRACK_ID_FIELD);
    Long markerId = (Long) item.get(MARKER_ID_FIELD);
    Intent intent;
    switch (itemId) {
      case R.id.list_context_menu_show_on_map:
        if (markerId != null) {
          intent = IntentUtils.newIntent(this, TrackDetailActivity.class)
              .putExtra(TrackDetailActivity.EXTRA_MARKER_ID, markerId);
        } else {
          intent = IntentUtils.newIntent(this, TrackDetailActivity.class)
              .putExtra(TrackDetailActivity.EXTRA_TRACK_ID, trackId);
        }
        startActivity(intent);
        return true;
      case R.id.list_context_menu_edit:
        if (markerId != null) {
          intent = IntentUtils.newIntent(this, MarkerEditActivity.class)
              .putExtra(MarkerEditActivity.EXTRA_MARKER_ID, markerId);
        } else {
          intent = IntentUtils.newIntent(this, TrackEditActivity.class)
              .putExtra(TrackEditActivity.EXTRA_TRACK_ID, trackId);
        }
        startActivity(intent);
        // Close the search result since its content can change after edit.
        finish();
        return true;
      case R.id.list_context_menu_delete:
        if (markerId != null) {
          DeleteOneMarkerDialogFragment.newInstance(markerId, trackId).show(
              getSupportFragmentManager(),
              DeleteOneMarkerDialogFragment.DELETE_ONE_MARKER_DIALOG_TAG);
        } else {
          DeleteOneTrackDialogFragment.newInstance(trackId).show(getSupportFragmentManager(),
              DeleteOneTrackDialogFragment.DELETE_ONE_TRACK_DIALOG_TAG);
        }
        return true;
      default:
        return false;
    }
  }

  /**
   * Handles the intent.
   * 
   * @param intent the intent
   */
  private void handleIntent(Intent intent) {
    if (!Intent.ACTION_SEARCH.equals(intent.getAction())) {
      Log.e(TAG, "Invalid intent action: " + intent);
      finish();
      return;
    }

    String textQuery = intent.getStringExtra(SearchManager.QUERY);
    setTitle(textQuery);

    Location currentLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
    final SearchQuery query = new SearchQuery(textQuery, currentLocation, -1L, System
        .currentTimeMillis());
    new Thread() {
        @Override
      public void run() {
        doSearch(query);
      }
    }.start();
  }

  /**
   * Do the search.
   * 
   * @param query the query
   */
  private void doSearch(SearchQuery query) {
    SortedSet<ScoredResult> scoredResults = searchEngine.search(query);
    final List<Map<String, Object>> displayResults = prepareResultsforDisplay(scoredResults);

    // Use the UI thread to display the results
    runOnUiThread(new Runnable() {
        @Override
      public void run() {
        arrayAdapter.clear();
        ApiAdapterFactory.getApiAdapter().addAllToArrayAdapter(arrayAdapter, displayResults);
      }
    });

    // Save the query as a suggestion for the future
    searchRecentSuggestions.saveRecentQuery(query.textQuery, null);
  }

  /**
   * Prepares the result for display.
   * 
   * @param scoredResults a list of score results
   * @return a list of result maps
   */
  private List<Map<String, Object>> prepareResultsforDisplay(
      Collection<ScoredResult> scoredResults) {
    ArrayList<Map<String, Object>> output = new ArrayList<Map<String, Object>>(scoredResults
        .size());
    for (ScoredResult result : scoredResults) {
      Map<String, Object> resultMap = new HashMap<String, Object>();
      if (result.track != null) {
        prepareTrackForDisplay(result.track, resultMap);
      } else {
        prepareMarkerForDisplay(result.waypoint, resultMap);
      }
      output.add(resultMap);
    }
    return output;
  }

  /**
   * Prepares a marker for display by filling in a result map.
   * 
   * @param waypoint the marker
   * @param resultMap the result map
   */
  private void prepareMarkerForDisplay(Waypoint waypoint, Map<String, Object> resultMap) {
    /*
     * TODO: It may be more appropriate to obtain the track name as a join in
     * the retrieval phase of the search.
     */
    String trackName = null;
    long trackId = waypoint.getTrackId();
    if (trackId != -1L) {
      Track track = myTracksProviderUtils.getTrack(trackId);
      if (track != null) {
        trackName = track.getName();
      }
    }

    boolean statistics = waypoint.getType() == Waypoint.TYPE_STATISTICS;
    resultMap.put(NAME_FIELD, waypoint.getName());
    resultMap.put(ICON_FIELD, statistics ? R.drawable.yellow_pushpin : R.drawable.blue_pushpin);
    resultMap.put(CATEGORY_FIELD, statistics ? null : waypoint.getCategory());

    long time = waypoint.getLocation().getTime();
    String startTime = StringUtils.formatDateTime(this, time);
    resultMap.put(START_TIME_FIELD, time == 0 || startTime.equals(trackName) ? null : startTime);

    // Display the marker's track name in the total time field
    resultMap.put(TOTAL_TIME_FIELD, trackName == null ? null
        : getString(R.string.search_list_marker_track_location, trackName));
    resultMap.put(TOTAL_DISTANCE_FIELD, null);
    resultMap.put(DESCRIPTION_FIELD, statistics ? null : waypoint.getDescription());
    resultMap.put(TRACK_ID_FIELD, waypoint.getTrackId());
    resultMap.put(MARKER_ID_FIELD, waypoint.getId());
  }

  /**
   * Prepares a track for display by filling in a result map.
   * 
   * @param track the track
   * @param resultMap the result map
   */
  private void prepareTrackForDisplay(Track track, Map<String, Object> resultMap) {
    boolean isRecording = track.getId() == recordingTrackId;
    TripStatistics tripStatitics = track.getStatistics();
    resultMap.put(NAME_FIELD, track.getName());
    resultMap.put(ICON_FIELD, isRecording ? R.drawable.menu_record_track : R.drawable.track);
    resultMap.put(CATEGORY_FIELD, track.getCategory());
    resultMap.put(TOTAL_TIME_FIELD, isRecording ? null : StringUtils.formatElapsedTime(
        tripStatitics.getTotalTime()));
    resultMap.put(TOTAL_DISTANCE_FIELD, isRecording ? null : StringUtils.formatDistance(
        this, tripStatitics.getTotalDistance(), metricUnits));
    String startTime = StringUtils.formatDateTime(this, tripStatitics.getStartTime());
    if (startTime.equals(track.getName())) {
      startTime = null;
    }
    resultMap.put(START_TIME_FIELD, startTime);
    resultMap.put(DESCRIPTION_FIELD, track.getDescription());
    resultMap.put(TRACK_ID_FIELD, track.getId());
    resultMap.put(MARKER_ID_FIELD, null);
  }
}
