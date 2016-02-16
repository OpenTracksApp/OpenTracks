/*
 * Copyright 2013 Google Inc.
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

import com.google.android.apps.mytracks.util.DialogUtils;
import com.google.android.maps.mytracks.R;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

/**
 * An activity for delete tracks.
 * 
 * @author Jimmy Shih
 */
public class DeleteActivity extends Activity {

  public static final String EXTRA_TRACK_IDS = "track_ids";

  private static final int DIALOG_PROGRESS_ID = 0;

  private DeleteAsyncTask deleteAsyncTask;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    
    setResult(RESULT_CANCELED);
    
    Intent intent = getIntent();
    long[] trackIds = intent.getLongArrayExtra(EXTRA_TRACK_IDS);

    Object retained = getLastNonConfigurationInstance();
    if (retained instanceof DeleteAsyncTask) {
      deleteAsyncTask = (DeleteAsyncTask) retained;
      deleteAsyncTask.setActivity(this);
    } else {
      deleteAsyncTask = new DeleteAsyncTask(this, trackIds);
      deleteAsyncTask.execute();
    }
  }

  @Override
  public Object onRetainNonConfigurationInstance() {
    deleteAsyncTask.setActivity(null);
    return deleteAsyncTask;
  }

  @Override
  protected Dialog onCreateDialog(int id) {
    if (id != DIALOG_PROGRESS_ID) {
      return null;
    }
    return DialogUtils.createSpinnerProgressDialog(
        this, R.string.track_delete_progress_message, new DialogInterface.OnCancelListener() {
            @Override
          public void onCancel(DialogInterface dialog) {
            deleteAsyncTask.cancel(true);
            dialog.dismiss();
            finish();
          }
        });
  }

  /**
   * Invokes when the associated AsyncTask completes.
   */
  public void onAsyncTaskCompleted() {
    removeDialog(DIALOG_PROGRESS_ID);
    setResult(RESULT_OK);
    finish();
  }

  /**
   * Shows the progress dialog.
   */
  public void showProgressDialog() {
    showDialog(DIALOG_PROGRESS_ID);
  }
}
