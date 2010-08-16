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
import com.google.android.apps.mytracks.content.Waypoint;
import com.google.android.apps.mytracks.content.WaypointsColumns;
import com.google.android.apps.mytracks.util.StringUtils;
import com.google.android.maps.mytracks.R;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnCreateContextMenuListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

/**
 * Activity which shows the list of waypoints in a track.
 *
 * @author Leif Hendrik Wilden
 */
public class MyTracksWaypointsList extends ListActivity
    implements View.OnClickListener {

  private int contextPosition = -1;
  private long trackId = -1;
  private long waypointId = -1;
  private ListView listView = null;
  private Button insertWaypointButton = null;
  private Button insertStatisticsButton = null;
  private long recordingTrackId = -1;
  private long selectedTrackId = -1;
  private MyTracksProviderUtils providerUtils;

  private Cursor waypointsCursor = null;

  private final OnCreateContextMenuListener contextMenuListener =
      new OnCreateContextMenuListener() {
        public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenuInfo menuInfo) {
          menu.setHeaderTitle(R.string.waypointslist_this_waypoint);
          AdapterView.AdapterContextMenuInfo info =
              (AdapterView.AdapterContextMenuInfo) menuInfo;
          contextPosition = info.position;
          waypointId = MyTracksWaypointsList.this.listView.getAdapter()
              .getItemId(contextPosition);
          int type = providerUtils.getWaypoint(info.id).getType();
          menu.add(0, MyTracksConstants.MENU_SHOW, 0,
              R.string.waypointslist_show_waypoint);
          menu.add(0, MyTracksConstants.MENU_EDIT, 0,
              R.string.waypointslist_edit_waypoint);
          menu.add(0, MyTracksConstants.MENU_DELETE, 0,
              R.string.waypointslist_delete_waypoint).setEnabled(
                  recordingTrackId < 0 || type == Waypoint.TYPE_WAYPOINT ||
                  info.id != providerUtils.getLastWaypointId(recordingTrackId));
        }
      };

  @Override
  protected void onListItemClick(ListView l, View v, int position, long id) {
    Intent result = new Intent();
    result.putExtra("trackid", trackId);
    result.putExtra("waypointid", id);
    setResult(MyTracksConstants.EDIT_WAYPOINT, result);
    finish();
  }

  @Override
  public boolean onMenuItemSelected(int featureId, MenuItem item) {
    if (!super.onMenuItemSelected(featureId, item)) {
      switch (item.getItemId()) {
        case MyTracksConstants.MENU_SHOW: {
          onListItemClick(null, null, 0, waypointId);
          return true;
        }
        case MyTracksConstants.MENU_EDIT: {
          Intent intent = new Intent(this, MyTracksWaypointDetails.class);
          intent.putExtra("trackid", trackId);
          intent.putExtra("waypointid", waypointId);
          startActivity(intent);
          return true;
        }
        case MyTracksConstants.MENU_DELETE: {
          deleteWaypoint(waypointId);
        }
      }
    }
    return false;
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    providerUtils = MyTracksProviderUtils.Factory.get(this);

    // We don't need a window title bar:
    requestWindowFeature(Window.FEATURE_NO_TITLE);

    setContentView(R.layout.mytracks_waypoints_list);

    listView = getListView();
    listView.setOnCreateContextMenuListener(contextMenuListener);

    insertWaypointButton =
        (Button) findViewById(R.id.waypointslist_btn_insert_waypoint);
    insertWaypointButton.setOnClickListener(this);
    insertStatisticsButton =
        (Button) findViewById(R.id.waypointslist_btn_insert_statistics);
    insertStatisticsButton.setOnClickListener(this);
    SharedPreferences preferences =
        getSharedPreferences(MyTracksSettings.SETTINGS_NAME, 0);
    if (preferences != null) {
      recordingTrackId =
          preferences.getLong(MyTracksSettings.RECORDING_TRACK, -1);
      selectedTrackId =
          preferences.getLong(MyTracksSettings.SELECTED_TRACK, -1);
    }
    boolean selectedRecording = selectedTrackId > 0
        && selectedTrackId == recordingTrackId;
    insertWaypointButton.setEnabled(selectedRecording);
    insertStatisticsButton.setEnabled(selectedRecording);

    if (getIntent() != null && getIntent().getExtras() != null) {
      trackId = getIntent().getExtras().getLong("trackid", -1);
    } else {
      trackId = -1;
    }

    final long firstWaypointId = providerUtils.getFirstWaypointId(trackId); 
    waypointsCursor = getContentResolver().query(
        WaypointsColumns.CONTENT_URI, null,
        WaypointsColumns.TRACKID + "=" + trackId + " AND "
        + WaypointsColumns._ID + "!=" + firstWaypointId, null, null);
    startManagingCursor(waypointsCursor);
    setListAdapter();
  }

  @Override
  public void onClick(View v) {
    switch (v.getId()) {
      case R.id.waypointslist_btn_insert_waypoint: {
        long id = MyTracks.getInstance().insertWaypointMarker();
        if (id >= 0) {
          Intent intent = new Intent(this, MyTracksWaypointDetails.class);
          intent.putExtra("waypointid", id);
          startActivity(intent);
        }
        break;
      }
      case R.id.waypointslist_btn_insert_statistics: {
        long id = MyTracks.getInstance().insertStatisticsMarker();
        if (id >= 0) {
          Intent intent = new Intent(this, MyTracksWaypointDetails.class);
          intent.putExtra("waypointid", id);
          startActivity(intent);
        }
        break;
      }

    }
  }

  private void setListAdapter() {
    // Get a cursor with all tracks
    SimpleCursorAdapter adapter = new SimpleCursorAdapter(
        this,
        R.layout.mytracks_marker_item,
        waypointsCursor,
        new String[] { WaypointsColumns.NAME, WaypointsColumns.TIME,
                       WaypointsColumns.CATEGORY, WaypointsColumns.TYPE },
        new int[] { R.id.waypointslist_item_name,
                    R.id.waypointslist_item_time, 
                    R.id.waypointslist_item_category,
                    R.id.waypointslist_item_icon });

    final int timeIdx =
        waypointsCursor.getColumnIndexOrThrow(WaypointsColumns.TIME);
    final int typeIdx =
        waypointsCursor.getColumnIndexOrThrow(WaypointsColumns.TYPE);
    adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
      @Override
      public boolean setViewValue(View view, Cursor cursor, int columnIndex) {    
        if (columnIndex == timeIdx) {
          long time = cursor.getLong(timeIdx);
          TextView textView = (TextView) view;
          textView.setText(String.format("%tc", time));
          textView.setVisibility(
              textView.getText().length() < 1 ? View.GONE : View.VISIBLE);
        } else if (columnIndex == typeIdx) {
          int type = cursor.getInt(typeIdx);
          ImageView imageView = (ImageView) view;
          imageView.setImageDrawable(getResources().getDrawable(
              type == Waypoint.TYPE_STATISTICS
                  ? R.drawable.ylw_pushpin
                  : R.drawable.blue_pushpin));
        } else {
          TextView textView = (TextView) view;
          textView.setText(cursor.getString(columnIndex));
          textView.setVisibility(
              textView.getText().length() < 1 ? View.GONE : View.VISIBLE);
        }
        return true;
      }
    });
    setListAdapter(adapter);
  }

  /**
   * Deletes the way point with the given id.
   * Prompts the user if he want to really delete it.
   */
  public void deleteWaypoint(final long waypointId) {
    AlertDialog dialog = null;
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setMessage(getString(R.string.marker_will_be_permanently_deleted));
    builder.setTitle(getString(R.string.are_you_sure_question));
    builder.setIcon(android.R.drawable.ic_dialog_alert);
    builder.setPositiveButton(getString(R.string.yes),
        new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int i) {
            dialog.dismiss();
            providerUtils.deleteWaypoint(waypointId,
                new StringUtils(MyTracksWaypointsList.this));
          }
        });
    builder.setNegativeButton(getString(R.string.no),
        new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int i) {
            dialog.dismiss();
          }
        });
    dialog = builder.create();
    dialog.show();
  }
}
