package com.google.android.apps.mytracks.io.sendtogoogle;

import static com.google.android.apps.mytracks.Constants.TAG;

import com.google.android.accounts.Account;
import com.google.android.apps.mytracks.AccountChooser;
import com.google.android.apps.mytracks.Constants;
import com.google.android.apps.mytracks.DialogManager;
import com.google.android.apps.mytracks.MyMapsList;
import com.google.android.apps.mytracks.ProgressIndicator;
import com.google.android.apps.mytracks.content.MyTracksProviderUtils;
import com.google.android.apps.mytracks.content.Track;
import com.google.android.apps.mytracks.io.AuthManager;
import com.google.android.apps.mytracks.io.AuthManagerFactory;
import com.google.android.apps.mytracks.io.SendToDocs;
import com.google.android.apps.mytracks.io.SendToFusionTables;
import com.google.android.apps.mytracks.io.SendToFusionTables.OnSendCompletedListener;
import com.google.android.apps.mytracks.io.SendToMyMaps;
import com.google.android.apps.mytracks.io.mymaps.MapsFacade;
import com.google.android.apps.mytracks.io.mymaps.MyMapsConstants;
import com.google.android.maps.mytracks.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Helper for managing the sending of tracks to Google services.
 *
 * @author Rodrigo Damazio
 */
public class SendActivity extends Activity implements ProgressIndicator {
  public static final String EXTRA_SHARE_LINK = "share_link";
  public static final String EXTRA_TRACK_ID = "track_id";

  // Services
  private MyTracksProviderUtils providerUtils;
  private SharedPreferences sharedPreferences;

  // Authentication
  private AuthManager lastAuth;
  private final HashMap<String, AuthManager> authMap =
    new HashMap<String, AuthManager>();
  private final AccountChooser accountChooser = new AccountChooser();

  // Send request information.
  private boolean shareRequested = false;
  private long sendTrackId;

  // Send result information, used by SendToGoogleResultDialog.
  private boolean sendToMyMapsSuccess = false;
  private boolean sendToFusionTablesSuccess = false;
  private boolean sendToDocsSuccess = false;
  private String sendToMyMapsMapId;
  private String sendToFusionTablesTableId;

  // TODO: Make these be used for showing results
  @SuppressWarnings("unused")
  private String sendToMyMapsMessage;
  @SuppressWarnings("unused")
  private String sendToFusionTablesMessage;
  @SuppressWarnings("unused")
  private String sendToDocsMessage;

  // State used while sending.
  private SendDialog sendDialog;
  private ProgressDialog progressDialog;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    providerUtils = MyTracksProviderUtils.Factory.get(this);
    sharedPreferences = getSharedPreferences(Constants.SETTINGS_NAME, 0);
  }

  @Override
  protected void onStart() {
    super.onStart();
    resetState();

    sendTrackId = getIntent().getLongExtra(EXTRA_TRACK_ID, -1);

    sendDialog = new SendDialog(this);
    sendDialog.setOwnerActivity(this);
    sendDialog.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(DialogInterface arg0, int which) {
        if (which != DialogInterface.BUTTON_POSITIVE) {
          finish();
          return;
        }

        doSend();
      }
    });
    sendDialog.show();
  }

  /**
   * Initiates the process to send tracks to google.
   * This is called once the user has selected sending options via the
   * SendToGoogleDialog.
   * 
   * TODO: Change this whole flow to an actual state machine.
   */
  private void doSend() {
    progressDialog = new ProgressDialog(this);
    progressDialog.setIcon(android.R.drawable.ic_dialog_info);
    progressDialog.setTitle(R.string.progress_title);
    progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
    progressDialog.setMessage("");
    progressDialog.setMax(100);
    progressDialog.setProgress(0);
    progressDialog.show();

    if (sendDialog.getSendToMyMaps()) {
      sendToGoogleMapsOrPickMap();
    } else if (sendDialog.getSendToFusionTables()) {
      authenticateToFusionTables(null);
    } else if (sendDialog.getSendToDocs()) {
      authenticateToGoogleDocs();
    } else  {
      Log.w(TAG, "Nowhere to upload to");
      onSendToGoogleDone();
    }
  }

  private void sendToGoogleMapsOrPickMap() {
    if (!sendDialog.getCreateNewMap()) {
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

    progressDialog.setProgress(0);
    progressDialog.setMessage(getString(
        R.string.progress_message_authenticating_mymaps));
    authenticate(results, Constants.AUTHENTICATE_TO_MY_MAPS,
        MyMapsConstants.SERVICE_NAME);
    // AUTHENTICATE_TO_MY_MAPS callback calls sendToGoogleMaps
  }

  private void sendToGoogleMaps(String mapId) {
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
    final SendToMyMaps sender = new SendToMyMaps(this, mapId, lastAuth,
        sendTrackId, this /*progressIndicator*/, onCompletion);

    HandlerThread handlerThread = new HandlerThread("SendToMyMaps");
    handlerThread.start();
    Handler handler = new Handler(handlerThread.getLooper());
    handler.post(sender);
  }

  private void onSendToGoogleMapsDone() {
    if (sendDialog.getSendToFusionTables()) {
      authenticateToFusionTables(null);
    } else if (sendDialog.getSendToDocs()) {
      authenticateToGoogleDocs();
    } else {
      onSendToGoogleDone();
    }
  }

  private void authenticateToFusionTables(Intent results) {
    if (results == null) { results = new Intent(); }

    progressDialog.setProgress(0);
    progressDialog.setMessage(getString(
        R.string.progress_message_authenticating_fusiontables));
    authenticate(results, Constants.AUTHENTICATE_TO_FUSION_TABLES,
        SendToFusionTables.SERVICE_ID);
    // AUTHENTICATE_TO_FUSION_TABLES callback calls sendToFusionTables
  }

  private void sendToFusionTables() {
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
    final SendToFusionTables sender = new SendToFusionTables(this, lastAuth,
        sendTrackId, this/*progressIndicator*/, onCompletion);

    HandlerThread handlerThread = new HandlerThread("SendToFusionTables");
    handlerThread.start();
    Handler handler = new Handler(handlerThread.getLooper());
    handler.post(sender);
  }

  private void onSendToFusionTablesDone() {
    if (sendDialog.getSendToDocs()) {
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

  private void sendToGoogleDocs() {
    Log.d(TAG, "Sending to Docs....");
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

        onSendToGoogleDocsDone();
      }
    };
    sender.setOnCompletion(onCompletion);
    sender.sendToDocs(sendTrackId);
  }

  private void onSendToGoogleDocsDone() {
    onSendToGoogleDone();
  }

  private void onSendToGoogleDone() {
    final boolean sentToMyMaps = sendDialog.getSendToMyMaps();
    final boolean sentToFusionTables = sendDialog.getSendToFusionTables();
    List<SendResult> results = makeSendToGoogleResults();

    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        progressDialog.dismiss();
        progressDialog = null;
        sendDialog = null;
      }
    });

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

    final boolean canShare = sendToFusionTablesTableId != null
        || sendToMyMapsMapId != null;

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
        results, onOkListener, onShareListener);
    DialogManager.showDialogSafely(this, sendToGoogleResultDialog);
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
    return track == null ? "" : SendToFusionTables.getMapVisualizationUrl(track);
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
  private void authenticate(final Intent results, final int requestCode,
      final String service) {
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
          chooseAccount(results, requestCode, service);
        }
      });
    } else {
      doLogin(results, requestCode, service, null);
    }
  }

  private void chooseAccount(final Intent results, final int requestCode,
      final String service) {
    accountChooser.chooseAccount(SendActivity.this,
        new AccountChooser.AccountHandler() {
          @Override
          public void handleAccountSelected(Account account) {
            if (account == null) {
              progressDialog.dismiss();
              return;
            }

            doLogin(results, requestCode, service, account);
          }
        });
  }

  private void doLogin(final Intent results, final int requestCode,
      final String service, final Account account) {
    lastAuth.doLogin(new Runnable() {
      public void run() {
        Log.i(TAG, "Loggin success for " + service + "!");
        onActivityResult(requestCode, RESULT_OK, results);
      }
    }, account);
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode,
      final Intent results) {
    switch (requestCode) {
      case Constants.GET_LOGIN: {
        // TODO: This is a result from inside the auth manager,
        //       make this return path explicit.
        if (resultCode != RESULT_OK || lastAuth == null ||
            !lastAuth.authResult(resultCode, results)) {
          progressDialog.dismiss();
        }
        break;
      }
      case Constants.GET_MAP: {
        // User picked a map to upload to
        if (resultCode == RESULT_OK) {
          results.putExtra("trackid", sendTrackId);
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

          sendToGoogleMaps(mapId);
        } else {
          onSendToGoogleDone();
        }
        break;
      }
      case Constants.AUTHENTICATE_TO_FUSION_TABLES: {
        // Authenticated with Google Fusion Tables
        if (results != null && resultCode == RESULT_OK) {
          sendToFusionTables();
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
          sendToGoogleDocs();
        } else {
          onSendToGoogleDone();
        }
        break;
      }
      case Constants.SHARE_LINK: {
        Track selectedTrack = providerUtils.getTrack(sendTrackId);
        if (selectedTrack != null) {
          if (!TextUtils.isEmpty(selectedTrack.getMapId())) {
            shareLinkToMap(MapsFacade.buildMapUrl(selectedTrack.getMapId()));
          } else if (!TextUtils.isEmpty(selectedTrack.getTableId())) {
            shareLinkToMap(getFusionTablesUrl(sendTrackId));
          } else {
            shareRequested = true;
            sendDialog.dismiss();
          }
        }
        break;
      }

      default: {
        Log.w(TAG, "Warning unhandled request code: " + requestCode);
      }
    }
  }

  /**
   * Resets status information for sending to MyMaps/Docs.
   */
  private void resetState() {
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
    Intent intent = new Intent(ctx, SendActivity.class);
    intent.putExtra(SendActivity.EXTRA_TRACK_ID, trackId);
    intent.putExtra(SendActivity.EXTRA_SHARE_LINK, shareLink);
    intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
    ctx.startActivity(intent);
  }
}
