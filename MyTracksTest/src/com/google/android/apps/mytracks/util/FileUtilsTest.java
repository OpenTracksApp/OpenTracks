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
  private static final String ORIGINAL_NAME = "Swim\10ming-^across: the/ pacific (ocean).";
  private static final String SANITIZED_NAME = "Swim_ming_across_the_pacific_ocean_";

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

  public void testSanitizeFileName() {
    assertEquals(SANITIZED_NAME, fileUtils.sanitizeFileName(ORIGINAL_NAME));
  }

  public void testTruncateFileName() {
    File directory = new File("/dir1/dir2/");
    String suffix = ".gpx";
    char[] name = new char[FileUtils.MAX_FAT32_PATH_LENGTH];
    for (int i = 0; i < name.length; i++) {
      name[i] = 'a';
    }
    String nameString = new String(name);
    
    int expectedLength = FileUtils.MAX_FAT32_PATH_LENGTH - directory.getPath().length()
        - suffix.length() - 1;
    char[] expected = new char[expectedLength];
    for (int i = 0; i < expected.length; i++) {
      expected[i] = 'a';
    }
    String expectedString = new String(expected);
    
    String truncated = fileUtils.truncateFileName(directory, nameString, suffix);
    assertEquals(expectedString, truncated);
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
