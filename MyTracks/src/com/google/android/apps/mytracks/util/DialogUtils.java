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

package com.google.android.apps.mytracks.util;

import com.google.android.maps.mytracks.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface.OnClickListener;

/**
 * Utilities for creating dialogs.
 * 
 * @author Jimmy Shih
 */
public class DialogUtils {

  private DialogUtils() {}
  
  /**
   * Creates a confirmation dialog.
   * 
   * @param context the context
   * @param messageId the id of the confirmation message
   * @param onClickListener the listener to invoke when the users clicks OK
   */
  public static Dialog createConfirmationDialog(
      Context context, int messageId, OnClickListener onClickListener) {
    return new AlertDialog.Builder(context)
        .setCancelable(true)
        .setIcon(android.R.drawable.ic_dialog_alert)
        .setMessage(messageId)
        .setNegativeButton(android.R.string.cancel, null)
        .setPositiveButton(android.R.string.ok, onClickListener)
        .setTitle(R.string.generic_confirm_title)
        .create();
  }
}
