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
import com.google.android.apps.mytracks.content.Track;
import com.google.android.apps.mytracks.content.TracksColumns;
import com.google.android.apps.mytracks.fragments.ConfirmDeleteDialogFragment;
import com.google.android.apps.mytracks.fragments.EnableSyncDialogFragment;
import com.google.android.apps.mytracks.fragments.EnableSyncDialogFragment.EnableSyncCaller;
import com.google.android.apps.mytracks.fragments.EulaDialogFragment;
import com.google.android.apps.mytracks.fragments.EulaDialogFragment.EulaCaller;
import com.google.android.apps.mytracks.fragments.FileTypeDialogFragment;
import com.google.android.apps.mytracks.fragments.FileTypeDialogFragment.FileTypeCaller;
import com.google.android.apps.mytracks.io.file.ImportActivity;
import com.google.android.apps.mytracks.io.file.SaveActivity;
import com.google.android.apps.mytracks.io.file.TrackFileFormat;
import com.google.android.apps.mytracks.io.sendtogoogle.SendRequest;
import com.google.android.apps.mytracks.io.sync.SyncUtils;
import com.google.android.apps.mytracks.services.ITrackRecordingService;
import com.google.android.apps.mytracks.services.MyTracksLocationManager;
import com.google.android.apps.mytracks.services.RemoveTempFilesService;
import com.google.android.apps.mytracks.services.TrackRecordingServiceConnection;
import com.google.android.apps.mytracks.settings.SettingsActivity;
import com.google.android.apps.mytracks.util.AnalyticsUtils;
import com.google.android.apps.mytracks.util.ApiAdapterFactory;
import com.google.android.apps.mytracks.util.EulaUtils;
import com.google.android.apps.mytracks.util.GoogleFeedbackUtils;
import com.google.android.apps.mytracks.util.GoogleLocationUtils;
import com.google.android.apps.mytracks.util.IntentUtils;
import com.google.android.apps.mytracks.util.ListItemUtils;
import com.google.android.apps.mytracks.util.PreferencesUtils;
import com.google.android.apps.mytracks.util.StringUtils;
import com.google.android.apps.mytracks.util.TrackIconUtils;
import com.google.android.apps.mytracks.util.TrackRecordingServiceConnectionUtils;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.maps.mytracks.BuildConfig;
import com.google.android.maps.mytracks.R;

import android.app.Dialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Looper;
import android.os.Parcelable;
import android.os.RemoteException;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.Toast;

import java.util.Locale;

/**
 * An activity displaying a list of tracks.
 * 
 * @author Leif Hendrik Wilden
 */
public class TrackListActivity extends AbstractSendToGoogleActivity
    implements EulaCaller, EnableSyncCaller, FileTypeCaller {

  private static final String TAG = TrackListActivity.class.getSimpleName();
  private static final String[] PROJECTION = new String[] { TracksColumns._ID, TracksColumns.NAME,
      TracksColumns.DESCRIPTION, TracksColumns.CATEGORY, TracksColumns.STARTTIME,
      TracksColumns.TOTALDISTANCE, TracksColumns.TOTALTIME, TracksColumns.ICON,
      TracksColumns.SHAREDWITHME, TracksColumns.SHAREDOWNER };

  // Callback when the trackRecordingServiceConnection binding changes.
  private final Runnable bindChangedCallback = new Runnable() {
      @Override
    public void run() {
      /*
       * After binding changes (e.g., becomes available), update the total time
       * in trackController.
       */
      runOnUiThread(new Runnable() {
          @Override
        public void run() {
          trackController.update(recordingTrackId != PreferencesUtils.RECORDING_TRACK_ID_DEFAULT,
              recordingTrackPaused);
        }
      });

      if (!startGps && !startNewRecording) {
        return;
      }

      ITrackRecordingService service = trackRecordingServiceConnection.getServiceIfBound();
      if (service == null) {
        Log.d(TAG, "service not available to start gps or a new recording");
        return;
      }
      if (startNewRecording) {
        startGps = false;
        try {
          long trackId = service.startNewTrack();
          startNewRecording = false;
          Intent intent = IntentUtils.newIntent(TrackListActivity.this, TrackDetailActivity.class)
              .putExtra(TrackDetailActivity.EXTRA_TRACK_ID, trackId);
          startActivity(intent);
          Toast.makeText(
              TrackListActivity.this, R.string.track_list_record_success, Toast.LENGTH_SHORT)
              .show();
        } catch (RemoteException e) {
          Toast.makeText(
              TrackListActivity.this, R.string.track_list_record_error, Toast.LENGTH_LONG).show();
          Log.e(TAG, "Unable to start a new recording.", e);
        }
      }
      if (startGps) {
        try {
          service.startGps();
          startGps = false;
        } catch (RemoteException e) {
          Toast.makeText(TrackListActivity.this, R.string.gps_starting_error, Toast.LENGTH_LONG)
              .show();
          Log.e(TAG, "Unable to start gps");
        }
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
          if (key == null || key.equals(
              PreferencesUtils.getKey(TrackListActivity.this, R.string.stats_units_key))) {
            metricUnits = PreferencesUtils.isMetricUnits(TrackListActivity.this);
          }
          if (key == null || key.equals(
              PreferencesUtils.getKey(TrackListActivity.this, R.string.recording_track_id_key))) {
            recordingTrackId = PreferencesUtils.getLong(
                TrackListActivity.this, R.string.recording_track_id_key);
            if (key != null && recordingTrackId != PreferencesUtils.RECORDING_TRACK_ID_DEFAULT) {
              trackRecordingServiceConnection.startAndBind();
            }
          }
          if (key == null || key.equals(PreferencesUtils.getKey(
              TrackListActivity.this, R.string.recording_track_paused_key))) {
            recordingTrackPaused = PreferencesUtils.getBoolean(TrackListActivity.this,
                R.string.recording_track_paused_key,
                PreferencesUtils.RECORDING_TRACK_PAUSED_DEFAULT);
          }
          if (key == null || key.equals(
              PreferencesUtils.getKey(TrackListActivity.this, R.string.drive_sync_key))) {
            driveSync = PreferencesUtils.getBoolean(TrackListActivity.this, R.string.drive_sync_key,
                PreferencesUtils.DRIVE_SYNC_DEFAULT);
          }
          if (key != null) {
            runOnUiThread(new Runnable() {
                @Override
              public void run() {
                ApiAdapterFactory.getApiAdapter().invalidMenu(TrackListActivity.this);
                getSupportLoaderManager().restartLoader(0, null, loaderCallbacks);
                boolean isRecording = recordingTrackId
                    != PreferencesUtils.RECORDING_TRACK_ID_DEFAULT;
                trackController.update(isRecording, recordingTrackPaused);
              }
            });
          }
        }
      };

  // Callback when an item is selected in the contextual action mode
  private final ContextualActionModeCallback
      contextualActionModeCallback = new ContextualActionModeCallback() {

          @Override
        public void onPrepare(Menu menu, int[] positions, long[] ids, boolean showSelectAll) {
          boolean isRecording = recordingTrackId != PreferencesUtils.RECORDING_TRACK_ID_DEFAULT;
          boolean isSingleSelection = ids.length == 1;
          boolean isSingleSelectionShareWithMe;
          if (isSingleSelection) {
            Track track = myTracksProviderUtils.getTrack(ids[0]);
            isSingleSelectionShareWithMe = track.isSharedWithMe();
          } else {
            isSingleSelectionShareWithMe = false;
          }

          // Not recording
          menu.findItem(R.id.list_context_menu_play).setVisible(!isRecording);
          // Not recording, one item, not sharedWithMe item
          menu.findItem(R.id.list_context_menu_share)
              .setVisible(!isRecording && isSingleSelection && !isSingleSelectionShareWithMe);
          // Always disable
          menu.findItem(R.id.list_context_menu_show_on_map).setVisible(false);
          // One item, not sharedWithMe item
          menu.findItem(R.id.list_context_menu_edit)
              .setVisible(isSingleSelection && !isSingleSelectionShareWithMe);
          // delete is always enabled
          menu.findItem(R.id.list_context_menu_select_all).setVisible(showSelectAll);
        }

          @Override
        public boolean onClick(int itemId, int[] positions, long[] ids) {
          return handleContextItem(itemId, ids);
        };
      };

  private final OnClickListener recordListener = new OnClickListener() {
    public void onClick(View v) {
      if (recordingTrackId == PreferencesUtils.RECORDING_TRACK_ID_DEFAULT) {
        // Not recording -> Recording
        AnalyticsUtils.sendPageViews(TrackListActivity.this, AnalyticsUtils.ACTION_RECORD_TRACK);
        updateMenuItems(false, true);
        startRecording();
      } else {
        if (recordingTrackPaused) {
          // Paused -> Resume
          AnalyticsUtils.sendPageViews(TrackListActivity.this, AnalyticsUtils.ACTION_RESUME_TRACK);
          updateMenuItems(false, true);
          TrackRecordingServiceConnectionUtils.resumeTrack(trackRecordingServiceConnection);
          trackController.update(true, false);
        } else {
          // Recording -> Paused
          AnalyticsUtils.sendPageViews(TrackListActivity.this, AnalyticsUtils.ACTION_PAUSE_TRACK);
          updateMenuItems(false, true);
          TrackRecordingServiceConnectionUtils.pauseTrack(trackRecordingServiceConnection);
          trackController.update(true, true);
        }
      }
    }
  };

  private final OnClickListener stopListener = new OnClickListener() {
      @Override
    public void onClick(View v) {
      AnalyticsUtils.sendPageViews(TrackListActivity.this, AnalyticsUtils.ACTION_STOP_RECORDING);
      updateMenuItems(false, false);
      TrackRecordingServiceConnectionUtils.stopRecording(
          TrackListActivity.this, trackRecordingServiceConnection, true);
    }
  };

  private final LoaderCallbacks<Cursor> loaderCallbacks = new LoaderCallbacks<Cursor>() {
      @Override
    public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
      return new CursorLoader(TrackListActivity.this, TracksColumns.CONTENT_URI, PROJECTION, null,
          null,
          "IFNULL(" + TracksColumns.SHAREDWITHME + ",0) ASC, " + TracksColumns.STARTTIME + " DESC");
    }

      @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
      sectionResourceCursorAdapter.swapCursor(cursor);
    }

      @Override
    public void onLoaderReset(Loader<Cursor> loader) {
      sectionResourceCursorAdapter.swapCursor(null);
    }
  };
  
  // The following are set in onCreate
  private MyTracksProviderUtils myTracksProviderUtils;
  private SharedPreferences sharedPreferences;
  private TrackRecordingServiceConnection trackRecordingServiceConnection;
  private TrackController trackController;
  private ListView listView;
  private SectionResourceCursorAdapter sectionResourceCursorAdapter;

  // Preferences
  private boolean metricUnits = true;
  private long recordingTrackId = PreferencesUtils.RECORDING_TRACK_ID_DEFAULT;
  private boolean recordingTrackPaused = PreferencesUtils.RECORDING_TRACK_PAUSED_DEFAULT;
  private boolean driveSync = PreferencesUtils.DRIVE_SYNC_DEFAULT;

  // Menu items
  private MenuItem searchMenuItem;
  private MenuItem startGpsMenuItem;
  private MenuItem refreshMenuItem;
  private MenuItem exportAllMenuItem;
  private MenuItem importAllMenuItem;
  private MenuItem deleteAllMenuItem;

  private boolean startGps = false; // true to start gps
  private boolean startNewRecording = false; // true to start a new recording

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    if (BuildConfig.DEBUG) {
      ApiAdapterFactory.getApiAdapter().enableStrictMode();
    }
    Intent intent = new Intent(this, RemoveTempFilesService.class);
    startService(intent);

    myTracksProviderUtils = MyTracksProviderUtils.Factory.get(this);
    sharedPreferences = getSharedPreferences(Constants.SETTINGS_NAME, Context.MODE_PRIVATE);

    trackRecordingServiceConnection = new TrackRecordingServiceConnection(
        this, bindChangedCallback);
    trackController = new TrackController(
        this, trackRecordingServiceConnection, true, recordListener, stopListener);

    setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL);
    // Show trackController when search dialog is dismissed
    SearchManager searchManager = (SearchManager) getSystemService(SEARCH_SERVICE);
    searchManager.setOnDismissListener(new SearchManager.OnDismissListener() {
        @Override
      public void onDismiss() {
        trackController.show();
      }
    });

    listView = (ListView) findViewById(R.id.track_list);
    listView.setEmptyView(findViewById(R.id.track_list_empty_view));
    listView.setOnItemClickListener(new OnItemClickListener() {
        @Override
      public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Intent newIntent = IntentUtils.newIntent(TrackListActivity.this, TrackDetailActivity.class)
            .putExtra(TrackDetailActivity.EXTRA_TRACK_ID, id);
        startActivity(newIntent);
      }
    });
    sectionResourceCursorAdapter = new SectionResourceCursorAdapter(
        this, R.layout.list_item, null, 0) {
        @Override
      public void bindView(View view, Context context, Cursor cursor) {
        int idIndex = cursor.getColumnIndex(TracksColumns._ID);
        int iconIndex = cursor.getColumnIndex(TracksColumns.ICON);
        int nameIndex = cursor.getColumnIndex(TracksColumns.NAME);
        int categoryIndex = cursor.getColumnIndex(TracksColumns.CATEGORY);
        int totalTimeIndex = cursor.getColumnIndexOrThrow(TracksColumns.TOTALTIME);
        int totalDistanceIndex = cursor.getColumnIndexOrThrow(TracksColumns.TOTALDISTANCE);
        int startTimeIndex = cursor.getColumnIndexOrThrow(TracksColumns.STARTTIME);
        int descriptionIndex = cursor.getColumnIndex(TracksColumns.DESCRIPTION);
        int sharedOwnerIndex = cursor.getColumnIndex(TracksColumns.SHAREDOWNER);

        boolean isRecording = cursor.getLong(idIndex) == recordingTrackId;
        String icon = cursor.getString(iconIndex);
        int iconId = TrackIconUtils.getIconDrawable(icon);
        String name = cursor.getString(nameIndex);
        String category = icon != null && !icon.equals("") ? null : cursor.getString(categoryIndex);
        String totalTime = StringUtils.formatElapsedTime(cursor.getLong(totalTimeIndex));
        String totalDistance = StringUtils.formatDistance(
            TrackListActivity.this, cursor.getDouble(totalDistanceIndex), metricUnits);
        long startTime = cursor.getLong(startTimeIndex);
        String description = cursor.getString(descriptionIndex);
        String sharedOwner = cursor.getString(sharedOwnerIndex);

        ListItemUtils.setListItem(TrackListActivity.this, view, isRecording, recordingTrackPaused,
            iconId, R.string.icon_track, name, category, totalTime, totalDistance, startTime,
            description, sharedOwner);
      }
    };
    listView.setAdapter(sectionResourceCursorAdapter);
    ApiAdapterFactory.getApiAdapter()
        .configureListViewContextualMenu(this, listView, contextualActionModeCallback);

    getSupportLoaderManager().initLoader(0, null, loaderCallbacks);
    showStartupDialogs();
  }

  @Override
  protected void onStart() {
    super.onStart();

    // Register shared preferences listener
    sharedPreferences.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);

    // Update shared preferences
    sharedPreferenceChangeListener.onSharedPreferenceChanged(null, null);

    // Update track recording service connection
    TrackRecordingServiceConnectionUtils.startConnection(this, trackRecordingServiceConnection);

    AnalyticsUtils.sendPageViews(this, AnalyticsUtils.PAGE_TRACK_LIST);
  }

  @Override
  protected void onResume() {
    super.onResume();
    
    // Update UI
    ApiAdapterFactory.getApiAdapter().invalidMenu(this);
    getSupportLoaderManager().restartLoader(0, null, loaderCallbacks);
    boolean isRecording = recordingTrackId != PreferencesUtils.RECORDING_TRACK_ID_DEFAULT;
    trackController.onResume(isRecording, recordingTrackPaused);
  }

  @Override
  protected void onPause() {
    super.onPause();

    // Update UI
    trackController.onPause();
  }

  @Override
  protected void onStop() {
    super.onStop();

    // Unregister shared preferences listener
    sharedPreferences.unregisterOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);

    trackRecordingServiceConnection.unbind();

    AnalyticsUtils.dispatch();
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == GOOGLE_PLAY_SERVICES_REQUEST_CODE) {
      checkGooglePlayServices();
    } else {
      super.onActivityResult(requestCode, resultCode, data);
    }
  }

  @Override
  protected int getLayoutResId() {
    return R.layout.track_list;
  }

  @Override
  protected boolean configureActionBarHomeAsUp() {
    return false;
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.track_list, menu);

    menu.findItem(R.id.track_list_feedback)
        .setVisible(ApiAdapterFactory.getApiAdapter().isGoogleFeedbackAvailable());

    searchMenuItem = menu.findItem(R.id.track_list_search);
    ApiAdapterFactory.getApiAdapter().configureSearchWidget(this, searchMenuItem, trackController);

    startGpsMenuItem = menu.findItem(R.id.track_list_start_gps);
    refreshMenuItem = menu.findItem(R.id.track_list_refresh);
    exportAllMenuItem = menu.findItem(R.id.track_list_export_all);
    importAllMenuItem = menu.findItem(R.id.track_list_import_all);
    deleteAllMenuItem = menu.findItem(R.id.track_list_delete_all);
    return true;
  }
  
  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    boolean isGpsStarted = TrackRecordingServiceConnectionUtils.isRecordingServiceRunning(this);
    boolean isRecording = recordingTrackId != PreferencesUtils.RECORDING_TRACK_ID_DEFAULT;
    updateMenuItems(isGpsStarted, isRecording);
    return super.onPrepareOptionsMenu(menu);
  }
  
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    Intent intent;
    switch (item.getItemId()) {
      case R.id.track_list_search:
        return ApiAdapterFactory.getApiAdapter().handleSearchMenuSelection(this);
      case R.id.track_list_start_gps:
        MyTracksLocationManager myTracksLocationManager = new MyTracksLocationManager(
            this, Looper.myLooper(), false);
        if (!myTracksLocationManager.isGpsProviderEnabled()) {
          intent = GoogleLocationUtils.isAvailable(TrackListActivity.this) ? new Intent(
              GoogleLocationUtils.ACTION_GOOGLE_LOCATION_SETTINGS)
              : new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
          intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
          startActivity(intent);
        } else {
          startGps = !TrackRecordingServiceConnectionUtils.isRecordingServiceRunning(
              this);      

          // Show toast
          Toast toast = Toast.makeText(
              this, startGps ? R.string.gps_starting : R.string.gps_stopping, Toast.LENGTH_SHORT);
          toast.setGravity(Gravity.CENTER, 0, 0);
          toast.show();

          // Invoke trackRecordingService
          if (startGps) {
            trackRecordingServiceConnection.startAndBind();
            bindChangedCallback.run();
          } else {
            ITrackRecordingService trackRecordingService = trackRecordingServiceConnection
                .getServiceIfBound();
            if (trackRecordingService != null) {
              try {
                trackRecordingService.stopGps();
              } catch (RemoteException e) {
                Log.e(TAG, "Unable to stop gps.", e);
              }
            }
            trackRecordingServiceConnection.unbindAndStop();
          }
          
          // Update menu after starting or stopping gps
          ApiAdapterFactory.getApiAdapter().invalidMenu(this);  
        }
        myTracksLocationManager.close();
        return true;
      case R.id.track_list_refresh:
        if (driveSync) {
          SyncUtils.syncNow(this);
        } else {
          PreferencesUtils.setString(
              this, R.string.google_account_key, PreferencesUtils.GOOGLE_ACCOUNT_DEFAULT);
          SendRequest sendRequest = new SendRequest(-1L);
          sendRequest.setSendDrive(true);
          sendRequest.setDriveSync(true);
          sendRequest.setDriveSyncConfirm(true);
          sendToGoogle(sendRequest);         
        }
        return true;
      case R.id.track_list_aggregated_statistics:
        intent = IntentUtils.newIntent(this, AggregatedStatsActivity.class);
        startActivity(intent);
        return true;
      case R.id.track_list_export_all:
        FileTypeDialogFragment.newInstance(R.id.track_list_export_all,
            R.string.export_all_title, R.string.export_all_option, 4)
            .show(getSupportFragmentManager(), FileTypeDialogFragment.FILE_TYPE_DIALOG_TAG);
        return true;
      case R.id.track_list_import_all:
        FileTypeDialogFragment.newInstance(R.id.track_list_import_all,
            R.string.import_selection_title, R.string.import_selection_option, 2)
            .show(getSupportFragmentManager(), FileTypeDialogFragment.FILE_TYPE_DIALOG_TAG);
        return true;
      case R.id.track_list_delete_all:
        ConfirmDeleteDialogFragment.newInstance(new long[] {-1L})
            .show(getSupportFragmentManager(), ConfirmDeleteDialogFragment.CONFIRM_DELETE_DIALOG_TAG);
        return true;
      case R.id.track_list_settings:
        intent = IntentUtils.newIntent(this, SettingsActivity.class);
        startActivity(intent);
        return true;
      case R.id.track_list_feedback:
        GoogleFeedbackUtils.bindFeedback(this);
        return true;
      case R.id.track_list_help:
        intent = IntentUtils.newIntent(this, HelpActivity.class);
        startActivity(intent);
        return true;
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
    if (handleContextItem(item.getItemId(), new long[] { info.id })) {
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

  @Override
  public boolean onSearchRequested() {
    // Hide trackController when search dialog is shown
    trackController.hide();
    return super.onSearchRequested();
  };

  @Override
  protected TrackRecordingServiceConnection getTrackRecordingServiceConnection() {
    return trackRecordingServiceConnection;
  }

  @Override
  protected void onDeleted() {
    // Do nothing
  }

  @Override
  public void onFileTypeDone(int menuId, TrackFileFormat trackFileFormat) {
    Intent intent;
    switch (menuId) {
      case R.id.track_list_export_all:
        AnalyticsUtils.sendPageViews(
            this, AnalyticsUtils.ACTION_EXPORT_ALL_PREFIX + trackFileFormat.getExtension());
        intent = IntentUtils.newIntent(this, SaveActivity.class)
            .putExtra(SaveActivity.EXTRA_TRACK_IDS, new long[] {-1L})   
            .putExtra(SaveActivity.EXTRA_TRACK_FILE_FORMAT, (Parcelable) trackFileFormat);
        startActivity(intent);
        break;
      case R.id.track_list_import_all:
        AnalyticsUtils.sendPageViews(
            this, AnalyticsUtils.ACTION_IMPORT_ALL_PREFIX + trackFileFormat.getExtension());
        intent = IntentUtils.newIntent(this, ImportActivity.class)
            .putExtra(ImportActivity.EXTRA_IMPORT_ALL, true)
            .putExtra(ImportActivity.EXTRA_TRACK_FILE_FORMAT, (Parcelable) trackFileFormat);
        startActivity(intent);
        break;
      default:
    }
  }

  /**
   * Shows start up dialogs.
   */
  public void showStartupDialogs() {
    if (!EulaUtils.hasAcceptEula(this)) {
      Fragment fragment = getSupportFragmentManager()
          .findFragmentByTag(EulaDialogFragment.EULA_DIALOG_TAG);
      if (fragment == null) {
        EulaDialogFragment.newInstance(false)
            .show(getSupportFragmentManager(), EulaDialogFragment.EULA_DIALOG_TAG);
      }
    } else {
      if (!EulaUtils.hasDefaultUnits(this)) {
        String statsUnits = getString(
            Locale.US.equals(Locale.getDefault()) ? R.string.stats_units_imperial
                : R.string.stats_units_metric);
        PreferencesUtils.setString(this, R.string.stats_units_key, statsUnits);        
        EulaUtils.setDefaultUnits(this);
      }
      checkGooglePlayServices();
    }
  }

  @Override
  public void onEulaDone() {
    if (EulaUtils.hasAcceptEula(this)) {
      showStartupDialogs();
      return;
    }
    finish();
  }

  private void checkGooglePlayServices() {
    int code = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
    if (code != ConnectionResult.SUCCESS) {
      Dialog dialog = GooglePlayServicesUtil.getErrorDialog(
          code, this, GOOGLE_PLAY_SERVICES_REQUEST_CODE, new DialogInterface.OnCancelListener() {

              @Override
            public void onCancel(DialogInterface dialogInterface) {
              finish();
            }
          });
      if (dialog != null) {
        dialog.show();
        return;
      }
    }
    showEnableSync();
  }

  private void showEnableSync() {
    if (EulaUtils.hasShowEnableSync(this)) {
      Fragment fragment = getSupportFragmentManager()
          .findFragmentByTag(EnableSyncDialogFragment.ENABLE_SYNC_DIALOG_TAG);
      if (fragment == null) {
        new EnableSyncDialogFragment().show(
            getSupportFragmentManager(), EnableSyncDialogFragment.ENABLE_SYNC_DIALOG_TAG);
      }
    }
  }

  @Override
  public void onEnableSyncDone(boolean enable) {
    EulaUtils.setShowEnableSync(this);
    if (enable) {
      SendRequest sendRequest = new SendRequest(-1L);
      sendRequest.setSendDrive(true);
      sendRequest.setDriveSync(true);
      sendToGoogle(sendRequest);
    }
  }

  /**
   * Updates the menu items.
   * 
   * @param isGpsStarted true if gps is started
   * @param isRecording true if recording
   */
  private void updateMenuItems(boolean isGpsStarted, boolean isRecording) {
    if (startGpsMenuItem != null) {
      startGpsMenuItem.setVisible(!isRecording);
      if (!isRecording) {
        startGpsMenuItem.setTitle(isGpsStarted ? R.string.menu_stop_gps : R.string.menu_start_gps);
        startGpsMenuItem.setIcon(
            isGpsStarted ? R.drawable.ic_menu_stop_gps : R.drawable.ic_menu_start_gps);
      }
    }
    if (refreshMenuItem != null) {
      refreshMenuItem.setTitle(driveSync ? R.string.menu_refresh : R.string.menu_sync_drive);
    }
    if (exportAllMenuItem != null) {
      exportAllMenuItem.setVisible(!isRecording);
    }
    if (importAllMenuItem != null) {
      importAllMenuItem.setVisible(!isRecording);
    }
    if (deleteAllMenuItem != null) {
      deleteAllMenuItem.setVisible(!isRecording);
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

  /**
   * Handles a context item selection.
   * 
   * @param itemId the menu item id
   * @param trackIds the track ids
   * @return true if handled.
   */
  private boolean handleContextItem(int itemId, long[] trackIds) {
    switch (itemId) {
      case R.id.list_context_menu_play:
        confirmPlay(trackIds);
        return true;
      case R.id.list_context_menu_share:
        shareTrack(trackIds[0]);
        return true;
      case R.id.list_context_menu_edit:
        Intent intent = IntentUtils.newIntent(this, TrackEditActivity.class)
            .putExtra(TrackEditActivity.EXTRA_TRACK_ID, trackIds[0]);
        startActivity(intent);
        return true;
      case R.id.list_context_menu_delete:
        if (trackIds.length > 1 && trackIds.length == listView.getCount()) {
          trackIds = new long[] {-1L};
        }
        ConfirmDeleteDialogFragment.newInstance(trackIds)
            .show(getSupportFragmentManager(), ConfirmDeleteDialogFragment.CONFIRM_DELETE_DIALOG_TAG);
        return true;
      case R.id.list_context_menu_select_all:
        int size = listView.getCount();
        for (int i = 0; i < size; i++) {
          listView.setItemChecked(i, true);
        }
        return false;
      default:
        return false;
    }
  }
}
