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

import junit.framework.TestCase;

/**
 * Tests for {@link FileUtils}.
 *
 * @author Rodrigo Damazio
 */
public class FileUtilsTest extends TestCase {

  /**
   * Tests {@link FileUtils#buildExternalDirectoryPath(String...)}.
   */
  public void testBuildExternalDirectoryPath() {
    String expectedName = Environment.getExternalStorageDirectory() + File.separator
        + Constants.SDCARD_TOP_DIR + File.separator + "a" + File.separator + "b" + File.separator
        + "c";
    String dirName = FileUtils.buildExternalDirectoryPath("a", "b", "c");
    assertEquals(expectedName, dirName);
  }

  /**
   * Tests {@link FileUtils#buildUniqueFileName(File, String, String)} when the
   * file is new.
   */
  public void testBuildUniqueFileName_new() {
    String filename = FileUtils.buildUniqueFileName(new File("/dir"), "Filename", "ext");
    assertEquals("Filename.ext", filename);
  }

  /**
   * Tests {@link FileUtils#buildUniqueFileName(File, String, String)} when the
   * file exists already.
   */
  public void testBuildUniqueFileName_exist() {
    // Expect "/default.prop" to exist on the phone/emulator
    String filename = FileUtils.buildUniqueFileName(new File("/"), "default", "prop");
    assertEquals("default(1).prop", filename);
  }

  /**
   * Tests {@link FileUtils#sanitizeFileName(String)} with special characters.
   * Verifies that they are sanitized.
   */
  public void testSanitizeFileName() {
    String name = "Swim\10ming-^across:/the/ pacific (ocean).";
    String expected = "Swim_ming-^across_the_ pacific (ocean)_";
    assertEquals(expected, FileUtils.sanitizeFileName(name));
  }

  /**
   * Tests {@link FileUtils#sanitizeFileName(String)} with i18n characters (in
   * Chinese and Russian). Verifies that they are allowed.
   */
  public void testSanitizeFileName_i18n() {
    String name = "您好-привет";
    String expected = "您好-привет";
    assertEquals(expected, FileUtils.sanitizeFileName(name));
  }

  /**
   * Tests {@link FileUtils#sanitizeFileName(String)} with special FAT32
   * characters. Verifies that they are allowed.
   */
  public void testSanitizeFileName_special_characters() {
    String name = "$%'-_@~`!(){}^#&+,;=[] ";
    String expected = "$%'-_@~`!(){}^#&+,;=[] ";
    assertEquals(expected, FileUtils.sanitizeFileName(name));
  }

  /**
   * Tests {@link FileUtils#sanitizeFileName(String)} with multiple escaped
   * characters in a row. Verifies that they are collapsed into one underscore.
   */
  public void testSanitizeFileName_collapse() {
    String name = "hello//there";
    String expected = "hello_there";
    assertEquals(expected, FileUtils.sanitizeFileName(name));
  }

  /**
   * Tests {@link FileUtils#truncateFileName(File, String, String)}. Verifies
   * the a long file name is truncated.
   */
  public void testTruncateFileName() {
    File directory = new File("/dir1/dir2/");
    String suffix = ".gpx";
    char[] name = new char[FileUtils.MAX_FAT32_PATH_LENGTH];
    for (int i = 0; i < name.length; i++) {
      name[i] = 'a';
    }
    String nameString = new String(name);
    String truncated = FileUtils.truncateFileName(directory, nameString, suffix);

    for (int i = 0; i < truncated.length(); i++) {
      assertEquals('a', truncated.charAt(i));
    }
    assertEquals(FileUtils.MAX_FAT32_PATH_LENGTH,
        new File(directory, truncated + suffix).getPath().length());
  }
}
