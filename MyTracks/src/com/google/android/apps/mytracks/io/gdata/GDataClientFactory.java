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
package com.google.android.apps.mytracks.io.gdata;

import com.google.android.apps.mytracks.Constants;
import com.google.wireless.gdata.client.GDataClient;

import android.content.Context;
import android.util.Log;

/**
 * This factory will fetch the right class for the platform.
 * 
 * @author Sandor Dornbush
 */
public class GDataClientFactory {

  private GDataClientFactory() { }

  /**
   * Creates a new GData client.
   * This factory will fetch the right class for the platform.
   * @return A GDataClient appropriate for this platform
   */
  public static GDataClient getGDataClient(Context context) {
    // TODO This should be moved into ApiAdapter
    try {
      // Try to use the official unbundled gdata client implementation.
      // This should work on Froyo and beyond.
      return new com.google.android.common.gdata.AndroidGDataClient(context);
    } catch (LinkageError e) {
      // On all other platforms use the client implementation packaged in the
      // apk.
      Log.i(Constants.TAG, "Using mytracks AndroidGDataClient.", e);
      return new com.google.android.apps.mytracks.io.gdata.AndroidGDataClient();
    }
  }
}
