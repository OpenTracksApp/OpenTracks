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
 * Utility class for determining if newer-API features are available on the
 * current device.
 *
 * @author Rodrigo Damazio
 */
public class ApiFeatures {

  /**
   * The API level of the Android version we're being run under.
   */
  public static final int ANDROID_API_LEVEL = Integer.parseInt(Build.VERSION.SDK);

  private static ApiFeatures instance;

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
  public static void injectInstance(ApiFeatures instance) {
    ApiFeatures.instance = instance;
  }

  /**
   * Allow subclasses for mocking, but no direct instantiation.
   */
  protected ApiFeatures() {}

  /**
   * Returns whether cloud backup (a.k.a. Froyo backup) is available.
   */
  public boolean hasBackup() {
    return getApiLevel() >= 8;
  }

  /**
   * Returns whether text-to-speech is available.
   */
  public boolean hasTextToSpeech() {
    if (getApiLevel() < 4) return false;

    try {
      Class.forName("android.speech.tts.TextToSpeech");
    } catch (ClassNotFoundException ex) {
      return false;
    } catch (LinkageError er) {
      return false;
    }

    return true;
  }

  public boolean hasModernSignalStrength() {
    return getApiLevel() >= 7;
  }

  // Visible for testing.
  protected int getApiLevel() {
    return ANDROID_API_LEVEL;
  }
}
