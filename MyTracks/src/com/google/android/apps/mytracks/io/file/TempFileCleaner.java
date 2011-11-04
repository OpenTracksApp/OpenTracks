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
package com.google.android.apps.mytracks.io.file;

import static com.google.android.apps.mytracks.Constants.TAG;

import com.google.android.apps.mytracks.util.FileUtils;

import android.os.Environment;
import android.util.Log;

import java.io.File;

/**
 * A class to clean up old temporary files.
 * @author Sandor Dornbush
 */
public class TempFileCleaner {

  private long currentTimeMillis;

  public static void clean() {
    (new TempFileCleaner(System.currentTimeMillis())).cleanImpl();
  }

  // @VisibleForTesting
  TempFileCleaner(long time) {
    currentTimeMillis = time;
  }

  private void cleanImpl() {
    if (!Environment.getExternalStorageState().equals(
        Environment.MEDIA_MOUNTED)) {
      return;  // Can't do anything now.
    }
    cleanTmpDirectory("csv");
    cleanTmpDirectory("gpx");
    cleanTmpDirectory("kml");
    cleanTmpDirectory("tcx");
  }

  private void cleanTmpDirectory(String name) {
    FileUtils fileUtils = new FileUtils();
    String dirName = fileUtils.buildExternalDirectoryPath(name, "tmp");
    cleanTmpDirectory(new File(dirName));
  }

  // @VisibleForTesting
  int cleanTmpDirectory(File dir) {
    if (!dir.exists()) {
      return 0;
    }
    int count = 0;
    long oldest = currentTimeMillis - 1000 * 3600;
    for (File f : dir.listFiles()) {
      if (f.lastModified() < oldest) {
        if (!f.delete()) {
          Log.w(TAG, "Failed to delete file " + f.getAbsolutePath());
        }
        count++;
      }
    }
    return count;
  }
}
