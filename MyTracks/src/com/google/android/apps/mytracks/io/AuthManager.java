/*
 * Copyright 2010 Google Inc.
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

import android.content.Intent;

/**
 * This interface describes a class that will fetch and maintain a Google
 * authentication token.
 *
 * @author Sandor Dornbush
 */
public interface AuthManager {
  /**
   * Callback for authentication token retrieval operations.
   */
  public interface AuthCallback {
    /**
     * Indicates that we're done fetching an auth token.
     *
     * @param success if true, indicates we have the requested auth token available
     *        to be retrieved using {@link AuthManager#getAuthToken}
     */
    void onAuthResult(boolean success);
  }

  /**
   * Initializes the login process. The user should be asked to login if they
   * haven't already. The {@link AuthCallback} provided will be executed when the
   * auth token fetching is done (successfully or not).
   *
   * @param whenFinished A {@link AuthCallback} to execute when the auth token
   *        fetching is done
   */
  void doLogin(AuthCallback whenFinished, Object o);

  /**
   * The {@link android.app.Activity} owner of this class should call this
   * function when it gets {@link android.app.Activity#onActivityResult} with
   * the request code passed into the constructor. The resultCode and results
   * should come directly from the {@link android.app.Activity#onActivityResult}
   * function. This function will return true if an auth token was successfully
   *  fetched or the process is not finished.
   *
   * @param resultCode The result code passed in to the
   *        {@link android.app.Activity}'s
   *        {@link android.app.Activity#onActivityResult} function
   * @param results The data passed in to the {@link android.app.Activity}'s
   *        {@link android.app.Activity#onActivityResult} function
   */
  void authResult(int resultCode, Intent results);

  /**
   * Returns the current auth token. Response may be null if no valid auth
   * token has been fetched.
   *
   * @return The current auth token or null if no auth token has been
   *         fetched
   */
  String getAuthToken();

  /**
   * Invalidates the existing auth token and request a new one. The
   * {@link Runnable} provided will be executed when the new auth token is
   * successfully fetched.
   *
   * @param whenFinished A {@link Runnable} to execute when a new auth token
   *        is successfully fetched
   */
  void invalidateAndRefresh(AuthCallback whenFinished);

  /**
   * Returns an object that represents the given account, if possible.
   *
   * @param accountName the name of the account
   * @param accountType the type of the account
   * @return the account object
   */
  Object getAccountObject(String accountName, String accountType);
}
