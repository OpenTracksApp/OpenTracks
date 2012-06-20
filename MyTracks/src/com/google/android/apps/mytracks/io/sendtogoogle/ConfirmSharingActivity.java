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

package com.google.android.apps.mytracks.io.sendtogoogle;

import com.google.android.apps.mytracks.util.DialogUtils;
import com.google.android.apps.mytracks.util.IntentUtils;
import com.google.android.apps.mytracks.util.PreferencesUtils;
import com.google.android.maps.mytracks.R;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

/**
 * An interstitial to confirm sharing with friends.
 * 
 * @author Jimmy Shih
 */
public class ConfirmSharingActivity extends Activity {

  private static final int DIALOG_ID = 0;

  private SendRequest sendRequest;
  private CheckBox checkBox;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    sendRequest = getIntent().getParcelableExtra(SendRequest.SEND_REQUEST_KEY);
    if (PreferencesUtils.getBoolean(this, R.string.show_confirm_sharing_dialog_key,
        PreferencesUtils.SHOW_CONFIRM_SHARING_DIALOG_DEFAULT)) {
      showDialog(DIALOG_ID);
    } else {
      startNextActivity();
    }
  }

  @Override
  protected Dialog onCreateDialog(int id) {
    if (id != DIALOG_ID) {
      return null;
    }
    View view = getLayoutInflater().inflate(R.layout.confirm_sharing, null);
    
    TextView textView = (TextView) view.findViewById(R.id.confirm_sharing_text_view);
    textView.setText(getString(
        R.string.share_track_confirm_message, getString(R.string.maps_public_unlisted_url)));
    
    checkBox = (CheckBox) view.findViewById(R.id.confirm_sharing_check_box);
    DialogInterface.OnClickListener okListener = new DialogInterface.OnClickListener() {
        @Override
      public void onClick(DialogInterface dialog, int button) {
        PreferencesUtils.setBoolean(ConfirmSharingActivity.this,
            R.string.show_confirm_sharing_dialog_key, !checkBox.isChecked());
        startNextActivity();
      }
    };
    DialogInterface.OnClickListener cancelListener = new DialogInterface.OnClickListener() {
        @Override
      public void onClick(DialogInterface dialog, int button) {
        finish();
      }
    };
  
    Dialog dialog = DialogUtils.createConfirmationDialog(
        this, -1, view, okListener, cancelListener);
    dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
        @Override
      public void onCancel(DialogInterface dialogInterface) {
        finish();
      }
    });
    return dialog;
  }

  /**
   * Starts the next activity for sharing with friends and finishes this
   * activity.
   */
  private void startNextActivity() {
    Intent intent = IntentUtils.newIntent(this, AccountChooserActivity.class)
        .putExtra(SendRequest.SEND_REQUEST_KEY, sendRequest);
    startActivity(intent);
    finish();
  }
}