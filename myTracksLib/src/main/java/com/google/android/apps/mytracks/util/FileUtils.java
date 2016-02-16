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

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;

import java.io.File;

/**
 * Utilities for dealing with files.
 * 
 * @author Rodrigo Damazio
 */
public class FileUtils {

  private FileUtils() {}

  /**
   * The backups directory under the My Tracks external storage directory.
   */
  public static final String BACKUPS_DIR = "backups";

  /**
   * The My Tracks external storage directory.
   */
  static final String SDCARD_TOP_DIR = "MyTracks";

  /**
   * The maximum FAT32 path length. See the FAT32 spec at
   * http://msdn.microsoft.com/en-us/windows/hardware/gg463080
   */
  static final int MAX_FAT32_PATH_LENGTH = 260;

  /**
   * The play tracks directory under the My Tracks cache directory.
   */
  public static final String PLAY_TRACKS_DIR = "temp";

  /**
   * The temp files directory under the My Tracks cache directory.
   */
  public static final String TEMP_FILES_DIR = "temp_files";

  /**
   * The pictures directory under the My Tracks external storage directory.
   */
  private static final String PICTURES_DIR = "pictures";

  /**
   * Returns true if the external storage is available.
   */
  public static boolean isExternalStorageAvailable() {
    String state = Environment.getExternalStorageState();
    return Environment.MEDIA_MOUNTED.equals(state)
        || Environment.MEDIA_MOUNTED_READ_ONLY.equals(state);
  }

  /**
   * Returns true if the external storage is writable.
   */
  public static boolean isExternalStorageWriteable() {
    String state = Environment.getExternalStorageState();
    return Environment.MEDIA_MOUNTED.equals(state);
  }

  /**
   * Returns true if the directory exists.
   * 
   * @param dir the directory
   */
  public static boolean isDirectory(File dir) {
    return dir.exists() && dir.isDirectory();
  }

  /**
   * Ensures the directory exists by creating it and its parents if necessary.
   * 
   * @return whether the directory exists (either already existed or was
   *         successfully created)
   */
  public static boolean ensureDirectoryExists(File dir) {
    if (isDirectory(dir)) {
      return true;
    }
    return dir.mkdirs();
  }

  public static File getPhotoDir() {
    return new File(getPath(PICTURES_DIR));
  }

  public static File getPhotoDir(long trackId) {
    return new File(getPath(PICTURES_DIR, "MyTracks" + Long.toString(trackId)));
  }

  /**
   * Gets the display name for a path on the external storage.
   * 
   * @param components the components
   */
  public static String getPathDisplayName(String... components) {
    StringBuilder dirNameBuilder = new StringBuilder();
    dirNameBuilder.append(File.separatorChar);
    dirNameBuilder.append(SDCARD_TOP_DIR);
    for (String component : components) {
      dirNameBuilder.append(File.separatorChar);
      dirNameBuilder.append(component);
    }
    return dirNameBuilder.toString();
  }

  /**
   * Gets a path on the external storage.
   * 
   * @param components the components
   */
  public static String getPath(String... components) {
    StringBuilder dirNameBuilder = new StringBuilder();
    dirNameBuilder.append(Environment.getExternalStorageDirectory());
    dirNameBuilder.append(getPathDisplayName(components));
    return dirNameBuilder.toString();
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
  public static synchronized String buildUniqueFileName(
      File directory, String fileBaseName, String extension) {
    return buildUniqueFileName(directory, fileBaseName, extension, 0);
  }

  /**
   * Gets the name from a file name, without the extension.
   * 
   * @param fileName the file name
   */
  public static String getName(String fileName) {
    int index = fileName.lastIndexOf('.');
    if (index == -1) {
      return fileName;
    }
    return fileName.substring(0, index);
  }

  /**
   * Gets the extension from a file name. Returns null if there is no extension.
   * 
   * @param fileName the file name
   */
  public static String getExtension(String fileName) {
    int index = fileName.lastIndexOf('.');
    if (index == -1) {
      return null;
    }
    return fileName.substring(index + 1);
  }
  
  public static void updateMediaScanner(Context context, Uri uri) {
    Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
    mediaScanIntent.setData(uri);
    context.sendBroadcast(mediaScanIntent);      
  }

  /**
   * Builds a filename with the given base and the given extension, possibly
   * adding a suffix to ensure the file doesn't exist.
   * 
   * @param directory the directory the filename will be located in
   * @param base the base for the filename
   * @param extension the extension for the filename
   * @param suffix the first numeric suffix to try to use, or 0 for none
   * @return the complete filename, without the directory
   */
  private static String buildUniqueFileName(
      File directory, String base, String extension, int suffix) {
    String suffixName = "";
    if (suffix > 0) {
      suffixName += "(" + Integer.toString(suffix) + ")";
    }
    suffixName += "." + extension;

    String baseName = sanitizeFileName(base);
    baseName = truncateFileName(directory, baseName, suffixName);
    String fullName = baseName + suffixName;

    if (!new File(directory, fullName).exists()) {
      return fullName;
    }
    return buildUniqueFileName(directory, base, extension, suffix + 1);
  }

  /**
   * Sanitizes the name as a valid fat32 filename. For simplicity, fat32
   * filename characters may be any combination of letters, digits, or
   * characters with code point values greater than 127. Replaces the invalid
   * characters with "_" and collapses multiple "_" together.
   * 
   * @param name name
   */
  static String sanitizeFileName(String name) {
    StringBuffer buffer = new StringBuffer(name.length());
    for (int i = 0; i < name.length(); i++) {
      int codePoint = name.codePointAt(i);
      char character = name.charAt(i);
      if (Character.isLetterOrDigit(character) || codePoint > 127 || isSpecialFat32(character)) {
        buffer.appendCodePoint(codePoint);
      } else {
        buffer.append("_");
      }
    }
    String result = buffer.toString();
    return result.replaceAll("_+", "_");
  }

  /**
   * Returns true if it is a special FAT32 character.
   * 
   * @param character the character
   */
  private static boolean isSpecialFat32(char character) {
    switch (character) {
      case '$':
      case '%':
      case '\'':
      case '-':
      case '_':
      case '@':
      case '~':
      case '`':
      case '!':
      case '(':
      case ')':
      case '{':
      case '}':
      case '^':
      case '#':
      case '&':
      case '+':
      case ',':
      case ';':
      case '=':
      case '[':
      case ']':
      case ' ':
        return true;
      default:
        return false;
    }
  }

  /**
   * Truncates the name if necessary so the filename path length (directory +
   * name + suffix) meets the Fat32 path limit.
   * 
   * @param directory directory
   * @param name name
   * @param suffix suffix
   */
  static String truncateFileName(File directory, String name, String suffix) {
    // 1 at the end accounts for the FAT32 filename trailing NUL character
    int requiredLength = directory.getPath().length() + suffix.length() + 1;
    if (name.length() + requiredLength > MAX_FAT32_PATH_LENGTH) {
      int limit = MAX_FAT32_PATH_LENGTH - requiredLength;
      return name.substring(0, limit);
    } else {
      return name;
    }
  }
}
