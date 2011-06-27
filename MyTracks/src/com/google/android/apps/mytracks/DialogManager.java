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
import android.content.DialogInterface;
import android.util.Log;
import android.view.WindowManager.BadTokenException;

/**
 * A class to handle all dialog related events for My Tracks.
 *
 * @author Sandor Dornbush
 */
public class DialogManager {

  public static void showMessageDialog(
      Activity ctx, int message, boolean success, DialogInterface.OnClickListener okListener) {
    if (ctx.isFinishing()) {
      Log.w(TAG, "Activity finishing - not showing dialog");
      return;
    }

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
   * The equivalent of {@link #showDialogSafely(int)}, but for a specific
   * dialog instance.
   */
  public static void showDialogSafely(Activity activity, final Dialog dialog) {
    if (activity.isFinishing()) {
      Log.w(TAG, "Activity finishing - not showing dialog");
      return;
    }

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
   * The equivalent of {@link #dismissDialogSafely(int)}, but for a specific
   * dialog instance.
   */
  public static void dismissDialogSafely(Activity activity, final Dialog dialog) {
    if (activity.isFinishing()) {
      Log.w(TAG, "Activity finishing - not dismissing dialog");
      return;
    }

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

  private DialogManager() {}
}
