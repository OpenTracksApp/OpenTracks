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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Random;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.TabActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.RemoteException;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager.BadTokenException;
import android.widget.RelativeLayout;
import android.widget.TabHost;
import android.widget.Toast;

import com.google.android.accounts.Account;
import com.google.android.apps.mymaps.MyMapsConstants;
import com.google.android.apps.mymaps.MyMapsList;
import com.google.android.apps.mymaps.VersionChecker;
import com.google.android.apps.mytracks.content.MyTracksProviderUtils;
import com.google.android.apps.mytracks.content.Track;
import com.google.android.apps.mytracks.content.Waypoint;
import com.google.android.apps.mytracks.io.AuthManager;
import com.google.android.apps.mytracks.io.AuthManagerFactory;
import com.google.android.apps.mytracks.io.GpxSaxImporter;
import com.google.android.apps.mytracks.io.SendToDocs;
import com.google.android.apps.mytracks.io.SendToMyMaps;
import com.google.android.apps.mytracks.io.TrackWriter;
import com.google.android.apps.mytracks.io.TrackWriterFactory;
import com.google.android.apps.mytracks.io.TrackWriterFactory.TrackFileFormat;
import com.google.android.apps.mytracks.services.ITrackRecordingService;
import com.google.android.apps.mytracks.services.TrackRecordingService;
import com.google.android.maps.mytracks.R;

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
  private static MyTracks instance = null;
  private ChartActivity chartActivity = null;

  public ChartActivity getChartActivity() {
    return chartActivity;
  }

  public void setChartActivity(ChartActivity chartActivity) {
    this.chartActivity = chartActivity;
  }

  /*
   * Authentication:
   */

  private AuthManager auth;
  private final HashMap<String, AuthManager> authMap =
      new HashMap<String, AuthManager>();
  private AccountChooser accountChooser = new AccountChooser();

  /*
   * Dialogs:
   */

  public static final int DIALOG_PROGRESS = 1;
  public static final int DIALOG_IMPORT_PROGRESS = 2;
  public static final int DIALOG_WRITE_PROGRESS = 3;
  public static final int DIALOG_SEND_TO_GOOGLE = 4;
  public static final int DIALOG_SEND_TO_GOOGLE_RESULT = 5;
  public static final int DIALOG_CHART_SETTINGS = 6;

  private ProgressDialog progressDialog;
  private ProgressDialog importProgressDialog;
  private ProgressDialog writeProgressDialog;
  private SendToGoogleDialog sendToGoogleDialog;
  private AlertDialog sendToGoogleResultDialog;
  private ChartSettingsDialog chartSettingsDialog;

  /*
   * Menu items:
   */

  private MenuItem startRecording;
  private MenuItem stopRecording;
  private MenuItem listTracks;
  private MenuItem listMarkers;
  private MenuItem settings;
  private MenuItem help;

  /*
   * Information on upload success to MyMaps/Docs.
   * Used by SendToGoogleResultDialog.
   */

  public boolean sendToMyMapsSuccess = false;
  public boolean sendToDocsSuccess = false;
  public String sendToMyMapsMapId = null;
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

  /**
   * The connection to the track recording service.
   */
  private final ServiceConnection serviceConnection = new ServiceConnection() {
    @Override
    public void onServiceConnected(ComponentName className, IBinder service) {
      Log.d(MyTracksConstants.TAG, "MyTracks: Service now connected.");
      trackRecordingService = ITrackRecordingService.Stub.asInterface(service);
      if (startNewTrackRequested) {
        startNewTrackRequested = false;
        try {
          recordingTrackId = trackRecordingService.startNewTrack();
          Toast.makeText(MyTracks.this,
              R.string.status_now_recording, Toast.LENGTH_SHORT).show();
          setSelectedAndRecordingTrack(recordingTrackId, recordingTrackId);
        } catch (RemoteException e) {
          Toast.makeText(MyTracks.this,
              R.string.error_unable_to_start_recording, Toast.LENGTH_SHORT)
                  .show();
          Log.w(MyTracksConstants.TAG, "Unable to start recording.", e);
        }
      }
    }

    @Override
    public void onServiceDisconnected(ComponentName className) {
      Log.d(MyTracksConstants.TAG, "MyTracks: Service now disconnected.");
      trackRecordingService = null;
    }
  };

  /*
   * Tabs/View navigation:
   */

  private static final int NUM_TABS = 3;
  private int currentTab = 0;

  private NavControls navControls;

  private final int icons[] =
      { R.drawable.arrow_grey, R.drawable.menu_by_time,
        R.drawable.menu_elevation };

  private final Runnable nextActivity = new Runnable() {
    public void run() {
      currentTab = (currentTab + 1) % NUM_TABS;
      navControls.setLeftIcon(icons[(currentTab + NUM_TABS - 1) % NUM_TABS]);
      navControls.setRightIcon(icons[(currentTab + NUM_TABS + 1) % NUM_TABS]);
      getTabHost().setCurrentTab(currentTab);
      navControls.show();
    }
  };

  private final Runnable prevActivity = new Runnable() {
    public void run() {
      currentTab--;
      if (currentTab < 0) {
        currentTab = NUM_TABS - 1;
      }
      navControls.setLeftIcon(icons[(currentTab + NUM_TABS - 1) % NUM_TABS]);
      navControls.setRightIcon(icons[(currentTab + NUM_TABS + 1) % NUM_TABS]);
      getTabHost().setCurrentTab(currentTab);
      navControls.show();
    }
  };

  private final Random random = new Random();

  public static MyTracks getInstance() {
    return instance;
  }

  /**
   * @return true if the activity is bound to the track recording service and
   * the service is recording a track.
   */
  public boolean isRecording() {
    if (trackRecordingService == null) {
      return false;
    }
    try {
      return trackRecordingService.isRecording();
    } catch (RemoteException e) {
      Log.e(MyTracksConstants.TAG, "MyTracks: Remote exception.", e);
      return false;
    }
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

    // The volume we want to control is the Text-To-Speech volume
    setVolumeControlStream(TextToSpeech.Engine.DEFAULT_STREAM);

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
    navControls = new NavControls(this, layout, prevActivity, nextActivity);
    navControls.setLeftIcon(icons[NUM_TABS - 1]);
    navControls.setRightIcon(icons[1]);
    navControls.show();
    tabHost.addView(layout);
    layout.setOnTouchListener(this);

    SharedPreferences prefs =
        getSharedPreferences(MyTracksSettings.SETTINGS_NAME, 0);
    if (prefs != null) {
      selectedTrackId = prefs.getLong(MyTracksSettings.SELECTED_TRACK, -1);
      recordingTrackId = prefs.getLong(MyTracksSettings.RECORDING_TRACK, -1);
      prefs.registerOnSharedPreferenceChangeListener(this);
    }

    // This will show the eula until the user accepts or quits the app.
    Eula.showEula(this);

    // Check if new version is available and prompt user with update options:
    new VersionChecker(this);

    // Check if we got invoked via the VIEW intent:
    Intent intent = getIntent();
    if (intent != null && intent.getAction() != null) {
      if (intent.getAction().equals(Intent.ACTION_VIEW)) {
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
            "Received an intent with unsupported action " + intent.getAction());
      }
    } else {
      Log.d(MyTracksConstants.TAG, "Received an intent with no action.");
    }
  }

  @Override
  protected void onPause() {
    // Called when activity is going into the background, but has not (yet) been
    // killed. Shouldn't block longer than approx. 2 seconds.
    Log.d(MyTracksConstants.TAG, "MyTracks.onPause");
    super.onPause();
    tryUnbindTrackRecordingService();
  }

  @Override
  protected void onResume() {
    // Called when the current activity is being displayed or re-displayed
    // to the user.
    Log.d(MyTracksConstants.TAG, "MyTracks.onResume");
    super.onResume();
    tryBindTrackRecordingService();
    navControls.setLeftIcon(icons[(currentTab + NUM_TABS - 1) % NUM_TABS]);
    navControls.setRightIcon(icons[(currentTab + NUM_TABS + 1) % NUM_TABS]);
  }

  @Override
  protected void onStop() {
    super.onStop();
    // Clean up any temporary GPX and KML files.
    cleanTmpDirectory("gpx");
    cleanTmpDirectory("kml");
  }

  private void cleanTmpDirectory(String name) {
    if (!Environment.getExternalStorageState().equals(
        Environment.MEDIA_MOUNTED)) {
      return; // Can't do anything now.
    }
    String sep = System.getProperty("file.separator");
    File dir = new File(
        Environment.getExternalStorageDirectory() + sep + name + sep + "tmp");
    if (!dir.exists()) {
      return;
    }
    File[] list = dir.listFiles();
    long now = System.currentTimeMillis();
    long oldest = now - 1000 * 3600;
    for (File f : list) {
      if (f.lastModified() < oldest) {
        f.delete();
      }
    }
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
  }

  /*
   * Menu events:
   * ============
   */

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    super.onCreateOptionsMenu(menu);
    startRecording = menu.add(0, MyTracksConstants.MENU_START_RECORDING, 0,
        R.string.start_recording);
    startRecording.setIcon(R.drawable.menu_record);
    stopRecording = menu.add(0, MyTracksConstants.MENU_STOP_RECORDING, 0,
        R.string.stop_recording);
    stopRecording.setIcon(R.drawable.menu_stop);
    listTracks = menu.add(0, MyTracksConstants.MENU_LIST_TRACKS, 0,
        R.string.list_tracks);
    listTracks.setIcon(R.drawable.menu_list_tracks);
    listMarkers = menu.add(0, MyTracksConstants.MENU_LIST_MARKERS, 0,
        R.string.list_markers);
    listMarkers.setIcon(R.drawable.menu_marker);
    settings = menu.add(0, MyTracksConstants.MENU_SETTINGS, 10001,
        R.string.settings);
    settings.setIcon(android.R.drawable.ic_menu_preferences);
    help = menu.add(0, MyTracksConstants.MENU_HELP, 10002, R.string.help);
    help.setIcon(android.R.drawable.ic_menu_info_details);
    return true;
  }

  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    boolean hasRecorded = providerUtils.getLastTrack() != null;
    boolean isRecording = isRecording();
    listTracks = menu.findItem(MyTracksConstants.MENU_LIST_TRACKS);
    listTracks.setEnabled(hasRecorded);
    listMarkers = menu.findItem(MyTracksConstants.MENU_LIST_MARKERS);
    listMarkers.setEnabled(hasRecorded && selectedTrackId >= 0);
    startRecording = menu.findItem(MyTracksConstants.MENU_START_RECORDING);
    startRecording.setEnabled(!isRecording);
    startRecording.setVisible(!isRecording);
    stopRecording = menu.findItem(MyTracksConstants.MENU_STOP_RECORDING);
    stopRecording.setEnabled(isRecording);
    stopRecording.setVisible(isRecording);
    return super.onPrepareOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case MyTracksConstants.MENU_START_RECORDING: {
        startRecording();
        return true;
      }
      case MyTracksConstants.MENU_STOP_RECORDING: {
        stopRecording();
        return true;
      }
      case MyTracksConstants.MENU_LIST_TRACKS: {
        Intent startIntent = new Intent(this, MyTracksList.class);
        startActivityForResult(startIntent, MyTracksConstants.SHOW_TRACK);
        return true;
      }
      case MyTracksConstants.MENU_LIST_MARKERS: {
        Intent startIntent = new Intent(this, MyTracksWaypointsList.class);
        startIntent.putExtra("trackid", selectedTrackId);
        startActivityForResult(startIntent, MyTracksConstants.SHOW_WAYPOINT);
        return true;
      }
      case MyTracksConstants.MENU_SETTINGS: {
        Intent startIntent = new Intent(this, MyTracksSettings.class);
        startActivity(startIntent);
        return true;
      }
      case MyTracksConstants.MENU_HELP: {
        Intent startIntent = new Intent(this, WelcomeActivity.class);
        startActivity(startIntent);
        return true;
      }
      case MyTracksConstants.MENU_CLEAR_MAP: {
        setSelectedTrack(-1);
        return true;
      }
    }
    return super.onOptionsItemSelected(item);
  }

  /*
   * Dialog events:
   * ==============
   */

  @Override
  protected Dialog onCreateDialog(int id) {
    switch (id) {
      case DIALOG_PROGRESS:
        progressDialog = new ProgressDialog(this);
        progressDialog.setIcon(android.R.drawable.ic_dialog_info);
        progressDialog.setTitle(getString(R.string.progress_title));
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setMessage("");
        progressDialog.setMax(100);
        progressDialog.setProgress(10);
        return progressDialog;
      case DIALOG_IMPORT_PROGRESS:
        importProgressDialog = new ProgressDialog(this);
        importProgressDialog.setIcon(android.R.drawable.ic_dialog_info);
        importProgressDialog.setTitle(getString(R.string.progress_title));
        importProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        importProgressDialog.setMessage(
            getString(R.string.import_progress_message));
        return importProgressDialog;
      case DIALOG_WRITE_PROGRESS:
        writeProgressDialog = new ProgressDialog(this);
        writeProgressDialog.setIcon(android.R.drawable.ic_dialog_info);
        writeProgressDialog.setTitle(getString(R.string.progress_title));
        writeProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        writeProgressDialog.setMessage(
            getString(R.string.write_progress_message));
        return writeProgressDialog;
      case DIALOG_SEND_TO_GOOGLE:
        sendToGoogleDialog = new SendToGoogleDialog(this);
        return sendToGoogleDialog;
      case DIALOG_SEND_TO_GOOGLE_RESULT:
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(android.R.drawable.ic_dialog_info);
        builder.setTitle("Title");
        builder.setMessage("Message");
        builder.setPositiveButton(getString(R.string.ok), null);
        builder.setNeutralButton(getString(R.string.share_map),
            new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int which) {
            shareLinkToMyMap(sendToMyMapsMapId);
            dialog.dismiss();
          }
        });
        sendToGoogleResultDialog = builder.create();
        return sendToGoogleResultDialog;
      case DIALOG_CHART_SETTINGS:
        chartSettingsDialog = new ChartSettingsDialog(this);
        return chartSettingsDialog;
    }
    return null;
  }

  @Override
  protected void onPrepareDialog(int id, Dialog dialog) {
    super.onPrepareDialog(id, dialog);
    Log.d(MyTracksConstants.TAG, "MyTracks.onPrepareDialog: " + id);
    switch (id) {
      case DIALOG_SEND_TO_GOOGLE:
        resetSendToGoogleStatus();
        break;
      case DIALOG_SEND_TO_GOOGLE_RESULT:
        boolean success = sendToMyMapsSuccess && sendToDocsSuccess;
        sendToGoogleResultDialog.setTitle(
            success ? R.string.success : R.string.error);
        sendToGoogleResultDialog.setIcon(success
            ? android.R.drawable.ic_dialog_info
            : android.R.drawable.ic_dialog_alert);
        sendToGoogleResultDialog.setMessage(getMapsResultMessage());

        boolean canShare = sendToMyMapsMapId != null;
        View share =
            sendToGoogleResultDialog.findViewById(android.R.id.button3);
        if (share != null) {
          share.setVisibility(canShare ? View.VISIBLE : View.GONE);
        }
        break;
      case DIALOG_CHART_SETTINGS:
        Log.d(MyTracksConstants.TAG, "MyTracks.onPrepare chart dialog");
        chartSettingsDialog.setup(chartActivity);
        break;
    }
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
          setProgressMessage(getString(
              R.string.progress_message_authenticating_mymaps));
          authenticate(results, MyTracksConstants.SEND_TO_GOOGLE,
              MyMapsConstants.MAPSHOP_SERVICE);
        } else {
          dismissDialogSafely(DIALOG_PROGRESS);
        }
        break;
      }
      case MyTracksConstants.GET_LOGIN: {
        if (resultCode == RESULT_OK && auth != null) {
          if (!auth.authResult(resultCode, results)) {
            dismissDialogSafely(DIALOG_PROGRESS);
          }
        } else {
          dismissDialogSafely(DIALOG_PROGRESS);
        }
        break;
      }
      case MyTracksConstants.SHOW_TRACK: {
        if (results != null) {
          final long trackId = results.getLongExtra("trackid", -1);
          if (trackId >= 0) {
            setSelectedTrack(trackId);
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
              getString(R.string.progress_message_authenticating_docs));
          authenticate(results,
              MyTracksConstants.AUTHENTICATE_TO_TRIX, "writely");
        } else {
          dismissDialogSafely(DIALOG_PROGRESS);
        }
        break;
      }
      case MyTracksConstants.AUTHENTICATE_TO_TRIX: {
        if (resultCode == RESULT_OK) {
          setProgressValue(30);
          setProgressMessage(
              getString(R.string.progress_message_authenticating_docs));
          authenticate(results, MyTracksConstants.SEND_TO_DOCS, "wise");
        } else {
          dismissDialogSafely(DIALOG_PROGRESS);
        }
        break;
      }
      case MyTracksConstants.SEND_TO_DOCS: {
        if (results != null && resultCode == RESULT_OK) {
          Log.d(MyTracksConstants.TAG, "Sending to Docs....");
          setProgressValue(50);
          setProgressMessage(getString(R.string.progress_message_sending_docs));
          final long trackId = results.getLongExtra("trackid", selectedTrackId);
          final SendToDocs sender = new SendToDocs(this, authMap.get("wise"),
              authMap.get("writely"), trackId);
          Runnable onCompletion = new Runnable() {
            public void run() {
              setProgressValue(100);
              dismissDialogSafely(DIALOG_PROGRESS);
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
          dismissDialogSafely(DIALOG_PROGRESS);
        }
        break;
      }
      case MyTracksConstants.SEND_TO_GOOGLE_DIALOG: {
        shareRequested = false;
        showDialogSafely(DIALOG_SEND_TO_GOOGLE);
        break;
      }
      case MyTracksConstants.SEND_TO_GOOGLE: {
        if (results != null && resultCode == RESULT_OK) {
          final String mapid;
          final long trackId;
          if (results.hasExtra("mapid")) {
            mapid = results.getStringExtra("mapid");
          } else {
            mapid = "new";
          }
          if (results.hasExtra("trackid")) {
            trackId = results.getLongExtra("trackid", -1);
          } else {
            trackId = selectedTrackId;
          }
          final SendToMyMaps sender = new SendToMyMaps(this, mapid, auth,
              trackId, this/*progressIndicator*/);
          Runnable onCompletion = new Runnable() {
            public void run() {
              sendToMyMapsMessage = sender.getStatusMessage();
              sendToMyMapsSuccess = sender.wasSuccess();
              if (sendToMyMapsSuccess) {
                sendToMyMapsMapId = sender.getMapId();
                // Update the map id for this track:
                try {
                  Track track = providerUtils.getTrack(trackId);
                  track.setMapId(sender.getMapId());
                  providerUtils.updateTrack(track);
                } catch (RuntimeException e) {
                  // If that fails whatever reasons we'll just log an error, but
                  // continue.
                  Log.w(MyTracksConstants.TAG, "Updating map id failed.", e);
                }
              }
              if (sendToGoogleDialog.getSendToDocs()) {
                onActivityResult(MyTracksConstants.AUTHENTICATE_TO_DOCS,
                    RESULT_OK, new Intent());
              } else {
                dismissDialogSafely(DIALOG_PROGRESS);
                runOnUiThread(new Runnable() {
                  public void run() {
                    handleMapsFinish();
                  }
                });
              }
            }
          };
          sender.setOnCompletion(onCompletion);
          sender.run();
        } else {
          dismissDialogSafely(DIALOG_PROGRESS);
        }
        break;
      }
      case MyTracksConstants.SAVE_GPX_FILE:
        if (exportFormat == null) { exportFormat = TrackFileFormat.GPX; }
        //$FALL-THROUGH$
      case MyTracksConstants.SAVE_KML_FILE:
        if (exportFormat == null) { exportFormat = TrackFileFormat.KML; }
        //$FALL-THROUGH$
      case MyTracksConstants.SAVE_CSV_FILE: {
        if (exportFormat == null) { exportFormat = TrackFileFormat.CSV; }

        if (results != null && resultCode == Activity.RESULT_OK) {
          final long trackId = results.getLongExtra("trackid", selectedTrackId);
          if (trackId >= 0) {
            saveTrack(trackId, exportFormat);
          }
        }
        break;
      }
      case MyTracksConstants.SHARE_LINK: {
        Track selectedTrack = providerUtils.getTrack(selectedTrackId);
        if (selectedTrack != null) {
          if (selectedTrack.getMapId().length() > 0) {
            shareLinkToMyMap(selectedTrack.getMapId());
          } else {
            shareRequested = true;
            showDialogSafely(DIALOG_SEND_TO_GOOGLE);
          }
        }
        break;
      }
      case MyTracksConstants.SHARE_GPX_FILE:
      case MyTracksConstants.SHARE_KML_FILE: {
        if (results != null && resultCode == RESULT_OK) {
          long trackId = results.getLongExtra("trackid", selectedTrackId);
          if (trackId >= 0) {
            TrackFileFormat format =
                (requestCode == MyTracksConstants.SHARE_GPX_FILE)
                ? TrackFileFormat.GPX
                : TrackFileFormat.KML;
            sendTrack(trackId, format);
          }
        }
        break;
      }
      case MyTracksConstants.CLEAR_MAP: {
        setSelectedTrack(-1);
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
  public void onSharedPreferenceChanged(
      SharedPreferences sharedPreferences, String key) {
    // The service itself cannot listen to changes (not supported by Android for
    // services that run in a separate process). So we'll notify it manually:
    if (key != null && trackRecordingService != null) {
      try {
        trackRecordingService.sharedPreferenceChanged(key);
      } catch (RemoteException e) {
        Log.w(MyTracksConstants.TAG,
            "MyTracks: Cannot notify track recording service of changes "
            + "to shared preferences: ", e);
      }
    }
    if (key != null && key.equals(MyTracksSettings.SELECTED_TRACK)) {
      selectedTrackId =
          sharedPreferences.getLong(MyTracksSettings.SELECTED_TRACK, -1);
    }
  }

  /**
   * Simulates the recording of a random location.
   * This is for debugging and testing only. Useful if there is no GPS signal
   * available.
   */
  public void recordRandomLocation() {
    if (trackRecordingService != null) {
      Location loc = new Location("gps");
      double latitude = 37.5 + random.nextDouble() / 1000;
      double longitude = -120.0 + random.nextDouble() / 1000;
      loc.setLatitude(latitude);
      loc.setLongitude(longitude);
      loc.setAltitude(random.nextDouble() * 100);
      loc.setTime(System.currentTimeMillis());
      loc.setSpeed(random.nextFloat());
      MyTracksMap map =
          (MyTracksMap) getLocalActivityManager().getActivity("tab1");
      if (map != null) {
        map.onLocationChanged(loc);
      }
      StatsActivity stats =
          (StatsActivity) getLocalActivityManager().getActivity("tab2");
      if (stats != null) {
        stats.onLocationChanged(loc);
      }
      try {
        trackRecordingService.recordLocation(loc);
      } catch (RemoteException e) {
        Log.e(MyTracksConstants.TAG, "MyTracks", e);
      }
    }
  }

  /**
   * Resets status information for sending to MyMaps/Docs.
   */
  private void resetSendToGoogleStatus() {
    sendToMyMapsMessage = "";
    sendToMyMapsSuccess = true;
    sendToDocsMessage = "";
    sendToDocsSuccess = true;
    sendToMyMapsMapId = null;
  }

  /**
   * Reads data from a provider, for example gmail attachment preview. Currently
   * fails (due to permissions?!). This is currently dead code not invoked from
   * anywhere.
   */
  protected String getContent(Uri uri) {
    Cursor cursor = null;
    try {
      cursor = managedQuery(uri,
          null /*projection*/, null /*selection*/, null /*selectionArgs*/,
          null /*sortOrder*/);
      if (cursor == null) {
        Toast.makeText(this,
            R.string.error_unable_to_read_file, Toast.LENGTH_LONG).show();
        return null;
      }
      if (cursor.moveToFirst()) {
        String cols[] = cursor.getColumnNames();
        String s = "Cols:";
        for (String c : cols) { s += c + ","; }
        Toast.makeText(this, s, Toast.LENGTH_LONG).show();
        return "";
      } else {
        Toast.makeText(this, R.string.error_generic, Toast.LENGTH_LONG).show();
        return null;
      }
    } finally {
      if (cursor != null) {
        stopManagingCursor(cursor);
        cursor.close();
      }
    }
  }

  private void importGpxFile(final String fileName) {
    showDialogSafely(DIALOG_IMPORT_PROGRESS);
    Thread t = new Thread() {
      @Override
      public void run() {
        int message = R.string.success;

        long[] trackIdsImported = null;

        try {
          try {
            InputStream is = new FileInputStream(fileName);
            trackIdsImported = GpxSaxImporter.importGPXFile(is, providerUtils);
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
            setSelectedTrack(trackIdsImported[trackIdsImported.length - 1]);
          } else {
            MyTracks.this.showMessageDialog(message, false/* success */);
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

  /**
   * Just like showDialog, but will catch a BadTokenException that sometimes
   * (very rarely) gets thrown. This might happen if the user hits the "back"
   * button immediately after sending tracks to google.
   *
   * @param id the dialog id
   */
  public void showDialogSafely(final int id) {
    runOnUiThread(new Runnable() {
      public void run() {
        try {
          showDialog(id);
        } catch (BadTokenException e) {
          Log.w(MyTracksConstants.TAG,
              "Could not display dialog with id " + id, e);
        } catch (IllegalStateException e) {
          Log.w(MyTracksConstants.TAG,
              "Could not display dialog with id " + id, e);
        }
      }
    });
  }

  /**
   * Dismisses the progress dialog if it is showing. Executed on the UI thread.
   */
  public void dismissDialogSafely(final int id) {
    runOnUiThread(new Runnable() {
      public void run() {
        try {
          dismissDialog(id);
        } catch (IllegalArgumentException e) {
          // This will be thrown if this dialog was not shown before.
        }
      }
    });
  }

  public void setProgressMessage(final String text) {
    runOnUiThread(new Runnable() {
      public void run() {
        synchronized (this) {
          if (progressDialog != null) {
            progressDialog.setMessage(text);
          }
        }
      }
    });
  }

  public void setProgressValue(final int percent) {
    runOnUiThread(new Runnable() {
      public void run() {
        synchronized (this) {
          if (progressDialog != null) {
            progressDialog.setProgress(percent);
          }
        }
      }
    });
  }

  /**
   * Shares a link to a My Map via external app (email, gmail, ...)
   * A chooser with apps that support text/plain will be shown to the user.
   */
  private void shareLinkToMyMap(String mapId) {
    Intent shareIntent = new Intent(Intent.ACTION_SEND);
    shareIntent.setType("text/plain");
    shareIntent.putExtra(Intent.EXTRA_SUBJECT,
        getResources().getText(R.string.share_map_subject).toString());

    SharedPreferences prefs =
        getSharedPreferences(MyTracksSettings.SETTINGS_NAME, 0);
    boolean shareUrlOnly = true;
    if (prefs != null) {
      shareUrlOnly = prefs.getBoolean(MyTracksSettings.SHARE_URL_ONLY, false);
    }

    String url = MyMapsConstants.MAPSHOP_BASE_URL + "?msa=0&msid=" + mapId;
    String msg = (shareUrlOnly
        ? url
        : String.format(
            getResources().getText(R.string.share_map_body_format).toString(),
            url));
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
          setSelectedTrack(-1);
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
                    dismissDialogSafely(DIALOG_PROGRESS);
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

  /**
   * Starts the track recording service (if not already running) and binds to
   * it. Starts recording a new track.
   */
  private void startRecording() {
    if (trackRecordingService == null) {
      startNewTrackRequested = true;
      Intent startIntent = new Intent(this, TrackRecordingService.class);
      startService(startIntent);
      tryBindTrackRecordingService();
    } else {
      try {
        recordingTrackId = trackRecordingService.startNewTrack();
        Toast.makeText(this, getString(R.string.status_now_recording),
            Toast.LENGTH_SHORT).show();
        setSelectedAndRecordingTrack(recordingTrackId, recordingTrackId);
      } catch (RemoteException e) {
        Toast.makeText(this,
            getString(R.string.error_unable_to_start_recording),
            Toast.LENGTH_SHORT).show();
        Log.e(MyTracksConstants.TAG,
            "Failed to start track recording service", e);
      }
    }
  }

  /**
   * Stops the track recording service and unbinds from it. Will display a toast
   * "Stopped recording" and pop up the Track Details activity.
   */
  private void stopRecording() {
    if (trackRecordingService != null) {
      try {
        trackRecordingService.endCurrentTrack();
      } catch (RemoteException e) {
        Log.e(MyTracksConstants.TAG, "Unable to stop recording.", e);
      }
      setRecordingTrack(-1);
      Intent intent = new Intent(MyTracks.this, MyTracksDetails.class);
      intent.putExtra("trackid", recordingTrackId);
      intent.putExtra("hasCancelButton", false);
      recordingTrackId = -1;
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
    if (sendToGoogleDialog == null) {
      return;
    }
    setProgressValue(0);
    setProgressMessage("");
    showDialogSafely(DIALOG_PROGRESS);
    if (sendToGoogleDialog.getSendToMyMaps()) {
      if (!sendToGoogleDialog.getCreateNewMap()) {
        Intent listIntent = new Intent(this, MyMapsList.class);
        startActivityForResult(listIntent, MyTracksConstants.GET_MAP);
      } else {
        setProgressValue(0);
        setProgressMessage(getString(
            R.string.progress_message_authenticating_mymaps));
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
  private void setSelectedTrack(final long trackId) {
    runOnUiThread(new Runnable() {
      public void run() {
        SharedPreferences prefs =
            getSharedPreferences(MyTracksSettings.SETTINGS_NAME, 0);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(MyTracksSettings.SELECTED_TRACK, trackId);
        editor.commit();
      }
    });
  }

  /**
   * Writes the recording track id to the shared preferences.
   * Executed on the UI thread.
   *
   * @param trackId the id of the track
   */
  private void setRecordingTrack(final long trackId) {
    runOnUiThread(new Runnable() {
      public void run() {
        SharedPreferences prefs =
            getSharedPreferences(MyTracksSettings.SETTINGS_NAME, 0);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(MyTracksSettings.RECORDING_TRACK, trackId);
        editor.commit();
      }
    });
  }

  /**
   * Writes the selected and the recording track id to the shared preferences.
   * Executed on UI thread.
   */
  private void setSelectedAndRecordingTrack(final long theSelectedTrackId,
      final long theRecordingTrackId) {
    runOnUiThread(new Runnable() {
      public void run() {
        SharedPreferences prefs =
            getSharedPreferences(MyTracksSettings.SETTINGS_NAME, 0);
        if (prefs != null) {
          SharedPreferences.Editor editor = prefs.edit();
          editor.putLong(MyTracksSettings.SELECTED_TRACK, theSelectedTrackId);
          editor.putLong(MyTracksSettings.RECORDING_TRACK, theRecordingTrackId);
          editor.commit();
        }
      }
    });
  }

  /**
   * Binds to track recording service if it is running.
   */
  private void tryBindTrackRecordingService() {
    Log.d(MyTracksConstants.TAG,
        "MyTracks: Trying to bind to track recording service...");
    bindService(new Intent(this, TrackRecordingService.class),
        serviceConnection, 0);
  }

  /**
   * Tries to unbind the track recording service. Catches exception silently in
   * case service is not registered anymore.
   */
  private void tryUnbindTrackRecordingService() {
    Log.d(MyTracksConstants.TAG,
        "MyTracks: Trying to unbind from track recording service...");
    try {
      unbindService(serviceConnection);
    } catch (IllegalArgumentException e) {
      Log.d(MyTracksConstants.TAG,
          "MyTracks: Tried unbinding, but service was not registered.", e);
    }
  }

  /**
   * Shows a dialog with the given message.
   * Does it on the UI thread.
   *
   * @param success if true, displays an info icon/title, otherwise an error
   *        icon/title
   * @param message resource string id
   */
  public void showMessageDialog(final int message, final boolean success) {
    runOnUiThread(new Runnable() {
      public void run() {
        AlertDialog dialog = null;
        AlertDialog.Builder builder = new AlertDialog.Builder(MyTracks.this);
        builder.setMessage(MyTracks.this.getString(message));
        builder.setNegativeButton(MyTracks.this.getString(R.string.ok), null);
        builder.setIcon(success ? android.R.drawable.ic_dialog_info :
          android.R.drawable.ic_dialog_alert);
        builder.setTitle(success ? R.string.success : R.string.error);
        dialog = builder.create();
        dialog.show();
      }
    });
  }

  /**
   * Saves the track with the given id to the SD card.
   *
   * @param trackId The id of the track to be sent
   */
  public void saveTrack(long trackId, TrackFileFormat format) {
    showDialogSafely(DIALOG_WRITE_PROGRESS);
    final TrackWriter writer =
        TrackWriterFactory.newWriter(this, providerUtils, trackId, format);
    writer.setOnCompletion(new Runnable() {
      public void run() {
        dismissDialogSafely(DIALOG_WRITE_PROGRESS);
        showMessageDialog(writer.getErrorMessage(), writer.wasSuccess());
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
    showDialogSafely(DIALOG_WRITE_PROGRESS);
    final TrackWriter writer =
        TrackWriterFactory.newWriter(this, providerUtils, trackId, format);
    String sep = System.getProperty("file.separator");
    String extension = format.getExtension();
    File dir = new File(Environment.getExternalStorageDirectory()
        + sep + extension + sep + "tmp");
    writer.setDirectory(dir);
    writer.setOnCompletion(new Runnable() {
      public void run() {
        dismissDialogSafely(DIALOG_WRITE_PROGRESS);
        if (!writer.wasSuccess()) {
          showMessageDialog(writer.getErrorMessage(), writer.wasSuccess());
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
      showDialogSafely(DIALOG_SEND_TO_GOOGLE_RESULT);
    }
  }

  private String getMapsResultMessage() {
    StringBuilder message = new StringBuilder();
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
}
