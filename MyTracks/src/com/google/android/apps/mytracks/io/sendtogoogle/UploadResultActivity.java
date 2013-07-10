/*
 * Copyright 2012 Google Inc.
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

import com.google.android.apps.mytracks.content.MyTracksProviderUtils;
import com.google.android.apps.mytracks.content.Track;
import com.google.android.apps.mytracks.fragments.ChooseActivityDialogFragment;
import com.google.android.apps.mytracks.fragments.ChooseActivityDialogFragment.ChooseActivityCaller;
import com.google.android.apps.mytracks.util.IntentUtils;
import com.google.android.maps.mytracks.R;
import com.google.common.annotations.VisibleForTesting;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;

/**
 * A dialog to show the result of uploading to Google services.
 * 
 * @author Jimmy Shih
 */
public class UploadResultActivity extends FragmentActivity implements ChooseActivityCaller {

  private static final String TAG = UploadResultActivity.class.getSimpleName();
  @VisibleForTesting
  static final int DIALOG_RESULT_ID = 0;
  @VisibleForTesting
  protected View view;

  private SendRequest sendRequest;
  private String shareUrl;
  private Dialog resultDialog;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    sendRequest = getIntent().getParcelableExtra(SendRequest.SEND_REQUEST_KEY);
    shareUrl = sendRequest.getShareUrl();

    Track track = MyTracksProviderUtils.Factory.get(this).getTrack(sendRequest.getTrackId());
    if (track == null) {
      Log.d(TAG, "No track for " + sendRequest.getTrackId());
      finish();
      return;
    }

    if (sendRequest.isDriveSuccess() && shareUrl != null) {
      new ChooseActivityDialogFragment().show(
          getSupportFragmentManager(), ChooseActivityDialogFragment.CHOOSE_ACTIVITY_DIALOG_TAG);
      return;
    }
    showDialog(DIALOG_RESULT_ID);
  }

  @Override
  protected Dialog onCreateDialog(int id) {
    if (id != DIALOG_RESULT_ID) {
      return null;
    }
    int serviceName;
    int serviceUrl;
    boolean success;
    if (sendRequest.isSendDrive()) {
      serviceName = R.string.export_google_drive;
      serviceUrl = R.string.export_google_drive_url;
      success = sendRequest.isDriveSuccess();
    } else if (sendRequest.isSendMaps()) {
      serviceName = R.string.export_google_maps;
      serviceUrl = R.string.export_google_maps_url;
      success = sendRequest.isMapsSuccess();
    } else if (sendRequest.isSendFusionTables()) {
      serviceName = R.string.export_google_fusion_tables;
      serviceUrl = R.string.export_google_fusion_tables_url;
      success = sendRequest.isFusionTablesSuccess();
    } else {
      serviceName = R.string.export_google_spreadsheets;
      serviceUrl = R.string.export_google_spreadsheets_url;
      success = sendRequest.isSpreadsheetsSuccess();
    }

    String message = getString(
        success ? R.string.export_google_success : R.string.export_google_error,
        getString(serviceName), getString(serviceUrl));
    AlertDialog.Builder builder = new AlertDialog.Builder(this).setCancelable(true)
        .setIcon(success ? R.drawable.ic_dialog_success : android.R.drawable.ic_dialog_alert)
        .setMessage(message)
        .setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
          public void onCancel(DialogInterface dialog) {
            finish();
          }
        })
        .setPositiveButton(R.string.generic_ok, new DialogInterface.OnClickListener() {
            @Override
          public void onClick(DialogInterface dialog, int which) {
            finish();
          }
        })
        .setTitle(success ? R.string.generic_success_title : R.string.generic_error_title); 

    // Add a Share URL button if shareUrl exists
    if (success && shareUrl != null) {
      builder.setNegativeButton(
          R.string.share_track_share_url, new DialogInterface.OnClickListener() {
              @Override
            public void onClick(DialogInterface dialog, int which) {
              new ChooseActivityDialogFragment().show(getSupportFragmentManager(),
                  ChooseActivityDialogFragment.CHOOSE_ACTIVITY_DIALOG_TAG);
            }
          });
    }
    resultDialog = builder.create();
    return resultDialog;
  }

  @Override
  public void onChooseActivityDone(String packageName, String className) {
    if (packageName != null && className != null) {
      Intent intent = IntentUtils.newShareUrlIntent(
          this, sendRequest.getTrackId(), shareUrl, packageName, className);
      startActivity(intent);
    }
    finish();
  }
}
