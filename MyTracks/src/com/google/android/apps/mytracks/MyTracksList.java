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
import com.google.android.apps.mytracks.util.StringUtils;
import com.google.android.apps.mytracks.util.UnitConversions;
import com.google.android.maps.mytracks.R;

import android.app.ListActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
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
 * deleting, sending to MyMaps, or writing to SD card.
 *
 * @author Leif Hendrik Wilden
 */
public class MyTracksList extends ListActivity
    implements SharedPreferences.OnSharedPreferenceChangeListener,
        View.OnClickListener {

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
        public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenuInfo menuInfo) {
          menu.setHeaderTitle(R.string.tracklist_this_track);
          AdapterView.AdapterContextMenuInfo info =
              (AdapterView.AdapterContextMenuInfo) menuInfo;
          contextPosition = info.position;
          trackId = MyTracksList.this.listView.getAdapter().getItemId(
              contextPosition);
          menu.add(0, MyTracksConstants.MENU_SHOW, 0,
              R.string.tracklist_show_track);
          menu.add(0, MyTracksConstants.MENU_EDIT, 0,
              R.string.tracklist_edit_track);
          if (!MyTracks.getInstance().isRecording()
              || trackId != recordingTrackId) {
            menu.add(0, MyTracksConstants.MENU_SEND_TO_GOOGLE, 0,
                R.string.tracklist_send_to_google);
            SubMenu share = menu.addSubMenu(0, MyTracksConstants.MENU_SHARE, 0,
                R.string.tracklist_share_track);
            share.add(0, MyTracksConstants.MENU_SHARE_LINK, 0,
                R.string.tracklist_share_linkfusion);
            share.add(0, MyTracksConstants.MENU_SHARE_GPX_FILE, 0,
                R.string.tracklist_share_gpx_file);
            share.add(0, MyTracksConstants.MENU_SHARE_KML_FILE, 0,
                R.string.tracklist_share_kml_file);
            share.add(0, MyTracksConstants.MENU_SHARE_CSV_FILE, 0,
                R.string.tracklist_share_csv_file);
            share.add(0, MyTracksConstants.MENU_SHARE_TCX_FILE, 0,
                R.string.tracklist_share_tcx_file);
            SubMenu save = menu.addSubMenu(0,
                MyTracksConstants.MENU_WRITE_TO_SD_CARD, 0,
                R.string.tracklist_write_to_sd);
            save.add(0, MyTracksConstants.MENU_SAVE_GPX_FILE, 0,
                R.string.tracklist_save_as_gpx);
            save.add(0, MyTracksConstants.MENU_SAVE_KML_FILE, 0,
                R.string.tracklist_save_as_kml);
            save.add(0, MyTracksConstants.MENU_SAVE_CSV_FILE, 0,
                R.string.tracklist_save_as_csv);
            save.add(0, MyTracksConstants.MENU_SAVE_TCX_FILE, 0,
                R.string.tracklist_save_as_tcx);
            menu.add(0, MyTracksConstants.MENU_DELETE, 0,
                R.string.tracklist_delete_track);
          }
        }
      };

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
    setResult(MyTracksConstants.SHOW_TRACK, result);
    finish();
  }

  @Override
  public boolean onMenuItemSelected(int featureId, MenuItem item) {
    if (!super.onMenuItemSelected(featureId, item)) {
      switch (item.getItemId()) {
        case MyTracksConstants.MENU_SHOW: {
          onListItemClick(null, null, 0, trackId);
          return true;
        }
        case MyTracksConstants.MENU_EDIT: {
          Intent intent = new Intent(this, MyTracksDetails.class);
          intent.putExtra("trackid", trackId);
          startActivity(intent);
          return true;
        }
        case MyTracksConstants.MENU_SHARE:
        case MyTracksConstants.MENU_WRITE_TO_SD_CARD:
          return false;
        default: {
          Intent result = new Intent();
          result.putExtra("trackid", trackId);
          setResult(
              MyTracksConstants.getActionFromMenuId(item.getItemId()), result);
          finish();
          return true;
        }
      }
    }
    return false;
  }

  @Override
  public void onClick(View v) {
    switch (v.getId()) {
      case R.id.tracklist_btn_delete_all: {
        Handler h = new MyTracksDeleteAllTracks(this, null);
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

    findViewById(R.id.tracklist_btn_delete_all).setOnClickListener(this);
    findViewById(R.id.tracklist_btn_export_all).setOnClickListener(this);
    findViewById(R.id.tracklist_btn_import_all).setOnClickListener(this);

    SharedPreferences preferences =
        getSharedPreferences(MyTracksSettings.SETTINGS_NAME, 0);
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

  private void setListAdapter() {
    // Get a cursor with all tracks
    SimpleCursorAdapter adapter = new SimpleCursorAdapter(
        this,
        R.layout.mytracks_list_item,
        tracksCursor,
        new String[] { TracksColumns.NAME, TracksColumns.STARTTIME,
                       TracksColumns.TOTALDISTANCE, TracksColumns.DESCRIPTION,
                       TracksColumns.CATEGORY },
        new int[] { R.id.trackdetails_item_name, R.id.trackdetails_item_time,
            R.id.trackdetails_item_stats, R.id.trackdetails_item_description,
            R.id.trackdetails_item_category });

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
          textView.setText(String.format("%tc", time));
        } else if (columnIndex == totalDistanceIdx) {
          double length = cursor.getDouble(totalDistanceIdx);
          String lengthUnit = null;
          if (metricUnits) {
            if (length > 1000) {
              length /= 1000;
              lengthUnit = getString(R.string.kilometer);
            } else {
              lengthUnit = getString(R.string.meter);
            }
          } else {
            if (length > UnitConversions.MI_TO_M) {
              length /= UnitConversions.MI_TO_M;
              lengthUnit = getString(R.string.mile);
            } else {
              length *= UnitConversions.M_TO_FT;
              lengthUnit = getString(R.string.feet);
            }
          }
          textView.setText(String.format("%s  %.2f %s",
              StringUtils.formatTime(cursor.getLong(totalTimeIdx)),
              length,
              lengthUnit));
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
}
