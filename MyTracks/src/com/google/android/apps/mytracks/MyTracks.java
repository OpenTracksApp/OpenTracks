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

import static com.google.android.apps.mytracks.DialogManager.DIALOG_IMPORT_PROGRESS;
import static com.google.android.apps.mytracks.DialogManager.DIALOG_PROGRESS;
import static com.google.android.apps.mytracks.DialogManager.DIALOG_SEND_TO_GOOGLE;
import static com.google.android.apps.mytracks.DialogManager.DIALOG_WRITE_PROGRESS;

import com.google.android.accounts.Account;
import com.google.android.apps.mymaps.MyMapsConstants;
import com.google.android.apps.mymaps.MyMapsList;
import com.google.android.apps.mytracks.content.MyTracksProviderUtils;
import com.google.android.apps.mytracks.content.Track;
import com.google.android.apps.mytracks.content.Waypoint;
import com.google.android.apps.mytracks.io.AuthManager;
import com.google.android.apps.mytracks.io.AuthManagerFactory;
import com.google.android.apps.mytracks.io.GpxImporter;
import com.google.android.apps.mytracks.io.SendToDocs;
import com.google.android.apps.mytracks.io.SendToMyMaps;
import com.google.android.apps.mytracks.io.TempFileCleaner;
import com.google.android.apps.mytracks.io.TrackWriter;
import com.google.android.apps.mytracks.io.TrackWriterFactory;
import com.google.android.apps.mytracks.io.SendToMyMaps.OnSendCompletedListener;
import com.google.android.apps.mytracks.io.TrackWriterFactory.TrackFileFormat;
import com.google.android.apps.mytracks.services.ITrackRecordingService;
import com.google.android.apps.mytracks.services.StatusAnnouncerFactory;
import com.google.android.apps.mytracks.services.TrackRecordingService;
import com.google.android.apps.mytracks.util.ApiFeatures;
import com.google.android.apps.mytracks.util.FileUtils;
import com.google.android.maps.mytracks.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.TabActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.RelativeLayout;
import android.widget.TabHost;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

/**
 * The super activity that embeds our sub activities.
 *
 * @author Leif Hendrik Wilden
 */
public class MyTracks extends TabActivity implements OnTouchListener,
    OnSharedPreferenceChangeListener, ProgressIndicator {

  private static final String WAYPOINT_ICON_URL =
      "http://maps.google.com/mapfiles/ms/micons/blue-pushpin.png";

  /**
   * Singleton instance
   */
  private static MyTracks instance;

  private ChartActivity chartActivity;

  /*
   * Authentication
   */
  private AuthManager auth;
  private final HashMap<String, AuthManager> authMap =
      new HashMap<String, AuthManager>();
  private final AccountChooser accountChooser = new AccountChooser();

  /*
   * Dialogs manager.
   */
  private DialogManager dialogManager;

  /*
   * Menu manager.
   */
  private MenuManager menuManager;

  /*
   * Information on upload success to MyMaps/Docs.
   * Used by SendToGoogleResultDialog.
   */

  public boolean sendToMyMapsSuccess = false;
  public boolean sendToDocsSuccess = false;
  public String sendToMyMapsMapId;
  public String sendToMyMapsMessage = "";
  public String sendToDocsMessage = "";

  /**
   * True if a new track should be created after the track recording service
   * binds.
   */
  private boolean startNewTrackRequested = false;

  private ITrackRecordingService trackRecordingService;

  /**
   * The id of the currently recording track.
   */
  private long recordingTrackId = -1;

  /**
   * The id of the currently selected track.
   */
  private long selectedTrackId = -1;

  /**
   * Does the user want to share the current track.
   */
  private boolean shareRequested = false;

  /**
   * Utilities to deal with the database.
   */
  private MyTracksProviderUtils providerUtils;

  private SharedPreferences sharedPreferences;

  /**
   * The connection to the track recording service.
   */
  private final ServiceConnection serviceConnection = new ServiceConnection() {
    @Override
    public void onServiceConnected(ComponentName className, IBinder service) {
      Log.d(MyTracksConstants.TAG, "MyTracks: Service now connected.");
      // Delay setting the service until we are done with initialization.
      ITrackRecordingService trackRecordingService =
          ITrackRecordingService.Stub.asInterface(service);
      try {
        // TODO: Send a start service intent and broadcast service started
        // message to avoid the hack below and a race condition.
        if (startNewTrackRequested) {
          startNewTrackRequested = false;
          startRecordingNewTrack(trackRecordingService);
        }
      } finally {
        MyTracks.this.trackRecordingService = trackRecordingService;
      }
    }

    @Override
    public void onServiceDisconnected(ComponentName className) {
      Log.d(MyTracksConstants.TAG, "MyTracks: Service now disconnected.");
      trackRecordingService = null;
    }
  };

  /**
   * Whether {@link #serviceConnection} is bound or not.
   */
  private boolean isBound = false;

  /*
   * Tabs/View navigation:
   */

  private NavControls navControls;

  private final Runnable changeTab = new Runnable() {
    public void run() {
      getTabHost().setCurrentTab(navControls.getCurrentIcons());
    }
  };

  public static MyTracks getInstance() {
    return instance;
  }

  /**
   * Checks whether we have a track recording session in progress.
   * In some cases, when the service has crashed or has been restarted
   * by the system, we fall back to the shared preferences.
   *
   * @return true if the activity is bound to the track recording service and
   *         the service is recording a track or in case the service is down,
   *         based on settings from the shared preferences.
   */
  public boolean isRecording() {
    if (trackRecordingService == null) {
      // Fall back to alternative check method.
      return isRecordingBasedOnSharedPreferences();
    }
    try {
      return trackRecordingService.isRecording();
    } catch (RemoteException e) {
      Log.e(MyTracksConstants.TAG, "MyTracks: Remote exception.", e);

      // Fall back to alternative check method.
      return isRecordingBasedOnSharedPreferences();
    }
  }

  private boolean isRecordingBasedOnSharedPreferences() {
    // TrackRecordingService guarantees that recordingTrackId is set to
    // -1 if the track has been stopped.
    return recordingTrackId >= 0;
  }

  /*
   * Application lifetime events:
   * ============================
   */

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    Log.d(MyTracksConstants.TAG, "MyTracks.onCreate");
    super.onCreate(savedInstanceState);
    instance = this;
    providerUtils = MyTracksProviderUtils.Factory.get(this);
    menuManager = new MenuManager(this);
    sharedPreferences = getSharedPreferences(MyTracksSettings.SETTINGS_NAME, 0);
    dialogManager = new DialogManager(this);

    // The volume we want to control is the Text-To-Speech volume
    int volumeStream =
        new StatusAnnouncerFactory(ApiFeatures.getInstance()).getVolumeStream();
    setVolumeControlStream(volumeStream);

    // We don't need a window title bar:
    requestWindowFeature(Window.FEATURE_NO_TITLE);

    final Resources res = getResources();
    final TabHost tabHost = getTabHost();
    tabHost.addTab(tabHost.newTabSpec("tab1")
        .setIndicator("Map", res.getDrawable(
            android.R.drawable.ic_menu_mapmode))
        .setContent(new Intent(this, MyTracksMap.class)));
    tabHost.addTab(tabHost.newTabSpec("tab2")
        .setIndicator("Stats", res.getDrawable(R.drawable.menu_stats))
        .setContent(new Intent(this, StatsActivity.class)));
    tabHost.addTab(tabHost.newTabSpec("tab3")
        .setIndicator("Chart", res.getDrawable(R.drawable.menu_elevation))
        .setContent(new Intent(this, ChartActivity.class)));

    // Hide the tab widget itself. We'll use overlayed prev/next buttons to
    // switch between the tabs:
    tabHost.getTabWidget().setVisibility(View.GONE);

    RelativeLayout layout = new RelativeLayout(this);
    LayoutParams params =
        new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
    layout.setLayoutParams(params);
    navControls =
        new NavControls(this, layout,
            getResources().obtainTypedArray(R.array.left_icons),
            getResources().obtainTypedArray(R.array.right_icons),
            changeTab);
    navControls.show();
    tabHost.addView(layout);
    layout.setOnTouchListener(this);

    if (sharedPreferences != null) {
      selectedTrackId =
          sharedPreferences.getLong(getString(R.string.selected_track_key), -1);
      recordingTrackId = sharedPreferences.getLong(
          getString(R.string.recording_track_key), -1);
      sharedPreferences.registerOnSharedPreferenceChangeListener(this);
      Log.d(MyTracksConstants.TAG, "recordingTrackId: " + recordingTrackId
          + ", selectedTrackId: " + selectedTrackId);
      if (recordingTrackId > 0) {
        Intent startIntent = new Intent(this, TrackRecordingService.class);
        startService(startIntent);
      }
    }

    // This will show the eula until the user accepts or quits the app.
    Eula.showEula(this);

    // Check if we got invoked via the VIEW intent:
    Intent intent = getIntent();
    String action;
    if (intent != null && (action = intent.getAction()) != null) {
      if (action.equals(Intent.ACTION_MAIN)) {
        // Do nothing.
      } else if (action.equals(Intent.ACTION_VIEW)) {
        if (intent.getScheme() != null && intent.getScheme().equals("file")) {
          Log.w(MyTracksConstants.TAG,
              "Received a VIEW intent with file scheme. Importing.");
          importGpxFile(intent.getData().getPath());
        } else {
          Log.w(MyTracksConstants.TAG,
              "Received a VIEW intent with unsupported scheme "
              + intent.getScheme());
        }
      } else {
        Log.w(MyTracksConstants.TAG,
            "Received an intent with unsupported action " + action);
      }
    } else {
      Log.d(MyTracksConstants.TAG, "Received an intent with no action.");
    }
  }

  @Override
  protected void onDestroy() {
    Log.d(MyTracksConstants.TAG, "MyTracks.onDestroy");
    tryUnbindTrackRecordingService();
    super.onDestroy();
  }

  @Override
  protected void onPause() {
    // Called when activity is going into the background, but has not (yet) been
    // killed. Shouldn't block longer than approx. 2 seconds.
    Log.d(MyTracksConstants.TAG, "MyTracks.onPause");
    tryUnbindTrackRecordingService();
    super.onPause();
  }

  @Override
  protected void onResume() {
    // Called when the current activity is being displayed or re-displayed
    // to the user.
    Log.d(MyTracksConstants.TAG, "MyTracks.onResume");
    tryBindTrackRecordingService();
    super.onResume();
  }

  @Override
  protected void onStop() {
    Log.d(MyTracksConstants.TAG, "MyTracks.onStop");
    // Clean up any temporary track files.
    TempFileCleaner.clean();
    super.onStop();
  }

  /*
   * Menu events:
   * ============
   */

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    super.onCreateOptionsMenu(menu);
    return menuManager.onCreateOptionsMenu(menu);
  }

  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    menuManager.onPrepareOptionsMenu(menu, providerUtils.getLastTrack() != null,
        isRecording(), selectedTrackId >= 0);
    return super.onPrepareOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    return menuManager.onOptionsItemSelected(item)
        ? true
        : super.onOptionsItemSelected(item);
  }

  /*
   * Dialog events:
   * ==============
   */

  @Override
  protected Dialog onCreateDialog(int id, Bundle args) {
    return dialogManager.onCreateDialog(id, args);
  }

  @Override
  protected void onPrepareDialog(int id, Dialog dialog) {
    super.onPrepareDialog(id, dialog);
    dialogManager.onPrepareDialog(id, dialog);
  }

  /*
   * Key events:
   * ===========
   */

  @Override
  public boolean onTrackballEvent(MotionEvent event) {
    if (isRecording()) {
      if (event.getAction() == MotionEvent.ACTION_DOWN) {
        insertStatisticsMarker();
        return true;
      }
    }
    return super.onTrackballEvent(event);
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode,
      final Intent results) {
    TrackFileFormat exportFormat = null;
    switch (requestCode) {
      case MyTracksConstants.GET_MAP: {
        if (resultCode == RESULT_OK) {
          results.putExtra("trackid", selectedTrackId);
          if (results.hasExtra("mapid")) {
            sendToMyMapsMapId = results.getStringExtra("mapid");
          }
          setProgressMessage(
              R.string.progress_message_authenticating_mymaps);
          authenticate(results, MyTracksConstants.SEND_TO_GOOGLE,
              MyMapsConstants.MAPSHOP_SERVICE);
        } else {
          dialogManager.dismissDialogSafely(DIALOG_PROGRESS);
        }
        break;
      }
      case MyTracksConstants.GET_LOGIN: {
        if (resultCode == RESULT_OK && auth != null) {
          if (!auth.authResult(resultCode, results)) {
            dialogManager.dismissDialogSafely(DIALOG_PROGRESS);
          }
        } else {
          dialogManager.dismissDialogSafely(DIALOG_PROGRESS);
        }
        break;
      }
      case MyTracksConstants.SHOW_TRACK: {
        if (results != null) {
          final long trackId = results.getLongExtra("trackid", -1);
          if (trackId >= 0) {
            setSelectedTrackId(trackId);
            // The track list passed the requested action as result code. Hand
            // it off to the onAcitivtyResult for further processing:
            if (resultCode != MyTracksConstants.SHOW_TRACK) {
              onActivityResult(resultCode, Activity.RESULT_OK, results);
            }
          }
        }
        break;
      }
      case MyTracksConstants.SHOW_WAYPOINT: {
        if (results != null) {
          final long waypointId = results.getLongExtra("waypointid", -1);
          if (waypointId >= 0) {
            MyTracksMap map =
                (MyTracksMap) getLocalActivityManager().getActivity("tab1");
            if (map != null) {
              getTabHost().setCurrentTab(0);
              map.showWaypoint(waypointId);
            }
          }
        }
        break;
      }
      case MyTracksConstants.DELETE_TRACK: {
        if (results != null && resultCode == RESULT_OK) {
          final long trackId = results.getLongExtra("trackid", selectedTrackId);
          deleteTrack(trackId);
        }
        break;
      }
      case MyTracksConstants.EDIT_DETAILS: {
        if (results != null && resultCode == RESULT_OK) {
          final long trackId = results.getLongExtra("trackid", selectedTrackId);
          Intent intent = new Intent(this, MyTracksDetails.class);
          intent.putExtra("trackid", trackId);
          startActivity(intent);
        }
        break;
      }
      case MyTracksConstants.AUTHENTICATE_TO_DOCS: {
        if (resultCode == RESULT_OK) {
          setProgressValue(0);
          setProgressMessage(
              R.string.progress_message_authenticating_docs);
          authenticate(results,
              MyTracksConstants.AUTHENTICATE_TO_TRIX, "writely");
        } else {
          dialogManager.dismissDialogSafely(DIALOG_PROGRESS);
        }
        break;
      }
      case MyTracksConstants.AUTHENTICATE_TO_TRIX: {
        if (resultCode == RESULT_OK) {
          setProgressValue(30);
          setProgressMessage(
              R.string.progress_message_authenticating_docs);
          authenticate(results, MyTracksConstants.SEND_TO_DOCS, "wise");
        } else {
          dialogManager.dismissDialogSafely(DIALOG_PROGRESS);
        }
        break;
      }
      case MyTracksConstants.SEND_TO_DOCS: {
        if (results != null && resultCode == RESULT_OK) {
          Log.d(MyTracksConstants.TAG, "Sending to Docs....");
          setProgressValue(50);
          setProgressMessage(R.string.progress_message_sending_docs);
          final long trackId = results.getLongExtra("trackid", selectedTrackId);
          final SendToDocs sender = new SendToDocs(this, authMap.get("wise"),
              authMap.get("writely"), trackId);
          Runnable onCompletion = new Runnable() {
            public void run() {
              setProgressValue(100);
              dialogManager.dismissDialogSafely(DIALOG_PROGRESS);
              runOnUiThread(new Runnable() {
                public void run() {
                  sendToDocsMessage = sender.getStatusMessage();
                  sendToDocsSuccess = sender.wasSuccess();
                  handleMapsFinish();
                }
              });
            }
          };
          sender.setOnCompletion(onCompletion);
          sender.run();
        } else {
          dialogManager.dismissDialogSafely(DIALOG_PROGRESS);
        }
        break;
      }
      case MyTracksConstants.SEND_TO_GOOGLE_DIALOG: {
        shareRequested = false;
        dialogManager.showDialogSafely(DIALOG_SEND_TO_GOOGLE);
        break;
      }
      case MyTracksConstants.SEND_TO_GOOGLE: {
        if (results != null && resultCode == RESULT_OK) {
          final String mapId;
          final long trackId;
          if (results.hasExtra("mapid")) {
            mapId = results.getStringExtra("mapid");
          } else {
            mapId = "new";
          }
          if (results.hasExtra("trackid")) {
            trackId = results.getLongExtra("trackid", -1);
          } else {
            trackId = selectedTrackId;
          }

          OnSendCompletedListener onCompletion = new OnSendCompletedListener() {
            @Override
            public void onSendCompleted(String mapId, boolean success,
                int statusMessage) {
              sendToMyMapsMessage = getString(statusMessage);
              sendToMyMapsSuccess = success;
              if (sendToMyMapsSuccess) {
                sendToMyMapsMapId = mapId;
                // Update the map id for this track:
                try {
                  Track track = providerUtils.getTrack(trackId);
                  track.setMapId(mapId);
                  providerUtils.updateTrack(track);
                } catch (RuntimeException e) {
                  // If that fails whatever reasons we'll just log an error, but
                  // continue.
                  Log.w(MyTracksConstants.TAG, "Updating map id failed.", e);
                }
              }
              if (dialogManager.getSendToGoogleDialog().getSendToDocs()) {
                onActivityResult(MyTracksConstants.AUTHENTICATE_TO_DOCS,
                    RESULT_OK, new Intent());
              } else {
                dialogManager.dismissDialogSafely(DIALOG_PROGRESS);
                runOnUiThread(new Runnable() {
                  public void run() {
                    handleMapsFinish();
                  }
                });
              }
            }
          };
          final SendToMyMaps sender = new SendToMyMaps(this, mapId, auth,
              trackId, this/*progressIndicator*/, onCompletion);

          HandlerThread handlerThread = new HandlerThread("SendToMyMaps");
          handlerThread.start();
          Handler handler = new Handler(handlerThread.getLooper());
          handler.post(sender);
        } else {
          dialogManager.dismissDialogSafely(DIALOG_PROGRESS);
        }
        break;
      }
      case MyTracksConstants.SAVE_GPX_FILE:
        if (exportFormat == null) { exportFormat = TrackFileFormat.GPX; }
        //$FALL-THROUGH$
      case MyTracksConstants.SAVE_KML_FILE:
        if (exportFormat == null) { exportFormat = TrackFileFormat.KML; }
        //$FALL-THROUGH$
      case MyTracksConstants.SAVE_CSV_FILE:
        if (exportFormat == null) { exportFormat = TrackFileFormat.CSV; }
        //$FALL-THROUGH$
      case MyTracksConstants.SAVE_TCX_FILE:
        if (exportFormat == null) { exportFormat = TrackFileFormat.TCX; }

        if (results != null && resultCode == Activity.RESULT_OK) {
          final long trackId = results.getLongExtra("trackid", selectedTrackId);
          if (trackId >= 0) {
            saveTrack(trackId, exportFormat);
          }
        }
        break;
      case MyTracksConstants.SHARE_LINK: {
        Track selectedTrack = providerUtils.getTrack(selectedTrackId);
        if (selectedTrack != null) {
          if (selectedTrack.getMapId().length() > 0) {
            shareLinkToMyMap(selectedTrack.getMapId());
          } else {
            shareRequested = true;
            dialogManager.showDialogSafely(DIALOG_SEND_TO_GOOGLE);
          }
        }
        break;
      }
      case MyTracksConstants.SHARE_GPX_FILE:
        if (exportFormat == null) { exportFormat = TrackFileFormat.GPX; }
        //$FALL-THROUGH$
      case MyTracksConstants.SHARE_KML_FILE:
        if (exportFormat == null) { exportFormat = TrackFileFormat.KML; }
        //$FALL-THROUGH$
      case MyTracksConstants.SHARE_CSV_FILE:
        if (exportFormat == null) { exportFormat = TrackFileFormat.CSV; }
        //$FALL-THROUGH$
      case MyTracksConstants.SHARE_TCX_FILE: {
        if (exportFormat == null) { exportFormat = TrackFileFormat.TCX; }

        if (results != null && resultCode == Activity.RESULT_OK) {
          final long trackId = results.getLongExtra("trackid", selectedTrackId);
          if (trackId >= 0) {
            sendTrack(trackId, exportFormat);
          }
        }
        break;
      }
      case MyTracksConstants.CLEAR_MAP: {
        setSelectedTrackId(-1);
        break;
      }
      case MyTracksConstants.WELCOME: {
        CheckUnits.check(this);
        break;
      }

      default: {
        Log.w(MyTracksConstants.TAG,
            "Warning unhandled request code: " + requestCode);
      }
    }
  }

  @Override
  public boolean onTouch(View v, MotionEvent event) {
    if (event.getAction() == MotionEvent.ACTION_DOWN) {
      navControls.show();
    }
    return false;
  }

  @Override
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
      String key) {
    if (key != null && key.equals(getString(R.string.selected_track_key))) {
      selectedTrackId = sharedPreferences.getLong(
          getString(R.string.selected_track_key), -1);
    }
    if (key != null && key.equals(getString(R.string.recording_track_key))) {
      recordingTrackId = sharedPreferences.getLong(
          getString(R.string.recording_track_key), -1);
    }
  }

  /**
   * Resets status information for sending to MyMaps/Docs.
   */
  public void resetSendToGoogleStatus() {
    sendToMyMapsMessage = "";
    sendToMyMapsSuccess = true;
    sendToDocsMessage = "";
    sendToDocsSuccess = true;
    sendToMyMapsMapId = null;
  }

  private void importGpxFile(final String fileName) {
    dialogManager.showDialogSafely(DIALOG_IMPORT_PROGRESS);
    Thread t = new Thread() {
      @Override
      public void run() {
        int message = R.string.success;

        long[] trackIdsImported = null;

        try {
          try {
            InputStream is = new FileInputStream(fileName);
            trackIdsImported = GpxImporter.importGPXFile(is, providerUtils);
          } catch (SAXException e) {
            Log.e(MyTracksConstants.TAG, "Caught an unexpected exception.", e);
            message = R.string.error_generic;
          } catch (ParserConfigurationException e) {
            Log.e(MyTracksConstants.TAG, "Caught an unexpected exception.", e);
            message = R.string.error_generic;
          } catch (IOException e) {
            Log.e(MyTracksConstants.TAG, "Caught an unexpected exception.", e);
            message = R.string.error_unable_to_read_file;
          } catch (NullPointerException e) {
            Log.e(MyTracksConstants.TAG, "Caught an unexpected exception.", e);
            message = R.string.error_invalid_gpx_format;
          } catch (OutOfMemoryError e) {
            Log.e(MyTracksConstants.TAG, "Caught an unexpected exception.", e);
            message = R.string.error_out_of_memory;
          }
          if (trackIdsImported != null && trackIdsImported.length > 0) {
            // select last track from import file
            setSelectedTrackId(trackIdsImported[trackIdsImported.length - 1]);
          } else {
            dialogManager.showMessageDialog(message, false/* success */);
          }
        } finally {
          runOnUiThread(new Runnable() {
            public void run() {
              dismissDialog(DIALOG_IMPORT_PROGRESS);
            }
          });
        }
      }
    };
    t.start();
  }

  // ProgressIndicator implementation

  @Override
  public void setProgressMessage(int resId) {
    dialogManager.setProgressMessage(getString(resId));
  }

  @Override
  public void clearProgressMessage() {
    dialogManager.setProgressMessage("");
  }

  @Override
  public void setProgressValue(final int percent) {
    dialogManager.setProgressValue(percent);
  }

  /**
   * Shares a link to a My Map via external app (email, gmail, ...)
   * A chooser with apps that support text/plain will be shown to the user.
   */
  public void shareLinkToMyMap(String mapId) {
    Intent shareIntent = new Intent(Intent.ACTION_SEND);
    shareIntent.setType("text/plain");
    shareIntent.putExtra(Intent.EXTRA_SUBJECT,
        getResources().getText(R.string.share_map_subject).toString());

    boolean shareUrlOnly = true;
    if (sharedPreferences != null) {
      shareUrlOnly = sharedPreferences.getBoolean(
          getString(R.string.share_url_only_key), false);
    }

    String url = MyMapsConstants.MAPSHOP_BASE_URL + "?msa=0&msid=" + mapId;
    String msg = shareUrlOnly ? url : String.format(
        getResources().getText(R.string.share_map_body_format).toString(), url);
    shareIntent.putExtra(Intent.EXTRA_TEXT, msg);
    startActivity(Intent.createChooser(shareIntent,
        getResources().getText(R.string.share_map).toString()));
  }

  /**
   * Deletes the track with the given id.
   * Prompts the user if he want to really delete the track first.
   * If the selected track is deleted, the selection will be removed.
   */
  public void deleteTrack(final long trackId) {
    AlertDialog dialog = null;
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setMessage(getString(R.string.track_will_be_permanently_deleted));
    builder.setTitle(getString(R.string.are_you_sure_question));
    builder.setIcon(android.R.drawable.ic_dialog_alert);
    builder.setPositiveButton(getString(R.string.yes),
        new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int i) {
        dialog.dismiss();
        providerUtils.deleteTrack(trackId);
        if (trackId == selectedTrackId) {
          setSelectedTrackId(-1);
        }
      }});
    builder.setNegativeButton(getString(R.string.no),
        new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int i) {
        dialog.dismiss();
      }
    });
    dialog = builder.create();
    dialog.show();
  }

  public Location getCurrentLocation() {
    // TODO: Let's look at more advanced algorithms to determine the best
    // current location.
    LocationManager locationManager =
        (LocationManager) getSystemService(Context.LOCATION_SERVICE);
    if (locationManager == null) {
      return null;
    }
    final long maxAgeMilliSeconds = 1000 * 60 * 1;  // 1 minute
    final long maxAgeNetworkMilliSeconds = 1000 * 60 * 10;  // 10 minutes
    final long now = System.currentTimeMillis();
    Location loc = locationManager.getLastKnownLocation(
        MyTracksConstants.GPS_PROVIDER);
    if (loc == null || loc.getTime() < now - maxAgeMilliSeconds) {
      // We don't have a recent GPS fix, just use cell towers if available
      loc = locationManager.getLastKnownLocation(
          LocationManager.NETWORK_PROVIDER);
      if (loc == null || loc.getTime() < now - maxAgeNetworkMilliSeconds) {
        // We don't have a recent cell tower location, let the user know:
        Toast.makeText(this, getString(R.string.status_no_location),
            Toast.LENGTH_LONG).show();
        return null;
      } else {
       // Let the user know we have only an approximate location:
       Toast.makeText(this, getString(R.string.status_approximate_location),
           Toast.LENGTH_LONG).show();
      }
    }
    return loc;
  }

  public Location getLastLocation() {
    if (providerUtils.getLastLocationId(recordingTrackId) < 0) {
      return null;
    }
    LocationManager locationManager =
        (LocationManager) getSystemService(Context.LOCATION_SERVICE);
    return locationManager.getLastKnownLocation(MyTracksConstants.GPS_PROVIDER);
  }

  /**
   * Inserts a waypoint marker.
   *
   * @return the id of the inserted statistics marker, or
   *   -1 unable to find location
   *   -2 track recording service is not running?
   *   -3 remote exception when contacting track recording service
   *   -4 inserting marker into provider failed
   */
  public long insertWaypointMarker() {
    Location location = getCurrentLocation();
    if (location == null) {
      Toast.makeText(this, R.string.error_unable_to_insert_marker,
          Toast.LENGTH_LONG).show();
      return -1;
    }
    if (trackRecordingService != null) {
      try {
        Waypoint wpt = new Waypoint();
        wpt.setName(getString(R.string.waypoint));
        wpt.setType(Waypoint.TYPE_WAYPOINT);
        wpt.setTrackId(recordingTrackId);
        wpt.setIcon(WAYPOINT_ICON_URL);
        wpt.setLocation(location);
        long waypointId = trackRecordingService.insertWaypointMarker(wpt);
        if (waypointId >= 0) {
          Toast.makeText(this, R.string.status_waypoint_inserted,
              Toast.LENGTH_LONG).show();
          return waypointId;
        } else {
          Toast.makeText(this, R.string.error_unable_to_insert_marker,
              Toast.LENGTH_LONG).show();
          Log.e(MyTracksConstants.TAG, "Cannot insert waypoint marker?");
          return -4;
        }
      } catch (RemoteException e) {
        Toast.makeText(this, R.string.error_unable_to_insert_marker,
            Toast.LENGTH_LONG).show();
        Log.e(MyTracksConstants.TAG, "Cannot insert waypoint marker.", e);
      }
      return -3;
    }
    return -2;
  }

  /**
   * Inserts a statistics marker.
   *
   * @return the id of the inserted statistics marker, or
   *   -1 unable to find location
   *   -2 track recording service is not running?
   *   -3 remote exception when contacting track recording service
   *   -4 inserting marker into provider failed
   */
  public long insertStatisticsMarker() {
    Location location = getLastLocation();
    if (location == null) {
      Toast.makeText(this, R.string.error_unable_to_insert_marker,
          Toast.LENGTH_LONG).show();
      return -1;
    }
    if (trackRecordingService != null) {
      try {
        long waypointId =
            trackRecordingService.insertStatisticsMarker(location);
        if (waypointId >= 0) {
          Toast.makeText(this, R.string.status_statistics_inserted,
              Toast.LENGTH_LONG).show();
          return waypointId;
        } else {
          Toast.makeText(this, R.string.error_unable_to_insert_marker,
              Toast.LENGTH_LONG).show();
          Log.e(MyTracksConstants.TAG, "Cannot insert statistics marker?");
          return -4;
        }
      } catch (RemoteException e) {
        Toast.makeText(this, R.string.error_unable_to_insert_marker,
            Toast.LENGTH_LONG).show();
        Log.e(MyTracksConstants.TAG, "Cannot insert statistics marker?", e);
      }
      return -3;
    }
    return -2;
  }

  /**
   * Initializes the authentication manager which obtains an authentication
   * token, prompting the user for a login and password if needed.
   */
  private void authenticate(final Intent results, final int requestCode,
      final String service) {
    auth = authMap.get(service);
    if (auth == null) {
      Log.i(MyTracksConstants.TAG,
          "Creating a new authentication for service: " + service);
      auth = AuthManagerFactory.getAuthManager(this,
          MyTracksConstants.GET_LOGIN,
          null,
          true,
          service);
      authMap.put(service, auth);
    }
    Log.d(MyTracksConstants.TAG, "Loggin in to " + service + "...");
    if (AuthManagerFactory.useModernAuthManager()) {
      runOnUiThread(new Runnable() {
        @Override
        public void run() {
          accountChooser.chooseAccount(MyTracks.this,
              new AccountChooser.AccountHandler() {
                @Override
                public void handleAccountSelected(Account account) {
                  if (account == null) {
                    dialogManager.dismissDialogSafely(DIALOG_PROGRESS);
                    return;
                  }
                  doLogin(results, requestCode, service, account);
                }
              });
        }
      });
    } else {
      doLogin(results, requestCode, service, null);
    }
  }

  private void doLogin(final Intent results, final int requestCode,
      final String service, final Account account) {
    auth.doLogin(new Runnable() {
      public void run() {
        Log.d(MyTracksConstants.TAG, "Loggin success for " + service + "!");
        onActivityResult(requestCode, RESULT_OK, results);
      }
    }, account);
  }

  private void startRecordingNewTrack(
      ITrackRecordingService trackRecordingService) {
    try {
      recordingTrackId = trackRecordingService.startNewTrack();
      // Select the recording track.
      setSelectedTrackId(recordingTrackId);
      Toast.makeText(this, getString(R.string.status_now_recording),
          Toast.LENGTH_SHORT).show();
    } catch (RemoteException e) {
      Toast.makeText(this,
          getString(R.string.error_unable_to_start_recording),
          Toast.LENGTH_SHORT).show();
      Log.w(MyTracksConstants.TAG, "Unable to start recording.", e);
    }
  }

  /**
   * Starts the track recording service (if not already running) and binds to
   * it. Starts recording a new track.
   */
  public void startRecording() {
    if (trackRecordingService == null) {
      startNewTrackRequested = true;
      Intent startIntent = new Intent(this, TrackRecordingService.class);
      startService(startIntent);
      tryBindTrackRecordingService();
    } else {
      startRecordingNewTrack(trackRecordingService);
    }
  }

  /**
   * Stops the track recording service and unbinds from it. Will display a toast
   * "Stopped recording" and pop up the Track Details activity.
   */
  public void stopRecording() {
    if (trackRecordingService != null) {
      // Save the track id as the shared preference will overwrite the recording track id.
      long currentTrackId = recordingTrackId;
      try {
        trackRecordingService.endCurrentTrack();
      } catch (RemoteException e) {
        Log.e(MyTracksConstants.TAG, "Unable to stop recording.", e);
      }
      Intent intent = new Intent(MyTracks.this, MyTracksDetails.class);
      intent.putExtra("trackid", currentTrackId);
      intent.putExtra("hasCancelButton", false);
      startActivity(intent);
    }
    tryUnbindTrackRecordingService();
    try {
      stopService(new Intent(MyTracks.this, TrackRecordingService.class));
    } catch (SecurityException e) {
      Log.e(MyTracksConstants.TAG,
          "Encountered a security exception when trying to stop service.", e);
    }
    trackRecordingService = null;
  }

  /**
   * Initiates the process to send tracks to google.
   * This is called once the user has selected sending options via the
   * SendToGoogleDialog.
   */
  public void sendToGoogle() {
    SendToGoogleDialog sendToGoogleDialog =
        dialogManager.getSendToGoogleDialog();
    if (sendToGoogleDialog == null) {
      return;
    }
    setProgressValue(0);
    clearProgressMessage();
    dialogManager.showDialogSafely(DIALOG_PROGRESS);
    if (sendToGoogleDialog.getSendToMyMaps()) {
      if (!sendToGoogleDialog.getCreateNewMap()) {
        Intent listIntent = new Intent(this, MyMapsList.class);
        startActivityForResult(listIntent, MyTracksConstants.GET_MAP);
      } else {
        setProgressValue(0);
        setProgressMessage(
            R.string.progress_message_authenticating_mymaps);
        authenticate(new Intent(), MyTracksConstants.SEND_TO_GOOGLE,
            MyMapsConstants.MAPSHOP_SERVICE);
      }
    } else {
      onActivityResult(MyTracksConstants.AUTHENTICATE_TO_DOCS, RESULT_OK,
          new Intent());
    }
  }

  /**
   * Writes the selected track id to the shared preferences.
   * Executed on the UI thread.
   *
   * @param trackId the id of the track
   */
  public void setSelectedTrackId(final long trackId) {
    sharedPreferences
        .edit()
        .putLong(getString(R.string.selected_track_key), trackId)
        .commit();
  }

  long getSelectedTrackId() {
    return selectedTrackId;
  }
  
  /**
   * Binds to track recording service if it is running.
   */
  private void tryBindTrackRecordingService() {
    Log.d(MyTracksConstants.TAG,
        "MyTracks: Trying to bind to track recording service...");
    bindService(new Intent(this, TrackRecordingService.class),
        serviceConnection, 0);
    Log.d(MyTracksConstants.TAG, "MyTracks: ...bind finished!");
    isBound = true;
  }

  /**
   * Tries to unbind the track recording service. Catches exception silently in
   * case service is not registered anymore.
   */
  private void tryUnbindTrackRecordingService() {
    if (isBound) {
      Log.d(MyTracksConstants.TAG,
          "MyTracks: Trying to unbind from track recording service...");
      try {
        unbindService(serviceConnection);
        Log.d(MyTracksConstants.TAG, "MyTracks: ...unbind finished!");
      } catch (IllegalArgumentException e) {
        Log.d(MyTracksConstants.TAG,
            "MyTracks: Tried unbinding, but service was not registered.", e);
      }
      isBound = false;
    }
  }

  /**
   * Saves the track with the given id to the SD card.
   *
   * @param trackId The id of the track to be sent
   */
  public void saveTrack(long trackId, TrackFileFormat format) {
    dialogManager.showDialogSafely(DIALOG_WRITE_PROGRESS);
    final TrackWriter writer =
        TrackWriterFactory.newWriter(this, providerUtils, trackId, format);
    writer.setOnCompletion(new Runnable() {
      public void run() {
        dialogManager.dismissDialogSafely(DIALOG_WRITE_PROGRESS);
        dialogManager.showMessageDialog(writer.getErrorMessage(),
            writer.wasSuccess());
      }
    });
    writer.writeTrackAsync();
  }

  /**
   * Sends the requested track as an email attachment.
   * This will leave the gpx file on the SD card for at least one hour.
   * Temporary gpx files will be deleted in onStop.
   *
   * @param trackId The id of the track to be sent
   */
  public void sendTrack(long trackId, final TrackFileFormat format) {
    dialogManager.showDialogSafely(DIALOG_WRITE_PROGRESS);
    final TrackWriter writer =
        TrackWriterFactory.newWriter(this, providerUtils, trackId, format);

    FileUtils fileUtils = new FileUtils();
    String extension = format.getExtension();
    String dirName = fileUtils.buildExternalDirectoryPath(extension, "tmp");

    File dir = new File(dirName);
    writer.setDirectory(dir);
    writer.setOnCompletion(new Runnable() {
      public void run() {
        dialogManager.dismissDialogSafely(DIALOG_WRITE_PROGRESS);
        if (!writer.wasSuccess()) {
          dialogManager.showMessageDialog(writer.getErrorMessage(),
              writer.wasSuccess());
        } else {
          Intent shareIntent = new Intent(Intent.ACTION_SEND);
          shareIntent.putExtra(Intent.EXTRA_SUBJECT,
              getResources().getText(R.string.send_track_subject).toString());
          shareIntent.putExtra(Intent.EXTRA_TEXT,
              getResources().getText(R.string.send_track_body_format)
              .toString());
          shareIntent.setType(format.getMimeType());
          Uri u = Uri.fromFile(new File(writer.getAbsolutePath()));
          shareIntent.putExtra(Intent.EXTRA_STREAM, u);
          startActivity(Intent.createChooser(shareIntent,
              getResources().getText(R.string.share_track).toString()));
        }
      }
    });
    writer.writeTrackAsync();
  }

  /**
   * Notifies that uploading to maps is finished.
   */
  private void handleMapsFinish() {
    if (shareRequested && sendToMyMapsSuccess) {
      // Just share
      Toast.makeText(this, getMapsResultMessage(), Toast.LENGTH_LONG).show();
      shareLinkToMyMap(sendToMyMapsMapId);
    } else {
      dialogManager.showDialogSafely(
          DialogManager.DIALOG_SEND_TO_GOOGLE_RESULT);
    }
  }

  public String getMapsResultMessage() {
    StringBuilder message = new StringBuilder();
    SendToGoogleDialog sendToGoogleDialog =
      dialogManager.getSendToGoogleDialog();
    if (sendToGoogleDialog.getSendToMyMaps()) {
      message.append(sendToMyMapsMessage);
    }
    if (sendToGoogleDialog.getSendToDocs()) {
      if (message.length() > 0) {
        message.append(' ');
      }
      message.append(sendToDocsMessage);
    }
    if (sendToMyMapsSuccess && sendToDocsSuccess) {
      message.append(' ');
      message.append(getString(R.string.status_mymap_info));
    }
    return message.toString();
  }

  public AccountChooser getAccountChooser() {
    return accountChooser;
  }

  public ChartActivity getChartActivity() {
    return chartActivity;
  }

  public void setChartActivity(ChartActivity chartActivity) {
    this.chartActivity = chartActivity;
  }

  public DialogManager getDialogManager() {
    return dialogManager;
  }

  public String getSendToMyMapsMapId() {
    return sendToMyMapsMapId;
  }

  public boolean getSendToGoogleSuccess() {
    return sendToMyMapsSuccess && sendToDocsSuccess;
  }

  // @VisibleForTesting
  long getRecordingTrackId() {
    return recordingTrackId;
  }
  
  // @VisibleForTesting
  SharedPreferences getSharedPreferences() {
    return sharedPreferences;
  }
  
  // @VisibleForTesting
  static void clearInstance() {
    instance = null;
  }
  
  // @VisibleForTesting
  ITrackRecordingService getTrackRecordingService() {
    return trackRecordingService;
  }
}
