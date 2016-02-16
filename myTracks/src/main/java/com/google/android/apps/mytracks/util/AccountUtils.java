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

package com.google.android.apps.mytracks.util;

import com.google.android.maps.mytracks.R;

import android.accounts.Account;
import android.content.Context;
import android.support.v4.app.FragmentActivity;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

/**
 * Utilities for account.
 * 
 * @author Jimmy Shih
 */
public class AccountUtils {

  private AccountUtils() {}

  /**
   * Sets up account spinner.
   * 
   * @param fragmentActivity tyhe fragment activity
   * @param spinner the spinner
   * @param accounts the accounts
   */
  public static void setupAccountSpinner(
      FragmentActivity fragmentActivity, Spinner spinner, Account[] accounts) {
    if (accounts.length > 1) {
      String shareTrackAccount = PreferencesUtils.getString(fragmentActivity,
          R.string.share_track_account_key, PreferencesUtils.SHARE_TRACK_ACCOUNT_DEFAULT);
      ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(
          fragmentActivity, R.layout.account_spinner_item);
      int selection = 0;
      for (int i = 0; i < accounts.length; i++) {
        String name = accounts[i].name;
        adapter.add(name);
        if (name.equals(shareTrackAccount)) {
          selection = i;
        }
      }
      adapter.setDropDownViewResource(R.layout.account_spinner_dropdown_item);
      spinner.setAdapter(adapter);
      spinner.setSelection(selection);
    }
  }

  /**
   * Updates the share track account preference.
   * 
   * @param context the context
   * @param account the account
   */
  public static void updateShareTrackAccountPreference(Context context, Account account) {
    if (account != null) {
      PreferencesUtils.setString(context, R.string.share_track_account_key, account.name);
    }
  }
}
