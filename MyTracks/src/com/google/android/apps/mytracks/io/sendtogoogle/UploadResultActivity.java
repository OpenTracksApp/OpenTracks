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
import com.google.android.apps.mytracks.io.fusiontables.SendFusionTablesUtils;
import com.google.android.apps.mytracks.io.maps.SendMapsUtils;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * A dialog to show the result of uploading to Google services.
 * 
 * @author Jimmy Shih
 */
public class UploadResultActivity extends FragmentActivity {

  private static final String TAG = UploadResultActivity.class.getSimpleName();
  private static final int DIALOG_RESULT_ID = 0;

  private SendRequest sendRequest;
  private String shareUrl;
  private Dialog resultDialog;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    sendRequest = getIntent().getParcelableExtra(SendRequest.SEND_REQUEST_KEY);
    shareUrl = null;

    Track track = MyTracksProviderUtils.Factory.get(this).getTrack(sendRequest.getTrackId());
    if (track == null) {
      Log.d(TAG, "No track for " + sendRequest.getTrackId());
      finish();
      return;
    }
    
    if (sendRequest.isSendMaps() && sendRequest.isMapsSuccess()) {
      shareUrl = SendMapsUtils.getMapUrl(track);
      if (sendRequest.getSharingAppPackageName() != null) {
        Intent intent = IntentUtils.newShareUrlIntent(this, sendRequest.getTrackId(), shareUrl,
            sendRequest.getSharingAppPackageName(), sendRequest.getSharingAppClassName());
        startActivity(intent);
        finish();
        return;
      }
    }
    if (shareUrl == null && sendRequest.isSendFusionTables()
        && sendRequest.isFusionTablesSuccess()) {
      shareUrl = SendFusionTablesUtils.getMapUrl(track);
    }
    showDialog(DIALOG_RESULT_ID);
  }

  @Override
  protected Dialog onCreateDialog(int id) {
    if (id != DIALOG_RESULT_ID) {
      return null;
    }
    View view = getLayoutInflater().inflate(R.layout.upload_result, null);

    LinearLayout mapsResult = (LinearLayout) view.findViewById(R.id.upload_result_maps_result);
    LinearLayout fusionTablesResult = (LinearLayout) view.findViewById(
        R.id.upload_result_fusion_tables_result);
    LinearLayout docsResult = (LinearLayout) view.findViewById(R.id.upload_result_docs_result);

    ImageView mapsResultIcon = (ImageView) view.findViewById(R.id.upload_result_maps_result_icon);
    ImageView fusionTablesResultIcon = (ImageView) view.findViewById(
        R.id.upload_result_fusion_tables_result_icon);
    ImageView docsResultIcon = (ImageView) view.findViewById(R.id.upload_result_docs_result_icon);

    TextView successFooter = (TextView) view.findViewById(R.id.upload_result_success_footer);
    TextView errorFooter = (TextView) view.findViewById(R.id.upload_result_error_footer);

    boolean hasError = false;
    if (!sendRequest.isSendMaps()) {
      mapsResult.setVisibility(View.GONE);
    } else {
      if (!sendRequest.isMapsSuccess()) {
        mapsResultIcon.setImageResource(R.drawable.failure);
        mapsResultIcon.setContentDescription(getString(R.string.generic_error_title));
        hasError = true;
      }
    }

    if (!sendRequest.isSendFusionTables()) {
      fusionTablesResult.setVisibility(View.GONE);
    } else {
      if (!sendRequest.isFusionTablesSuccess()) {
        fusionTablesResultIcon.setImageResource(R.drawable.failure);
        fusionTablesResultIcon.setContentDescription(getString(R.string.generic_error_title));
        hasError = true;
      }
    }

    if (!sendRequest.isSendDocs()) {
      docsResult.setVisibility(View.GONE);
    } else {
      if (!sendRequest.isDocsSuccess()) {
        docsResultIcon.setImageResource(R.drawable.failure);
        docsResultIcon.setContentDescription(getString(R.string.generic_error_title));
        hasError = true;
      }
    }

    if (hasError) {
      successFooter.setVisibility(View.GONE);
    } else {
      errorFooter.setVisibility(View.GONE);
    }

    AlertDialog.Builder builder = new AlertDialog.Builder(this)
        .setCancelable(true)
        .setIcon(hasError ? android.R.drawable.ic_dialog_alert : android.R.drawable.ic_dialog_info)
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
        .setTitle(hasError ? R.string.generic_error_title : R.string.generic_success_title)
        .setView(view);

    // Add a Share URL button if shareUrl exists
    if (shareUrl != null) {
      builder.setNegativeButton(
          R.string.share_track_share_url, new DialogInterface.OnClickListener() {
              @Override
            public void onClick(DialogInterface dialog, int which) {
              ChooseActivityDialogFragment.newInstance(sendRequest.getTrackId(), shareUrl).show(
                  getSupportFragmentManager(),
                  ChooseActivityDialogFragment.CHOOSE_ACTIVITY_DIALOG_TAG);
            }
          });
    }
    resultDialog = builder.create();
    return resultDialog;
  }

  @VisibleForTesting
  Dialog getDialog() {
    return resultDialog;
  }
}
