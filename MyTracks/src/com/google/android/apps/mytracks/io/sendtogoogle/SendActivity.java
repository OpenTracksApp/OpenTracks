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

import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import com.google.android.apps.mytracks.Constants;
import com.google.android.apps.mytracks.MapsList;
import com.google.android.apps.mytracks.ProgressIndicator;
import com.google.android.apps.mytracks.content.MyTracksProviderUtils;
import com.google.android.apps.mytracks.content.Track;
import com.google.android.apps.mytracks.io.AuthManager;
import com.google.android.apps.mytracks.io.AuthManager.AuthCallback;
import com.google.android.apps.mytracks.io.AuthManagerFactory;
import com.google.android.apps.mytracks.io.SendToDocs;
import com.google.android.apps.mytracks.io.SendToMaps;
import com.google.android.apps.mytracks.io.maps.MapsConstants;
import com.google.android.apps.mytracks.io.maps.MapsFacade;
import com.google.android.apps.mytracks.util.SystemUtils;
import com.google.android.maps.mytracks.R;

import android.accounts.Account;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import java.util.HashMap;

/**
 * Helper activity for managing the sending of tracks to Google services.
 *
 * @author Rodrigo Damazio
 */
public class SendActivity extends Activity implements ProgressIndicator {
  
  // Items in the intent that starts the activity.
  public static final String TRACK_ID = "trackId";
  public static final String SHARE_URL = "shareUrl";
  public static final String SEND_MAPS = "sendMaps";
  public static final String SEND_FUSION_TABLES = "sendFusionTables";
  public static final String SEND_DOCS = "sendDocs";
  public static final String CREATE_MAP = "createMap";
  
  // Keys for saved state variables.
  private static final String STATE_ACCOUNT = "account";
  private static final String STATE_STATE = "state";
  private static final String STATE_DOCS_SUCCESS = "docsSuccess";
  private static final String STATE_FUSION_SUCCESS = "fusionSuccess";
  private static final String STATE_MAPS_SUCCESS = "mapsSuccess";
  private static final String STATE_TABLE_ID = "tableId";
  private static final String STATE_MAP_ID = "mapId";

  /** States for the state machine that defines the upload process. */
  private enum SendState {
    CHOOSE_ACCOUNT,
    START,
    AUTHENTICATE_MAPS,
    PICK_MAP,
    SEND_TO_MAPS,
    SEND_TO_MAPS_DONE,
    AUTHENTICATE_FUSION_TABLES,
    SEND_TO_FUSION_TABLES_DONE,
    AUTHENTICATE_DOCS,
    AUTHENTICATE_TRIX,
    SEND_TO_DOCS,
    SEND_TO_DOCS_DONE,
    SHOW_RESULTS,
    FINISH,
    DONE,
    NOT_READY
  }

  private static final int PROGRESS_DIALOG = 1;
  private ProgressDialog progressDialog;

  // Set in Activity.onCreate
  private MyTracksProviderUtils providerUtils;
  private GoogleAnalyticsTracker tracker;

  // Send request information. Set by the intent that starts the activity.
  private long sendTrackId;
  private boolean shareRequest;
  private boolean sendToMaps;
  private boolean sendToMapsNewMap;
  private boolean sendToFusionTables;
  private boolean sendToDocs;
  
  // Authentication information.
  private Account account;
  private HashMap<String, AuthManager> authMap = new HashMap<String, AuthManager>();
  
  // Current sending state.
  private SendState currentState;
  
  // Send result information. Used by the results dialog.
  private boolean sendToMapsSuccess;
  private boolean sendToFusionTablesSuccess;
  private boolean sendToDocsSuccess;
  private String sendToMapsMapId;
  private String sendToFusionTablesTableId;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    Log.d(TAG, "SendActivity.onCreate");
    super.onCreate(savedInstanceState);

    if (!handleIntent()) {
      finish();
      return;
     }
    
    providerUtils = MyTracksProviderUtils.Factory.get(this);

    tracker = GoogleAnalyticsTracker.getInstance();
    // Start the tracker in manual dispatch mode...
    tracker.start(getString(R.string.my_tracks_analytics_id),
        getApplicationContext());
    tracker.setProductVersion("android-mytracks",
        SystemUtils.getMyTracksVersion(this));

    resetState();

    if (savedInstanceState != null) {
      restoreInstanceState(savedInstanceState);
    }

    // If we had the instance restored after it was done, reset it.
    if (currentState == SendState.DONE) {
      resetState();
    }

    // Execute the state machine, at the start or restored state.
    Log.w(TAG, "Starting at state " + currentState);
    executeStateMachine(currentState);
  }

  private boolean handleIntent() {
    Intent intent = getIntent();
    sendTrackId = intent.getLongExtra(TRACK_ID, -1L);
    if (sendTrackId == -1L) {
      return false;
    }
    sendToMaps = intent.getBooleanExtra(SEND_MAPS, false);
    sendToFusionTables = intent.getBooleanExtra(SEND_FUSION_TABLES, false);
    sendToDocs = intent.getBooleanExtra(SEND_DOCS, false);
    if (!sendToMaps && !sendToFusionTables && !sendToDocs) {
      return false;
    }
    sendToMapsNewMap = intent.getBooleanExtra(CREATE_MAP, true);
    return true;
  }

  @Override
  protected Dialog onCreateDialog(int id) {
    switch (id) {
      case PROGRESS_DIALOG:
        return createProgressDialog();
      default:
        return null;
    }
  }

  private void restoreInstanceState(Bundle savedInstanceState) {
    account = savedInstanceState.getParcelable(STATE_ACCOUNT);
    
    currentState = SendState.values()[savedInstanceState.getInt(STATE_STATE)];

    sendToMapsSuccess = savedInstanceState.getBoolean(STATE_MAPS_SUCCESS);
    sendToFusionTablesSuccess = savedInstanceState.getBoolean(STATE_FUSION_SUCCESS);
    sendToDocsSuccess = savedInstanceState.getBoolean(STATE_DOCS_SUCCESS);
    sendToMapsMapId = savedInstanceState.getString(STATE_MAP_ID);
    sendToFusionTablesTableId = savedInstanceState.getString(STATE_TABLE_ID);
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putParcelable(STATE_ACCOUNT, account);
    
    outState.putInt(STATE_STATE, currentState.ordinal());

    outState.putBoolean(STATE_MAPS_SUCCESS, sendToMapsSuccess);
    outState.putBoolean(STATE_FUSION_SUCCESS, sendToFusionTablesSuccess);
    outState.putBoolean(STATE_DOCS_SUCCESS, sendToDocsSuccess);
    outState.putString(STATE_MAP_ID, sendToMapsMapId);
    outState.putString(STATE_TABLE_ID, sendToFusionTablesTableId);
  }

  @Override
  protected void onStop() {
    super.onStop();
    Log.d(TAG, "SendActivity.onStop, state=" + currentState);
    tracker.dispatch();
    tracker.stop();
  }

  private void executeStateMachine(SendState startState) {
    currentState = startState;

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
      case CHOOSE_ACCOUNT:
        return chooseAccount();
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
      case FINISH:
        return onAllDone();
      default:
        Log.e(TAG, "Reached a non-executable state");
        return null;
    }
  }

  private SendState chooseAccount() {
    Intent intent = new Intent(this, AccountChooserActivity.class);
    startActivityForResult(intent, Constants.CHOOSE_ACCOUNT);
    return SendState.NOT_READY;
  }
  
  /**
   * Initiates the process to send tracks to google.
   * This is called once the user has selected sending options via the
   * SendToGoogleDialog.
   */
  private SendState startSend() {
    if (sendToMaps) {
      return SendState.AUTHENTICATE_MAPS;
    } else if (sendToFusionTables) {
      return SendState.AUTHENTICATE_FUSION_TABLES;
    } else if (sendToDocs) {
      return SendState.AUTHENTICATE_DOCS;
    } else {
      Log.w(TAG, "Nowhere to upload to");
      return SendState.FINISH;
    }
  }

  private Dialog createProgressDialog() {
    progressDialog = new ProgressDialog(this);
    progressDialog.setCancelable(false);
    progressDialog.setIcon(android.R.drawable.ic_dialog_info);
    progressDialog.setTitle(R.string.generic_progress_title);
    progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
    progressDialog.setMax(100);
    progressDialog.setProgress(0);

    return progressDialog;
  }

  private SendState authenticateToGoogleMaps() {
    Log.d(TAG, "SendActivity.authenticateToGoogleMaps");
    showDialog(PROGRESS_DIALOG);
    progressDialog.setProgress(0);
    progressDialog.setMessage(getAuthenticatingProgressMessage(SendType.MAPS));
    authenticate(Constants.AUTHENTICATE_TO_MAPS, MapsConstants.SERVICE_NAME);
    // AUTHENTICATE_TO_MAPS callback calls sendToGoogleMaps
    return SendState.NOT_READY;
  }

  private SendState pickMap() {
    if (!sendToMapsNewMap) {
      // Ask the user to choose a map to upload into
      Intent listIntent = new Intent(this, MapsList.class);
      listIntent.putExtra(MapsList.EXTRA_ACCOUNT_NAME, account.name);
      listIntent.putExtra(MapsList.EXTRA_ACCOUNT_TYPE, account.type);
      startActivityForResult(listIntent, Constants.GET_MAP);
      // The callback for GET_MAP calls authenticateToGoogleMaps
      return SendState.NOT_READY;
    } else {
      return SendState.SEND_TO_MAPS;
    }
  }

  private SendState sendToGoogleMaps() {
    tracker.trackPageView("/send/maps");

    SendToMaps.OnSendCompletedListener onCompletion = new SendToMaps.OnSendCompletedListener() {
      @Override
      public void onSendCompleted(String mapId, boolean success) {
        sendToMapsSuccess = success;
        if (sendToMapsSuccess) {
          sendToMapsMapId = mapId;
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

    if (sendToMapsMapId == null) {
      sendToMapsMapId = SendToMaps.NEW_MAP_ID;
    }

    final SendToMaps sender = new SendToMaps(this, sendToMapsMapId,
        getAuthManager(MapsConstants.SERVICE_NAME), sendTrackId, this /*progressIndicator*/,
        onCompletion);

    new Thread(sender, "SendToMaps").start();

    return SendState.NOT_READY;
  }

  private SendState onSendToGoogleMapsDone() {
    if (sendToFusionTables) {
      return SendState.AUTHENTICATE_FUSION_TABLES;
    } else if (sendToDocs) {
      return SendState.AUTHENTICATE_DOCS;
    } else {
      return SendState.SHOW_RESULTS;
    }
  }

  private SendState authenticateToFusionTables() {
    tracker.trackPageView("/send/fusion_tables");
    Intent intent = new Intent(this, SendFusionTablesActivity.class)
        .putExtra(SendFusionTablesActivity.ACCOUNT, account)
        .putExtra(SendFusionTablesActivity.TRACK_ID, sendTrackId);
    startActivityForResult(intent, Constants.SEND_FUSION_TABLES);
    return SendState.NOT_READY;
  }

  private SendState onSendToFusionTablesDone() {
    if (sendToDocs) {
      return SendState.AUTHENTICATE_DOCS;
    } else {
      return SendState.SHOW_RESULTS;
    }
  }

  private SendState authenticateToGoogleDocs() {
    showDialog(PROGRESS_DIALOG);
    setProgressValue(0);
    setProgressMessage(getAuthenticatingProgressMessage(SendType.DOCS));
    authenticate(Constants.AUTHENTICATE_TO_DOCLIST, SendToDocs.GDATA_SERVICE_NAME_DOCLIST);
    // AUTHENTICATE_TO_DOCLIST callback calls authenticateToGoogleTrix
    return SendState.NOT_READY;
  }

  private SendState authenticateToGoogleTrix() {
    setProgressValue(30);
    setProgressMessage(getAuthenticatingProgressMessage(SendType.DOCS));
    authenticate(Constants.AUTHENTICATE_TO_TRIX, SendToDocs.GDATA_SERVICE_NAME_TRIX);
    // AUTHENTICATE_TO_TRIX callback calls sendToGoogleDocs
    return SendState.NOT_READY;
  }

  private SendState sendToGoogleDocs() {
    Log.d(TAG, "Sending to Docs....");
    tracker.trackPageView("/send/docs");

    setProgressValue(50);
    String format = getString(R.string.send_google_progress_sending);
    String serviceName = getString(SendType.DOCS.getServiceName());
    setProgressMessage(String.format(format, serviceName));
    final SendToDocs sender = new SendToDocs(this,
        getAuthManager(SendToDocs.GDATA_SERVICE_NAME_TRIX),
        getAuthManager(SendToDocs.GDATA_SERVICE_NAME_DOCLIST),
        this);
    Runnable onCompletion = new Runnable() {
      public void run() {
        setProgressValue(100);
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
    String mapsUrl = sendToMaps && sendToMapsSuccess
                     ? MapsFacade.buildMapUrl(sendToMapsMapId)
                     : null;
    String fusionTablesUrl = sendToFusionTables && sendToFusionTablesSuccess 
                             ? SendFusionTablesUtils.getMapUrl(providerUtils.getTrack(sendTrackId))
                             : null;
    Intent intent = new Intent(this, UploadResultActivity.class)
        .putExtra(UploadResultActivity.HAS_MAPS_RESULT, sendToMaps)
        .putExtra(UploadResultActivity.HAS_FUSION_TABLES_RESULT, sendToFusionTables)
        .putExtra(UploadResultActivity.HAS_DOCS_RESULT, sendToDocs)
        .putExtra(UploadResultActivity.MAPS_SUCCESS, sendToMapsSuccess)
        .putExtra(UploadResultActivity.FUSION_TABLES_SUCCESS, sendToFusionTablesSuccess)
        .putExtra(UploadResultActivity.DOCS_SUCCESS, sendToDocsSuccess)
        .putExtra(UploadResultActivity.SHARE_REQUEST, shareRequest)
        .putExtra(UploadResultActivity.MAPS_URL, mapsUrl)
        .putExtra(UploadResultActivity.FUSION_TABLES_URL, fusionTablesUrl);
    startActivity(intent);
    return SendState.FINISH;
  }
 
  private SendState onAllDone() {
    finish();
    return SendState.DONE;
  }

    /**
   * Initializes the authentication manager which obtains an authentication
   * token, prompting the user for a login and password if needed.
   */
  private void authenticate(final int requestCode, final String service) {
    AuthManager authManager = getAuthManager(service);
    authManager.doLogin(new AuthCallback() {
      @Override
      public void onAuthResult(boolean success) {
        Log.i(TAG, "Login success for " + service + ": " + success);
        if (!success) {
          executeStateMachine(SendState.SHOW_RESULTS);
          return;
        }

        onLoginSuccess(requestCode);
      }
    }, account);
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode,
      final Intent results) {
    SendState nextState = null;
    switch (requestCode) {
      case Constants.GET_MAP: {
        // User picked a map to upload to
        Log.d(TAG, "Get map result: " + resultCode);
        if (resultCode == RESULT_OK) {
          if (results.hasExtra("mapid")) {
            sendToMapsMapId = results.getStringExtra("mapid");
          }
          nextState = SendState.SEND_TO_MAPS;
        } else {
          nextState = SendState.FINISH;
        }
        break;
      }
      case Constants.CHOOSE_ACCOUNT: {
        if (resultCode == RESULT_CANCELED) {
          nextState = SendState.FINISH;
          break;
        }
        account = results.getParcelableExtra(AccountChooserActivity.ACCOUNT);
        if (account == null) {
          nextState = SendState.FINISH;
          break;
        }
        nextState = SendState.START;
        break;
      }
      case Constants.SEND_FUSION_TABLES: {
        if (resultCode == RESULT_CANCELED) {
          nextState = SendState.FINISH;
          break;
        }
        sendToFusionTablesSuccess = results.getBooleanExtra(
            SendFusionTablesActivity.SUCCESS, false);
        sendToFusionTablesTableId = results.getStringExtra(SendFusionTablesActivity.TABLE_ID);
        nextState = SendState.SEND_TO_FUSION_TABLES_DONE;
        break;
      }
      default: {
        Log.e(TAG, "Unrequested result: " + requestCode);
        return;
      }
    }

    if (nextState != null) {
      executeStateMachine(nextState);
    }
  }

  private void onLoginSuccess(int requestCode) {
    SendState nextState;
    switch (requestCode) {
      case Constants.AUTHENTICATE_TO_MAPS:
        // Authenticated with Google Maps
        nextState = SendState.PICK_MAP;
        break;
      case Constants.AUTHENTICATE_TO_DOCLIST:
        // Authenticated with Google Docs
        nextState = SendState.AUTHENTICATE_TRIX;
        break;
      case Constants.AUTHENTICATE_TO_TRIX:
        // Authenticated with Trix
        nextState = SendState.SEND_TO_DOCS;
        break;

      default: {
        Log.e(TAG, "Unrequested login code: " + requestCode);
        return;
      }
    }

    executeStateMachine(nextState);
  }

  private AuthManager getAuthManager(String service) {
    AuthManager authManager = authMap.get(service);
    if (authManager == null) {
      authManager = AuthManagerFactory.getAuthManager(this,
          Constants.GET_LOGIN,
          null,
          true,
          service);
      authMap.put(service, authManager);
    }
    return authManager;
  }

  /**
   * Resets status information for sending to Maps/Fusion Tables/Docs.
   */
  private void resetState() {
    account = null;
    authMap.clear();
    currentState = SendState.CHOOSE_ACCOUNT;
    sendToMapsSuccess = false;
    sendToFusionTablesSuccess = false;
    sendToDocsSuccess = false;
    sendToMapsMapId = null;
    sendToFusionTablesTableId = null;
  }

  /**
   * Gets a progress message indicating My Tracks is authenticating to a
   * service.
   *
   * @param type the type of service
   */
  private String getAuthenticatingProgressMessage(SendType type) {
    String format = getString(R.string.send_google_progress_authenticating);
    String serviceName = getString(type.getServiceName());
    return String.format(format, serviceName);
  }

  @Override
  public void setProgressMessage(final String message) {
    runOnUiThread(new Runnable() {
      public void run() {
        if (progressDialog != null) {
          progressDialog.setMessage(message);
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
}
