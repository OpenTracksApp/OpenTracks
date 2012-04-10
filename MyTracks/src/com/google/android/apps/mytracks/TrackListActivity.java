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
import com.google.android.apps.mytracks.fragments.DeleteAllTrackDialogFragment;
import com.google.android.apps.mytracks.fragments.EulaDialogFragment;
import com.google.android.apps.mytracks.io.file.TrackWriterFactory.TrackFileFormat;
import com.google.android.apps.mytracks.services.ITrackRecordingService;
import com.google.android.apps.mytracks.services.ServiceUtils;
import com.google.android.apps.mytracks.services.TrackRecordingServiceConnection;
import com.google.android.apps.mytracks.util.ApiAdapterFactory;
import com.google.android.apps.mytracks.util.EulaUtils;
import com.google.android.apps.mytracks.util.StringUtils;
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
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * An activity displaying a list of tracks.
 *
 * @author Leif Hendrik Wilden
 */
public class TrackListActivity extends FragmentActivity {

  private static final String TAG = TrackListActivity.class.getSimpleName();

  private static final String[] PROJECTION = new String[] {
      TracksColumns._ID,
      TracksColumns.NAME,
      TracksColumns.CATEGORY,
      TracksColumns.TOTALTIME,
      TracksColumns.TOTALDISTANCE,
      TracksColumns.STARTTIME,
      TracksColumns.DESCRIPTION };

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
        startTrackDetailActivity(recordingTrackId);
        Toast.makeText(
            TrackListActivity.this, R.string.track_list_record_success, Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Started a new recording");
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
  private final OnSharedPreferenceChangeListener sharedPreferenceChangeListener =
    new OnSharedPreferenceChangeListener() {
    @Override
    public void onSharedPreferenceChanged(SharedPreferences preferences, String key) {
      // Note that key can be null
      if (getString(R.string.metric_units_key).equals(key)) {
        metricUnits = preferences.getBoolean(getString(R.string.metric_units_key), true);
      }
      if (getString(R.string.recording_track_key).equals(key)) {
        recordingTrackId = sharedPreferences.getLong(getString(R.string.recording_track_key), -1L);
        if (isRecording()) {
          trackRecordingServiceConnection.startAndBind();
        }
        updateMenu();
      }
      adapter.notifyDataSetChanged();
    }
  };

  private TrackRecordingServiceConnection trackRecordingServiceConnection;
  private SharedPreferences sharedPreferences;
  private boolean metricUnits;
  private long recordingTrackId;
  private ListView listView;
  private ResourceCursorAdapter adapter;

  // True to start a new recording.
  private boolean startNewRecording = false;

  private MenuItem recordTrack;
  private MenuItem stopRecording;
  private MenuItem search;
  private MenuItem importAll;
  private MenuItem exportAll;
  private MenuItem deleteAll;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setVolumeControlStream(TextToSpeech.Engine.DEFAULT_STREAM);
    setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL);
    setContentView(R.layout.track_list);

    trackRecordingServiceConnection = new TrackRecordingServiceConnection(
        this, bindChangedCallback);

    sharedPreferences = getSharedPreferences(Constants.SETTINGS_NAME, Context.MODE_PRIVATE);
    sharedPreferences.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
    metricUnits = sharedPreferences.getBoolean(getString(R.string.metric_units_key), true);
    recordingTrackId = sharedPreferences.getLong(getString(R.string.recording_track_key), -1L);

    listView = (ListView) findViewById(R.id.track_list);
    listView.setOnItemClickListener(new OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        startTrackDetailActivity(id);
      }
    });
    adapter = new ResourceCursorAdapter(this, R.layout.track_list_item, null, 0) {
      @Override
      public void bindView(View view, Context context, Cursor cursor) {
        int idIndex = cursor.getColumnIndex(TracksColumns._ID);
        int nameIndex = cursor.getColumnIndex(TracksColumns.NAME);
        int categoryIndex = cursor.getColumnIndex(TracksColumns.CATEGORY);
        int timeIndex = cursor.getColumnIndexOrThrow(TracksColumns.TOTALTIME);
        int distanceIndex = cursor.getColumnIndexOrThrow(TracksColumns.TOTALDISTANCE);
        int startIndex = cursor.getColumnIndexOrThrow(TracksColumns.STARTTIME);
        int descriptionIndex = cursor.getColumnIndex(TracksColumns.DESCRIPTION);

        boolean isRecording = cursor.getLong(idIndex) == recordingTrackId;

        TextView name = (TextView) view.findViewById(R.id.track_list_item_name);
        name.setText(cursor.getString(nameIndex));
        name.setCompoundDrawablesWithIntrinsicBounds(
            isRecording ? R.drawable.menu_record_track : R.drawable.track, 0, 0, 0);

        TextView category = (TextView) view.findViewById(R.id.track_list_item_category);
        category.setText(cursor.getString(categoryIndex));

        TextView time = (TextView) view.findViewById(R.id.track_list_item_time);
        time.setText(StringUtils.formatElapsedTime(cursor.getLong(timeIndex)));
        time.setVisibility(isRecording ? View.GONE : View.VISIBLE);

        TextView distance = (TextView) view.findViewById(R.id.track_list_item_distance);
        distance.setText(StringUtils.formatDistance(
            TrackListActivity.this, cursor.getDouble(distanceIndex), metricUnits));
        distance.setVisibility(isRecording ? View.GONE : View.VISIBLE);

        TextView start = (TextView) view.findViewById(R.id.track_list_item_start);
        start.setText(
            StringUtils.formatDateTime(TrackListActivity.this, cursor.getLong(startIndex)));
        start.setVisibility(start.getText().equals(name.getText()) ? View.GONE : View.VISIBLE);

        TextView description = (TextView) view.findViewById(R.id.track_list_item_description);
        description.setText(cursor.getString(descriptionIndex));
        description.setVisibility(description.getText().length() == 0 ? View.GONE : View.VISIBLE);
      }
    };
    listView.setAdapter(adapter);

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
        adapter.swapCursor(cursor);
      }

      @Override
      public void onLoaderReset(Loader<Cursor> loader) {
        adapter.swapCursor(null);
      }
    });

    if (!EulaUtils.getEulaValue(this)) {
      Fragment fragment = getSupportFragmentManager()
          .findFragmentByTag(EulaDialogFragment.EULA_DIALOG_TAG);
      if (fragment == null) {
        EulaDialogFragment.newInstance(false).show(
            getSupportFragmentManager(), EulaDialogFragment.EULA_DIALOG_TAG);
      }
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
    trackRecordingServiceConnection.bindIfRunning();
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
    menu.findItem(R.id.track_list_export_gpx)
        .setTitle(getString(R.string.menu_export_all_format, fileTypes[0]));
    menu.findItem(R.id.track_list_export_kml)
        .setTitle(getString(R.string.menu_export_all_format, fileTypes[1]));
    menu.findItem(R.id.track_list_export_csv)
        .setTitle(getString(R.string.menu_export_all_format, fileTypes[2]));
    menu.findItem(R.id.track_list_export_tcx)
        .setTitle(getString(R.string.menu_export_all_format, fileTypes[3]));
    
    recordTrack = menu.findItem(R.id.track_list_record_track);
    stopRecording = menu.findItem(R.id.track_list_stop_recording);
    search = menu.findItem(R.id.track_list_search);
    importAll = menu.findItem(R.id.track_list_import_all);
    exportAll = menu.findItem(R.id.track_list_export_all);
    deleteAll = menu.findItem(R.id.track_list_delete_all);

    ApiAdapterFactory.getApiAdapter().configureSearchWidget(this, search);
    updateMenu();
    return true;
  }

  /**
   * Updates the menu based on whether My Tracks is recording or not.
   */
  private void updateMenu() {
    boolean isRecording = isRecording();
    updateMenuItems(isRecording);
  }

  /**
   * Updates the menu items.
   *
   * @param isRecording true if recording
   */
  private void updateMenuItems(boolean isRecording) {
    if (recordTrack != null) {
      recordTrack.setVisible(!isRecording);
    }
    if (stopRecording != null) {
      stopRecording.setVisible(isRecording);
    }
    if (importAll != null) {
      importAll.setVisible(!isRecording);
    }
    if (exportAll != null) {
      exportAll.setVisible(!isRecording);
    }
    if (deleteAll != null) {
      deleteAll.setVisible(!isRecording);
    }
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.track_list_record_track:
        updateMenuItems(true);
        startRecording();
        return true;
      case R.id.track_list_stop_recording:
        updateMenuItems(false);
        stopRecording();
        return true;
      case R.id.track_list_search:
        return ApiAdapterFactory.getApiAdapter().handleSearchMenuSelection(this);
      case R.id.track_list_import_all:
        startActivity(
            new Intent(this, ImportActivity.class).putExtra(ImportActivity.EXTRA_IMPORT_ALL, true));
        return true;
      case R.id.track_list_export_gpx:
        startActivity(new Intent(this, ExportActivity.class).putExtra(
            ExportActivity.EXTRA_TRACK_FILE_FORMAT, (Parcelable) TrackFileFormat.GPX));
        return true;
      case R.id.track_list_export_kml:
        startActivity(new Intent(this, ExportActivity.class).putExtra(
            ExportActivity.EXTRA_TRACK_FILE_FORMAT, (Parcelable) TrackFileFormat.KML));
        return true;
      case R.id.track_list_export_csv:
        startActivity(new Intent(this, ExportActivity.class).putExtra(
            ExportActivity.EXTRA_TRACK_FILE_FORMAT, (Parcelable) TrackFileFormat.CSV));
        return true;
      case R.id.track_list_export_tcx:
        startActivity(new Intent(this, ExportActivity.class).putExtra(
            ExportActivity.EXTRA_TRACK_FILE_FORMAT, (Parcelable) TrackFileFormat.TCX));
        return true;
      case R.id.track_list_delete_all:
        new DeleteAllTrackDialogFragment().show(
            getSupportFragmentManager(), DeleteAllTrackDialogFragment.DELETE_ALL_TRACK_DIALOG_TAG);
        return true;
      case R.id.track_list_aggregated_statistics:
        startActivity(new Intent(this, AggregatedStatsActivity.class));
        return true;
      case R.id.track_list_settings:
        startActivity(new Intent(this, SettingsActivity.class));
        return true;
      case R.id.track_list_help:
        startActivity(new Intent(this, HelpActivity.class));
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  /**
   * Starts {@link TrackDetailActivity}.
   * 
   * @param trackId the track id.
   */
  private void startTrackDetailActivity(long trackId) {
    Intent intent = new Intent(TrackListActivity.this, TrackDetailActivity.class)
        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK)
        .putExtra(TrackDetailActivity.EXTRA_TRACK_ID, trackId);
    startActivity(intent);
  }

  /**
   * Returns true if recording.
   */
  private boolean isRecording() {
    return ServiceUtils.isRecording(
        this, trackRecordingServiceConnection.getServiceIfBound(), sharedPreferences);
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

  /**
   * Stops the current recording.
   */
  private void stopRecording() {
    ITrackRecordingService service = trackRecordingServiceConnection.getServiceIfBound();
    if (service != null) {
      try {
        /*
         * Remembers the recordingTrackId before endCurrentTrack sets the shared
         * preferences, R.string.recording_track_key, to -1L, and the
         * sharedPreferenceChangedListener sets the recordingTrackId variable to
         * -1L.
         */
        long trackId = recordingTrackId;
        service.endCurrentTrack();
        if (trackId != -1L) {
          Intent intent = new Intent(this, TrackEditActivity.class)
              .putExtra(TrackEditActivity.EXTRA_SHOW_CANCEL, false)
              .putExtra(TrackEditActivity.EXTRA_TRACK_ID, trackId);
          startActivity(intent);
        }
      } catch (Exception e) {
        Log.d(TAG, "Unable to stop recording.", e);
      }
    }
    trackRecordingServiceConnection.stop();
  }

  @Override
  public boolean onKeyUp(int keyCode, KeyEvent event) {
    if (keyCode == KeyEvent.KEYCODE_SEARCH) {
      if (ApiAdapterFactory.getApiAdapter().handleSearchKey(search)) {
        return true;
      }
    }
    return super.onKeyUp(keyCode, event);
  }
}
