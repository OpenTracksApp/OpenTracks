/*
 * Copyright 2011 Google Inc.
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

import com.google.android.maps.mytracks.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.view.View;
import android.widget.ListView;

import java.util.List;

/**
 * Creates the dialog used to display the results of sending track data to
 * Google.  The dialog lists the services to which data was uploaded, along
 * with an indicator of success/failure.  If possible, a button is displayed
 * offering to share the uploaded track.
 *
 * Implementation note: This class is a factory, rather than a {@link Dialog}
 * subclass, because alert dialogs have to be created with
 * {@link AlertDialog.Builder}.  Attempts to subclass {@link AlertDialog}
 * directly have been unsuccessful, as {@link AlertDialog.Builder} uses a
 * private subclass to implement most of the interesting behavior.
 *
 * @author Matthew Simmons
 */
public class ResultDialogFactory {
  private ResultDialogFactory() {}

  /**
   * Create a send-to-Google result dialog.  The caller is responsible for
   * showing it.
   *
   * @param activity the activity associated with the dialog
   * @param results the results to be displayed in the dialog
   * @param onOkClickListener the listener to invoke if the OK button is
   *     clicked
   * @param onShareClickListener the listener to invoke if the Share button is
   *     clicked.  If no share listener is provided, the Share button will not
   *     be displayed.
   * @return the created dialog
   */
  public static AlertDialog makeDialog(Activity activity, List<SendResult> results,
      DialogInterface.OnClickListener onOkClickListener,
      DialogInterface.OnClickListener onShareClickListener,
      DialogInterface.OnCancelListener onCancelListener) {
    boolean success = true;
    for (SendResult result : results) {
      if (!result.isSuccess()) {
        success = false;
        break;
      }
    }

    AlertDialog.Builder builder = new AlertDialog.Builder(activity)
        .setView(makeDialogContent(activity, results, success));

    if (success) {
      builder.setTitle(R.string.generic_success_title);
      builder.setIcon(android.R.drawable.ic_dialog_info);
    } else {
      builder.setTitle(R.string.generic_error_title);
      builder.setIcon(android.R.drawable.ic_dialog_alert);
    }

    builder.setPositiveButton(activity.getString(R.string.generic_ok), onOkClickListener);
    if (onShareClickListener != null) {
      builder.setNegativeButton(activity.getString(R.string.send_google_result_share_url),
          onShareClickListener);
    }

    builder.setOnCancelListener(onCancelListener);

    return builder.create();
  }

  private static View makeDialogContent(Activity activity, List<SendResult> results,
      boolean success) {
    ResultListAdapter resultListAdapter = new ResultListAdapter(activity,
        R.layout.send_to_google_result_list_item, results);

    View content = activity.getLayoutInflater().inflate(R.layout.send_to_google_result, null);
    ListView resultList = (ListView) content.findViewById(R.id.send_to_google_result_list);
    resultList.setAdapter(resultListAdapter);

    content.findViewById(R.id.send_to_google_result_comment)
        .setVisibility(success ? View.VISIBLE : View.GONE);
    content.findViewById(R.id.send_to_google_result_error)
        .setVisibility(success ? View.GONE : View.VISIBLE);

    return content;
  }
}
