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

import com.google.android.apps.mytracks.MyTracksConstants;

import android.os.Environment;

import java.io.File;
import java.util.HashSet;

/**
 * Utilities for dealing with files.
 *
 * @author Rodrigo Damazio
 */
public class FileUtils {

  /**
   * A set of characters that are prohibited from being in file names.
   */
  private static final HashSet<Character> PROHIBITED_CHARACTERS =
      new HashSet<Character>();

  static {
    for (int i = 0; i < 48; i++) {
      PROHIBITED_CHARACTERS.add(new Character((char) i));
    }
    for (int i = 58; i < 65; i++) {
      PROHIBITED_CHARACTERS.add(new Character((char) i));
    }
    for (int i = 91; i < 97; i++) {
      PROHIBITED_CHARACTERS.add(new Character((char) i));
    }
    for (int i = 123; i < 128; i++) {
      PROHIBITED_CHARACTERS.add(new Character((char) i));
    }
  }

  /**
   * Builds a path inside the My Tracks directory in the SD card.
   *
   * @param components the path components inside the mytracks directory
   * @return the full path to the destination
   */
  public String buildExternalDirectoryPath(String... components) {
    StringBuilder dirNameBuilder = new StringBuilder();
    dirNameBuilder.append(Environment.getExternalStorageDirectory());
    dirNameBuilder.append(File.separatorChar);
    dirNameBuilder.append(MyTracksConstants.SDCARD_TOP_DIR);
    for (String component : components) {
      dirNameBuilder.append(File.separatorChar);
      dirNameBuilder.append(component);
    }
    return dirNameBuilder.toString();
  }

  /**
   * Returns whether the SD card is available.
   */
  public boolean isSdCardAvailable() {
    return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
  }

  /**
   * Normalizes the input string and make sure it is a valid fat32 file name.
   */
  public String sanitizeName(String name) {
    StringBuilder cleaned = new StringBuilder();
    for (int i = 0; i < name.length(); i++) {
      char c = name.charAt(i);
      if (!PROHIBITED_CHARACTERS.contains(c)) {
        cleaned.append(c);
      }
    }

    return (cleaned.length() > 260)
        ? cleaned.substring(0, 260)
        : cleaned.toString();
  }

  public boolean ensureDirectoryExists(File dir) {
    if (dir.exists() && dir.isDirectory()) {
      return true;
    }

    if (dir.mkdirs()) {
      return true;
    }

    return false;
  }
}
