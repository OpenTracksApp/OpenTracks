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
package com.google.android.apps.mytracks.io;

import android.os.Environment;

import java.io.File;

/**
 * A class to clean up old temporary files.
 * @author Sandor Dornbush
 */
public class TempFileCleaner {
  
  private long currentTimeMillis;

  public TempFileCleaner() {
    currentTimeMillis = System.currentTimeMillis();
  }

  public TempFileCleaner(long time) {
    currentTimeMillis = time;
  }
  
  public void clean() {
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
    String sep = System.getProperty("file.separator");
    cleanTmpDirectory(
        new File(
            Environment.getExternalStorageDirectory() + sep + name + sep
            + "tmp"));
  }

  // VisibleForTesting
  protected int cleanTmpDirectory(File dir) {
    if (!dir.exists()) {
      return 0;
    }
    File[] list = dir.listFiles();
    int count = 0;
    long oldest = currentTimeMillis - 1000 * 3600;
    for (File f : list) {
      if (f.lastModified() < oldest) {
        f.delete();
        count++;
      }
    }
    return count;
  }
}
