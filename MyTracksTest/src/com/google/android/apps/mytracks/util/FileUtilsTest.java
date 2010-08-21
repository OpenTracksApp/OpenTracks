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

import junit.framework.TestCase;

/**
 * Tests for {@link FileUtils}.
 *
 * @author Rodrigo Damazio
 */
public class FileUtilsTest extends TestCase {
  private static final String ORIGINAL_NAME = "Swimming across the pacific";
  private static final String SANITIZED_NAME = "Swimmingacrossthepacific";

  private FileUtils fileUtils;

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    fileUtils = new FileUtils();
  }

  public void testBuildExternalDirectoryPath() {
    String expectedName = Environment.getExternalStorageDirectory()
        + File.separator
        + MyTracksConstants.SDCARD_TOP_DIR
        + File.separator
        + "a"
        + File.separator
        + "b"
        + File.separator
        + "c";

    String dirName = fileUtils.buildExternalDirectoryPath("a", "b", "c");
    assertEquals(expectedName, dirName);
  }

  public void testSanitizeName() {
    assertEquals(SANITIZED_NAME, fileUtils.sanitizeName(ORIGINAL_NAME));
  }
}
