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
import static com.google.android.apps.mytracks.DialogManager.DIALOG_IMPORT_PROGRESS;
import static com.google.android.apps.mytracks.DialogManager.DIALOG_PROGRESS;
import static com.google.android.apps.mytracks.DialogManager.DIALOG_SEND_TO_GOOGLE;
import static com.google.android.apps.mytracks.DialogManager.DIALOG_WRITE_PROGRESS;

import com.google.android.accounts.Account;
import com.google.android.apps.mytracks.content.MyTracksProviderUtils;
import com.google.android.apps.mytracks.content.Track;
import com.google.android.apps.mytracks.content.WaypointCreationRequest;
import com.google.android.apps.mytracks.io.AuthManager;
import com.google.android.apps.mytracks.io.AuthManagerFactory;
import com.google.android.apps.mytracks.io.GpxImporter;
import com.google.android.apps.mytracks.io.SendToDocs;
import com.google.android.apps.mytracks.io.SendToFusionTables;
import com.google.android.apps.mytracks.io.SendToFusionTables.OnSendCompletedListener;
import com.google.android.apps.mytracks.io.SendToMyMaps;
import com.google.android.apps.mytracks.io.TempFileCleaner;
import com.google.android.apps.mytracks.io.TrackWriter;
import com.google.android.apps.mytracks.io.TrackWriterFactory;
import com.google.android.apps.mytracks.io.TrackWriterFactory.TrackFileFormat;
import com.google.android.apps.mytracks.io.mymaps.MapsFacade;
import com.google.android.apps.mytracks.io.mymaps.MyMapsConstants;
import com.google.android.apps.mytracks.io.sendtogoogle.ResultDialogFactory;
import com.google.android.apps.mytracks.io.sendtogoogle.SendDialog;
import com.google.android.apps.mytracks.io.sendtogoogle.SendResult;
import com.google.android.apps.mytracks.io.sendtogoogle.SendType;
import com.google.android.apps.mytracks.services.ITrackRecordingService;
import com.google.android.apps.mytracks.services.StatusAnnouncerFactory;
import com.google.android.apps.mytracks.services.TrackRecordingService;
import com.google.android.apps.mytracks.util.ApiFeatures;
import com.google.android.apps.mytracks.util.FileUtils;
import com.google.android.apps.mytracks.util.SystemUtils;
import com.google.android.maps.mytracks.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.TabActivity;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.widget.RelativeLayout;
import android.widget.TabHost;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

/**
 * The super activity that embeds our sub activities.
 *
 * @author Leif Hendrik Wilden
 */
public class MyTracks extends TabActivity implements OnTouchListener,
    ProgressIndicator {
  /**
   * Singleton instance
   */
  private static MyTracks instance;

  private TrackDataHub dataHub;
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

  public long sendToTrackId = -1;
  public boolean sendToMyMapsSuccess = false;
  public boolean sendToFusionTablesSuccess = false;
  public boolean sendToDocsSuccess = false;
  public String sendToMyMapsMapId;
  public String sendToMyMapsMessage = "";
  public String sendToFusionTablesTableId;
  public String sendToFusionTablesMessage = "";
  public String sendToDocsMessage = "";

  /**
   * True if a new track should be created after the track recording service
   * binds.
   */
  private boolean startNewTrackRequested = false;

  private ITrackRecordingService trackRecordingService;

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
      Log.d(Constants.TAG, "MyTracks: Service now connected.");
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
      Log.d(TAG, "MyTracks: Service now disconnected.");
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
      return dataHub.isRecording();
    }
    try {
      return trackRecordingService.isRecording();
      // TODO: We catch Exception, because after eliminating the service process
      // all exceptions it may throw are no longer wrapped in a RemoteException.
    } catch (Exception e) {
      Log.e(TAG, "MyTracks: Remote exception.", e);

      // Fall back to alternative check method.
      return dataHub.isRecording();
    }
  }

  /*
   * Application lifetime events:
   * ============================
   */

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    Log.d(TAG, "MyTracks.onCreate");
    super.onCreate(savedInstanceState);
    instance = this;
    ApiFeatures apiFeatures = ApiFeatures.getInstance();
    if (!SystemUtils.isRelease(this)) {
      apiFeatures.getApiPlatformAdapter().enableStrictMode();
    }

    providerUtils = MyTracksProviderUtils.Factory.get(this);
    dataHub = new TrackDataHub(this, providerUtils);
    menuManager = new MenuManager(this);
    sharedPreferences = getSharedPreferences(Constants.SETTINGS_NAME, 0);
    dialogManager = new DialogManager(this);

    // The volume we want to control is the Text-To-Speech volume
    int volumeStream =
        new StatusAnnouncerFactory(apiFeatures).getVolumeStream();
    setVolumeControlStream(volumeStream);

    // We don't need a window title bar:
    requestWindowFeature(Window.FEATURE_NO_TITLE);

    final Resources res = getResources();
    final TabHost tabHost = getTabHost();
    tabHost.addTab(tabHost.newTabSpec("tab1")
        .setIndicator("Map", res.getDrawable(
            android.R.drawable.ic_menu_mapmode))
        .setContent(new Intent(this, MapActivity.class)));
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

    // This will show the eula until the user accepts or quits the app.
    Eula.showEulaRequireAcceptance(this);

    // Check if we got invoked via the VIEW intent:
    Intent intent = getIntent();
    String action;
    if (intent != null && (action = intent.getAction()) != null) {
      if (action.equals(Intent.ACTION_MAIN)) {
        // Do nothing.
      } else if (action.equals(Intent.ACTION_VIEW)) {
        if (intent.getScheme() != null && intent.getScheme().equals("file")) {
          Log.w(TAG, "Received a VIEW intent with file scheme. Importing.");
          importGpxFile(intent.getData().getPath());
        } else {
          Log.w(TAG, "Received a VIEW intent with unsupported scheme " + intent.getScheme());
        }
      } else {
        Log.w(TAG, "Received an intent with unsupported action " + action);
      }
    } else {
      Log.d(TAG, "Received an intent with no action.");
    }
  }

  @Override
  protected void onDestroy() {
    Log.d(TAG, "MyTracks.onDestroy");

    dataHub.destroy();

    tryUnbindTrackRecordingService();
    super.onDestroy();
  }

  @Override
  protected void onStop() {
    Log.d(TAG, "MyTracks.onStop");

    dataHub.stop();

    // Clean up any temporary track files.
    TempFileCleaner.clean();
    super.onStop();
  }

  @Override
  protected void onPause() {
    // Called when activity is going into the background, but has not (yet) been
    // killed. Shouldn't block longer than approx. 2 seconds.
    Log.d(TAG, "MyTracks.onPause");
    tryUnbindTrackRecordingService();
    super.onPause();
  }

  @Override
  protected void onResume() {
    // Called when the current activity is being displayed or re-displayed
    // to the user.
    Log.d(TAG, "MyTracks.onResume");
    tryBindTrackRecordingService();
    super.onResume();
  }

  @Override
  protected void onStart() {
    Log.d(TAG, "MyTracks.onStart");
    super.onStart();
    dataHub.start();
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
        isRecording(), dataHub.isATrackSelected());
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
  protected Dialog onCreateDialog(int id) {
    return dialogManager.onCreateDialog(id, null);
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
        try {
          insertWaypoint(WaypointCreationRequest.DEFAULT_STATISTICS);
        } catch (RemoteException e) {
          Log.e(TAG, "Cannot insert statistics marker.", e);
        } catch (IllegalStateException e) {
          Log.e(TAG, "Cannot insert statistics marker.", e);
        }
        return true;
      }
    }
    return super.onTrackballEvent(event);
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode,
      final Intent results) {
    TrackFileFormat exportFormat = null;
    final long trackId = results.getLongExtra("trackid", dataHub.getSelectedTrackId());
    switch (requestCode) {
      case Constants.GET_LOGIN: {
        if (resultCode != RESULT_OK || auth == null || !auth.authResult(resultCode, results)) {
          dialogManager.dismissDialogSafely(DIALOG_PROGRESS);
        }
        break;
      }
      case Constants.SHOW_TRACK: {
        if (results != null) {
          if (trackId >= 0) {
            dataHub.loadTrack(trackId);

            // The track list passed the requested action as result code. Hand
            // it off to the onAcitivtyResult for further processing:
            if (resultCode != Constants.SHOW_TRACK) {
              onActivityResult(resultCode, Activity.RESULT_OK, results);
            }
          }
        }
        break;
      }
      case Constants.SHOW_WAYPOINT: {
        if (results != null) {
          final long waypointId = results.getLongExtra("waypointid", -1);
          if (waypointId >= 0) {
            MapActivity map =
                (MapActivity) getLocalActivityManager().getActivity("tab1");
            if (map != null) {
              getTabHost().setCurrentTab(0);
              map.showWaypoint(waypointId);
            }
          }
        }
        break;
      }
      case Constants.DELETE_TRACK: {
        if (results != null && resultCode == RESULT_OK) {
          deleteTrack(trackId);
        }
        break;
      }
      case Constants.EDIT_DETAILS: {
        if (results != null && resultCode == RESULT_OK) {
          Intent intent = new Intent(this, TrackDetails.class);
          intent.putExtra("trackid", trackId);
          startActivity(intent);
        }
        break;
      }
      case Constants.SEND_TO_GOOGLE_DIALOG: {
        shareRequested = false;
        dialogManager.showDialogSafely(DIALOG_SEND_TO_GOOGLE);
        break;
      }
      case Constants.GET_MAP: {
        // User picked a map to upload to
        if (resultCode == RESULT_OK) {
          results.putExtra("trackid", dataHub.getSelectedTrackId());
          if (results.hasExtra("mapid")) {
            sendToMyMapsMapId = results.getStringExtra("mapid");
          }
          authenticateToGoogleMaps(results);
        } else {
          onSendToGoogleDone();
        }
        break;
      }
      case Constants.AUTHENTICATE_TO_MY_MAPS: {
        // Authenticated with Google My Maps
        if (results != null && resultCode == RESULT_OK) {
          final String mapId;
          if (results.hasExtra("mapid")) {
            mapId = results.getStringExtra("mapid");
          } else {
            mapId = "new";
          }

          sendToGoogleMaps(trackId, mapId);
        } else {
          onSendToGoogleDone();
        }
        break;
      }
      case Constants.AUTHENTICATE_TO_FUSION_TABLES: {
        // Authenticated with Google Fusion Tables
        if (results != null && resultCode == RESULT_OK) {
          sendToFusionTables(trackId);
        } else {
          onSendToGoogleDone();
        }
        break;
      }
      case Constants.AUTHENTICATE_TO_DOCLIST: {
        // Authenticated with Google Docs
        if (resultCode == RESULT_OK) {
          authenticateToGoogleTrix();
        } else {
          onSendToGoogleDone();
        }
        break;
      }
      case Constants.AUTHENTICATE_TO_TRIX: {
        // Authenticated with Trix
        if (resultCode == RESULT_OK) {
          sendToGoogleDocs(trackId);
        } else {
          onSendToGoogleDone();
        }
        break;
      }
      case Constants.SAVE_GPX_FILE:
        if (exportFormat == null) { exportFormat = TrackFileFormat.GPX; }
        //$FALL-THROUGH$
      case Constants.SAVE_KML_FILE:
        if (exportFormat == null) { exportFormat = TrackFileFormat.KML; }
        //$FALL-THROUGH$
      case Constants.SAVE_CSV_FILE:
        if (exportFormat == null) { exportFormat = TrackFileFormat.CSV; }
        //$FALL-THROUGH$
      case Constants.SAVE_TCX_FILE:
        if (exportFormat == null) { exportFormat = TrackFileFormat.TCX; }

        if (results != null && resultCode == Activity.RESULT_OK) {
          if (trackId >= 0) {
            saveTrack(trackId, exportFormat);
          }
        }
        break;
      case Constants.SHARE_LINK: {
        Track selectedTrack = providerUtils.getTrack(dataHub.getSelectedTrackId());
        if (selectedTrack != null) {
          if (!TextUtils.isEmpty(selectedTrack.getMapId())) {
            shareLinkToMap(MapsFacade.buildMapUrl(selectedTrack.getMapId()));
          } else if (!TextUtils.isEmpty(selectedTrack.getTableId())) {
            shareLinkToMap(getFusionTablesUrl(dataHub.getSelectedTrackId()));
          } else {
            shareRequested = true;
            dialogManager.showDialogSafely(DIALOG_SEND_TO_GOOGLE);
          }
        }
        break;
      }
      case Constants.SHARE_GPX_FILE:
        if (exportFormat == null) { exportFormat = TrackFileFormat.GPX; }
        //$FALL-THROUGH$
      case Constants.SHARE_KML_FILE:
        if (exportFormat == null) { exportFormat = TrackFileFormat.KML; }
        //$FALL-THROUGH$
      case Constants.SHARE_CSV_FILE:
        if (exportFormat == null) { exportFormat = TrackFileFormat.CSV; }
        //$FALL-THROUGH$
      case Constants.SHARE_TCX_FILE: {
        if (exportFormat == null) { exportFormat = TrackFileFormat.TCX; }

        if (results != null && resultCode == Activity.RESULT_OK) {
          if (trackId >= 0) {
            sendTrack(trackId, exportFormat);
          }
        }
        break;
      }
      case Constants.CLEAR_MAP: {
        dataHub.unloadCurrentTrack();
        break;
      }
      case Constants.WELCOME: {
        CheckUnits.check(this);
        break;
      }

      default: {
        Log.w(TAG, "Warning unhandled request code: " + requestCode);
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

  /**
   * Resets status information for sending to MyMaps/Docs.
   */
  public void resetSendToGoogleStatus() {
    sendToMyMapsMapId = null;
    sendToMyMapsMessage = "";
    sendToMyMapsSuccess = true;
    sendToFusionTablesMessage = "";
    sendToFusionTablesSuccess = true;
    sendToDocsMessage = "";
    sendToDocsSuccess = true;
    sendToFusionTablesTableId = null;
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
            Log.e(TAG, "Caught an unexpected exception.", e);
            message = R.string.error_generic;
          } catch (ParserConfigurationException e) {
            Log.e(TAG, "Caught an unexpected exception.", e);
            message = R.string.error_generic;
          } catch (IOException e) {
            Log.e(TAG, "Caught an unexpected exception.", e);
            message = R.string.error_unable_to_read_file;
          } catch (NullPointerException e) {
            Log.e(TAG, "Caught an unexpected exception.", e);
            message = R.string.error_invalid_gpx_format;
          } catch (OutOfMemoryError e) {
            Log.e(TAG, "Caught an unexpected exception.", e);
            message = R.string.error_out_of_memory;
          }
          if (trackIdsImported != null && trackIdsImported.length > 0) {
            // select last track from import file
            dataHub.loadTrack(trackIdsImported[trackIdsImported.length - 1]);
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
   * Shares a link to a My Map or Fusion Table via external app (email, gmail, ...)
   * A chooser with apps that support text/plain will be shown to the user.
   */
  public void shareLinkToMap(String url) {
    Intent shareIntent = new Intent(Intent.ACTION_SEND);
    shareIntent.setType("text/plain");
    shareIntent.putExtra(Intent.EXTRA_SUBJECT,
        getResources().getText(R.string.share_map_subject).toString());

    boolean shareUrlOnly = true;
    if (sharedPreferences != null) {
      shareUrlOnly = sharedPreferences.getBoolean(
          getString(R.string.share_url_only_key), false);
    }

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
        if (trackId == dataHub.getSelectedTrackId()) {
          dataHub.unloadCurrentTrack();
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

  /**
   * Inserts a waypoint marker.
   *
   * @return Id of the inserted statistics marker.
   * @throws RemoteException If the call on the service failed.
   */
  public long insertWaypoint(WaypointCreationRequest request) throws RemoteException {
    if (trackRecordingService == null) {
      throw new IllegalStateException("The recording service is not bound.");
    }
    try {
      long waypointId = trackRecordingService.insertWaypoint(request);
      if (waypointId >= 0) {
        Toast.makeText(this, R.string.status_statistics_inserted,
            Toast.LENGTH_LONG).show();
      }
      return waypointId;
    } catch (RemoteException e) {
      Toast.makeText(this, R.string.error_unable_to_insert_marker,
          Toast.LENGTH_LONG).show();
      throw e;
    }
  }

  /**
   * Initializes the authentication manager which obtains an authentication
   * token, prompting the user for a login and password if needed.
   */
  private void authenticate(final Intent results, final int requestCode,
      final String service) {
    auth = authMap.get(service);
    if (auth == null) {
      Log.i(TAG, "Creating a new authentication for service: " + service);
      auth = AuthManagerFactory.getAuthManager(this,
          Constants.GET_LOGIN,
          null,
          true,
          service);
      authMap.put(service, auth);
    }
    Log.d(TAG, "Logging in to " + service + "...");
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
        Log.d(TAG, "Loggin success for " + service + "!");
        onActivityResult(requestCode, RESULT_OK, results);
      }
    }, account);
  }

  private void startRecordingNewTrack(
      ITrackRecordingService trackRecordingService) {
    try {
      long recordingTrackId = trackRecordingService.startNewTrack();
      // Select the recording track.
      dataHub.loadTrack(recordingTrackId);
      Toast.makeText(this, getString(R.string.status_now_recording),
          Toast.LENGTH_SHORT).show();
      // TODO: We catch Exception, because after eliminating the service process
      // all exceptions it may throw are no longer wrapped in a RemoteException.
    } catch (Exception e) {
      Toast.makeText(this,
          getString(R.string.error_unable_to_start_recording),
          Toast.LENGTH_SHORT).show();
      Log.w(TAG, "Unable to start recording.", e);
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
      long currentTrackId = sharedPreferences.getLong(getString(R.string.recording_track_key), -1);
      try {
        trackRecordingService.endCurrentTrack();
        // TODO: We catch Exception, because after eliminating the service process
        // all exceptions it may throw are no longer wrapped in a RemoteException.
      } catch (Exception e) {
        Log.e(TAG, "Unable to stop recording.", e);
      }

      if (currentTrackId > 0) {
        Intent intent = new Intent(MyTracks.this, TrackDetails.class);
        intent.putExtra("trackid", currentTrackId);
        intent.putExtra("hasCancelButton", false);
        startActivity(intent);
      }
    }
    tryUnbindTrackRecordingService();
    try {
      stopService(new Intent(MyTracks.this, TrackRecordingService.class));
    } catch (SecurityException e) {
      Log.e(TAG, "Encountered a security exception when trying to stop service.", e);
    }
    trackRecordingService = null;
  }

  /**
   * Initiates the process to send tracks to google.
   * This is called once the user has selected sending options via the
   * SendToGoogleDialog.
   * 
   * TODO: Change this whole flow to an actual state machine.
   */
  public void sendToGoogle() {
    SendDialog sendToGoogleDialog =
        dialogManager.getSendToGoogleDialog();
    if (sendToGoogleDialog == null) {
      return;
    }
    setProgressValue(0);
    clearProgressMessage();
    dialogManager.showDialogSafely(DIALOG_PROGRESS);

    if (sendToGoogleDialog.getSendToMyMaps()) {
      sendToGoogleMapsOrPickMap(sendToGoogleDialog);
    } else if (sendToGoogleDialog.getSendToFusionTables()) {
      authenticateToFusionTables(null);
    } else if (sendToGoogleDialog.getSendToDocs()) {
      authenticateToGoogleDocs();
    } else  {
      Log.w(TAG, "Nowhere to upload to");
      onSendToGoogleDone();
    }
  }

  private void sendToGoogleMapsOrPickMap(SendDialog sendToGoogleDialog) {
    if (!sendToGoogleDialog.getCreateNewMap()) {
      // Ask the user to choose a map to upload into
      Intent listIntent = new Intent(this, MyMapsList.class);
      startActivityForResult(listIntent, Constants.GET_MAP);
      // The callback for GET_MAP calls authenticateToGoogleMaps
    } else {
      authenticateToGoogleMaps(null);
    }
  }

  private void authenticateToGoogleMaps(Intent results) {
    if (results == null) { results = new Intent(); }

    setProgressValue(0);
    setProgressMessage(
        R.string.progress_message_authenticating_mymaps);
    authenticate(results, Constants.AUTHENTICATE_TO_MY_MAPS,
        MyMapsConstants.SERVICE_NAME);
    // AUTHENTICATE_TO_MY_MAPS callback calls sendToGoogleMaps
  }

  private void sendToGoogleMaps(final long trackId, String mapId) {
    SendToMyMaps.OnSendCompletedListener onCompletion = new SendToMyMaps.OnSendCompletedListener() {
      @Override
      public void onSendCompleted(String mapId, boolean success, int statusMessage) {
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
            Log.w(TAG, "Updating map id failed.", e);
          }
        }

        onSendToGoogleMapsDone();
      }
    };
    final SendToMyMaps sender = new SendToMyMaps(this, mapId, auth,
        trackId, this /*progressIndicator*/, onCompletion);

    HandlerThread handlerThread = new HandlerThread("SendToMyMaps");
    handlerThread.start();
    Handler handler = new Handler(handlerThread.getLooper());
    handler.post(sender);
  }

  private void onSendToGoogleMapsDone() {
    SendDialog sendToGoogleDialog = dialogManager.getSendToGoogleDialog();
    if (sendToGoogleDialog.getSendToFusionTables()) {
      authenticateToFusionTables(null);
    } else if (sendToGoogleDialog.getSendToDocs()) {
      authenticateToGoogleDocs();
    } else {
      onSendToGoogleDone();
    }
  }

  private void authenticateToFusionTables(Intent results) {
    if (results == null) { results = new Intent(); }

    setProgressValue(0);
    setProgressMessage(R.string.progress_message_authenticating_fusiontables);
    authenticate(results, Constants.AUTHENTICATE_TO_FUSION_TABLES,
        SendToFusionTables.SERVICE_ID);
    // AUTHENTICATE_TO_FUSION_TABLES callback calls sendToFusionTables
  }

  private void sendToFusionTables(final long trackId) {
    OnSendCompletedListener onCompletion = new OnSendCompletedListener() {
      @Override
      public void onSendCompleted(String tableId, boolean success,
          int statusMessage) {
        sendToFusionTablesMessage = getString(statusMessage);
        sendToFusionTablesSuccess = success;
        if (sendToFusionTablesSuccess) {
          sendToFusionTablesTableId = tableId;
          // Update the table id for this track:
          try {
            Track track = providerUtils.getTrack(trackId);
            track.setTableId(tableId);
            providerUtils.updateTrack(track);
          } catch (RuntimeException e) {
            // If that fails whatever reasons we'll just log an error, but
            // continue.
            Log.w(TAG, "Updating table id failed.", e);
          }
        }
        
        onSendToFusionTablesDone();
      }
    };
    sendToTrackId = trackId;
    final SendToFusionTables sender = new SendToFusionTables(this, auth,
        trackId, this/*progressIndicator*/, onCompletion);

    HandlerThread handlerThread = new HandlerThread("SendToFusionTables");
    handlerThread.start();
    Handler handler = new Handler(handlerThread.getLooper());
    handler.post(sender);
  }

  private void onSendToFusionTablesDone() {
    SendDialog sendToGoogleDialog = dialogManager.getSendToGoogleDialog();
    if (sendToGoogleDialog.getSendToDocs()) {
      authenticateToGoogleDocs();
    } else {
      onSendToGoogleDone();
    }
  }

  private void authenticateToGoogleDocs() {
    setProgressValue(0);
    setProgressMessage(
        R.string.progress_message_authenticating_docs);
    authenticate(new Intent(),
        Constants.AUTHENTICATE_TO_DOCLIST,
        SendToDocs.GDATA_SERVICE_NAME_DOCLIST);
    // AUTHENTICATE_TO_DOCLIST callback calls authenticateToGoogleTrix
  }

  private void authenticateToGoogleTrix() {
    setProgressValue(30);
    setProgressMessage(
        R.string.progress_message_authenticating_docs);
    authenticate(new Intent(),
        Constants.AUTHENTICATE_TO_TRIX,
        SendToDocs.GDATA_SERVICE_NAME_TRIX);
    // AUTHENTICATE_TO_TRIX callback calls sendToGoogleDocs
  }

  private void sendToGoogleDocs(final long trackId) {
    Log.d(TAG, "Sending to Docs....");
    setProgressValue(50);
    setProgressMessage(R.string.progress_message_sending_docs);
    final SendToDocs sender = new SendToDocs(this, 
        authMap.get(SendToDocs.GDATA_SERVICE_NAME_TRIX),
        authMap.get(SendToDocs.GDATA_SERVICE_NAME_DOCLIST), trackId);
    sendToTrackId = trackId;
    Runnable onCompletion = new Runnable() {
      public void run() {
        setProgressValue(100);
        dialogManager.dismissDialogSafely(DIALOG_PROGRESS);
        sendToDocsMessage = sender.getStatusMessage();
        sendToDocsSuccess = sender.wasSuccess();

        onSendToGoogleDocsDone();
      }
    };
    sender.setOnCompletion(onCompletion);
    sender.run();
  }

  private void onSendToGoogleDocsDone() {
    onSendToGoogleDone();
  }

  private void onSendToGoogleDone() {
    SendDialog sendToGoogleDialog = dialogManager.getSendToGoogleDialog();
    final boolean sentToMyMaps = sendToGoogleDialog.getSendToMyMaps();
    final boolean sentToFusionTables = sendToGoogleDialog.getSendToFusionTables();
    dialogManager.dismissDialogSafely(DIALOG_PROGRESS);

    // We've finished sending the track to the user-selected services.  Now
    // we tell them the results of the upload, and optionally share the track.
    // There are a few different paths through this code:
    //
    // 1. The user pre-requested a share (shareRequested == true).  We're going
    //    to display the result dialog *without* the share button (the share
    //    listener will be null).  The OK button listener will initiate the
    //    share.
    //
    // 2. The user did not pre-request a share, and the set of services to
    //    which we succeeded in uploading the track are compatible with
    //    sharing.  We'll display a share button (the share listener will be
    //    non-null), and will share the link if the user clicks it.
    //
    // 3. The user did not pre-request a share, and the set of services to
    //    which we succeeded in uploading the track are incompatible with
    //    sharing.  We won't display a share button.

    final boolean canShare = getSendToFusionTablesTableId() != null
        || getSendToMyMapsMapId() != null;

    DialogInterface.OnClickListener doShareListener = null;
    if (canShare) {
      doShareListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
          shareLinkToMap(sentToMyMaps, sentToFusionTables);
        }
      };
    }

    DialogInterface.OnClickListener onOkListener = (canShare && shareRequested)
        ? doShareListener : null;
    DialogInterface.OnClickListener onShareListener = (canShare && !shareRequested)
        ? doShareListener : null;

    AlertDialog sendToGoogleResultDialog = ResultDialogFactory.makeDialog(this,
        makeSendToGoogleResults(sendToGoogleDialog), onOkListener, onShareListener);
    DialogManager.showDialogSafely(this, sendToGoogleResultDialog);
  }

  boolean shareLinkToMap(boolean sentToMyMaps, boolean sentToFusionTables) {
    String url = null;
    if (sentToMyMaps && sendToMyMapsSuccess) {
      // Prefer a link to My Maps
      url = MapsFacade.buildMapUrl(sendToMyMapsMapId);
    } else if (sentToFusionTables && sendToFusionTablesSuccess) {
      // Otherwise try using the link to fusion tables
      url = getFusionTablesUrl(sendToTrackId);
    }

    if (url != null) {
      shareLinkToMap(url);
      return true;
    }

    return false;
  }

  protected String getFusionTablesUrl(long sendToTrackId2) {
    Track track = providerUtils.getTrack(sendToTrackId);
    return SendToFusionTables.getMapVisualizationUrl(track);
  }

  /**
   * Creates a list of {@link SendResult} instances based on the set of
   * services selected in {@link SendDialog} and the results as known to
   * this class.
   */
  private List<SendResult> makeSendToGoogleResults(SendDialog dialog) {
    List<SendResult> results = new ArrayList<SendResult>();
    if (dialog.getSendToMyMaps()) {
      results.add(new SendResult(SendType.MYMAPS, sendToMyMapsSuccess));
    }
    if (dialog.getSendToFusionTables()) {
      results.add(new SendResult(SendType.FUSION_TABLES, sendToFusionTablesSuccess));
    }
    if (dialog.getSendToDocs()) {
      results.add(new SendResult(SendType.DOCS, sendToDocsSuccess));
    }

    return results;
  }

  void clearSelectedTrack() {
    dataHub.unloadCurrentTrack();
  }

  long getSelectedTrackId() {
    return dataHub.getSelectedTrackId();
  }

  /**
   * Binds to track recording service if it is running.
   */
  private void tryBindTrackRecordingService() {
    Log.d(TAG,
        "MyTracks: Trying to bind to track recording service...");
    bindService(new Intent(this, TrackRecordingService.class),
        serviceConnection, 0);
    Log.d(TAG, "MyTracks: ...bind finished!");
    isBound = true;
  }

  /**
   * Tries to unbind the track recording service. Catches exception silently in
   * case service is not registered anymore.
   */
  private void tryUnbindTrackRecordingService() {
    if (isBound) {
      Log.d(TAG, "MyTracks: Trying to unbind from track recording service...");
      try {
        unbindService(serviceConnection);
        Log.d(TAG, "MyTracks: ...unbind finished!");
      } catch (IllegalArgumentException e) {
        Log.d(TAG, "MyTracks: Tried unbinding, but service was not registered.", e);
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

  public String getSendToFusionTablesTableId() {
    return sendToFusionTablesTableId;
  }

  public boolean getSendToGoogleSuccess() {
    return sendToFusionTablesSuccess && sendToDocsSuccess;
  }

  // @VisibleForTesting
  long getRecordingTrackId() {
    return sharedPreferences.getLong(getString(R.string.recording_track_key), -1);
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

  public TrackDataHub getDataHub() {
    return dataHub;
  }
}
