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
package com.google.android.apps.mytracks.util;

import android.os.Build;

/**
 * A factory to get the {@link ApiAdapter} for the current device.
 *
 * @author Rodrigo Damazio
 */
public class ApiAdapterFactory {

  private static ApiAdapter apiAdapter;

  /**
   * Gets the {@link ApiAdapter} for the current device.
   */
  public static ApiAdapter getApiAdapter() {
    if (apiAdapter == null) {
      if (Build.VERSION.SDK_INT >= 17) {
        apiAdapter = new Api17Adapter();
      } else if (Build.VERSION.SDK_INT >= 16) {
        apiAdapter = new Api16Adapter();
      } else if (Build.VERSION.SDK_INT >= 14) {
        apiAdapter = new Api14Adapter();
      } else if (Build.VERSION.SDK_INT >= 11) {
        apiAdapter = new Api11Adapter();
      } else if (Build.VERSION.SDK_INT >= 10) {
        apiAdapter = new Api10Adapter();
      } else if (Build.VERSION.SDK_INT >= 9) {
        apiAdapter = new Api9Adapter();
      } else {
        apiAdapter = new Api8Adapter();
      }
    }
    return apiAdapter;
  }
}
