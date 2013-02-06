/*
 * Copyright 2009 Google Inc.
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
import com.google.android.apps.mytracks.content.Track;
import com.google.android.apps.mytracks.content.Waypoint.WaypointType;
import com.google.android.apps.mytracks.content.WaypointsColumns;
import com.google.android.apps.mytracks.fragments.DeleteOneMarkerDialogFragment;
import com.google.android.apps.mytracks.fragments.DeleteOneMarkerDialogFragment.DeleteOneMarkerCaller;
import com.google.android.apps.mytracks.util.ApiAdapterFactory;
import com.google.android.apps.mytracks.util.IntentUtils;
import com.google.android.apps.mytracks.util.ListItemUtils;
import com.google.android.apps.mytracks.util.PreferencesUtils;
import com.google.android.apps.mytracks.util.StringUtils;
import com.google.android.maps.mytracks.R;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.ResourceCursorAdapter;
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
import android.widget.ListView;

/**
 * Activity to show a list of markers in a track.
 * 
 * @author Leif Hendrik Wilden
 */
public class MarkerListActivity extends AbstractMyTracksActivity implements DeleteOneMarkerCaller {

  public static final String EXTRA_TRACK_ID = "track_id";

  private static final String TAG = MarkerListActivity.class.getSimpleName();

  private static final String[] PROJECTION = new String[] { WaypointsColumns._ID,
      WaypointsColumns.NAME, WaypointsColumns.DESCRIPTION, WaypointsColumns.CATEGORY,
      WaypointsColumns.TYPE, WaypointsColumns.TIME };

  // Callback when an item is selected in the contextual action mode
  private ContextualActionModeCallback
      contextualActionModeCallback = new ContextualActionModeCallback() {
          @Override
        public boolean onClick(int itemId, int position, long id) {
          return handleContextItem(itemId, id);
        }

          @Override
        public boolean canEdit(int position, long id) {
          Track track = myTracksProviderUtils.getTrack(trackId);
          return !track.isSharedWithMe();
        }

          @Override
        public boolean canDelete(int poistion, long id) {
          Track track = myTracksProviderUtils.getTrack(trackId);
          return !track.isSharedWithMe();
        }
      };

  /*
   * Note that sharedPreferenceChangeListener cannot be an anonymous inner
   * class. Anonymous inner class will get garbage collected.
   */
  private final OnSharedPreferenceChangeListener
      sharedPreferenceChangeListener = new OnSharedPreferenceChangeListener() {
          @Override
        public void onSharedPreferenceChanged(SharedPreferences preferences, String key) {
          // Note that the key can be null
          if (key == null || key.equals(
              PreferencesUtils.getKey(MarkerListActivity.this, R.string.recording_track_id_key))) {
            recordingTrackId = PreferencesUtils.getLong(
                MarkerListActivity.this, R.string.recording_track_id_key);
          }
          if (key == null || key.equals(PreferencesUtils.getKey(
              MarkerListActivity.this, R.string.recording_track_paused_key))) {
            recordingTrackPaused = PreferencesUtils.getBoolean(MarkerListActivity.this,
                R.string.recording_track_paused_key,
                PreferencesUtils.RECORDING_TRACK_PAUSED_DEFAULT);
          }
          if (key != null) {
            runOnUiThread(new Runnable() {
                @Override
              public void run() {
                updateMenu();
              }
            });
          }
        }
      };

  private MyTracksProviderUtils myTracksProviderUtils;
  private SharedPreferences sharedPreferences;

  private long recordingTrackId = PreferencesUtils.RECORDING_TRACK_ID_DEFAULT;
  private boolean recordingTrackPaused = PreferencesUtils.RECORDING_TRACK_PAUSED_DEFAULT;

  private long trackId = -1;
  private ResourceCursorAdapter resourceCursorAdapter;

  // UI elements
  private MenuItem insertMarkerMenuItem;
  private MenuItem searchMenuItem;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    myTracksProviderUtils = MyTracksProviderUtils.Factory.get(this);
    sharedPreferences = getSharedPreferences(Constants.SETTINGS_NAME, Context.MODE_PRIVATE);
    trackId = getIntent().getLongExtra(EXTRA_TRACK_ID, -1L);
    if (trackId == -1L) {
      Log.d(TAG, "invalid track id");
      finish();
      return;
    }

    setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL);

    ListView listView = (ListView) findViewById(R.id.marker_list);
    listView.setEmptyView(findViewById(R.id.marker_list_empty));
    listView.setOnItemClickListener(new OnItemClickListener() {
        @Override
      public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Intent intent = IntentUtils.newIntent(MarkerListActivity.this, MarkerDetailActivity.class)
            .putExtra(MarkerDetailActivity.EXTRA_MARKER_ID, id);
        startActivity(intent);
      }
    });
    resourceCursorAdapter = new ResourceCursorAdapter(this, R.layout.list_item, null, 0) {
        @Override
      public void bindView(View view, Context context, Cursor cursor) {
        int typeIndex = cursor.getColumnIndex(WaypointsColumns.TYPE);
        int nameIndex = cursor.getColumnIndex(WaypointsColumns.NAME);
        int categoryIndex = cursor.getColumnIndex(WaypointsColumns.CATEGORY);
        int timeIndex = cursor.getColumnIndexOrThrow(WaypointsColumns.TIME);
        int descriptionIndex = cursor.getColumnIndex(WaypointsColumns.DESCRIPTION);

        boolean statistics = WaypointType.values()[cursor.getInt(typeIndex)]
            == WaypointType.STATISTICS;
        int iconId = statistics ? R.drawable.yellow_pushpin : R.drawable.blue_pushpin;
        String category = statistics ? null : cursor.getString(categoryIndex);
        String description = statistics ? null : cursor.getString(descriptionIndex);
        long time = cursor.getLong(timeIndex);
        String startTime = time == 0L ? null
            : StringUtils.formatRelativeDateTime(MarkerListActivity.this, time);

        ListItemUtils.setListItem(MarkerListActivity.this, view, false, true, iconId,
            R.string.icon_marker, cursor.getString(nameIndex), category, null, null, startTime,
            description);
      }
    };
    listView.setAdapter(resourceCursorAdapter);
    ApiAdapterFactory.getApiAdapter()
        .configureListViewContextualMenu(this, listView, contextualActionModeCallback);

    final long firstWaypointId = myTracksProviderUtils.getFirstWaypointId(trackId);
    getSupportLoaderManager().initLoader(0, null, new LoaderCallbacks<Cursor>() {
        @Override
      public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
        return new CursorLoader(MarkerListActivity.this, WaypointsColumns.CONTENT_URI, PROJECTION,
            WaypointsColumns.TRACKID + "=? AND " + WaypointsColumns._ID + "!=?",
            new String[] { String.valueOf(trackId), String.valueOf(firstWaypointId) }, null);
      }

        @Override
      public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        resourceCursorAdapter.swapCursor(cursor);
      }

        @Override
      public void onLoaderReset(Loader<Cursor> loader) {
        resourceCursorAdapter.swapCursor(null);
      }
    });
  }

  @Override
  protected void onStart() {
    super.onStart();
    sharedPreferences.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
    sharedPreferenceChangeListener.onSharedPreferenceChanged(null, null);
  }

  @Override
  protected void onResume() {
    super.onResume();
    updateMenu();
  }

  @Override
  protected void onStop() {
    super.onStop();
    sharedPreferences.unregisterOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
  }

  @Override
  protected int getLayoutResId() {
    return R.layout.marker_list;
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.marker_list, menu);
    insertMarkerMenuItem = menu.findItem(R.id.marker_list_insert_marker);
    searchMenuItem = menu.findItem(R.id.marker_list_search);
    ApiAdapterFactory.getApiAdapter().configureSearchWidget(this, searchMenuItem);
    updateMenu();
    return true;
  }

  private void updateMenu() {
    if (insertMarkerMenuItem != null) {
      insertMarkerMenuItem.setVisible(trackId == recordingTrackId && !recordingTrackPaused);
    }
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.marker_list_insert_marker:
        Intent intent = IntentUtils.newIntent(this, MarkerEditActivity.class)
            .putExtra(MarkerEditActivity.EXTRA_TRACK_ID, trackId);
        startActivity(intent);
        return true;
      case R.id.marker_list_search:
        return ApiAdapterFactory.getApiAdapter().handleSearchMenuSelection(this);
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  @Override
  public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
    super.onCreateContextMenu(menu, v, menuInfo);
    getMenuInflater().inflate(R.menu.list_context_menu, menu);
    Track track = myTracksProviderUtils.getTrack(trackId);
    menu.findItem(R.id.list_context_menu_edit).setVisible(!track.isSharedWithMe());
    menu.findItem(R.id.list_context_menu_delete).setVisible(!track.isSharedWithMe());
  }

  @Override
  public boolean onContextItemSelected(MenuItem item) {
    if (handleContextItem(item.getItemId(), ((AdapterContextMenuInfo) item.getMenuInfo()).id)) {
      return true;
    }
    return super.onContextItemSelected(item);
  }

  /**
   * Handles a context item selection.
   * 
   * @param itemId the menu item id
   * @param markerId the marker id
   * @return true if handled.
   */
  private boolean handleContextItem(int itemId, long markerId) {
    Intent intent;
    switch (itemId) {
      case R.id.list_context_menu_show_on_map:
        intent = IntentUtils.newIntent(this, TrackDetailActivity.class)
            .putExtra(TrackDetailActivity.EXTRA_MARKER_ID, markerId);
        startActivity(intent);
        return true;
      case R.id.list_context_menu_edit:
        intent = IntentUtils.newIntent(this, MarkerEditActivity.class)
            .putExtra(MarkerEditActivity.EXTRA_MARKER_ID, markerId);
        startActivity(intent);
        return true;
      case R.id.list_context_menu_delete:
        DeleteOneMarkerDialogFragment.newInstance(markerId, trackId).show(
            getSupportFragmentManager(),
            DeleteOneMarkerDialogFragment.DELETE_ONE_MARKER_DIALOG_TAG);
        return true;
      default:
        return false;
    }
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

  @Override
  public void onMarkerDeleted() {
    // Do nothing
  }
}
