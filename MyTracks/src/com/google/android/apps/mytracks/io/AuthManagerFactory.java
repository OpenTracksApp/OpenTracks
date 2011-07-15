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

import static com.google.android.apps.mytracks.Constants.TAG;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

/**
 * A factory for getting the platform specific AuthManager.
 *
 * @author Sandor Dornbush
 */
public class AuthManagerFactory {

  private AuthManagerFactory() {
  }

  /**
   * Returns whether the modern AuthManager should be used
   */
  public static boolean useModernAuthManager() {
    return Integer.parseInt(Build.VERSION.SDK) >= 7;
  }

  /**
   * Get a right {@link AuthManager} for the platform.
   * @return A new AuthManager
   */
  public static AuthManager getAuthManager(Activity activity, int code,
      Bundle extras, boolean requireGoogle, String service) {
    if (useModernAuthManager()) {
      Log.i(TAG, "Creating modern auth manager: " + service);
      return new ModernAuthManager(activity, service);
    } else {
      Log.i(TAG, "Creating legacy auth manager: " + service);
      return new AuthManagerOld(activity, code, extras, requireGoogle, service);
    }
  }

}
