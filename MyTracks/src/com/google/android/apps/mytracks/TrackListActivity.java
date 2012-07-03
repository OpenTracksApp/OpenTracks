/*
 * Copyright 2008 Google Inc.
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

import com.google.android.apps.mytracks.content.TracksColumns;
import com.google.android.apps.mytracks.fragments.CheckUnitsDialogFragment;
import com.google.android.apps.mytracks.fragments.DeleteAllTrackDialogFragment;
import com.google.android.apps.mytracks.fragments.DeleteOneTrackDialogFragment;
import com.google.android.apps.mytracks.fragments.DeleteOneTrackDialogFragment.DeleteOneTrackCaller;
import com.google.android.apps.mytracks.fragments.EulaDialogFragment;
import com.google.android.apps.mytracks.fragments.WelcomeDialogFragment;
import com.google.android.apps.mytracks.io.file.SaveActivity;
import com.google.android.apps.mytracks.io.file.TrackWriterFactory.TrackFileFormat;
import com.google.android.apps.mytracks.services.ITrackRecordingService;
import com.google.android.apps.mytracks.services.TrackRecordingServiceConnection;
import com.google.android.apps.mytracks.settings.SettingsActivity;
import com.google.android.apps.mytracks.util.AnalyticsUtils;
import com.google.android.apps.mytracks.util.ApiAdapterFactory;
import com.google.android.apps.mytracks.util.EulaUtils;
import com.google.android.apps.mytracks.util.IntentUtils;
import com.google.android.apps.mytracks.util.ListItemUtils;
import com.google.android.apps.mytracks.util.PreferencesUtils;
import com.google.android.apps.mytracks.util.StringUtils;
import com.google.android.apps.mytracks.util.TrackIconUtils;
import com.google.android.apps.mytracks.util.TrackRecordingServiceConnectionUtils;
import com.google.android.maps.mytracks.R;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Parcelable;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
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
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

/**
 * An activity displaying a list of tracks.
 *
 * @author Leif Hendrik Wilden
 */
public class TrackListActivity extends FragmentActivity implements DeleteOneTrackCaller {

  private static final String TAG = TrackListActivity.class.getSimpleName();

  private static final String[] PROJECTION = new String[] {
      TracksColumns._ID,
      TracksColumns.NAME,
      TracksColumns.DESCRIPTION,
      TracksColumns.CATEGORY,
      TracksColumns.STARTTIME,
      TracksColumns.TOTALDISTANCE,
      TracksColumns.TOTALTIME,
      TracksColumns.ICON};

  // Callback when the trackRecordingServiceConnection binding changes.
  private final Runnable bindChangedCallback = new Runnable() {
    @Override
    public void run() {
      if (!startNewRecording) {
        return;
      }

      ITrackRecordingService service = trackRecordingServiceConnection.getServiceIfBound();
      if (service == null) {
        Log.d(TAG, "service not available to start a new recording");
        return;
      }
      try {
        recordingTrackId = service.startNewTrack();
        startNewRecording = false;
        Intent intent = IntentUtils.newIntent(TrackListActivity.this, TrackDetailActivity.class)
            .putExtra(TrackDetailActivity.EXTRA_TRACK_ID, recordingTrackId);
        startActivity(intent);
        Toast.makeText(
            TrackListActivity.this, R.string.track_list_record_success, Toast.LENGTH_SHORT).show();
      } catch (Exception e) {
        Toast.makeText(TrackListActivity.this, R.string.track_list_record_error, Toast.LENGTH_LONG)
            .show();
        Log.e(TAG, "Unable to start a new recording.", e);
      }
    }
  };

  /*
   * Note that sharedPreferenceChangeListenr cannot be an anonymous inner class.
   * Anonymous inner class will get garbage collected.
   */
  private final OnSharedPreferenceChangeListener
      sharedPreferenceChangeListener = new OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences preferences, String key) {
          boolean updateList = false;
          // Note that key can be null
          if (PreferencesUtils.getKey(TrackListActivity.this, R.string.metric_units_key)
              .equals(key)) {
            metricUnits = PreferencesUtils.getBoolean(TrackListActivity.this,
                R.string.metric_units_key, PreferencesUtils.METRIC_UNITS_DEFAULT);
            updateList = true;
          }
          if (PreferencesUtils.getKey(TrackListActivity.this, R.string.recording_track_id_key)
              .equals(key)) {
            recordingTrackId = PreferencesUtils.getLong(
                TrackListActivity.this, R.string.recording_track_id_key);
            if (TrackRecordingServiceConnectionUtils.isRecording(
                TrackListActivity.this, trackRecordingServiceConnection)) {
              trackRecordingServiceConnection.startAndBind();
            }
            updateMenu();
            updateList = true;
          }
          if (updateList) {
            resourceCursorAdapter.notifyDataSetChanged();
          }
        }
      };

  // Callback when an item is selected in the contextual action mode
  private ContextualActionModeCallback contextualActionModeCallback =
    new ContextualActionModeCallback() {
    @Override
    public boolean onClick(int itemId, int position, long id) {
      return handleContextItem(itemId, id);
    }
  };
  
  private TrackRecordingServiceConnection trackRecordingServiceConnection;
  private boolean metricUnits;
  private long recordingTrackId;
  private ListView listView;
  private ResourceCursorAdapter resourceCursorAdapter;

  // True to start a new recording.
  private boolean startNewRecording = false;

  private MenuItem recordTrackMenuItem;
  private MenuItem stopRecordingMenuItem;
  private MenuItem searchMenuItem;
  private MenuItem importMenuItem;
  private MenuItem saveAllMenuItem;
  private MenuItem deleteAllMenuItem;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setVolumeControlStream(TextToSpeech.Engine.DEFAULT_STREAM);
    setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL);
    setContentView(R.layout.track_list);

    trackRecordingServiceConnection = new TrackRecordingServiceConnection(
        this, bindChangedCallback);

    getSharedPreferences(Constants.SETTINGS_NAME, Context.MODE_PRIVATE)
        .registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
    metricUnits = PreferencesUtils.getBoolean(
        this, R.string.metric_units_key, PreferencesUtils.METRIC_UNITS_DEFAULT);
    recordingTrackId = PreferencesUtils.getLong(this, R.string.recording_track_id_key);

    ImageButton recordImageButton = (ImageButton) findViewById(R.id.track_list_record_button);
    recordImageButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        updateMenuItems(true);
        startRecording();
      }
    });

    listView = (ListView) findViewById(R.id.track_list);
    listView.setEmptyView(findViewById(R.id.track_list_empty));  
    listView.setOnItemClickListener(new OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Intent intent = IntentUtils.newIntent(TrackListActivity.this, TrackDetailActivity.class)
            .putExtra(TrackDetailActivity.EXTRA_TRACK_ID, id);
        startActivity(intent);
      }
    });
    resourceCursorAdapter = new ResourceCursorAdapter(this, R.layout.list_item, null, 0) {
        @Override
      public void bindView(View view, Context context, Cursor cursor) {
        int idIndex = cursor.getColumnIndex(TracksColumns._ID);
        int nameIndex = cursor.getColumnIndex(TracksColumns.NAME);
        int descriptionIndex = cursor.getColumnIndex(TracksColumns.DESCRIPTION);
        int categoryIndex = cursor.getColumnIndex(TracksColumns.CATEGORY);
        int startTimeIndex = cursor.getColumnIndexOrThrow(TracksColumns.STARTTIME);
        int totalDistanceIndex = cursor.getColumnIndexOrThrow(TracksColumns.TOTALDISTANCE);
        int totalTimeIndex = cursor.getColumnIndexOrThrow(TracksColumns.TOTALTIME);
        int iconIndex = cursor.getColumnIndex(TracksColumns.ICON);
        
        boolean isRecording = cursor.getLong(idIndex) == recordingTrackId;
        String name = cursor.getString(nameIndex);
        int iconId = isRecording ? R.drawable.menu_record_track
            : TrackIconUtils.getIconDrawable(cursor.getString(iconIndex));
        String iconContentDescription = getString(isRecording ? R.string.icon_recording
            : R.string.icon_track);
        String category = cursor.getString(categoryIndex);
        String totalTime = isRecording 
            ? null : StringUtils.formatElapsedTime(cursor.getLong(totalTimeIndex));
        String totalDistance = isRecording ? null : StringUtils.formatDistance(
            TrackListActivity.this, cursor.getDouble(totalDistanceIndex), metricUnits);
        long startTime = cursor.getLong(startTimeIndex);
        String description = cursor.getString(descriptionIndex);
        ListItemUtils.setListItem(TrackListActivity.this,
            view,
            name,
            iconId,
            iconContentDescription,
            category,
            totalTime,
            totalDistance,
            startTime,
            description);
      }
    };
    listView.setAdapter(resourceCursorAdapter);
    ApiAdapterFactory.getApiAdapter()
        .configureListViewContextualMenu(this, listView, contextualActionModeCallback);
   
    getSupportLoaderManager().initLoader(0, null, new LoaderCallbacks<Cursor>() {
      @Override
      public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
        return new CursorLoader(TrackListActivity.this,
            TracksColumns.CONTENT_URI,
            PROJECTION,
            null,
            null,
            TracksColumns._ID + " DESC");
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

    showStartupDialogs();
  }

  /**
   * Shows start up dialogs.
   */
  public void showStartupDialogs() {
    if (!EulaUtils.getAcceptEula(this)) {
      Fragment fragment = getSupportFragmentManager()
          .findFragmentByTag(EulaDialogFragment.EULA_DIALOG_TAG);
      if (fragment == null) {
        EulaDialogFragment.newInstance(false)
            .show(getSupportFragmentManager(), EulaDialogFragment.EULA_DIALOG_TAG);
      }
    } else if (EulaUtils.getShowWelcome(this)) {
      Fragment fragment = getSupportFragmentManager()
          .findFragmentByTag(WelcomeDialogFragment.WELCOME_DIALOG_TAG);
      if (fragment == null) {
        new WelcomeDialogFragment().show(
            getSupportFragmentManager(), WelcomeDialogFragment.WELCOME_DIALOG_TAG);
      }
    } else if (EulaUtils.getShowCheckUnits(this)) {
      Fragment fragment = getSupportFragmentManager()
          .findFragmentByTag(CheckUnitsDialogFragment.CHECK_UNITS_DIALOG_TAG);
      if (fragment == null) {
        new CheckUnitsDialogFragment().show(
            getSupportFragmentManager(), CheckUnitsDialogFragment.CHECK_UNITS_DIALOG_TAG);
      }
    } else {
      enableEmptyView();
    }
  }
  
  /**
   * Enables the content of the empty view.
   */
  private void enableEmptyView() {
    View emptyMessage = findViewById(R.id.track_list_empty_message);
    emptyMessage.setVisibility(View.VISIBLE);
    View recordButton = findViewById(R.id.track_list_record_button);
    recordButton.setVisibility(View.VISIBLE);
  }

  @Override
  protected void onResume() {
    super.onResume();
    TrackRecordingServiceConnectionUtils.resume(this, trackRecordingServiceConnection);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    trackRecordingServiceConnection.unbind();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.track_list, menu);
    String fileTypes[] = getResources().getStringArray(R.array.file_types);
    menu.findItem(R.id.track_list_save_all_gpx)
        .setTitle(getString(R.string.menu_save_format, fileTypes[0]));
    menu.findItem(R.id.track_list_save_all_kml)
        .setTitle(getString(R.string.menu_save_format, fileTypes[1]));
    menu.findItem(R.id.track_list_save_all_csv)
        .setTitle(getString(R.string.menu_save_format, fileTypes[2]));
    menu.findItem(R.id.track_list_save_all_tcx)
        .setTitle(getString(R.string.menu_save_format, fileTypes[3]));
    
    recordTrackMenuItem = menu.findItem(R.id.track_list_record_track);
    stopRecordingMenuItem = menu.findItem(R.id.track_list_stop_recording);
    searchMenuItem = menu.findItem(R.id.track_list_search);
    importMenuItem = menu.findItem(R.id.track_list_import);
    saveAllMenuItem = menu.findItem(R.id.track_list_save_all);
    deleteAllMenuItem = menu.findItem(R.id.track_list_delete_all);

    ApiAdapterFactory.getApiAdapter().configureSearchWidget(this, searchMenuItem);
    updateMenu();
    return true;
  }

  /**
   * Updates the menu based on whether My Tracks is recording or not.
   */
  private void updateMenu() {
    updateMenuItems(
        TrackRecordingServiceConnectionUtils.isRecording(this, trackRecordingServiceConnection));
  }

  /**
   * Updates the menu items.
   *
   * @param isRecording true if recording
   */
  private void updateMenuItems(boolean isRecording) {
    if (recordTrackMenuItem != null) {
      recordTrackMenuItem.setVisible(!isRecording);
    }
    if (stopRecordingMenuItem != null) {
      stopRecordingMenuItem.setVisible(isRecording);
    }
    if (importMenuItem != null) {
      importMenuItem.setVisible(!isRecording);
    }
    if (saveAllMenuItem != null) {
      saveAllMenuItem.setVisible(!isRecording);
    }
    if (deleteAllMenuItem != null) {
      deleteAllMenuItem.setVisible(!isRecording);
    }
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    Intent intent;
    switch (item.getItemId()) {
      case R.id.track_list_record_track:
        AnalyticsUtils.sendPageViews(this, "/action/record_track");
        updateMenuItems(true);
        startRecording();
        return true;
      case R.id.track_list_stop_recording:
        updateMenuItems(false);
        TrackRecordingServiceConnectionUtils.stop(this, trackRecordingServiceConnection, true);
        return true;
      case R.id.track_list_search:
        return ApiAdapterFactory.getApiAdapter().handleSearchMenuSelection(this);
      case R.id.track_list_import:
        AnalyticsUtils.sendPageViews(this, "/action/import");
        intent = IntentUtils.newIntent(this, ImportActivity.class)
            .putExtra(ImportActivity.EXTRA_IMPORT_ALL, true);
        startActivity(intent);
        return true;
      case R.id.track_list_save_all_gpx:
        startSaveActivity(TrackFileFormat.GPX);
        return true;
      case R.id.track_list_save_all_kml:
        startSaveActivity(TrackFileFormat.KML);
        return true;
      case R.id.track_list_save_all_csv:
        startSaveActivity(TrackFileFormat.CSV);
        return true;
      case R.id.track_list_save_all_tcx:
        startSaveActivity(TrackFileFormat.TCX);
        return true;
      case R.id.track_list_delete_all:
        new DeleteAllTrackDialogFragment().show(
            getSupportFragmentManager(), DeleteAllTrackDialogFragment.DELETE_ALL_TRACK_DIALOG_TAG);
        return true;
      case R.id.track_list_aggregated_statistics:
        intent = IntentUtils.newIntent(this, AggregatedStatsActivity.class);
        startActivity(intent);
        return true;
      case R.id.track_list_settings:
        intent = IntentUtils.newIntent(this, SettingsActivity.class);
        startActivity(intent);
        return true;
      case R.id.track_list_help:
        intent = IntentUtils.newIntent(this, HelpActivity.class);
        startActivity(intent);
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  /**
   * Starts the {@link SaveActivity} to save all tracks.
   *
   * @param trackFileFormat the track file format
   */
  private void startSaveActivity(TrackFileFormat trackFileFormat) {
    AnalyticsUtils.sendPageViews(this, "/action/save_all");
    Intent intent = IntentUtils.newIntent(this, SaveActivity.class)
        .putExtra(SaveActivity.EXTRA_TRACK_FILE_FORMAT, (Parcelable) trackFileFormat);
    startActivity(intent);
  }
  
  @Override
  public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
    super.onCreateContextMenu(menu, v, menuInfo);
    getMenuInflater().inflate(R.menu.list_context_menu, menu);
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
   * @param trackId the track id
   * @return true if handled.
   */
  private boolean handleContextItem(int itemId, long trackId) {
    Intent intent;
    switch (itemId) {
      case R.id.list_context_menu_show_on_map:
        intent = IntentUtils.newIntent(this, TrackDetailActivity.class)
            .putExtra(TrackDetailActivity.EXTRA_TRACK_ID, trackId);
        startActivity(intent);
        return true;
      case R.id.list_context_menu_edit:
        intent = IntentUtils.newIntent(this, TrackEditActivity.class)
            .putExtra(TrackEditActivity.EXTRA_TRACK_ID, trackId);
        startActivity(intent);
        return true;
      case R.id.list_context_menu_delete:
        DeleteOneTrackDialogFragment.newInstance(trackId).show(
            getSupportFragmentManager(), DeleteOneTrackDialogFragment.DELETE_ONE_TRACK_DIALOG_TAG);
        return true;
      default:
        return false;
    }
  }

  /**
   * Starts a new recording.
   */
  private void startRecording() {
    startNewRecording = true;
    trackRecordingServiceConnection.startAndBind();

    /*
     * If the binding has happened, then invoke the callback to start a new
     * recording. If the binding hasn't happened, then invoking the callback
     * will have no effect. But when the binding occurs, the callback will get
     * invoked.
     */
    bindChangedCallback.run();
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
  public TrackRecordingServiceConnection getTrackRecordingServiceConnection() {
    return trackRecordingServiceConnection;
  }
}
