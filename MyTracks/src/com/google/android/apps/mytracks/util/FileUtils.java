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

import android.os.Environment;

import java.io.File;
import java.util.regex.Pattern;

/**
 * Utilities for dealing with files.
 *
 * @author Rodrigo Damazio
 */
public class FileUtils {
  /**
   * The maximum length of a filename, as per the FAT32 specification.
   */
  private static final int MAX_FILENAME_LENGTH = 260;

  /**
   * A set of characters that are prohibited from being in file names.
   */
  private static final Pattern PROHIBITED_CHAR_PATTERN =
      Pattern.compile("[^ A-Za-z0-9_.()-]+");

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
    dirNameBuilder.append(Constants.SDCARD_TOP_DIR);
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
    return Environment.MEDIA_MOUNTED.equals(
        Environment.getExternalStorageState());
  }

  /**
   * Normalizes the input string and make sure it is a valid fat32 file name.
   *
   * @param name the name to normalize
   * @return the sanitized name
   */
  String sanitizeName(String name) {
    String cleaned = PROHIBITED_CHAR_PATTERN.matcher(name).replaceAll("");

    return (cleaned.length() > MAX_FILENAME_LENGTH)
        ? cleaned.substring(0, MAX_FILENAME_LENGTH)
        : cleaned.toString();
  }

  /**
   * Ensures the given directory exists by creating it and its parents if
   * necessary.
   * 
   * @return whether the directory exists (either already existed or was
   *         successfully created)
   */
  public boolean ensureDirectoryExists(File dir) {
    if (dir.exists() && dir.isDirectory()) {
      return true;
    }

    if (dir.mkdirs()) {
      return true;
    }

    return false;
  }

  /**
   * Builds a filename with the given base name (prefix) and the given
   * extension, possibly adding a suffix to ensure the file doesn't exist.
   *
   * @param directory the directory the file will live in
   * @param fileBaseName the prefix for the file name
   * @param extension the file's extension
   * @return the complete file name, without the directory
   */
  public synchronized String buildUniqueFileName(File directory,
      String fileBaseName, String extension) {
    return buildUniqueFileName(directory, fileBaseName, extension, 0);
  }

  /**
   * Builds a filename with the given base name (prefix) and the given
   * extension, possibly adding a suffix to ensure the file doesn't exist.
   *
   * @param directory the directory the file will live in
   * @param fileBaseName the prefix for the file name
   * @param extension the file's extension
   * @param suffix the first numeric suffix to try to use, or 0 for none
   * @return the complete file name, without the directory
   */
  private String buildUniqueFileName(File directory, String fileBaseName,
      String extension, int suffix) {
    String suffixedBaseName = fileBaseName;
    if (suffix > 0) {
      suffixedBaseName += " (" + Integer.toString(suffix) + ")";
    }

    String fullName = suffixedBaseName + "." + extension;
    String sanitizedName = sanitizeName(fullName);
    if (!fileExists(directory, sanitizedName)) {
      return sanitizedName;
    }

    return buildUniqueFileName(directory, fileBaseName, extension, suffix + 1);
  }

  /**
   * Checks whether a file with the given name exists in the given directory.
   * This is isolated so it can be overridden in tests.
   */
  protected boolean fileExists(File directory, String fullName) {
    File file = new File(directory, fullName);
    return file.exists();
  }
}
