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

import android.app.Activity;
import android.app.Dialog;
import android.util.Log;
import android.view.WindowManager.BadTokenException;

/**
 * A class to handle all dialog related events for My Tracks.
 *
 * @author Sandor Dornbush
 */
public class DialogManager {

  /**
   * The equivalent of {@link Dialog#show()}, but for a specific dialog
   * instance.
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
   * The equivalent of {@link Dialog#dismiss()}, but for a specific dialog
   * instance.
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
