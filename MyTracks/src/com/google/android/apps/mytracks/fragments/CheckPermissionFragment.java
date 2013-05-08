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

import com.google.android.apps.mytracks.io.sendtogoogle.SendToGoogleUtils;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;

import java.io.IOException;

/**
 * A non UI fragment to check permission.
 * 
 * @author Jimmy Shih
 */
public class CheckPermissionFragment extends Fragment {

  /**
   * Interface for caller of this fragment.
   * 
   * @author Jimmy Shih
   */
  public interface CheckPermissionCaller {

    /**
     * Called when check permission is done.
     * 
     * @param scope the permission scope
     * @param success true if success
     * @param intent if not success, intent to prompt for permission
     */
    public void onCheckPermissionDone(String scope, boolean success, Intent intent);
  }

  public static final String CHECK_PERMISSION_TAG = "checkPermission";

  private static final String TAG = CheckPermissionFragment.class.getSimpleName();
  private static final String KEY_ACCOUNT_NAME = "accountName";
  private static final String KEY_SCOPE = "scope";

  private CheckPermissionCaller caller;

  public static CheckPermissionFragment newInstance(String accountName, String scope) {
    Bundle bundle = new Bundle();
    bundle.putString(KEY_ACCOUNT_NAME, accountName);
    bundle.putString(KEY_SCOPE, scope);

    CheckPermissionFragment checkPermissionFragment = new CheckPermissionFragment();
    checkPermissionFragment.setArguments(bundle);
    return checkPermissionFragment;
  }

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    try {
      caller = (CheckPermissionCaller) activity;
    } catch (ClassCastException e) {
      throw new ClassCastException(
          activity.toString() + " must implement " + CheckPermissionCaller.class.getSimpleName());
    }
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Only want one instance
    setRetainInstance(true);

    Thread thread = new Thread(new Runnable() {
        @Override
      public void run() {
        String accountName = getArguments().getString(KEY_ACCOUNT_NAME);
        String scope = getArguments().getString(KEY_SCOPE);
        try {
          SendToGoogleUtils.getGoogleAccountCredential(getActivity(), accountName, scope);
          finish(scope, true, null);
        } catch (UserRecoverableAuthException e) {
          finish(scope, false, e.getIntent());
        } catch (GoogleAuthException e) {
          Log.e(TAG, "GoogleAuthException", e);
          finish(scope, false, null);
        } catch (UserRecoverableAuthIOException e) {
          finish(scope, false, e.getIntent());
        } catch (IOException e) {
          Log.e(TAG, "IOException", e);
          finish(scope, false, null);
        }
      }
    });
    thread.start();
  }

  private void finish(final String scope, final boolean success, final Intent intent) {
    getFragmentManager().popBackStack();
    getActivity().runOnUiThread(new Runnable() {

        @Override
      public void run() {
        caller.onCheckPermissionDone(scope, success, intent);
      }
    });
  }
}