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
import com.google.android.apps.mytracks.content.MyTracksProviderUtils;
import com.google.android.apps.mytracks.content.Track;
import com.google.android.apps.mytracks.io.docs.SendDocsActivity;
import com.google.android.apps.mytracks.io.fusiontables.SendFusionTablesActivity;
import com.google.android.apps.mytracks.io.fusiontables.SendFusionTablesUtils;
import com.google.android.apps.mytracks.io.maps.ChooseMapActivity;
import com.google.android.apps.mytracks.io.maps.SendMapsActivity;
import com.google.android.apps.mytracks.io.maps.SendMapsUtils;
import com.google.android.apps.mytracks.util.SystemUtils;
import com.google.android.maps.mytracks.R;

import android.accounts.Account;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

/**
 * Helper activity for managing the sending of tracks to Google services.
 *
 * @author Rodrigo Damazio
 */
public class SendActivity extends Activity {
  
  // Items in the intent that starts the activity.
  public static final String TRACK_ID = "trackId";
  public static final String SHARE_URL = "shareUrl";
  public static final String SEND_MAPS = "sendMaps";
  public static final String SEND_FUSION_TABLES = "sendFusionTables";
  public static final String SEND_DOCS = "sendDocs";
  public static final String CREATE_MAP = "createMap";
  
  // Keys for saved state variables.
  private static final String STATE_STATE = "state";
  private static final String STATE_ACCOUNT = "account";
  private static final String STATE_MAP_ID = "mapId";
  private static final String STATE_DOCS_SUCCESS = "docsSuccess";
  private static final String STATE_FUSION_TABLES_SUCCESS = "fusionTablesSuccess";
  private static final String STATE_MAPS_SUCCESS = "mapsSuccess";

  /** States for the state machine that defines the upload process. */
  private enum SendState {
    CHOOSE_ACCOUNT,
    CHOOSE_MAP,
    START,
    SEND_TO_MAPS,
    SEND_TO_MAPS_DONE,
    SEND_TO_FUSION_TABLES,
    SEND_TO_FUSION_TABLES_DONE,
    SEND_TO_DOCS,
    SEND_TO_DOCS_DONE,
    SHOW_RESULTS,
    FINISH,
    DONE,
    NOT_READY
  }

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
  
  // Current sending state.
  private SendState currentState;
  
  // Authentication information.
  private Account account;
  
  // Map id from choosing a Google Map
  private String mapId;
  
  // Send result information. Used by the results dialog.
  private boolean sendToMapsSuccess;
  private boolean sendToFusionTablesSuccess;
  private boolean sendToDocsSuccess;

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
      finish();
      return;
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

  private void restoreInstanceState(Bundle savedInstanceState) {
    currentState = SendState.values()[savedInstanceState.getInt(STATE_STATE)];
    account = savedInstanceState.getParcelable(STATE_ACCOUNT);    
    mapId = savedInstanceState.getString(STATE_MAP_ID);
    sendToMapsSuccess = savedInstanceState.getBoolean(STATE_MAPS_SUCCESS);
    sendToFusionTablesSuccess = savedInstanceState.getBoolean(STATE_FUSION_TABLES_SUCCESS);
    sendToDocsSuccess = savedInstanceState.getBoolean(STATE_DOCS_SUCCESS);
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putInt(STATE_STATE, currentState.ordinal());
    outState.putParcelable(STATE_ACCOUNT, account);
    outState.putString(STATE_MAP_ID, mapId);
    outState.putBoolean(STATE_MAPS_SUCCESS, sendToMapsSuccess);
    outState.putBoolean(STATE_FUSION_TABLES_SUCCESS, sendToFusionTablesSuccess);
    outState.putBoolean(STATE_DOCS_SUCCESS, sendToDocsSuccess);
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
      case CHOOSE_MAP:
        return chooseMap();
      case START:
        return startSend();
      case SEND_TO_MAPS:
        return sendToGoogleMaps();
      case SEND_TO_MAPS_DONE:
        return onSendToGoogleMapsDone();
      case SEND_TO_FUSION_TABLES:
        return sendToFusionTables();
      case SEND_TO_FUSION_TABLES_DONE:
        return onSendToFusionTablesDone();
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
      return SendState.SEND_TO_MAPS;
    } else if (sendToFusionTables) {
      return SendState.SEND_TO_FUSION_TABLES;
    } else if (sendToDocs) {
      return SendState.SEND_TO_DOCS;
    } else {
      Log.w(TAG, "Nowhere to upload to");
      return SendState.FINISH;
    }
  }

  private SendState chooseMap() {
    if (!sendToMapsNewMap) {
      Intent intent = new Intent(this, ChooseMapActivity.class)
          .putExtra(ChooseMapActivity.ACCOUNT, account);
      startActivityForResult(intent, Constants.CHOOSE_MAP);
      return SendState.NOT_READY;
    } else {
      return SendState.START;
    }
  }

  private SendState sendToGoogleMaps() {
    tracker.trackPageView("/send/maps");
    Intent intent = new Intent(this, SendMapsActivity.class)
        .putExtra(SendMapsActivity.ACCOUNT, account)
        .putExtra(SendMapsActivity.TRACK_ID, sendTrackId)
        .putExtra(SendMapsActivity.MAP_ID, mapId);
    startActivityForResult(intent, Constants.SEND_MAPS);
    return SendState.NOT_READY;
  }

  private SendState onSendToGoogleMapsDone() {
    if (sendToFusionTables) {
      return SendState.SEND_TO_FUSION_TABLES;
    } else if (sendToDocs) {
      return SendState.SEND_TO_DOCS;
    } else {
      return SendState.SHOW_RESULTS;
    }
  }

  private SendState sendToFusionTables() {
    tracker.trackPageView("/send/fusion_tables");
    Intent intent = new Intent(this, SendFusionTablesActivity.class)
        .putExtra(SendFusionTablesActivity.ACCOUNT, account)
        .putExtra(SendFusionTablesActivity.TRACK_ID, sendTrackId);
    startActivityForResult(intent, Constants.SEND_FUSION_TABLES);
    return SendState.NOT_READY;
  }

  private SendState onSendToFusionTablesDone() {
    if (sendToDocs) {
      return SendState.SEND_TO_DOCS;
    } else {
      return SendState.SHOW_RESULTS;
    }
  }

  private SendState sendToGoogleDocs() {
    tracker.trackPageView("/send/docs");
    Intent intent = new Intent(this, SendDocsActivity.class)
        .putExtra(SendFusionTablesActivity.ACCOUNT, account)
        .putExtra(SendFusionTablesActivity.TRACK_ID, sendTrackId);
    startActivityForResult(intent, Constants.SEND_DOCS);
    return SendState.NOT_READY;
  }

  private SendState onSendToGoogleDocsDone() {
    return SendState.SHOW_RESULTS;
  }

  private SendState onSendToGoogleDone() {
    tracker.dispatch();
    Track track = providerUtils.getTrack(sendTrackId);
    String mapsUrl = sendToMaps && sendToMapsSuccess ? SendMapsUtils.getMapUrl(track) : null;
    String fusionTablesUrl = sendToFusionTables && sendToFusionTablesSuccess 
                             ? SendFusionTablesUtils.getMapUrl(track) : null;
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

  @Override
  public void onActivityResult(int requestCode, int resultCode,
      final Intent results) {
    SendState nextState = null;
    switch (requestCode) {
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
        nextState = SendState.CHOOSE_MAP;
        break;
      }
      case Constants.CHOOSE_MAP: {
        if (resultCode == RESULT_CANCELED) {
          nextState = SendState.FINISH;
          break;
        }
        mapId = results.getStringExtra(ChooseMapActivity.MAP_ID);
        if (mapId == null) {
          nextState = SendState.FINISH;
          break;
        }
        nextState = SendState.START;
        break;
      }
      case Constants.SEND_MAPS: {
        if (resultCode == RESULT_CANCELED) {
          nextState = SendState.FINISH;
          break;
        }
        sendToMapsSuccess = results.getBooleanExtra(SendMapsActivity.SUCCESS, false);
        nextState = SendState.SEND_TO_MAPS_DONE;
        break;
      }
      case Constants.SEND_FUSION_TABLES: {
        if (resultCode == RESULT_CANCELED) {
          nextState = SendState.FINISH;
          break;
        }
        sendToFusionTablesSuccess = results.getBooleanExtra(
            SendFusionTablesActivity.SUCCESS, false);
        nextState = SendState.SEND_TO_FUSION_TABLES_DONE;
        break;
      }
      case Constants.SEND_DOCS: {
        if (resultCode == RESULT_CANCELED) {
          nextState = SendState.FINISH;
          break;
        }
        sendToDocsSuccess = results.getBooleanExtra(SendDocsActivity.SUCCESS, false);
        nextState = SendState.SEND_TO_DOCS_DONE;
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

  /**
   * Resets status information for sending to Maps/Fusion Tables/Docs.
   */
  private void resetState() {
    currentState = SendState.CHOOSE_ACCOUNT;
    account = null;
    mapId = null;
    sendToMapsSuccess = false;
    sendToFusionTablesSuccess = false;
    sendToDocsSuccess = false;
  }
}
