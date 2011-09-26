/*
 * Copyright 2009 Google Inc.
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
package com.google.android.apps.mytracks.io;

import com.google.android.googlelogindist.GoogleLoginServiceConstants;
import com.google.android.googlelogindist.GoogleLoginServiceHelper;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/**
 * AuthManager keeps track of the current auth token for a user. The advantage
 * over just passing around a String is that this class can renew the auth
 * token if necessary, and it will change for all classes using this
 * AuthManager.
 */
public class AuthManagerOld implements AuthManager {
  /** The activity that will handle auth result callbacks. */
  private final Activity activity;

  /** The code used to tell the activity that it is an auth result. */
  private final int code;

  /** Extras to pass into the getCredentials function. */
  private final Bundle extras;

  /** True if the account must be a Google account (not a domain account). */
  private final boolean requireGoogle;

  /** The name of the service to authorize for. */
  private final String service;

  /** The handler to call when a new auth token is fetched. */
  private AuthCallback authCallback;

  /** The most recently fetched auth token or null if none is available. */
  private String authToken;

  /**
   * AuthManager requires many of the same parameters as
   * {@link GoogleLoginServiceHelper#getCredentials(Activity, int, Bundle,
   * boolean, String, boolean)}. The activity must have
   * a handler in {@link Activity#onActivityResult} that calls
   * {@link #authResult(int, Intent)} if the request code is the code given
   * here.
   *
   * @param activity An activity with a handler in
   *        {@link Activity#onActivityResult} that calls
   *        {@link #authResult(int, Intent)} when {@literal code} is the request
   *        code
   * @param code The request code to pass to
   *        {@link Activity#onActivityResult} when
   *        {@link #authResult(int, Intent)} should be called
   * @param extras A {@link Bundle} of extras for
   *        {@link GoogleLoginServiceHelper}
   * @param requireGoogle True if the account must be a Google account
   * @param service The name of the service to authenticate as
   */
  public AuthManagerOld(Activity activity, int code, Bundle extras,
      boolean requireGoogle, String service) {
    this.activity = activity;
    this.code = code;
    this.extras = extras;
    this.requireGoogle = requireGoogle;
    this.service = service;
  }

  @Override
  public void doLogin(AuthCallback callback, Object o) {
    authCallback = callback;
    activity.runOnUiThread(new LoginRunnable());
  }

  /**
   * Runnable which actually gets login credentials.
   */
  private class LoginRunnable implements Runnable {
    @Override
    public void run() {
      GoogleLoginServiceHelper.getCredentials(
          activity, code, extras, requireGoogle, service, true);
    }
  }

  @Override
  public void authResult(int resultCode, Intent results) {
    if (resultCode != Activity.RESULT_OK) {
      runAuthCallback(false);
      return;
    }

    authToken = results.getStringExtra(GoogleLoginServiceConstants.AUTHTOKEN_KEY);
    if (authToken == null) {
      // Retry, without prompting the user.
      GoogleLoginServiceHelper.getCredentials(
          activity, code, extras, requireGoogle, service, false);
    } else {
      // Notify all active listeners that we have a new auth token.
      runAuthCallback(true);
    }
  }

  private void runAuthCallback(boolean success) {
    authCallback.onAuthResult(success);
    authCallback = null;
  }

  @Override
  public String getAuthToken() {
    return authToken;
  }

  @Override
  public void invalidateAndRefresh(AuthCallback callback) {
    authCallback = callback;
    activity.runOnUiThread(new Runnable() {
      public void run() {
        GoogleLoginServiceHelper.invalidateAuthToken(activity, code, authToken);
      }
    });
  }

  @Override
  public Object getAccountObject(String accountName, String accountType) {
    throw new UnsupportedOperationException("Legacy auth manager knows nothing about accounts");
  }
}
