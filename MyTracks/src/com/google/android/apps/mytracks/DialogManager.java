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

import static com.google.android.apps.mytracks.Constants.TAG;

import com.google.android.apps.mytracks.io.sendtogoogle.SendDialog;
import com.google.android.maps.mytracks.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager.BadTokenException;

/**
 * A class to handle all dialog related events for My Tracks.
 *
 * @author Sandor Dornbush
 */
public class DialogManager {

  public static final int DIALOG_CHART_SETTINGS = 1;
  public static final int DIALOG_IMPORT_PROGRESS = 2;
  public static final int DIALOG_PROGRESS = 3;
  public static final int DIALOG_SEND_TO_GOOGLE = 4;
  public static final int DIALOG_WRITE_PROGRESS = 5;

  private ProgressDialog progressDialog;
  private ProgressDialog importProgressDialog;
  private ProgressDialog writeProgressDialog;
  private SendDialog sendToGoogleDialog;
  private ChartSettingsDialog chartSettingsDialog;

  private MyTracks activity;

  public DialogManager(MyTracks activity) {
    this.activity = activity;
  }

  protected Dialog onCreateDialog(int id, Bundle args) {
    switch (id) {
      case DIALOG_CHART_SETTINGS:
        chartSettingsDialog = new ChartSettingsDialog(activity);
        return chartSettingsDialog;
      case DIALOG_IMPORT_PROGRESS:
        importProgressDialog = new ProgressDialog(activity);
        importProgressDialog.setIcon(android.R.drawable.ic_dialog_info);
        importProgressDialog.setTitle(
            activity.getString(R.string.progress_title));
        importProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        importProgressDialog.setMessage(
            activity.getString(R.string.import_progress_message));
        return importProgressDialog;
      case DIALOG_PROGRESS:
        progressDialog = new ProgressDialog(activity);
        progressDialog.setIcon(android.R.drawable.ic_dialog_info);
        progressDialog.setTitle(activity.getString(R.string.progress_title));
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setMessage("");
        progressDialog.setMax(100);
        progressDialog.setProgress(10);
        return progressDialog;
      case DIALOG_SEND_TO_GOOGLE:
        sendToGoogleDialog = new SendDialog(activity);
        return sendToGoogleDialog;
      case DIALOG_WRITE_PROGRESS:
        writeProgressDialog = new ProgressDialog(activity);
        writeProgressDialog.setIcon(android.R.drawable.ic_dialog_info);
        writeProgressDialog.setTitle(
            activity.getString(R.string.progress_title));
        writeProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        writeProgressDialog.setMessage(
            activity.getString(R.string.write_progress_message));
        return writeProgressDialog;
    }
    return null;
  }

  protected void onPrepareDialog(int id, Dialog dialog) {
    switch (id) {
      case DIALOG_SEND_TO_GOOGLE:
        activity.resetSendToGoogleStatus();
        break;
      case DIALOG_CHART_SETTINGS:
        Log.d(TAG, "MyTracks.onPrepare chart dialog");
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
  public SendDialog getSendToGoogleDialog() {
    return sendToGoogleDialog;
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
    activity.runOnUiThread(new Runnable() {
      public void run() {
        AlertDialog dialog = null;
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setMessage(activity.getString(message));
        builder.setNegativeButton(activity.getString(R.string.ok), null);
        builder.setIcon(success ? android.R.drawable.ic_dialog_info :
          android.R.drawable.ic_dialog_alert);
        builder.setTitle(success ? R.string.success : R.string.error);
        dialog = builder.create();
        dialog.show();
      }
    });
  }

  /**
   * Just like showDialog, but will catch a {@link BadTokenException} that
   * sometimes (very rarely) gets thrown. This might happen if the user hits
   * the "back" button immediately after sending tracks to google.
   *
   * @param id the dialog id
   */
  public void showDialogSafely(final int id) {
    activity.runOnUiThread(new Runnable() {
      public void run() {
        try {
          activity.showDialog(id);
        } catch (BadTokenException e) {
          Log.w(TAG, "Could not display dialog with id " + id, e);
        } catch (IllegalStateException e) {
          Log.w(TAG, "Could not display dialog with id " + id, e);
        }
      }
    });
  }

  /**
   * The equivalent of {@link #showDialogSafely(int)}, but for a specific
   * dialog instance.
   */
  public static void showDialogSafely(Activity activity, final Dialog dialog) {
    activity.runOnUiThread(new Runnable() {
      public void run() {
        try {
          dialog.show();
        } catch (BadTokenException e) {
          Log.w(TAG, "Could not display dialog", e);
        } catch (IllegalStateException e) {
          Log.w(TAG, "Could not display dialog", e);
        }
      }
    });
  }

  /**
   * Dismisses the progress dialog if it is showing. Executed on the UI thread.
   */
  public void dismissDialogSafely(final int id) {
    activity.runOnUiThread(new Runnable() {
      public void run() {
        try {
          activity.dismissDialog(id);
        } catch (IllegalArgumentException e) {
          // This will be thrown if this dialog was not shown before.
        }
      }
    });
  }

}
