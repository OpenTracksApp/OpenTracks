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

import com.google.android.apps.mytracks.Constants;
import com.google.android.apps.mytracks.content.MyTracksProviderUtils;
import com.google.android.apps.mytracks.content.Track;
import com.google.android.apps.mytracks.io.fusiontables.SendFusionTablesUtils;
import com.google.android.apps.mytracks.io.maps.SendMapsUtils;
import com.google.android.maps.mytracks.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * A dialog to show the result of uploading to Google services.
 *
 * @author Jimmy Shih
 */
public class UploadResultActivity extends Activity {

  private static final String TEXT_PLAIN_TYPE = "text/plain";
  private static final int RESULT_DIALOG = 1;

  private SendRequest sendRequest;
  private Track track;
  private String shareUrl;
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    sendRequest = getIntent().getParcelableExtra(SendRequest.SEND_REQUEST_KEY);
    track = null;
    shareUrl = null;
    
    if (sendRequest.isSendMaps() && sendRequest.isMapsSuccess()) {
      shareUrl = SendMapsUtils.getMapUrl(getTrack());
    }
    if (shareUrl == null && sendRequest.isSendFusionTables()
        && sendRequest.isFusionTablesSuccess()) {
      shareUrl = SendFusionTablesUtils.getMapUrl(getTrack());
    }
  }

  private Track getTrack() {
    if (track == null) {
      track = MyTracksProviderUtils.Factory.get(this).getTrack(sendRequest.getTrackId());
    }
    return track;
  }

  @Override
  protected void onResume() {
    super.onResume();
    showDialog(RESULT_DIALOG);
  }

  @Override
  protected Dialog onCreateDialog(int id) {
    switch (id) {
      case RESULT_DIALOG:
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.upload_result, null);
        builder.setView(view);

        LinearLayout mapsResult = (LinearLayout) view.findViewById(R.id.upload_result_maps_result);
        LinearLayout fusionTablesResult = (LinearLayout) view.findViewById(
            R.id.upload_result_fusion_tables_result);
        LinearLayout docsResult = (LinearLayout) view.findViewById(R.id.upload_result_docs_result);

        ImageView mapsResultIcon = (ImageView) view.findViewById(
            R.id.upload_result_maps_result_icon);
        ImageView fusionTablesResultIcon = (ImageView) view.findViewById(
            R.id.upload_result_fusion_tables_result_icon);
        ImageView docsResultIcon = (ImageView) view.findViewById(
            R.id.upload_result_docs_result_icon);

        TextView successFooter = (TextView) view.findViewById(R.id.upload_result_success_footer);
        TextView errorFooter = (TextView) view.findViewById(R.id.upload_result_error_footer);

        boolean hasError = false;
        if (!sendRequest.isSendMaps()) {
          mapsResult.setVisibility(View.GONE);
        } else {
          if (!sendRequest.isMapsSuccess()) {
            mapsResultIcon.setImageResource(R.drawable.failure);
            hasError = true;
          }
        }

        if (!sendRequest.isSendFusionTables()) {
          fusionTablesResult.setVisibility(View.GONE);
        } else {
          if (!sendRequest.isFusionTablesSuccess()) {
            fusionTablesResultIcon.setImageResource(R.drawable.failure);
            hasError = true;
          }
        }

        if (!sendRequest.isSendDocs()) {
          docsResult.setVisibility(View.GONE);
        } else {
          if (!sendRequest.isDocsSuccess()) {
            docsResultIcon.setImageResource(R.drawable.failure);
            hasError = true;
          }
        }

        if (hasError) {
          builder.setTitle(R.string.generic_error_title);
          builder.setIcon(android.R.drawable.ic_dialog_alert);
          successFooter.setVisibility(View.GONE);
        } else {
          builder.setTitle(R.string.generic_success_title);
          builder.setIcon(android.R.drawable.ic_dialog_info);
          errorFooter.setVisibility(View.GONE);
        }

        builder.setCancelable(true);
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
          @Override
          public void onCancel(DialogInterface dialog) {
            finish();
          }
        });
        builder.setPositiveButton(
            getString(R.string.generic_ok), new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialog, int which) {
                if (!sendRequest.isShowAll() && shareUrl != null) {
                  startShareUrlActivity(shareUrl);
                }
                finish();
              }
            });

        // Add a Share URL button if showing all the options and a shareUrl
        // exists
        if (sendRequest.isShowAll() && shareUrl != null) {
          builder.setNegativeButton(getString(R.string.send_google_result_share_url),
              new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                  startShareUrlActivity(shareUrl);
                  finish();
                }
              });
        }
        return builder.create();
      default:
        return null;
    }
  }

  /**
   * Starts an activity to share the url.
   *
   * @param url the url
   */
  private void startShareUrlActivity(String url) {
    SharedPreferences prefs = getSharedPreferences(Constants.SETTINGS_NAME, Context.MODE_PRIVATE);
    boolean shareUrlOnly = prefs.getBoolean(getString(R.string.share_url_only_key), false);

    Intent intent = new Intent(Intent.ACTION_SEND);
    intent.setType(TEXT_PLAIN_TYPE);
    intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_track_subject));
    intent.putExtra(Intent.EXTRA_TEXT,
        shareUrlOnly ? url : getString(R.string.share_track_url_body_format, url));
    startActivity(Intent.createChooser(intent, getString(R.string.share_track_picker_title)));
  }
}
