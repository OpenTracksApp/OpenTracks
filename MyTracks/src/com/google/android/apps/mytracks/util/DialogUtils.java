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
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;

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
   * @param message the confirmation message
   * @param onClickListener the listener to invoke when the user clicks OK
   */
  public static Dialog createConfirmationDialog(
      Context context, String message, DialogInterface.OnClickListener onClickListener) {
    return new AlertDialog.Builder(context)
        .setCancelable(true)
        .setIcon(android.R.drawable.ic_dialog_alert)
        .setMessage(message)
        .setNegativeButton(android.R.string.cancel, null)
        .setPositiveButton(android.R.string.ok, onClickListener)
        .setTitle(R.string.generic_confirm_title)
        .create();
  }

  /**
   * Creates a spinner progress dialog.
   *
   * @param context the context
   * @param message the progress message
   * @param onCancelListener the listener to invoke when the user cancels
   */
  public static ProgressDialog createSpinnerProgressDialog(
      Context context, String message, DialogInterface.OnCancelListener onCancelListener) {
    ProgressDialog progressDialog = new ProgressDialog(context);
    progressDialog.setCancelable(true);
    progressDialog.setIcon(android.R.drawable.ic_dialog_info);
    progressDialog.setIndeterminate(true);
    progressDialog.setMessage(message);
    progressDialog.setOnCancelListener(onCancelListener);
    progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
    progressDialog.setTitle(R.string.generic_progress_title);
    return progressDialog;
  }
  
  /**
   * Creates a horizontal progress dialog.
   *
   * @param context the context
   * @param message the progress message
   * @param onCancelListener the listener to invoke when the user cancels
   */
  public static ProgressDialog createHorizontalProgressDialog(
      Context context, String message, DialogInterface.OnCancelListener onCancelListener) {
    ProgressDialog progressDialog = new ProgressDialog(context);
    progressDialog.setCancelable(true);
    progressDialog.setIcon(android.R.drawable.ic_dialog_info);
    progressDialog.setIndeterminate(true);
    progressDialog.setMessage(message);
    progressDialog.setOnCancelListener(onCancelListener);
    progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
    progressDialog.setTitle(R.string.generic_progress_title);   
    return progressDialog;
  }
}
