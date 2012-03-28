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
import com.google.android.apps.mytracks.util.CheckUnitsUtils;
import com.google.android.apps.mytracks.util.DialogUtils;
import com.google.android.apps.mytracks.util.EulaUtils;
import com.google.android.apps.mytracks.util.StringUtils;
import com.google.android.maps.mytracks.R;

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
import android.speech.tts.TextToSpeech;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
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
  private static final String CHECK_UNITS_DIALOG_TAG = "checkUnitsDialog";
  private static final String DELETE_ALL_DIALOG_TAG = "deleteAllDialog";
  private static final String EULA_DIALOG_TAG = "eulaDialog";  
  private static final String EXPORT_ALL_DIALOG_TAG = "exportAllDialog";
  
  private static final int WELCOME_ACTIVITY_REQUEST_CODE = 0;
  
  private static final String[] PROJECTION = new String[] {
      TracksColumns._ID,
      TracksColumns.NAME,
      TracksColumns.CATEGORY,
      TracksColumns.TOTALTIME,
      TracksColumns.TOTALDISTANCE,
      TracksColumns.STARTTIME,
      TracksColumns.DESCRIPTION };

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

    listView = (ListView) findViewById(R.id.track_list);
    listView.setOnItemClickListener(new OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Intent intent = new Intent(TrackListActivity.this, TrackDetailActivity.class)
            .putExtra(TrackDetailActivity.TRACK_ID, id);
        startActivity(intent);
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
      new EulaDialogFragment().show(getSupportFragmentManager(), EULA_DIALOG_TAG);
    }
  }

  @Override
  protected void onStart() {
    super.onStart();
    if (ServiceUtils.isRecording(
        this, trackRecordingServiceConnection.getServiceIfBound(), sharedPreferences)) {
      trackRecordingServiceConnection.startAndBind();
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
  public void onActivityResult(int requestCode, int resultCode, final Intent results) {
    if (requestCode == WELCOME_ACTIVITY_REQUEST_CODE) {
      if (!CheckUnitsUtils.getCheckUnitsValue(this)) {
        /*
         * See bug http://code.google.com/p/android/issues/detail?id=23761.
         * Instead of
         * new CheckUnitsDialogFragment().show(getSupportFragmentManager(), CHECK_UNITS_DIALOG_TAG)
         * Need to use commitAllowingStateLoss with the support package.
         */
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.add(new CheckUnitsDialogFragment(), CHECK_UNITS_DIALOG_TAG);
        fragmentTransaction.commitAllowingStateLoss();
      }
    }
  }
  
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.track_list, menu);

    recordTrack = menu.findItem(R.id.menu_record_track);
    stopRecording = menu.findItem(R.id.menu_stop_recording);
    search = menu.findItem(R.id.menu_search);
    importAll = menu.findItem(R.id.menu_import_all);
    exportAll = menu.findItem(R.id.menu_export_all);
    deleteAll = menu.findItem(R.id.menu_delete_all);

    ApiAdapterFactory.getApiAdapter().configureSearchWidget(this, search);
    updateMenu();
    return true;
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
        return ApiAdapterFactory.getApiAdapter().handleSearchMenuSelection(this);
      case R.id.menu_import_all:
        startActivity(
            new Intent(this, ImportActivity.class).putExtra(ImportActivity.EXTRA_IMPORT_ALL, true));
        return true;
      case R.id.menu_export_all:
        new ExportAllDialogFragment().show(getSupportFragmentManager(), EXPORT_ALL_DIALOG_TAG);
        return true;
      case R.id.menu_delete_all:
        new DeleteAllDialogFragment().show(getSupportFragmentManager(), DELETE_ALL_DIALOG_TAG);
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
          Intent intent = new Intent(this, TrackEditActivity.class)
              .putExtra(TrackEditActivity.SHOW_CANCEL, false)
              .putExtra(TrackEditActivity.TRACK_ID, recordingTrackId);
          startActivity(intent);
        }
        recordingTrackId = -1L;
        adapter.notifyDataSetChanged();
      } catch (Exception e) {
        Log.d(TAG, "Unable to stop recording.", e);
      }
    }
  }

  /**
   * Check Units DialogFragment.
   */
  private static class CheckUnitsDialogFragment extends DialogFragment {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

      return new AlertDialog.Builder(getActivity()).setCancelable(true)
          .setOnCancelListener(new DialogInterface.OnCancelListener() {
            public void onCancel(DialogInterface dialog) {
              CheckUnitsUtils.setCheckUnitsValue(getActivity());
            }
          })
          .setPositiveButton(R.string.generic_ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
              CheckUnitsUtils.setCheckUnitsValue(getActivity());

              int position = ((AlertDialog) dialog).getListView().getSelectedItemPosition();
              SharedPreferences sharedPreferences = getActivity()
                  .getSharedPreferences(Constants.SETTINGS_NAME, Context.MODE_PRIVATE);
              ApiAdapterFactory.getApiAdapter().applyPreferenceChanges(sharedPreferences.edit()
                  .putBoolean(getString(R.string.metric_units_key), position == 0));
            }
          })
          .setSingleChoiceItems(new CharSequence[] { getString(R.string.preferred_units_metric),
              getString(R.string.preferred_units_imperial) }, 0, null)
          .setTitle(R.string.preferred_units_title)
          .create();
    }
  }

  /**
   * Delete All DialogFragment.
   */
  private static class DeleteAllDialogFragment extends DialogFragment {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
      return DialogUtils.createConfirmationDialog(getActivity(),
          R.string.track_list_delete_all_confirm_message, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
              MyTracksProviderUtils.Factory.get(getActivity()).deleteAllTracks();
              /*
               * TODO Verify that selected_track_key is still needed with the
               * ICS navigation design
               */
              SharedPreferences sharedPreferences = getActivity()
                  .getSharedPreferences(Constants.SETTINGS_NAME, Context.MODE_PRIVATE);
              Editor editor = sharedPreferences.edit();
              // TODO: Go through data manager
              editor.putLong(getString(R.string.selected_track_key), -1L);
              ApiAdapterFactory.getApiAdapter().applyPreferenceChanges(editor);
            }
          });
    }
  };

  /**
   * Eula DialogFragment.
   */
  private static class EulaDialogFragment extends DialogFragment {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
      return new AlertDialog.Builder(getActivity())
          .setCancelable(true)
          .setMessage(
              EulaUtils.getEulaMessage(getActivity()))
          .setNegativeButton(R.string.eula_decline, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
              getActivity().finish();
            }
          })
          .setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
              getActivity().finish();
            }
          })
          .setPositiveButton(R.string.eula_accept, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
              EulaUtils.setEulaValue(getActivity());
              getActivity().startActivityForResult(
                  new Intent(getActivity(), WelcomeActivity.class), WELCOME_ACTIVITY_REQUEST_CODE);
            }
          })
          .setTitle(R.string.eula_title)
          .create();
    }
  }

  /**
   * Export All DialogFragment.
   */
  private static class ExportAllDialogFragment extends DialogFragment {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
      String exportFileFormat = getString(R.string.track_list_export_file);
      String fileTypes[] = getResources().getStringArray(R.array.file_types);
      String[] choices = new String[fileTypes.length];
      for (int i = 0; i < fileTypes.length; i++) {
        choices[i] = String.format(exportFileFormat, fileTypes[i]);
      }
      return new AlertDialog.Builder(getActivity()).setNegativeButton(R.string.generic_cancel, null)
          .setPositiveButton(R.string.generic_ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
              int index = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
              Intent intent = new Intent(getActivity(), ExportActivity.class).putExtra(
                  ExportActivity.EXTRA_TRACK_FILE_FORMAT,
                  (Parcelable) TrackFileFormat.values()[index]);
              getActivity().startActivity(intent);
            }
          })
          .setSingleChoiceItems(choices, 0, null)
          .setTitle(R.string.menu_export_all)
          .create();
    }
  };

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
