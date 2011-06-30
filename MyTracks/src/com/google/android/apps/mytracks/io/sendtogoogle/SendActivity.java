/*
 * Copyright 2011 Google Inc.
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
package com.google.android.apps.mytracks.io.sendtogoogle;

import static com.google.android.apps.mytracks.Constants.TAG;

import com.google.android.accounts.Account;
import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import com.google.android.apps.mytracks.AccountChooser;
import com.google.android.apps.mytracks.Constants;
import com.google.android.apps.mytracks.MyMapsList;
import com.google.android.apps.mytracks.ProgressIndicator;
import com.google.android.apps.mytracks.content.MyTracksProviderUtils;
import com.google.android.apps.mytracks.content.Track;
import com.google.android.apps.mytracks.content.TracksColumns;
import com.google.android.apps.mytracks.io.AuthManager;
import com.google.android.apps.mytracks.io.AuthManagerFactory;
import com.google.android.apps.mytracks.io.SendToDocs;
import com.google.android.apps.mytracks.io.SendToFusionTables;
import com.google.android.apps.mytracks.io.SendToFusionTables.OnSendCompletedListener;
import com.google.android.apps.mytracks.io.SendToMyMaps;
import com.google.android.apps.mytracks.io.mymaps.MapsFacade;
import com.google.android.apps.mytracks.io.mymaps.MyMapsConstants;
import com.google.android.apps.mytracks.util.SystemUtils;
import com.google.android.apps.mytracks.util.UriUtils;
import com.google.android.maps.mytracks.R;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Helper activity for managing the sending of tracks to Google services.
 *
 * @author Rodrigo Damazio
 */
public class SendActivity extends Activity implements ProgressIndicator {
  private static final String EXTRA_SHARE_LINK = "share_link";

  /** States for the state machine that defines the upload process. */
  private enum SendState {
    SEND_OPTIONS,
    START,
    AUTHENTICATE_MAPS,
    PICK_MAP,
    SEND_TO_MAPS,
    SEND_TO_MAPS_DONE,
    AUTHENTICATE_FUSION_TABLES,
    SEND_TO_FUSION_TABLES,
    SEND_TO_FUSION_TABLES_DONE,
    AUTHENTICATE_DOCS,
    AUTHENTICATE_TRIX,
    SEND_TO_DOCS,
    SEND_TO_DOCS_DONE,
    SHOW_RESULTS,
    SHARE_LINK,
    FINISH,
    DONE,
    NOT_READY
  }

  private static final int SEND_DIALOG = 1;
  private static final int PROGRESS_DIALOG = 2;
  /* @VisibleForTesting */
  static final int DONE_DIALOG = 3;

  // UI
  private SendDialog sendDialog;
  private ProgressDialog progressDialog;

  // Services
  private MyTracksProviderUtils providerUtils;
  private SharedPreferences sharedPreferences;
  private GoogleAnalyticsTracker tracker;

  // Authentication
  private AuthManager lastAuth;
  private final HashMap<String, AuthManager> authMap =
    new HashMap<String, AuthManager>();
  private final AccountChooser accountChooser = new AccountChooser();

  // Send request information.
  private boolean shareRequested = false;
  private long sendTrackId;

  // Send result information, used by results dialog.
  private boolean sendToMyMapsSuccess = false;
  private boolean sendToFusionTablesSuccess = false;
  private boolean sendToDocsSuccess = false;
  // TODO: Make these be used for showing results
  private String sendToMyMapsMessage;
  private String sendToFusionTablesMessage;
  private String sendToDocsMessage;

  // Send result information, used to share a link.
  private String sendToMyMapsMapId;
  private String sendToFusionTablesTableId;

  // Current sending state.
  private SendState currentState;

  private final OnCancelListener finishOnCancelListener = new OnCancelListener() {
    @Override
    public void onCancel(DialogInterface arg0) {
      onAllDone();
    }
  };

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    Log.d(TAG, "SendActivity.onCreate");
    super.onCreate(savedInstanceState);

    providerUtils = MyTracksProviderUtils.Factory.get(this);
    sharedPreferences = getSharedPreferences(Constants.SETTINGS_NAME, 0);

    resetState();

    tracker = GoogleAnalyticsTracker.getInstance();
    // Start the tracker in manual dispatch mode...
    tracker.start(getString(R.string.google_analytics_id), getApplicationContext());
    tracker.setProductVersion("android-mytracks", SystemUtils.getMyTracksVersion(this));

    if (!handleIntent()) {
      finish();
      return;
    }
    if (savedInstanceState != null) {
      restoreInstanceState(savedInstanceState);
    }

    executeStateMachine(null);
  }

  private boolean handleIntent() {
    Intent intent = getIntent();
    String action = intent.getAction();
    String type = intent.getType();
    Uri data = intent.getData();
    if (!Intent.ACTION_SEND.equals(action) ||
        !TracksColumns.CONTENT_ITEMTYPE.equals(type) ||
        !UriUtils.matchesContentUri(data, TracksColumns.CONTENT_URI)) {
      Log.e(TAG, "Got bad send intent: " + intent);
      return false;
    }

    sendTrackId = ContentUris.parseId(data);
    shareRequested = intent.getBooleanExtra(EXTRA_SHARE_LINK, false);

    return true;
  }

  @Override
  protected Dialog onCreateDialog(int id) {
    switch (id) {
      case SEND_DIALOG:
        return createSendDialog();
      case PROGRESS_DIALOG:
        return createProgressDialog();
      case DONE_DIALOG:
        return createDoneDialog();
    }

    return null;
  }

  private Dialog createSendDialog() {
    sendDialog = new SendDialog(this);
    sendDialog.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {
        if (which != DialogInterface.BUTTON_POSITIVE) {
          finish();
          return;
        }

        dialog.dismiss();

        executeStateMachine(SendState.START);
      }
    });
    sendDialog.setOnCancelListener(finishOnCancelListener);
    return sendDialog;
  }

  private void restoreInstanceState(Bundle savedInstanceState) {
    currentState = SendState.values()[savedInstanceState.getInt("state")];

    sendToMyMapsSuccess = savedInstanceState.getBoolean("mapsSuccess");
    sendToFusionTablesSuccess = savedInstanceState.getBoolean("fusionSuccess");
    sendToDocsSuccess = savedInstanceState.getBoolean("docsSuccess");

    sendToMyMapsMessage = savedInstanceState.getString("mapsMsg");
    sendToFusionTablesMessage = savedInstanceState.getString("fusionMsg");
    sendToDocsMessage = savedInstanceState.getString("docsMsg");

    sendToMyMapsMapId = savedInstanceState.getString("mapId");
    sendToFusionTablesTableId = savedInstanceState.getString("tableId");
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);

    outState.putInt("state", currentState.ordinal());

    outState.putBoolean("mapsSuccess", sendToMyMapsSuccess);
    outState.putBoolean("fusionSuccess", sendToFusionTablesSuccess);
    outState.putBoolean("docsSuccess", sendToDocsSuccess);

    outState.putString("mapsMsg", sendToMyMapsMessage);
    outState.putString("fusionMsg", sendToFusionTablesMessage);
    outState.putString("docsMsg", sendToDocsMessage);

    outState.putString("mapId", sendToMyMapsMapId);
    outState.putString("tableId", sendToFusionTablesTableId);

    // TODO: Ideally we should serialize/restore the authenticator map and lastAuth somehow,
    //       but it's highly unlikely we'll get killed while an auth dialog is displayed.
  }

  @Override
  protected void onStop() {
    Log.d(TAG, "SendActivity.onStop" + currentState);
    tracker.dispatch();
    tracker.stop();

    super.onStop();
  }

  @Override
  protected void onDestroy() {
    Log.d(TAG, "SendActivity.onDestroy, state=" + currentState);
    super.onDestroy();
  }

  private void executeStateMachine(SendState startState) {
    if (startState != null) {
      currentState = startState;
    }

    // If a state handler returns NOT_READY, it means it's waiting for some
    // event, and will call this method again when it happens.
    while (currentState != SendState.DONE &&
           currentState != SendState.NOT_READY) {
      Log.d(TAG, "Executing state " + currentState);
      currentState = executeState(currentState);
      Log.d(TAG, "New state is " + currentState);
    }
  }

  private SendState executeState(SendState state) {
    switch (state) {
      case SEND_OPTIONS:
        return showSendOptions();
      case START:
        return startSend();
      case AUTHENTICATE_MAPS:
        return authenticateToGoogleMaps();
      case PICK_MAP:
        return pickMap();
      case SEND_TO_MAPS:
        return sendToGoogleMaps();
      case SEND_TO_MAPS_DONE:
        return onSendToGoogleMapsDone();
      case AUTHENTICATE_FUSION_TABLES:
        return authenticateToFusionTables();
      case SEND_TO_FUSION_TABLES:
        return sendToFusionTables();
      case SEND_TO_FUSION_TABLES_DONE:
        return onSendToFusionTablesDone();
      case AUTHENTICATE_DOCS:
        return authenticateToGoogleDocs();
      case AUTHENTICATE_TRIX:
        return authenticateToGoogleTrix();
      case SEND_TO_DOCS:
        return sendToGoogleDocs();
      case SEND_TO_DOCS_DONE:
        return onSendToGoogleDocsDone();
      case SHOW_RESULTS:
        return onSendToGoogleDone();
      case SHARE_LINK:
        return shareLink();
      case FINISH:
        return onAllDone();
      default:
        Log.e(TAG, "Reached a non-executable state");
        return null;
    }
  }

  private SendState showSendOptions() {
    showDialog(SEND_DIALOG);

    return SendState.NOT_READY;
  }

  /**
   * Initiates the process to send tracks to google.
   * This is called once the user has selected sending options via the
   * SendToGoogleDialog.
   */
  private SendState startSend() {
    showDialog(PROGRESS_DIALOG);

    if (sendDialog.getSendToMyMaps()) {
      return SendState.AUTHENTICATE_MAPS;
    } else if (sendDialog.getSendToFusionTables()) {
      return SendState.AUTHENTICATE_FUSION_TABLES;
    } else if (sendDialog.getSendToDocs()) {
      return SendState.AUTHENTICATE_DOCS;
    } else  {
      Log.w(TAG, "Nowhere to upload to");
      return SendState.FINISH;
    }
  }

  private Dialog createProgressDialog() {
    progressDialog = new ProgressDialog(this);
    progressDialog.setCancelable(false);
    progressDialog.setIcon(android.R.drawable.ic_dialog_info);
    progressDialog.setTitle(R.string.progress_title);
    progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
    progressDialog.setMax(100);
    progressDialog.setProgress(0);

    return progressDialog;
  }

  private SendState authenticateToGoogleMaps() {
    Log.d(TAG, "SendActivity.authenticateToGoogleMaps");
    progressDialog.setProgress(0);
    progressDialog.setMessage(getString(
        R.string.progress_message_authenticating_mymaps));
    authenticate(Constants.AUTHENTICATE_TO_MY_MAPS, MyMapsConstants.SERVICE_NAME);
    // AUTHENTICATE_TO_MY_MAPS callback calls sendToGoogleMaps
    return SendState.NOT_READY;
  }

  private SendState pickMap() {
    if (!sendDialog.getCreateNewMap()) {
      // Ask the user to choose a map to upload into
      Intent listIntent = new Intent(this, MyMapsList.class);
      startActivityForResult(listIntent, Constants.GET_MAP);
      // The callback for GET_MAP calls authenticateToGoogleMaps
      return SendState.NOT_READY;
    } else {
      return SendState.SEND_TO_MAPS;
    }
  }

  private SendState sendToGoogleMaps() {
    tracker.trackPageView("/send/maps");

    SendToMyMaps.OnSendCompletedListener onCompletion = new SendToMyMaps.OnSendCompletedListener() {
      @Override
      public void onSendCompleted(String mapId, boolean success, int statusMessage) {
        // TODO: Use this message
        sendToMyMapsMessage = getString(statusMessage);
        sendToMyMapsSuccess = success;
        if (sendToMyMapsSuccess) {
          sendToMyMapsMapId = mapId;
          // Update the map id for this track:
          try {
            Track track = providerUtils.getTrack(sendTrackId);
            if (track != null) {
              track.setMapId(mapId);
              providerUtils.updateTrack(track);
            } else {
              Log.w(TAG, "Updating map id failed.");
            }
          } catch (RuntimeException e) {
            // If that fails whatever reasons we'll just log an error, but
            // continue.
            Log.w(TAG, "Updating map id failed.", e);
          }
        }

        executeStateMachine(SendState.SEND_TO_MAPS_DONE);
      }
    };

    if (sendToMyMapsMapId == null) {
      sendToMyMapsMapId = SendToMyMaps.NEW_MAP_ID;
    }

    final SendToMyMaps sender = new SendToMyMaps(this, sendToMyMapsMapId, lastAuth,
        sendTrackId, this /*progressIndicator*/, onCompletion);

    // TODO: We're leaking this thread!
    HandlerThread handlerThread = new HandlerThread("SendToMyMaps");
    handlerThread.start();
    Handler handler = new Handler(handlerThread.getLooper());
    handler.post(sender);

    return SendState.NOT_READY;
  }

  private SendState onSendToGoogleMapsDone() {
    if (sendDialog.getSendToFusionTables()) {
      return SendState.AUTHENTICATE_FUSION_TABLES;
    } else if (sendDialog.getSendToDocs()) {
      return SendState.AUTHENTICATE_DOCS;
    } else {
      return SendState.SHOW_RESULTS;
    }
  }

  private SendState authenticateToFusionTables() {
    progressDialog.setProgress(0);
    progressDialog.setMessage(getString(
        R.string.progress_message_authenticating_fusiontables));
    authenticate(Constants.AUTHENTICATE_TO_FUSION_TABLES, SendToFusionTables.SERVICE_ID);
    // AUTHENTICATE_TO_FUSION_TABLES callback calls sendToFusionTables
    return SendState.NOT_READY;
  }

  private SendState sendToFusionTables() {
    tracker.trackPageView("/send/fusion_tables");

    OnSendCompletedListener onCompletion = new OnSendCompletedListener() {
      @Override
      public void onSendCompleted(String tableId, boolean success,
          int statusMessage) {
        // TODO: Use this message
        sendToFusionTablesMessage = getString(statusMessage);
        sendToFusionTablesSuccess = success;
        if (sendToFusionTablesSuccess) {
          sendToFusionTablesTableId = tableId;
          // Update the table id for this track:
          try {
            Track track = providerUtils.getTrack(sendTrackId);
            if (track != null) {
              track.setTableId(tableId);
              providerUtils.updateTrack(track);
            } else {
              Log.w(TAG, "Updating table id failed.");
            }
          } catch (RuntimeException e) {
            // If that fails whatever reasons we'll just log an error, but
            // continue.
            Log.w(TAG, "Updating table id failed.", e);
          }
        }

        executeStateMachine(SendState.SEND_TO_FUSION_TABLES_DONE);
      }
    };
    final SendToFusionTables sender = new SendToFusionTables(this, lastAuth,
        sendTrackId, this /*progressIndicator*/, onCompletion);

    // TODO: We're leaking this thread!
    HandlerThread handlerThread = new HandlerThread("SendToFusionTables");
    handlerThread.start();
    Handler handler = new Handler(handlerThread.getLooper());
    handler.post(sender);

    return SendState.NOT_READY;
  }

  private SendState onSendToFusionTablesDone() {
    if (sendDialog.getSendToDocs()) {
      return SendState.AUTHENTICATE_DOCS;
    } else {
      return SendState.SHOW_RESULTS;
    }
  }

  private SendState authenticateToGoogleDocs() {
    setProgressValue(0);
    setProgressMessage(
        R.string.progress_message_authenticating_docs);
    authenticate(Constants.AUTHENTICATE_TO_DOCLIST, SendToDocs.GDATA_SERVICE_NAME_DOCLIST);
    // AUTHENTICATE_TO_DOCLIST callback calls authenticateToGoogleTrix
    return SendState.NOT_READY;
  }

  private SendState authenticateToGoogleTrix() {
    setProgressValue(30);
    setProgressMessage(
        R.string.progress_message_authenticating_docs);
    authenticate(Constants.AUTHENTICATE_TO_TRIX, SendToDocs.GDATA_SERVICE_NAME_TRIX);
    // AUTHENTICATE_TO_TRIX callback calls sendToGoogleDocs
    return SendState.NOT_READY;
  }

  private SendState sendToGoogleDocs() {
    Log.d(TAG, "Sending to Docs....");
    tracker.trackPageView("/send/docs");

    setProgressValue(50);
    setProgressMessage(R.string.progress_message_sending_docs);
    final SendToDocs sender = new SendToDocs(this,
        authMap.get(SendToDocs.GDATA_SERVICE_NAME_TRIX),
        authMap.get(SendToDocs.GDATA_SERVICE_NAME_DOCLIST),
        this);
    Runnable onCompletion = new Runnable() {
      public void run() {
        setProgressValue(100);

        // TODO: Use this message
        sendToDocsMessage = sender.getStatusMessage();
        sendToDocsSuccess = sender.wasSuccess();

        executeStateMachine(SendState.SEND_TO_DOCS_DONE);
      }
    };
    sender.setOnCompletion(onCompletion);
    sender.sendToDocs(sendTrackId);

    return SendState.NOT_READY;
  }

  private SendState onSendToGoogleDocsDone() {
    return SendState.SHOW_RESULTS;
  }

  private SendState onSendToGoogleDone() {
    tracker.dispatch();

    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        Log.d(TAG, "Sending to Google done.");
        dismissDialog(PROGRESS_DIALOG);
        dismissDialog(SEND_DIALOG);

        // Ensure a new done dialog is created each time.
        // This is required because the send results must be available at the
        // time the dialog is created.
        removeDialog(DONE_DIALOG);
        showDialog(DONE_DIALOG);
      }
    });

    return SendState.NOT_READY;
  }

  private Dialog createDoneDialog() {
    Log.d(TAG, "Creating done dialog");
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

    List<SendResult> results = makeSendToGoogleResults();
    final boolean canShare = sendToFusionTablesTableId != null || sendToMyMapsMapId != null;

    final OnClickListener finishListener = new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {
        dialog.dismiss();
        executeStateMachine(SendState.FINISH);
      }
    };

    DialogInterface.OnClickListener doShareListener = null;
    if (canShare) {
      doShareListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
          dialog.dismiss();

          executeStateMachine(SendState.SHARE_LINK);
        }
      };
    }

    DialogInterface.OnClickListener onOkListener = (canShare && shareRequested)
        ? doShareListener : finishListener;
    DialogInterface.OnClickListener onShareListener = (canShare && !shareRequested)
        ? doShareListener : null;

    return ResultDialogFactory.makeDialog(this, results, onOkListener, onShareListener, finishOnCancelListener);
  }

  private SendState shareLink() {
    boolean sentToMyMaps = sendDialog.getSendToMyMaps();
    boolean sentToFusionTables = sendDialog.getSendToFusionTables();
    if (!shareLinkToMap(sentToMyMaps, sentToFusionTables)) {
      Log.w(TAG, "Failed to share link");
    }
    return SendState.FINISH;
  }

  private SendState onAllDone() {
    Log.d(TAG, "All sending done.");
    removeDialog(PROGRESS_DIALOG);
    removeDialog(SEND_DIALOG);
    removeDialog(DONE_DIALOG);
    progressDialog = null;
    sendDialog = null;
    finish();
    return SendState.DONE;
  }

  boolean shareLinkToMap(boolean sentToMyMaps, boolean sentToFusionTables) {
    String url = null;
    if (sentToMyMaps && sendToMyMapsSuccess) {
      // Prefer a link to My Maps
      url = MapsFacade.buildMapUrl(sendToMyMapsMapId);
    } else if (sentToFusionTables && sendToFusionTablesSuccess) {
      // Otherwise try using the link to fusion tables
      url = getFusionTablesUrl(sendTrackId);
    }

    if (url != null) {
      shareLinkToMap(url);
      return true;
    }

    return false;
  }

  /**
   * Shares a link to a My Map or Fusion Table via external app (email, gmail, ...)
   * A chooser with apps that support text/plain will be shown to the user.
   */
  private void shareLinkToMap(String url) {
    boolean shareUrlOnly = sharedPreferences.getBoolean(
        getString(R.string.share_url_only_key), false);
    String msg = shareUrlOnly ? url : String.format(
        getResources().getText(R.string.share_map_body_format).toString(), url);

    Intent shareIntent = new Intent(Intent.ACTION_SEND);
    shareIntent.setType("text/plain");
    shareIntent.putExtra(Intent.EXTRA_SUBJECT,
        getResources().getText(R.string.share_map_subject).toString());
    shareIntent.putExtra(Intent.EXTRA_TEXT, msg);
    startActivity(Intent.createChooser(shareIntent,
        getResources().getText(R.string.share_map).toString()));
  }

  protected String getFusionTablesUrl(long trackId) {
    Track track = providerUtils.getTrack(trackId);
    return SendToFusionTables.getMapVisualizationUrl(track);
  }

  /**
   * Creates a list of {@link SendResult} instances based on the set of
   * services selected in {@link SendDialog} and the results as known to
   * this class.
   */
  private List<SendResult> makeSendToGoogleResults() {
    List<SendResult> results = new ArrayList<SendResult>();
    if (sendDialog.getSendToMyMaps()) {
      results.add(new SendResult(SendType.MYMAPS, sendToMyMapsSuccess));
    }
    if (sendDialog.getSendToFusionTables()) {
      results.add(new SendResult(SendType.FUSION_TABLES, sendToFusionTablesSuccess));
    }
    if (sendDialog.getSendToDocs()) {
      results.add(new SendResult(SendType.DOCS, sendToDocsSuccess));
    }

    return results;
  }

  /**
   * Initializes the authentication manager which obtains an authentication
   * token, prompting the user for a login and password if needed.
   */
  private void authenticate(final int requestCode, final String service) {
    lastAuth = authMap.get(service);
    if (lastAuth == null) {
      Log.i(TAG, "Creating a new authentication for service: " + service);
      lastAuth = AuthManagerFactory.getAuthManager(this,
          Constants.GET_LOGIN,
          null,
          true,
          service);
      authMap.put(service, lastAuth);
    }

    Log.d(TAG, "Logging in to " + service + "...");
    if (AuthManagerFactory.useModernAuthManager()) {
      runOnUiThread(new Runnable() {
        @Override
        public void run() {
          chooseAccount(requestCode, service);
        }
      });
    } else {
      doLogin(requestCode, service, null);
    }
  }

  private void chooseAccount(final int requestCode, final String service) {
    accountChooser.chooseAccount(SendActivity.this,
        new AccountChooser.AccountHandler() {
          @Override
          public void onAccountSelected(Account account) {
            if (account == null) {
              dismissDialog(PROGRESS_DIALOG);
              finish();
              return;
            }

            doLogin(requestCode, service, account);
          }
        });
  }

  private void doLogin(final int requestCode, final String service, final Account account) {
    // There's a chance that we go to a different activity, so close the dialog.
    lastAuth.doLogin(new Runnable() {
      public void run() {
        Log.i(TAG, "Login success for " + service + "!");
        onActivityResult(requestCode, RESULT_OK, new Intent());
      }
    }, account);
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode,
      final Intent results) {
    Log.w(TAG, "Result: req=" + requestCode + "; res=" + resultCode + "; result=" + results);
    SendState nextState = null;
    switch (requestCode) {
      case Constants.GET_LOGIN: {
        // TODO: This is a result from inside the auth manager,
        //       make this return path explicit.
        if (resultCode != RESULT_OK || lastAuth == null ||
            !lastAuth.authResult(resultCode, results)) {
          nextState = SendState.FINISH;
        }
      }
      case Constants.GET_MAP: {
        // User picked a map to upload to
        Log.d(TAG, "Get map result: " + resultCode);
        if (resultCode == RESULT_OK) {
          if (results.hasExtra("mapid")) {
            sendToMyMapsMapId = results.getStringExtra("mapid");
          }
          nextState = SendState.SEND_TO_MAPS;
        } else {
          nextState = SendState.FINISH;
        }
        break;
      }
      case Constants.AUTHENTICATE_TO_MY_MAPS: {
        // Authenticated with Google My Maps
        if (results != null && resultCode == RESULT_OK) {
          nextState = SendState.PICK_MAP;
        } else {
          nextState = SendState.SHOW_RESULTS;
        }
        break;
      }
      case Constants.AUTHENTICATE_TO_FUSION_TABLES: {
        // Authenticated with Google Fusion Tables
        if (results != null && resultCode == RESULT_OK) {
          nextState = SendState.SEND_TO_FUSION_TABLES;
        } else {
          nextState = SendState.SHOW_RESULTS;
        }
        break;
      }
      case Constants.AUTHENTICATE_TO_DOCLIST: {
        // Authenticated with Google Docs
        if (resultCode == RESULT_OK) {
          nextState = SendState.AUTHENTICATE_TRIX;
        } else {
          nextState = SendState.SHOW_RESULTS;
        }
        break;
      }
      case Constants.AUTHENTICATE_TO_TRIX: {
        // Authenticated with Trix
        if (resultCode == RESULT_OK) {
          nextState = SendState.SEND_TO_DOCS;
        } else {
          nextState = SendState.SHOW_RESULTS;
        }
        break;
      }

      default: {
        Log.w(TAG, "Warning unhandled request code: " + requestCode);
        return;
      }
    }

    if (nextState != null) {
      executeStateMachine(nextState);
    }
  }

  /**
   * Resets status information for sending to MyMaps/Docs.
   */
  private void resetState() {
    currentState = SendState.SEND_OPTIONS;
    sendToMyMapsMapId = null;
    sendToMyMapsMessage = "";
    sendToMyMapsSuccess = true;
    sendToFusionTablesMessage = "";
    sendToFusionTablesSuccess = true;
    sendToDocsMessage = "";
    sendToDocsSuccess = true;
    sendToFusionTablesTableId = null;
  }

  @Override
  public void setProgressMessage(final int resId) {
    runOnUiThread(new Runnable() {
      public void run() {
        if (progressDialog != null) {
          progressDialog.setMessage(getString(resId));
        }
      }
    });
  }

  @Override
  public void setProgressValue(final int percent) {
    runOnUiThread(new Runnable() {
      public void run() {
        if (progressDialog != null) {
          progressDialog.setProgress(percent);
        }
      }
    });
  }

  @Override
  public void clearProgressMessage() {
    progressDialog.setMessage("");
  }

  public static void sendToGoogle(Context ctx, long trackId, boolean shareLink) {
    Uri uri = ContentUris.withAppendedId(TracksColumns.CONTENT_URI, trackId);

    Intent intent = new Intent(ctx, SendActivity.class);
    intent.setAction(Intent.ACTION_SEND);
    intent.setDataAndType(uri, TracksColumns.CONTENT_ITEMTYPE);
    intent.putExtra(SendActivity.EXTRA_SHARE_LINK, shareLink);
    ctx.startActivity(intent);
  }
}
