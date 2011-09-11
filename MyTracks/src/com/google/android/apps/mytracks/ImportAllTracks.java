/*
 * Copyright 2011 Google Inc.
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
package com.google.android.apps.mytracks;

import com.google.android.apps.mytracks.content.MyTracksProviderUtils;
import com.google.android.apps.mytracks.io.file.GpxImporter;
import com.google.android.apps.mytracks.util.FileUtils;
import com.google.android.apps.mytracks.util.SystemUtils;
import com.google.android.maps.mytracks.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

/**
 * A class that will import all GPX tracks in /sdcard/MyTracks/gpx/
 *
 * @author David Piggott
 */
public class ImportAllTracks {

  private final Activity activity;
  private WakeLock wakeLock;
  private ProgressDialog progress;
  private FileUtils fileUtils;
  private String gpxPath;
  private int gpxFileCount;
  private int importSuccessCount;

  public ImportAllTracks(Activity activity) {
    this.activity = activity;
    Log.i(Constants.TAG, "ImportAllTracks: Starting");
    fileUtils = new FileUtils();
    gpxPath = fileUtils.buildExternalDirectoryPath("gpx");

    new Thread(runner).start();
  }

  private final Runnable runner = new Runnable() {
    public void run() {
      aquireLocksAndImport();
    }
  };

  /**
   * Makes sure that we keep the phone from sleeping. See if there is a current
   * track. Acquire a wake lock if there is no current track.
   */
  private void aquireLocksAndImport() {
    SharedPreferences prefs = activity.getSharedPreferences(Constants.SETTINGS_NAME, 0);
    long recordingTrackId = -1;
    if (prefs != null) {
      recordingTrackId = prefs.getLong(activity.getString(R.string.recording_track_key), -1);
    }
    if (recordingTrackId != -1) {
      wakeLock = SystemUtils.acquireWakeLock(activity, wakeLock);
    }

    // Now we can safely import everything.
    importAll();

    // Release the wake lock if we acquired one.
    // TODO check what happens if we started recording after getting this lock.
    if (wakeLock != null && wakeLock.isHeld()) {
      wakeLock.release();
      Log.i(Constants.TAG, "ImportAllTracks: Releasing wake lock.");
    }

    activity.runOnUiThread(new Thread() {
      @Override
      public void run() {
        showDoneDialog();
      }
    });
  }

  private void showDoneDialog() {
    Log.i(Constants.TAG, "ImportAllTracks: Done");
    AlertDialog.Builder builder = new AlertDialog.Builder(activity);
    if (gpxFileCount == 0) {
      builder.setMessage(activity.getString(R.string.import_multi_empty, gpxPath + "/"));
    } else {
      builder.setMessage(activity.getString(R.string.import_multi_done, importSuccessCount, gpxFileCount,
          gpxPath + "/"));
    }
    builder.setPositiveButton(R.string.ok, null);
    builder.show();
  }

  private void makeProgressDialog(final int trackCount) {
    String importMsg = activity.getString(R.string.tracklist_btn_import_all);
    progress = new ProgressDialog(activity);
    progress.setIcon(android.R.drawable.ic_dialog_info);
    progress.setTitle(importMsg);
    progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
    progress.setMax(trackCount);
    progress.setProgress(0);
    progress.show();
  }

  /**
   * Actually import the tracks. This should be called after the wake locks have
   * been acquired.
   */
  private void importAll() {
    MyTracksProviderUtils providerUtils = MyTracksProviderUtils.Factory.get(activity);

    if (!fileUtils.isSdCardAvailable()) {
      return;
    }

    List<File> gpxFiles = getGpxFiles();
    gpxFileCount = gpxFiles.size();
    if (gpxFileCount == 0) {
      return;
    }

    Log.i(Constants.TAG, "ImportAllTracks: Importing: " + gpxFileCount + " tracks.");
    activity.runOnUiThread(new Runnable() {
      public void run() {
        makeProgressDialog(gpxFileCount);
      }
    });

    Iterator<File> gpxFilesIterator = gpxFiles.iterator();
    for (int currentFileNumber = 0; gpxFilesIterator.hasNext(); currentFileNumber++) {
      File currentFile = gpxFilesIterator.next();
      final int status = currentFileNumber;
      activity.runOnUiThread(new Runnable() {
        public void run() {
          synchronized (this) {
            if (progress == null) {
              return;
            }
            progress.setProgress(status);
          }
        }
      });
      if (importFile(currentFile, providerUtils)) {
        importSuccessCount++;
      }
    }

    if (progress != null) {
      synchronized (this) {
        progress.dismiss();
        progress = null;
      }
    }
  }

  /**
   * Attempts to import a GPX file. Returns true on success, issues error
   * notifications and returns false on failure.
   */
  private boolean importFile(File gpxFile, MyTracksProviderUtils providerUtils) {
    Log.i(Constants.TAG, "ImportAllTracks: importing: " + gpxFile.getName());
    try {
      GpxImporter.importGPXFile(new FileInputStream(gpxFile), providerUtils);
      return true;
    } catch (FileNotFoundException e) {
      Log.w(Constants.TAG, "GPX file wasn't found/went missing: "
          + gpxFile.getAbsolutePath(), e);
    } catch (ParserConfigurationException e) {
      Log.w(Constants.TAG, "Error parsing file: " + gpxFile.getAbsolutePath(), e);
    } catch (SAXException e) {
      Log.w(Constants.TAG, "Error parsing file: " + gpxFile.getAbsolutePath(), e);
    } catch (IOException e) {
      Log.w(Constants.TAG, "Error reading file: " + gpxFile.getAbsolutePath(), e);
    }
    Toast.makeText(activity, activity.getString(R.string.import_error, gpxFile.getName()),
        Toast.LENGTH_LONG).show();
    return false;
  }

  /**
   * Returns a list of the GPX Files found in the GPX directory.
   */
  private List<File> getGpxFiles() {
    List<File> gpxFiles = new LinkedList<File>();
    File[] gpxFileCandidates = new File(gpxPath).listFiles();
    if (gpxFileCandidates != null) {
      for (File file : gpxFileCandidates) {
        if (!file.isDirectory() && file.getName().endsWith(".gpx")) {
          gpxFiles.add(file);
        }
      }
    }
    return gpxFiles;
  }
}
