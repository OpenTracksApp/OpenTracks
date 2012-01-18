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

import com.google.android.apps.mytracks.Constants;

import android.os.Build;
import android.util.Log;

/**
 * Utility class for determining if newer-API features are available on the
 * current device.
 *
 * @author Rodrigo Damazio
 */
public class ApiFeatures {

  /**
   * The API level of the Android version we are being run under.
   */
  private static final int ANDROID_API_LEVEL = Integer.parseInt(
      Build.VERSION.SDK);

  private static ApiFeatures instance;

  /**
   * The API level adapter for the Android version we are being run under.
   */
  private ApiLevelAdapter apiLevelAdapter;

  /**
   * Returns the singleton instance of this class.
   */
  public static ApiFeatures getInstance() {
    if (instance == null) {
      instance = new ApiFeatures();
    }
    return instance;
  }

  /**
   * Injects a specific singleton instance, to be used for unit tests.
   */
  @SuppressWarnings("hiding")
  public static void injectInstance(ApiFeatures instance) {
    ApiFeatures.instance = instance;
  }

  /**
   * Allow subclasses for mocking, but no direct instantiation.
   */
  protected ApiFeatures() {
    // It is safe to import unsupported classes as long as we only actually
    // load the class when supported.
    if (getApiLevel() >= 9) {
      apiLevelAdapter = new ApiLevel9Adapter();
    } else if (getApiLevel() >= 8) {
      apiLevelAdapter = new ApiLevel8Adapter();
    } else if (getApiLevel() >= 5) {
      apiLevelAdapter = new ApiLevel5Adapter();
    } else {
      apiLevelAdapter = new ApiLevel3Adapter();
    }

    Log.i(Constants.TAG, "Using API level adapter " + apiLevelAdapter.getClass());
  }

  public ApiLevelAdapter getApiAdapter() {
    return apiLevelAdapter;
  }

  // API Level 4 Changes
  
  /**
   * Returns whether text-to-speech is available.
   */
  public boolean hasTextToSpeech() {
    return getApiLevel() >= 4;
  }

  // API Level 5 Changes
  
  /**
   * There's a bug (#1587) in Cupcake and Donut which prevents you from
   * using a SQLiteQueryBuilder twice.  That is, if you call buildQuery
   * on a given instance (to log the statement for debugging), and then
   * call query on the same instance to make it actually do the query,
   * it'll regenerate the query for the second call, and will screw it
   * up.  Specifically, it'll add extra parens which don't belong.
   */
  public boolean canReuseSQLiteQueryBuilder() {
    return getApiLevel() >= 5;
  }
  
  // API Level 10 changes
  
  /**
   * Returns true if BluetoothDevice.createInsecureRfcommSocketToServiceRecord
   * is available.
   */
  public boolean hasBluetoothDeviceCreateInsecureRfcommSocketToServiceRecord() {
    return getApiLevel() >= 10;
  }
  
  // Visible for testing.
  protected int getApiLevel() {
    return ANDROID_API_LEVEL;
  }
}
