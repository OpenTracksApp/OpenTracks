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
import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;

/**
 * Tests for {@link FileUtils}.
 *
 * @author Rodrigo Damazio
 */
public class FileUtilsTest extends TestCase {
  private FileUtils fileUtils;
  private Set<String> existingFiles;

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    existingFiles = new HashSet<String>();
    fileUtils = new FileUtils() {
      @Override
      protected boolean fileExists(File directory, String fullName) {
        return existingFiles.contains(fullName);
      }
    };
  }

  public void testBuildExternalDirectoryPath() {
    String expectedName = Environment.getExternalStorageDirectory()
        + File.separator
        + Constants.SDCARD_TOP_DIR
        + File.separator
        + "a"
        + File.separator
        + "b"
        + File.separator
        + "c";

    String dirName = fileUtils.buildExternalDirectoryPath("a", "b", "c");
    assertEquals(expectedName, dirName);
  }

  /**
   * Tests sanitize filename.
   */
  public void testSanitizeFileName() {
    String name = "Swim\10ming-^across:/the/ pacific (ocean).";
    String expected = "Swim_ming-^across_the_ pacific (ocean)_";
    assertEquals(expected, fileUtils.sanitizeFileName(name));
  }

  /**
   * Tests characters in other languages, like Chinese and Russian, are allowed.
   */
  public void testSanitizeFileName_i18n() {
    String name = "您好-привет";
    String expected = "您好-привет";
    
    assertEquals(expected, fileUtils.sanitizeFileName(name));
  }
  
  /**
   * Tests special FAT32 characters are allowed.
   */
  public void testSanitizeFileName_special_characters() {
    String name = "$%'-_@~`!(){}^#&+,;=[] ";
    String expected = "$%'-_@~`!(){}^#&+,;=[] ";
    
    assertEquals(expected, fileUtils.sanitizeFileName(name));
  }

  /**
   * Testing collapsing multiple underscores characters.
   */
  public void testSanitizeFileName_collapse() {
    String name = "hello//there";
    String expected = "hello_there";
    
    assertEquals(expected, fileUtils.sanitizeFileName(name));
  }
  
  public void testTruncateFileName() {
    File directory = new File("/dir1/dir2/");
    String suffix = ".gpx";
    char[] name = new char[FileUtils.MAX_FAT32_PATH_LENGTH];
    for (int i = 0; i < name.length; i++) {
      name[i] = 'a';
    }
    String nameString = new String(name);
    
    String truncated = fileUtils.truncateFileName(directory, nameString, suffix);
    for (int i = 0; i < truncated.length(); i++) {
      assertEquals('a', truncated.charAt(i));
    }
    assertEquals(FileUtils.MAX_FAT32_PATH_LENGTH,
        new File(directory, truncated + suffix).getPath().length());
  }
  
  public void testBuildUniqueFileName_someExist() {
    existingFiles = new HashSet<String>();
    existingFiles.add("Filename.ext");
    existingFiles.add("Filename(1).ext");
    existingFiles.add("Filename(2).ext");
    existingFiles.add("Filename(3).ext");
    existingFiles.add("Filename(4).ext");

    String filename = fileUtils.buildUniqueFileName(new File("/dir/"), "Filename", "ext");
    assertEquals("Filename(5).ext", filename);
  }

  public void testBuildUniqueFileName_oneExists() {
    existingFiles.add("Filename.ext");

    String filename = fileUtils.buildUniqueFileName(new File("/dir/"), "Filename", "ext");
    assertEquals("Filename(1).ext", filename);
  }

  public void testBuildUniqueFileName_noneExists() {
    String filename = fileUtils.buildUniqueFileName(new File("/dir/"), "Filename", "ext");
    assertEquals("Filename.ext", filename);
  }
}
