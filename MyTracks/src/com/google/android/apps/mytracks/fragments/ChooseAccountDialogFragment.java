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

package com.google.android.apps.mytracks.fragments;

import com.google.android.apps.mytracks.Constants;
import com.google.android.apps.mytracks.util.PreferencesUtils;
import com.google.android.maps.mytracks.R;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;

/**
 * A DialogFragment to choose an account.
 * 
 * @author Jimmy Shih
 */
public class ChooseAccountDialogFragment extends DialogFragment {

  /**
   * Interface for caller of this dialog fragment.
   * 
   * @author Jimmy Shih
   */
  public interface ChooseAccountCaller {

    /**
     * Called when choose account is done.
     */
    public void onChooseAccountDone();
  }

  public static final String CHOOSE_ACCOUNT_DIALOG_TAG = "chooseAccount";

  private ChooseAccountCaller caller;
  private FragmentActivity fragmentActivity;
  private Account[] accounts;

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    try {
      caller = (ChooseAccountCaller) activity;
    } catch (ClassCastException e) {
      throw new ClassCastException(
          activity.toString() + " must implement " + ChooseAccountCaller.class.getSimpleName());
    }
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    fragmentActivity = getActivity();
    accounts = AccountManager.get(fragmentActivity).getAccountsByType(Constants.ACCOUNT_TYPE);

    if (accounts.length == 1) {
      PreferencesUtils.setString(fragmentActivity, R.string.google_account_key, accounts[0].name);
      dismiss();
      caller.onChooseAccountDone();
      return;
    }

    String googleAccount = PreferencesUtils.getString(
        fragmentActivity, R.string.google_account_key, PreferencesUtils.GOOGLE_ACCOUNT_DEFAULT);
    for (int i = 0; i < accounts.length; i++) {
      if (accounts[i].name.equals(googleAccount)) {
        dismiss();
        caller.onChooseAccountDone();
        return;
      }
    }
  }

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    if (accounts.length == 0) {
      return new AlertDialog.Builder(fragmentActivity).setMessage(
          R.string.send_google_no_account_message).setTitle(R.string.send_google_no_account_title)
          .setPositiveButton(R.string.generic_ok, null).create();
    }
    String[] choices = new String[accounts.length];
    for (int i = 0; i < accounts.length; i++) {
      choices[i] = accounts[i].name;
    }
    return new AlertDialog.Builder(fragmentActivity).setNegativeButton(
        R.string.generic_cancel, null)
        .setPositiveButton(R.string.generic_ok, new OnClickListener() {
            @Override
          public void onClick(DialogInterface dialog, int which) {
            int position = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
            PreferencesUtils.setString(
                fragmentActivity, R.string.google_account_key, accounts[position].name);
            caller.onChooseAccountDone();
          }
        }).setSingleChoiceItems(choices, 0, null)
        .setTitle(R.string.send_google_choose_account_title).create();
  }
}