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
import android.view.View;

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
   * @param messageId the message id
   * @param okListener the listener when OK is clicked
   */
  public static Dialog createConfirmationDialog(
      Context context, int messageId, DialogInterface.OnClickListener okListener) {
    return createConfirmationDialog(
        context, R.string.generic_confirm_title, messageId, null, okListener, null);
  }

  /**
   * Creates a confirmation dialog.
   * 
   * @param context the context
   * @param titleId the titleId
   * @param messageId the messageId
   * @param view the view
   * @param okListener the listener when OK is clicked
   * @param cancelListener the listener when cancel is clicked
   */
  public static Dialog createConfirmationDialog(Context context, int titleId, int messageId,
      View view, DialogInterface.OnClickListener okListener,
      DialogInterface.OnClickListener cancelListener) {
    AlertDialog.Builder builder = new AlertDialog.Builder(context).setCancelable(true)
        .setIcon(android.R.drawable.ic_dialog_alert)
        .setNegativeButton(R.string.generic_no, cancelListener)
        .setPositiveButton(R.string.generic_yes, okListener).setTitle(titleId);
    if (messageId != -1) {
      builder.setMessage(messageId);
    }
    if (view != null) {
      builder.setView(view);
    }

    return builder.create();
  }
  
  /**
   * Creates a spinner progress dialog.
   *
   * @param context the context
   * @param messageId the progress message id
   * @param onCancelListener the cancel listener
   */
  public static ProgressDialog createSpinnerProgressDialog(
      Context context, int messageId, DialogInterface.OnCancelListener onCancelListener) {
    return createProgressDialog(true, context, messageId, onCancelListener);
  }

  /**
   * Creates a horizontal progress dialog.
   *
   * @param context the context
   * @param messageId the progress message id
   * @param onCancelListener the cancel listener
   * @param formatArgs the format arguments for the messageId
   */
  public static ProgressDialog createHorizontalProgressDialog(Context context, int messageId,
      DialogInterface.OnCancelListener onCancelListener, Object... formatArgs) {
    return createProgressDialog(false, context, messageId, onCancelListener, formatArgs);
  }

  /**
   * Creates a progress dialog.
   *
   * @param spinner true to use the spinner style
   * @param context the context
   * @param messageId the progress message id
   * @param onCancelListener the cancel listener
   * @param formatArgs the format arguments for the message id
   */
  private static ProgressDialog createProgressDialog(boolean spinner, Context context,
      int messageId, DialogInterface.OnCancelListener onCancelListener, Object... formatArgs) {
    ProgressDialog progressDialog = new ProgressDialog(context, R.style.MyTracks);
    progressDialog.setCancelable(true);
    progressDialog.setCanceledOnTouchOutside(false);
    progressDialog.setIcon(android.R.drawable.ic_dialog_info);
    progressDialog.setIndeterminate(true);
    progressDialog.setMessage(context.getString(messageId, formatArgs));
    progressDialog.setOnCancelListener(onCancelListener);
    progressDialog.setProgressStyle(spinner ? ProgressDialog.STYLE_SPINNER
        : ProgressDialog.STYLE_HORIZONTAL);
    progressDialog.setTitle(R.string.generic_progress_title);
    return progressDialog;
  }
}
