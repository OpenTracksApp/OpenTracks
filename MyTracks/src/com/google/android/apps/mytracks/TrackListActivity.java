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

import com.google.android.apps.mytracks.content.MyTracksProviderUtils;
import com.google.android.apps.mytracks.content.TracksColumns;
import com.google.android.apps.mytracks.io.file.TrackWriterFactory.TrackFileFormat;
import com.google.android.apps.mytracks.services.ITrackRecordingService;
import com.google.android.apps.mytracks.services.ServiceUtils;
import com.google.android.apps.mytracks.services.TrackRecordingServiceConnection;
import com.google.android.apps.mytracks.util.ApiAdapterFactory;
import com.google.android.apps.mytracks.util.DialogUtils;
import com.google.android.apps.mytracks.util.StringUtils;
import com.google.android.maps.mytracks.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

/**
 * An activity displaying a list of tracks.
 * 
 * @author Leif Hendrik Wilden
 */
public class TrackListActivity extends Activity {

  private static final String TAG = TrackListActivity.class.getSimpleName();
  private static final int DIALOG_EXPORT_ALL_ID = 0;
  private static final int DIALOG_DELETE_ALL_ID = 1;

  // Callback when the trackRecordingServiceConnection binding changes
  private final Runnable bindChangedCallback = new Runnable() {
      @Override
    public void run() {
      synchronized (trackRecordingServiceConnection) {
        if (startNewRecording) {
          startRecording();
        } else {
          updateMenu();
          adapter.notifyDataSetChanged();
        }
      }
    }
  };

  private TrackRecordingServiceConnection trackRecordingServiceConnection;
  private SharedPreferences sharedPreferences;
  private boolean metricUnits;
  private long recordingTrackId;
  private ListView listView;
  private ResourceCursorAdapter adapter;

  // True to start a new recording
  private boolean startNewRecording = false;

  private MenuItem recordTrack;
  private MenuItem stopRecording;
  private MenuItem importAll;
  private MenuItem exportAll;
  private MenuItem deleteAll;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.track_list);

    trackRecordingServiceConnection = new TrackRecordingServiceConnection(
        this, bindChangedCallback);

    sharedPreferences = getSharedPreferences(Constants.SETTINGS_NAME, Context.MODE_PRIVATE);
    sharedPreferences.registerOnSharedPreferenceChangeListener(
        new SharedPreferences.OnSharedPreferenceChangeListener() {
          @Override
          public void onSharedPreferenceChanged(SharedPreferences preferences, String key) {
            if (key == null) {
              return;
            }
            if (key.equals(getString(R.string.metric_units_key))) {
              metricUnits = preferences.getBoolean(getString(R.string.metric_units_key), true);
            }
            if (key.equals(getString(R.string.recording_track_key))) {
              recordingTrackId = sharedPreferences.getLong(
                  getString(R.string.recording_track_key), -1L);
            }
            adapter.notifyDataSetChanged();
          }
        });
    metricUnits = sharedPreferences.getBoolean(getString(R.string.metric_units_key), true);
    recordingTrackId = sharedPreferences.getLong(getString(R.string.recording_track_key), -1L);

    MyTracksProviderUtils myTracksProviderUtils = MyTracksProviderUtils.Factory.get(this);
    Cursor trackCursor = myTracksProviderUtils.getTracksCursor(
        null, null, TracksColumns._ID + " DESC");
    startManagingCursor(trackCursor);
    listView = (ListView) findViewById(R.id.track_list);
    listView.setOnItemClickListener(new OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        setResult(Constants.SHOW_TRACK, new Intent().putExtra("trackid", id));
        finish();
      }
    });
    adapter = new ResourceCursorAdapter(this, R.layout.track_list_item, trackCursor, true) {
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
  }

  @Override
  protected void onStart() {
    super.onStart();
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
    return true;
  }

  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    recordTrack = menu.findItem(R.id.menu_record_track);
    stopRecording = menu.findItem(R.id.menu_stop_recording);
    importAll = menu.findItem(R.id.menu_import_all);
    exportAll = menu.findItem(R.id.menu_export_all);
    deleteAll = menu.findItem(R.id.menu_delete_all);
    updateMenu();
    return super.onPrepareOptionsMenu(menu);
  }

  /**
   * Updates the menu based on whether My Tracks is recording or not.
   */
  private void updateMenu() {
    boolean isRecording = ServiceUtils.isRecording(
        this, trackRecordingServiceConnection.getServiceIfBound(), sharedPreferences);
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
      case R.id.menu_record_track:
        updateMenuItems(true);
        synchronized (trackRecordingServiceConnection) {
          startNewRecording = true;
          trackRecordingServiceConnection.startAndBind();

          /*
           * If the binding has happened, then invoke the callback to start a
           * new recording. If the binding hasn't happened, then invoking the
           * callback will have no effect. But when the binding occurs, the
           * callback will get invoked.
           */
          bindChangedCallback.run();
        }
        return true;
      case R.id.menu_stop_recording:
        updateMenuItems(false);
        stopRecording();
        trackRecordingServiceConnection.stop();
        return true;
      case R.id.menu_search:
        onSearchRequested();
        return true;
      case R.id.menu_import_all:
        startActivity(new Intent(this, ImportActivity.class)
            .putExtra(ImportActivity.EXTRA_IMPORT_ALL, true));
        return true;
      case R.id.menu_export_all:
        showDialog(DIALOG_EXPORT_ALL_ID);
        return true;
      case R.id.menu_delete_all:
        showDialog(DIALOG_DELETE_ALL_ID);
        return true;
      case R.id.menu_aggregated_statistics:
        startActivity(new Intent(this, AggregatedStatsActivity.class));
        return true;
      case R.id.menu_settings:
        startActivity(new Intent(this, SettingsActivity.class));
        return true;
      case R.id.menu_help:
        startActivity(new Intent(this, WelcomeActivity.class));
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  /**
   * Starts a new recording.
   */
  private void startRecording() {
    ITrackRecordingService service = trackRecordingServiceConnection.getServiceIfBound();
    if (service != null) {
      try {
        recordingTrackId = service.startNewTrack();
        startNewRecording = false;
        adapter.notifyDataSetChanged();
        Toast.makeText(TrackListActivity.this, R.string.track_record_success, Toast.LENGTH_SHORT)
            .show();
        Log.d(TAG, "Started a new recording");
      } catch (Exception e) {
        Toast.makeText(TrackListActivity.this, R.string.track_record_error, Toast.LENGTH_LONG)
            .show();
        Log.d(TAG, "Unable to start a new recording.", e);
      }
    } else {
      Log.d(TAG, "service not available to start a new recording");
    }
  }

  /**
   * Stops the current recording.
   */
  private void stopRecording() {
    ITrackRecordingService service = trackRecordingServiceConnection.getServiceIfBound();
    if (service != null) {
      try {
        service.endCurrentTrack();
        if (recordingTrackId != -1L) {
          Intent intent = new Intent(this, TrackDetail.class)
              .putExtra(TrackDetail.SHOW_CANCEL, false)
              .putExtra(TrackDetail.TRACK_ID, recordingTrackId);
          startActivity(intent);
        }
        recordingTrackId = -1L;
        adapter.notifyDataSetChanged();
      } catch (Exception e) {
        Log.d(TAG, "Unable to stop recording.", e);
      }
    }
  }

  @Override
  protected Dialog onCreateDialog(int id) {
    switch (id) {
      case DIALOG_EXPORT_ALL_ID:
        return createExportAllDialog();
      case DIALOG_DELETE_ALL_ID:
        return DialogUtils.createConfirmationDialog(this,
            R.string.track_list_delete_all_confirm_message, new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialog, int which) {
                MyTracksProviderUtils.Factory.get(TrackListActivity.this).deleteAllTracks();
                /*
                 * TODO Verify that selected_track_key is still needed with the
                 * ICS navigation design
                 */
                Editor editor = sharedPreferences.edit();
                // TODO: Go through data manager
                editor.putLong(getString(R.string.selected_track_key), -1L);
                ApiAdapterFactory.getApiAdapter().applyPreferenceChanges(editor);
              }
            });
      default:
        return null;
    }
  }

  /**
   * Creates an export all dialog.
   */
  private AlertDialog createExportAllDialog() {
    String exportFileFormat = getString(R.string.track_list_export_file);
    String fileTypes[] = getResources().getStringArray(R.array.file_types);
    String[] choices = new String[fileTypes.length];
    for (int i = 0; i < fileTypes.length; i++) {
      choices[i] = String.format(exportFileFormat, fileTypes[i]);
    }
    return new AlertDialog.Builder(this).setNegativeButton(R.string.generic_cancel, null)
        .setPositiveButton(R.string.generic_ok, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            int index = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
            Intent intent = new Intent(TrackListActivity.this, ExportActivity.class).putExtra(
                ExportActivity.EXTRA_TRACK_FILE_FORMAT,
                (Parcelable) TrackFileFormat.values()[index]);
            TrackListActivity.this.startActivity(intent);
          }
        })
        .setSingleChoiceItems(choices, 0, null)
        .setTitle(R.string.track_list_export_all)
        .create();
  }
}
