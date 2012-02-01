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

import com.google.android.apps.mytracks.Constants;
import com.google.android.apps.mytracks.io.docs.SendDocsActivity;
import com.google.android.apps.mytracks.io.fusiontables.SendFusionTablesActivity;
import com.google.android.apps.mytracks.io.maps.ChooseMapActivity;
import com.google.android.apps.mytracks.io.maps.SendMapsActivity;
import com.google.android.apps.mytracks.util.ApiAdapterFactory;
import com.google.android.maps.mytracks.R;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;

/**
 * A chooser to select an account.
 *
 * @author jshih@google.com (Jimmy Shih)
 */
public class AccountChooserActivity extends Activity {

  private static final int NO_ACCOUNT_DIALOG = 1;
  private static final int CHOOSE_ACCOUNT_DIALOG = 2;

  private SendRequest sendRequest;
  private Account[] accounts;
  private int selectedAccountIndex;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    sendRequest = getIntent().getParcelableExtra(SendRequest.SEND_REQUEST_KEY);
    accounts = AccountManager.get(this).getAccountsByType(Constants.ACCOUNT_TYPE);

    if (accounts.length == 1) {
      startNextActivity(accounts[0]);
      return;
    }

    SharedPreferences prefs = getSharedPreferences(Constants.SETTINGS_NAME, Context.MODE_PRIVATE);
    String preferredAccount = prefs.getString(getString(R.string.preferred_account_key), "");

    selectedAccountIndex = 0;
    for (int i = 0; i < accounts.length; i++) {
      if (accounts[i].name.equals(preferredAccount)) {
        selectedAccountIndex = i;
        break;
      }
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (accounts.length == 0) {
      showDialog(NO_ACCOUNT_DIALOG);
    } else {
      showDialog(CHOOSE_ACCOUNT_DIALOG);
    }
  }

  @Override
  protected Dialog onCreateDialog(int id) {
    AlertDialog.Builder builder;
    switch (id) {
      case NO_ACCOUNT_DIALOG:
        builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.send_google_no_account_title);
        builder.setMessage(R.string.send_google_no_account_message);
        builder.setCancelable(true);
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
          @Override
          public void onCancel(DialogInterface dialog) {
            finish();
          }
        });
        builder.setPositiveButton(R.string.generic_ok, new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int which) {
            finish();
          }
        });
        return builder.create();
      case CHOOSE_ACCOUNT_DIALOG:
        builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.send_google_choose_account_title);
        
        String[] choices = new String[accounts.length];
        for (int i = 0; i < accounts.length; i++) {
          choices[i] = accounts[i].name;
        }
        builder.setSingleChoiceItems(choices, selectedAccountIndex, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            selectedAccountIndex = which;
          }
        });
        
        builder.setCancelable(true);
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
          @Override
          public void onCancel(DialogInterface dialog) {
            finish();
          }
        });
        builder.setNegativeButton(R.string.generic_cancel, new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int which) {
            finish();
          }
        });
        builder.setPositiveButton(R.string.generic_ok, new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int which) {
            Account account = accounts[selectedAccountIndex];
            SharedPreferences prefs = getSharedPreferences(
                Constants.SETTINGS_NAME, Context.MODE_PRIVATE);
            Editor editor = prefs.edit();
            editor.putString(getString(R.string.preferred_account_key), account.name);
            ApiAdapterFactory.getApiAdapter().applyPreferenceChanges(editor);
            startNextActivity(account);
          }
        });
        return builder.create();
      default:
        return null;
    }
  }

  /**
   * Starts the next activity. If
   * <p>
   * sendMaps and newMap -> {@link SendMapsActivity}
   * <p>
   * sendMaps and !newMap -> {@link ChooseMapActivity}
   * <p>
   * !sendMaps && sendFusionTables -> {@link SendFusionTablesActivity}
   * <p>
   * !sendMaps && !sendFusionTables && sendDocs -> {@link SendDocsActivity}
   * <p>
   * !sendMaps && !sendFusionTables && !sendDocs -> {@link UploadResultActivity}
   *
   * @param account the chosen account
   */
  private void startNextActivity(Account account) {
    sendRequest.setAccount(account);
    
    Class<?> next;
    if (sendRequest.isSendMaps()) {
      next = sendRequest.isNewMap() ? SendMapsActivity.class : ChooseMapActivity.class;
    } else if (sendRequest.isSendFusionTables()) {
      next = SendFusionTablesActivity.class;
    } else if (sendRequest.isSendDocs()) {
      next = SendDocsActivity.class;
    } else {
      next = UploadResultActivity.class;
    }
    Intent intent = new Intent(this, next)
        .putExtra(SendRequest.SEND_REQUEST_KEY, sendRequest);
    startActivity(intent);
    finish();
  }
}