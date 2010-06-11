/*
 * Copyright 2008 Google Inc.
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

import com.google.android.apps.mytracks.content.MyTracksProviderUtils;
import com.google.android.maps.mytracks.R;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * A utility class that can be used to delete all tracks and track points
 * from the provider, including asking for confirmation from the user via
 * a dialog.
 *
 * @author Leif Hendrik Wilden
 */
public class MyTracksDeleteAllTracks extends Handler {

  private final Context context;
  private final Runnable done;

  public MyTracksDeleteAllTracks(Context context, Runnable done) {
    this.context = context;
    this.done = done;
  }

  @Override
  public void handleMessage(Message msg) {
    super.handleMessage(msg);

    AlertDialog dialog = null;
    AlertDialog.Builder builder = new AlertDialog.Builder(context);
    builder.setMessage(
        context.getString(R.string.all_data_will_be_permanently_deleted));
    builder.setTitle(context.getString(R.string.are_you_sure_question));
    builder.setIcon(android.R.drawable.ic_dialog_alert);
    builder.setPositiveButton(context.getString(R.string.yes),
        new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int i) {
            dialog.dismiss();
            Log.w(MyTracksConstants.TAG, "deleting all!");
            MyTracksProviderUtils.Factory.get(context).deleteAllTracks();
            SharedPreferences prefs =
                context.getSharedPreferences(MyTracksSettings.SETTINGS_NAME, 0);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putLong(MyTracksSettings.SELECTED_TRACK, -1);
            editor.commit();
            if (done != null) {
              Handler h = new Handler();
              h.post(done);
            }
          }
        });
    builder.setNegativeButton(context.getString(R.string.no),
        new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int i) {
            dialog.dismiss();
          }
        });
    dialog = builder.create();
    dialog.show();
  }
}
