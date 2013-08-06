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
import com.google.android.apps.mytracks.content.Waypoint.WaypointType;
import com.google.android.apps.mytracks.fragments.ConfirmDeleteDialogFragment;
import com.google.android.apps.mytracks.fragments.DeleteMarkerDialogFragment;
import com.google.android.apps.mytracks.fragments.DeleteMarkerDialogFragment.DeleteMarkerCaller;
import com.google.android.apps.mytracks.services.MyTracksLocationManager;
import com.google.android.apps.mytracks.services.TrackRecordingServiceConnection;
import com.google.android.apps.mytracks.stats.TripStatistics;
import com.google.android.apps.mytracks.util.ApiAdapterFactory;
import com.google.android.apps.mytracks.util.IntentUtils;
import com.google.android.apps.mytracks.util.ListItemUtils;
import com.google.android.apps.mytracks.util.PreferencesUtils;
import com.google.android.apps.mytracks.util.StringUtils;
import com.google.android.apps.mytracks.util.TrackIconUtils;
import com.google.android.apps.mytracks.util.TrackRecordingServiceConnectionUtils;
import com.google.android.gms.location.LocationListener;
import com.google.android.maps.mytracks.R;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
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
public class SearchListActivity extends AbstractSendToGoogleActivity implements DeleteMarkerCaller {

  private static final String TAG = SearchListActivity.class.getSimpleName();

  private static final String IS_RECORDING_FIELD = "isRecording";
  private static final String IS_PAUSED_FIELD = "isPaused";
  private static final String ICON_ID_FIELD = "icon";
  private static final String ICON_CONTENT_DESCRIPTION_ID_FIELD = "iconContentDescription";
  private static final String NAME_FIELD = "name";
  private static final String CATEGORY_FIELD = "category";
  private static final String TOTAL_TIME_FIELD = "totalTime";
  private static final String TOTAL_DISTANCE_FIELD = "totalDistance";
  private static final String START_TIME_FIELD = "startTime";
  private static final String DESCRIPTION_FIELD = "description";
  private static final String SHARED_OWNER_FIELD = "sharedOwner";
  private static final String TRACK_ID_FIELD = "trackId";
  private static final String MARKER_ID_FIELD = "markerId";

  private final OnSharedPreferenceChangeListener
      sharedPreferenceChangeListener = new OnSharedPreferenceChangeListener() {
          @Override
        public void onSharedPreferenceChanged(SharedPreferences preferences, String key) {
          if (key == null || key.equals(
              PreferencesUtils.getKey(SearchListActivity.this, R.string.stats_units_key))) {
            metricUnits = PreferencesUtils.isMetricUnits(SearchListActivity.this);
          }
          if (key == null || key.equals(
              PreferencesUtils.getKey(SearchListActivity.this, R.string.recording_track_id_key))) {
            recordingTrackId = PreferencesUtils.getLong(
                SearchListActivity.this, R.string.recording_track_id_key);
          }
          if (key == null || key.equals(PreferencesUtils.getKey(
              SearchListActivity.this, R.string.recording_track_paused_key))) {
            recordingTrackPaused = PreferencesUtils.getBoolean(SearchListActivity.this,
                R.string.recording_track_paused_key,
                PreferencesUtils.RECORDING_TRACK_PAUSED_DEFAULT);
          }
          if (key != null) {
            runOnUiThread(new Runnable() {
                @Override
              public void run() {
                arrayAdapter.notifyDataSetChanged();
              }
            });
          }
        }
      };

  // Callback when an item is selected in the contextual action mode
  private ContextualActionModeCallback
      contextualActionModeCallback = new ContextualActionModeCallback() {
          @Override
        public boolean onClick(int itemId, int[] positions, long[] ids) {
          return handleContextItem(itemId, positions);          
        }
          @Override
        public void onPrepare(Menu menu, int[] positions, long[] ids, boolean showSelectAll) {
          boolean isRecording = recordingTrackId != PreferencesUtils.RECORDING_TRACK_ID_DEFAULT;
          boolean isSingleSelection = positions.length == 1;
          boolean isSingleSelectionShareWithMe;
          boolean isSingleSelectionTrack;
          if (isSingleSelection) {
            Map<String, Object> item = arrayAdapter.getItem(positions[0]);
            Long trackId = (Long) item.get(TRACK_ID_FIELD);
            Track track = myTracksProviderUtils.getTrack(trackId);

            isSingleSelectionShareWithMe = track.isSharedWithMe();
            isSingleSelectionTrack = item.get(MARKER_ID_FIELD) == null;
          } else {
            isSingleSelectionShareWithMe = false;
            isSingleSelectionTrack = false;
          }
          // Not recording, one item, item is a track
          menu.findItem(R.id.list_context_menu_play)
              .setVisible(!isRecording && isSingleSelection && isSingleSelectionTrack);
          // Not recording, one item, item is a track, not shareWithMe item
          menu.findItem(R.id.list_context_menu_share).setVisible(!isRecording && isSingleSelection
              && isSingleSelectionTrack && !isSingleSelectionShareWithMe);
          // One item, item is a marker
          menu.findItem(R.id.list_context_menu_show_on_map)
              .setVisible(isSingleSelection && !isSingleSelectionTrack);
          // One item, can be a track or a marker, cannot be a sharedWithMe item
          menu.findItem(R.id.list_context_menu_edit)
              .setVisible(isSingleSelection && !isSingleSelectionShareWithMe);
          // One item. If track, no restriction. If marker, cannot be a
          // shareWithMe item
          menu.findItem(R.id.list_context_menu_delete).setVisible(
              isSingleSelection && (isSingleSelectionTrack || !isSingleSelectionShareWithMe));
          // Disable select all, no action is available for multiple selection
          menu.findItem(R.id.list_context_menu_select_all).setVisible(false);
        }
      };

  private MyTracksProviderUtils myTracksProviderUtils;
  private SharedPreferences sharedPreferences;
  private TrackRecordingServiceConnection trackRecordingServiceConnection;
  private SearchEngine searchEngine;
  private SearchRecentSuggestions searchRecentSuggestions;
  private ArrayAdapter<Map<String, Object>> arrayAdapter;

  private boolean metricUnits = true;
  private long recordingTrackId = PreferencesUtils.RECORDING_TRACK_ID_DEFAULT;
  private boolean recordingTrackPaused = PreferencesUtils.RECORDING_TRACK_PAUSED_DEFAULT;

  // UI elements
  private ListView listView;
  private MenuItem searchMenuItem;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL);

    myTracksProviderUtils = MyTracksProviderUtils.Factory.get(this);
    sharedPreferences = getSharedPreferences(Constants.SETTINGS_NAME, Context.MODE_PRIVATE);
    trackRecordingServiceConnection = new TrackRecordingServiceConnection(this, null);
    searchEngine = new SearchEngine(myTracksProviderUtils);
    searchRecentSuggestions = SearchEngineProvider.newHelper(this);

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
        boolean isRecording = (Boolean) resultMap.get(IS_RECORDING_FIELD);
        boolean isPaused = (Boolean) resultMap.get(IS_PAUSED_FIELD);
        int iconId = (Integer) resultMap.get(ICON_ID_FIELD);
        int iconContentDescriptionId = (Integer) resultMap.get(ICON_CONTENT_DESCRIPTION_ID_FIELD);
        String name = (String) resultMap.get(NAME_FIELD);
        String category = (String) resultMap.get(CATEGORY_FIELD);
        String totalTime = (String) resultMap.get(TOTAL_TIME_FIELD);
        String totalDistance = (String) resultMap.get(TOTAL_DISTANCE_FIELD);
        Long startTime = (Long) resultMap.get(START_TIME_FIELD);
        String description = (String) resultMap.get(DESCRIPTION_FIELD);
        String sharedOwner = (String) resultMap.get(SHARED_OWNER_FIELD);
        ListItemUtils.setListItem(SearchListActivity.this, view, isRecording, isPaused, iconId,
            iconContentDescriptionId, name, category, totalTime, totalDistance, startTime,
            description, sharedOwner);
        return view;
      }
    };
    listView.setAdapter(arrayAdapter);
    ApiAdapterFactory.getApiAdapter()
        .configureListViewContextualMenu(this, listView, contextualActionModeCallback);
    handleIntent(getIntent());
  }

  @Override
  protected void onStart() {
    super.onStart();
    sharedPreferences.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
    sharedPreferenceChangeListener.onSharedPreferenceChanged(null, null);
    TrackRecordingServiceConnectionUtils.startConnection(this, trackRecordingServiceConnection);
  }

  @Override
  protected void onResume() {
    super.onResume();
    arrayAdapter.notifyDataSetChanged();
  }

  @Override
  protected void onStop() {
    super.onStop();
    sharedPreferences.unregisterOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
    trackRecordingServiceConnection.unbind();
  }

  @Override
  protected int getLayoutResId() {
    return R.layout.search_list;
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
    ApiAdapterFactory.getApiAdapter().configureSearchWidget(this, searchMenuItem, null);
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

    AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
    contextualActionModeCallback.onPrepare(
        menu, new int[] { info.position }, new long[] { info.id }, false);
  }

  @Override
  public boolean onContextItemSelected(MenuItem item) {
    AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
    if (handleContextItem(item.getItemId(), new int[] {info.position})) {
      return true;
    }
    return super.onContextItemSelected(item);
  }

  @Override
  public boolean onKeyUp(int keyCode, KeyEvent event) {
    if (keyCode == KeyEvent.KEYCODE_SEARCH && searchMenuItem != null) {
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
   * @param positions the positions of the selected rows
   * @return true if handled.
   */
  private boolean handleContextItem(int itemId, int[] positions) {
    if (positions.length != 1) {
      return false;
    }
    Map<String, Object> item = arrayAdapter.getItem(positions[0]);
    Long trackId = (Long) item.get(TRACK_ID_FIELD);
    Long markerId = (Long) item.get(MARKER_ID_FIELD);
    Intent intent;
    switch (itemId) {
      case R.id.list_context_menu_play:
        confirmPlay(new long[] {trackId});
        return true;
      case R.id.list_context_menu_share:
        shareTrack(trackId);
        return true;
      case R.id.list_context_menu_show_on_map:
        intent = IntentUtils.newIntent(this, TrackDetailActivity.class)
            .putExtra(TrackDetailActivity.EXTRA_MARKER_ID, markerId);
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
          DeleteMarkerDialogFragment.newInstance(new long[] { markerId }).show(
              getSupportFragmentManager(), DeleteMarkerDialogFragment.DELETE_MARKER_DIALOG_TAG);
        } else {
          ConfirmDeleteDialogFragment.newInstance(new long[] { trackId })
              .show(getSupportFragmentManager(), ConfirmDeleteDialogFragment.CONFIRM_DELETE_DIALOG_TAG);
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

    final String textQuery = intent.getStringExtra(SearchManager.QUERY);
    setTitle(textQuery);

    final MyTracksLocationManager myTracksLocationManager = new MyTracksLocationManager(
        this, Looper.myLooper(), true);
    LocationListener locationListener = new LocationListener() {
        @Override
      public void onLocationChanged(final Location location) {
        myTracksLocationManager.close();
        new Thread() {
            @Override
          public void run() {
            SearchQuery query = new SearchQuery(
                textQuery, location, -1L, System.currentTimeMillis());
            doSearch(query);
          }
        }.start();
      }
    };
    myTracksLocationManager.requestLastLocation(locationListener);
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
    ArrayList<Map<String, Object>> output = new ArrayList<Map<String, Object>>(
        scoredResults.size());
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
    String sharedOwner = null;
    long trackId = waypoint.getTrackId();
    if (trackId != -1L) {
      Track track = myTracksProviderUtils.getTrack(trackId);
      if (track != null) {
        trackName = track.getName();
        sharedOwner = track.getSharedOwner();
      }
    }

    boolean statistics = waypoint.getType() == WaypointType.STATISTICS;

    resultMap.put(IS_RECORDING_FIELD, false);
    resultMap.put(IS_PAUSED_FIELD, true);
    resultMap.put(ICON_ID_FIELD,
        statistics ? R.drawable.ic_marker_yellow_pushpin : R.drawable.ic_marker_blue_pushpin);
    resultMap.put(ICON_CONTENT_DESCRIPTION_ID_FIELD, R.string.icon_marker);
    resultMap.put(NAME_FIELD, waypoint.getName());
    resultMap.put(CATEGORY_FIELD, statistics ? null : waypoint.getCategory());
    // Display the marker's track name in the total time field
    resultMap.put(TOTAL_TIME_FIELD, trackName == null ? null
        : getString(R.string.search_list_marker_track_location, trackName));
    resultMap.put(TOTAL_DISTANCE_FIELD, null);
    resultMap.put(START_TIME_FIELD, waypoint.getLocation().getTime());
    resultMap.put(DESCRIPTION_FIELD, statistics ? null : waypoint.getDescription());
    resultMap.put(SHARED_OWNER_FIELD, sharedOwner);
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
    TripStatistics tripStatitics = track.getTripStatistics();
    String icon = track.getIcon();
    String category = icon != null && !icon.equals("") ? null : track.getCategory();

    resultMap.put(IS_RECORDING_FIELD, track.getId() == recordingTrackId);
    resultMap.put(IS_PAUSED_FIELD, recordingTrackPaused);
    resultMap.put(ICON_ID_FIELD, TrackIconUtils.getIconDrawable(icon));
    resultMap.put(ICON_CONTENT_DESCRIPTION_ID_FIELD, R.string.icon_track);
    resultMap.put(NAME_FIELD, track.getName());
    resultMap.put(CATEGORY_FIELD, category);
    resultMap.put(TOTAL_TIME_FIELD, StringUtils.formatElapsedTime(tripStatitics.getTotalTime()));
    resultMap.put(TOTAL_DISTANCE_FIELD,
        StringUtils.formatDistance(this, tripStatitics.getTotalDistance(), metricUnits));
    resultMap.put(START_TIME_FIELD, tripStatitics.getStartTime());
    resultMap.put(DESCRIPTION_FIELD, track.getDescription());
    resultMap.put(SHARED_OWNER_FIELD, track.getSharedOwner());
    resultMap.put(TRACK_ID_FIELD, track.getId());
    resultMap.put(MARKER_ID_FIELD, null);
  }

  @Override
  public void onDeleteMarkerDone() {
    runOnUiThread(new Runnable() {
        @Override
      public void run() {
        handleIntent(getIntent());
      }
    });
  }
  
  @Override
  protected TrackRecordingServiceConnection getTrackRecordingServiceConnection() {
    return trackRecordingServiceConnection;
  }

  @Override
  protected void onDeleted() {
    runOnUiThread(new Runnable() {
        @Override
      public void run() {
        handleIntent(getIntent());
      }
    });
  }
}
