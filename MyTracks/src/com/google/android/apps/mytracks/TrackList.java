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

import static com.google.android.apps.mytracks.Constants.TAG;

import com.google.android.apps.mytracks.content.TracksColumns;
import com.google.android.apps.mytracks.io.file.SaveActivity;
import com.google.android.apps.mytracks.io.sendtogoogle.SendRequest;
import com.google.android.apps.mytracks.io.sendtogoogle.UploadServiceChooserActivity;
import com.google.android.apps.mytracks.services.ServiceUtils;
import com.google.android.apps.mytracks.services.TrackRecordingServiceConnection;
import com.google.android.apps.mytracks.util.PlayTrackUtils;
import com.google.android.apps.mytracks.util.StringUtils;
import com.google.android.maps.mytracks.R;

import android.app.Dialog;
import android.app.ListActivity;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.View.OnCreateContextMenuListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

/**
 * A list activity displaying all the recorded tracks. There's a context
 * menu (via long press) displaying various options such as showing, editing,
 * deleting, sending to Google, or writing to SD card.
 *
 * @author Leif Hendrik Wilden
 */
public class TrackList extends ListActivity
    implements SharedPreferences.OnSharedPreferenceChangeListener,
        View.OnClickListener {

  private static final int DIALOG_INSTALL_EARTH = 0;
  
  private int contextPosition = -1;
  private long trackId = -1;
  private ListView listView = null;
  private boolean metricUnits = true;

  private Cursor tracksCursor = null;

  /**
   * The id of the currently recording track.
   */
  private long recordingTrackId = -1;

  private final OnCreateContextMenuListener contextMenuListener =
      new OnCreateContextMenuListener() {
        @Override
        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
          menu.setHeaderTitle(R.string.track_list_context_menu_title);
          AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
          contextPosition = info.position;
          trackId = TrackList.this.listView.getAdapter().getItemId(contextPosition);
          menu.add(Menu.NONE, Constants.MENU_SHOW, Menu.NONE, R.string.track_list_show_on_map);
          menu.add(Menu.NONE, Constants.MENU_EDIT, Menu.NONE, R.string.track_list_edit_track);
          if (!isRecording() || trackId != recordingTrackId) {
            String saveFileFormat = getString(R.string.track_list_save_file);
            String shareFileFormat = getString(R.string.track_list_share_file);
            String fileTypes[] = getResources().getStringArray(R.array.file_types);

            menu.add(Menu.NONE, Constants.MENU_PLAY, Menu.NONE, R.string.track_list_play);
            menu.add(Menu.NONE, Constants.MENU_SEND_TO_GOOGLE, Menu.NONE,
                R.string.track_list_send_google);
            SubMenu share = menu.addSubMenu(
                Menu.NONE, Constants.MENU_SHARE, Menu.NONE, R.string.track_list_share_track);
            share.add(
                Menu.NONE, Constants.MENU_SHARE_MAP, Menu.NONE, R.string.track_list_share_map);
            share.add(Menu.NONE, Constants.MENU_SHARE_FUSION_TABLE, Menu.NONE,
                R.string.track_list_share_fusion_table);
            share.add(Menu.NONE, Constants.MENU_SHARE_GPX_FILE, Menu.NONE,
                String.format(shareFileFormat, fileTypes[0]));
            share.add(Menu.NONE, Constants.MENU_SHARE_KML_FILE, Menu.NONE,
                String.format(shareFileFormat, fileTypes[1]));
            share.add(Menu.NONE, Constants.MENU_SHARE_CSV_FILE, Menu.NONE,
                String.format(shareFileFormat, fileTypes[2]));
            share.add(Menu.NONE, Constants.MENU_SHARE_TCX_FILE, Menu.NONE,
                String.format(shareFileFormat, fileTypes[3]));
            SubMenu save = menu.addSubMenu(
                Menu.NONE, Constants.MENU_WRITE_TO_SD_CARD, Menu.NONE, R.string.track_list_save_sd);
            save.add(Menu.NONE, Constants.MENU_SAVE_GPX_FILE, Menu.NONE,
                String.format(saveFileFormat, fileTypes[0]));
            save.add(Menu.NONE, Constants.MENU_SAVE_KML_FILE, Menu.NONE,
                String.format(saveFileFormat, fileTypes[1]));
            save.add(Menu.NONE, Constants.MENU_SAVE_CSV_FILE, Menu.NONE,
                String.format(saveFileFormat, fileTypes[2]));
            save.add(Menu.NONE, Constants.MENU_SAVE_TCX_FILE, Menu.NONE,
                String.format(saveFileFormat, fileTypes[3]));
            menu.add(Menu.NONE, Constants.MENU_DELETE, Menu.NONE, R.string.track_list_delete_track);
          }
        }
      };

  private final Runnable serviceBindingChanged = new Runnable() {
    @Override
    public void run() {
      updateButtonsEnabled();
    }
  };

  private TrackRecordingServiceConnection serviceConnection;
  private SharedPreferences preferences;

  @Override
  public void onSharedPreferenceChanged(
      SharedPreferences sharedPreferences, String key) {
    if (key == null) {
      return;
    }
    if (key.equals(getString(R.string.metric_units_key))) {
      metricUnits = sharedPreferences.getBoolean(
          getString(R.string.metric_units_key), true);
      if (tracksCursor != null && !tracksCursor.isClosed()) {
        tracksCursor.requery();
      }
    }
    if (key.equals(getString(R.string.recording_track_key))) {
      recordingTrackId = sharedPreferences.getLong(
          getString(R.string.recording_track_key), -1);
    }
  }

  @Override
  protected void onListItemClick(ListView l, View v, int position, long id) {
    Intent result = new Intent();
    result.putExtra("trackid", id);
    setResult(Constants.SHOW_TRACK, result);
    finish();
  }

  @Override
  public boolean onMenuItemSelected(int featureId, MenuItem item) {
    Intent intent;
    switch (item.getItemId()) {
      case Constants.MENU_SHOW: {
        onListItemClick(null, null, 0, trackId);
        return true;
      }
      case Constants.MENU_EDIT: {
        intent = new Intent(this, TrackDetail.class).putExtra(TrackDetail.TRACK_ID, trackId);
        startActivity(intent);
        return true;
      }
      case Constants.MENU_PLAY:
        if (PlayTrackUtils.isEarthInstalled(this)) {
          PlayTrackUtils.playTrack(this, trackId);
          return true;
        } else {
          showDialog(DIALOG_INSTALL_EARTH);
          return true;
        }
      case Constants.MENU_SEND_TO_GOOGLE:
        intent = new Intent(this, UploadServiceChooserActivity.class)
            .putExtra(SendRequest.SEND_REQUEST_KEY, new SendRequest(trackId, true, true, true));
        startActivity(intent);
        return true;
      case Constants.MENU_SHARE_MAP:
        intent = new Intent(this, UploadServiceChooserActivity.class)
            .putExtra(SendRequest.SEND_REQUEST_KEY, new SendRequest(trackId, true, false, false));
        startActivity(intent);
        return true;
      case Constants.MENU_SHARE_FUSION_TABLE:
        intent = new Intent(this, UploadServiceChooserActivity.class)
            .putExtra(SendRequest.SEND_REQUEST_KEY, new SendRequest(trackId, false, true, false));
        startActivity(intent);
        return true;
      case Constants.MENU_SHARE_GPX_FILE:
      case Constants.MENU_SHARE_KML_FILE:
      case Constants.MENU_SHARE_CSV_FILE:
      case Constants.MENU_SHARE_TCX_FILE:
      case Constants.MENU_SAVE_GPX_FILE:
      case Constants.MENU_SAVE_KML_FILE:
      case Constants.MENU_SAVE_CSV_FILE:
      case Constants.MENU_SAVE_TCX_FILE:
        SaveActivity.handleExportTrackAction(
            this, trackId, Constants.getActionFromMenuId(item.getItemId()));
        return true;
      case Constants.MENU_DELETE:
        Uri uri = ContentUris.withAppendedId(TracksColumns.CONTENT_URI, trackId);
        intent = new Intent(Intent.ACTION_DELETE)
            .setDataAndType(uri, TracksColumns.CONTENT_ITEMTYPE);
        startActivity(intent);
        return true;
      default:
        Log.w(TAG, "Unknown menu item: " + item.getItemId() + "(" + item.getTitle() + ")");
        return super.onMenuItemSelected(featureId, item);
    }
  }

  @Override
  public void onClick(View v) {
    switch (v.getId()) {
      case R.id.tracklist_btn_delete_all: {
        Handler h = new DeleteAllTracks(this, null);
        h.handleMessage(null);
        break;
      }
      case R.id.tracklist_btn_export_all: {
        new ExportAllTracks(this);
        break;
      }
      case R.id.tracklist_btn_import_all: {
        new ImportAllTracks(this);
        break;
      }
    }
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // We don't need a window title bar:
    requestWindowFeature(Window.FEATURE_NO_TITLE);

    setContentView(R.layout.mytracks_list);

    listView = getListView();
    listView.setOnCreateContextMenuListener(contextMenuListener);

    preferences = getSharedPreferences(Constants.SETTINGS_NAME, Context.MODE_PRIVATE);
    serviceConnection = new TrackRecordingServiceConnection(this, serviceBindingChanged);

    View deleteAll = findViewById(R.id.tracklist_btn_delete_all);
    deleteAll.setOnClickListener(this);

    View exportAll = findViewById(R.id.tracklist_btn_export_all);
    exportAll.setOnClickListener(this);

    updateButtonsEnabled();

    findViewById(R.id.tracklist_btn_import_all).setOnClickListener(this);

    preferences.registerOnSharedPreferenceChangeListener(this);
    metricUnits =
        preferences.getBoolean(getString(R.string.metric_units_key), true);
    recordingTrackId =
        preferences.getLong(getString(R.string.recording_track_key), -1);

    tracksCursor = getContentResolver().query(
        TracksColumns.CONTENT_URI, null, null, null, "_id DESC");
    startManagingCursor(tracksCursor);
    setListAdapter();
  }

  @Override
  protected void onStart() {
    super.onStart();

    serviceConnection.bindIfRunning();
  }

  @Override
  protected void onDestroy() {
    serviceConnection.unbind();

    super.onDestroy();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.search_only, menu);
    return true;
  }

  @Override
  protected Dialog onCreateDialog(int id) {
    switch (id) {
      case DIALOG_INSTALL_EARTH:
        return PlayTrackUtils.createInstallEarthDialog(this);
      default:
        return null;
    }
  }

  /* Callback from menu/search_only.xml */
  public void onSearch(@SuppressWarnings("unused") MenuItem i) {
    onSearchRequested();
  }

  private void updateButtonsEnabled() {
    View deleteAll = findViewById(R.id.tracklist_btn_delete_all);
    View exportAll = findViewById(R.id.tracklist_btn_export_all);

    boolean notRecording = !isRecording();
    deleteAll.setEnabled(notRecording);
    exportAll.setEnabled(notRecording);
  }

  private void setListAdapter() {
    // Get a cursor with all tracks
    SimpleCursorAdapter adapter = new SimpleCursorAdapter(
        this,
        R.layout.mytracks_list_item,
        tracksCursor,
        new String[] { TracksColumns.NAME, TracksColumns.STARTTIME,
                       TracksColumns.TOTALDISTANCE, TracksColumns.DESCRIPTION,
                       TracksColumns.CATEGORY },
        new int[] { R.id.track_list_item_name, R.id.track_list_item_time,
            R.id.track_list_item_stats, R.id.track_list_item_description,
            R.id.track_list_item_category });

    final int startTimeIdx =
        tracksCursor.getColumnIndexOrThrow(TracksColumns.STARTTIME);
    final int totalTimeIdx =
        tracksCursor.getColumnIndexOrThrow(TracksColumns.TOTALTIME);
    final int totalDistanceIdx =
        tracksCursor.getColumnIndexOrThrow(TracksColumns.TOTALDISTANCE);

    adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
      @Override
      public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
        TextView textView = (TextView) view;
        if (columnIndex == startTimeIdx) {
          long time = cursor.getLong(startTimeIdx);
          textView.setText(StringUtils.formatDateTime(TrackList.this, time));
        } else if (columnIndex == totalDistanceIdx) {
          double totalDistance = cursor.getDouble(totalDistanceIdx);
          long totalTime = cursor.getLong(totalTimeIdx);
          String totalDistanceStr = StringUtils.formatTimeDistance(
              TrackList.this, totalTime, totalDistance, metricUnits);
          textView.setText(totalDistanceStr);
        } else {
          textView.setText(cursor.getString(columnIndex));
          if (textView.getText().length() < 1) {
            textView.setVisibility(View.GONE);
          } else {
            textView.setVisibility(View.VISIBLE);
          }
        }
        return true;
      }

    });
    setListAdapter(adapter);
  }

  private boolean isRecording() {
    return ServiceUtils.isRecording(TrackList.this, serviceConnection.getServiceIfBound(), preferences);
  }
}
