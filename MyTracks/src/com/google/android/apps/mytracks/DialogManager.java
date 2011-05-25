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

import com.google.android.maps.mytracks.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.view.WindowManager.BadTokenException;

/**
 * A class to handle all dialog related events for My Tracks.
 *
 * @author Sandor Dornbush
 */
public class DialogManager {

  private MyTracks activity;

  public DialogManager(MyTracks activity) {
    this.activity = activity;
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
        showMessageDialog(activity, message, success, null);
      }
    });
  }

  public static void showMessageDialog(
      Context ctx, int message, boolean success, DialogInterface.OnClickListener okListener) {
    AlertDialog dialog = null;
    AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
    builder.setMessage(message);
    builder.setNeutralButton(R.string.ok, okListener);
    builder.setIcon(success ? android.R.drawable.ic_dialog_info :
        android.R.drawable.ic_dialog_alert);
    builder.setTitle(success ? R.string.success : R.string.error);
    dialog = builder.create();
    dialog.show();
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

  /**
   * The equivalent of {@link #dismissDialogSafely(int)}, but for a specific
   * dialog instance.
   */
  public static void dismissDialogSafely(Activity activity, final Dialog dialog) {
    activity.runOnUiThread(new Runnable() {
      public void run() {
        try {
          dialog.dismiss();
        } catch (IllegalArgumentException e) {
          // This will be thrown if this dialog was not shown before.
        }
      }
    });
  }
}
