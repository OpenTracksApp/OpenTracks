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

package com.google.android.apps.mytracks.io.sync;

import com.google.android.apps.mytracks.Constants;
import com.google.android.apps.mytracks.content.TracksColumns;
import com.google.android.apps.mytracks.util.PreferencesUtils;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.maps.mytracks.R;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.services.drive.DriveScopes;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;

import java.io.IOException;

/**
 * Utilites for Google Drive sync.
 * 
 * @author Jimmy Shih
 */
public class SyncUtils {

  private SyncUtils() {}

  public static final String DRIVE_IDS_QUERY = TracksColumns.DRIVEID + " IS NOT NULL AND "
      + TracksColumns.DRIVEID + "!=''";
  public static final String NO_DRIVE_ID_QUERY = TracksColumns.DRIVEID + " IS NULL OR "
      + TracksColumns.DRIVEID + "=''";
  
  private static final String SYNC_AUTHORITY = "com.google.android.maps.mytracks";
  
  /**
   * Checks permission to access Google Drive.
   * 
   * @param context the context
   * @param accountName the account name
   */
  public static GoogleAccountCredential checkDrivePermission(Context context, String accountName)
      throws IOException, GoogleAuthException {
    GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
        context, DriveScopes.DRIVE);
    credential.setSelectedAccountName(accountName);
    credential.getToken();
    return credential;
  }

  /**
   * Syncs now for the current account.
   * 
   * @param context the context
   */
  public static void syncNow(Context context) {    
    Account[] accounts = AccountManager.get(context).getAccountsByType(Constants.ACCOUNT_TYPE);
    String googleAccount = PreferencesUtils.getString(
        context, R.string.google_account_key, PreferencesUtils.GOOGLE_ACCOUNT_DEFAULT);
    for (Account account : accounts) {
      if (account.name.equals(googleAccount)) {
        ContentResolver.cancelSync(account, SYNC_AUTHORITY);
        ContentResolver.requestSync(account, SYNC_AUTHORITY, new Bundle());
        break;
      }
    }
  }

  /**
   * Disables sync.
   * 
   * @param account the account
   */
  public static void disableSync(Account account) {
    ContentResolver.cancelSync(account, SYNC_AUTHORITY);
    ContentResolver.setIsSyncable(account, SYNC_AUTHORITY, 0);
    ContentResolver.setSyncAutomatically(account, SYNC_AUTHORITY, false);
  }

  /**
   * Enables sync.
   * 
   * @param account the account
   */
  public static void enableSync(Account account) {
    ContentResolver.setIsSyncable(account, SYNC_AUTHORITY, 1);
    ContentResolver.setSyncAutomatically(account, SYNC_AUTHORITY, true);
    ContentResolver.requestSync(account, SYNC_AUTHORITY, new Bundle());
  }
}
