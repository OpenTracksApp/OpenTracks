/*
 * Copyright 2010 Google Inc.
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

import com.google.android.maps.mytracks.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

/**
 * A class to handle a dialog related events for My Tracks.
 * 
 * @author Sandor Dornbush
 */
public class DialogManager {

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

  private MyTracks activity;

  public DialogManager(MyTracks activity) {
    this.activity = activity;
  }
  
  protected Dialog onCreateDialog(int id, Bundle args) {
    switch (id) {
      case DIALOG_PROGRESS:
        progressDialog = new ProgressDialog(activity);
        progressDialog.setIcon(android.R.drawable.ic_dialog_info);
        progressDialog.setTitle(activity.getString(R.string.progress_title));
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setMessage("");
        progressDialog.setMax(100);
        progressDialog.setProgress(10);
        return progressDialog;
      case DIALOG_IMPORT_PROGRESS:
        importProgressDialog = new ProgressDialog(activity);
        importProgressDialog.setIcon(android.R.drawable.ic_dialog_info);
        importProgressDialog.setTitle(
            activity.getString(R.string.progress_title));
        importProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        importProgressDialog.setMessage(
            activity.getString(R.string.import_progress_message));
        return importProgressDialog;
      case DIALOG_WRITE_PROGRESS:
        writeProgressDialog = new ProgressDialog(activity);
        writeProgressDialog.setIcon(android.R.drawable.ic_dialog_info);
        writeProgressDialog.setTitle(
            activity.getString(R.string.progress_title));
        writeProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        writeProgressDialog.setMessage(
            activity.getString(R.string.write_progress_message));
        return writeProgressDialog;
      case DIALOG_SEND_TO_GOOGLE:
        sendToGoogleDialog = new SendToGoogleDialog(activity);
        return sendToGoogleDialog;
      case DIALOG_SEND_TO_GOOGLE_RESULT:
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setIcon(android.R.drawable.ic_dialog_info);
        builder.setTitle("Title");
        builder.setMessage("Message");
        builder.setPositiveButton(activity.getString(R.string.ok), null);
        builder.setNeutralButton(activity.getString(R.string.share_map),
            new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int which) {
            activity.shareLinkToMyMap(activity.getSendToMyMapsMapId());
            dialog.dismiss();
          }
        });
        sendToGoogleResultDialog = builder.create();
        return sendToGoogleResultDialog;
      case DIALOG_CHART_SETTINGS:
        chartSettingsDialog = new ChartSettingsDialog(activity);
        return chartSettingsDialog;
    }
    return null;
  }

  protected void onPrepareDialog(int id, Dialog dialog) {
    switch (id) {
      case DIALOG_SEND_TO_GOOGLE:
        activity.resetSendToGoogleStatus();
        break;
      case DIALOG_SEND_TO_GOOGLE_RESULT:
        boolean success = activity.getSendToGoogleSuccess();
        sendToGoogleResultDialog.setTitle(
            success ? R.string.success : R.string.error);
        sendToGoogleResultDialog.setIcon(success
            ? android.R.drawable.ic_dialog_info
            : android.R.drawable.ic_dialog_alert);
        sendToGoogleResultDialog.setMessage(activity.getMapsResultMessage());

        boolean canShare = activity.getSendToMyMapsMapId() != null;
        View share =
            sendToGoogleResultDialog.findViewById(android.R.id.button3);
        if (share != null) {
          share.setVisibility(canShare ? View.VISIBLE : View.GONE);
        }
        break;
      case DIALOG_CHART_SETTINGS:
        Log.d(MyTracksConstants.TAG, "MyTracks.onPrepare chart dialog");
        chartSettingsDialog.setup(activity.getChartActivity());
        break;
    }
  }

  public void setProgressMessage(final String message) {
    activity.runOnUiThread(new Runnable() {
      public void run() {
        synchronized (this) {
          if (progressDialog != null) {
            progressDialog.setMessage(message);
          }
        }
      }
    });
  }
  
  public void setProgressValue(final int percent) {
    activity.runOnUiThread(new Runnable() {
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
   * @return the sendToGoogleDialog
   */
  public SendToGoogleDialog getSendToGoogleDialog() {
    return sendToGoogleDialog;
  }
}
