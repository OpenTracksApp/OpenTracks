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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.widget.Toast;

import com.google.android.apps.mytracks.content.MyTracksProviderUtils;
import com.google.android.apps.mytracks.io.GpxImporter;
import com.google.android.maps.mytracks.R;

/**
 * A class that will import all GPX tracks in /sdcard/MyTracks/gpx/
 *
 * @author David Piggott
 */
public class ImportAllTracks {

  private final Activity activity;
  private WakeLock wakeLock;
  private ProgressDialog progress;

  public ImportAllTracks(Activity activity) {
    this.activity = activity;
    Log.i(MyTracksConstants.TAG, "ImportAllTracks: Starting");
    HandlerThread handlerThread;
    Handler handler;
    handlerThread = new HandlerThread("ImportAllTracks");
    handlerThread.start();
    handler = new Handler(handlerThread.getLooper());
    handler.post(runner);
  }

  private final Runnable runner = new Runnable() {
    public void run() {
      aquireLocksAndImport();
    }
  };

  /**
   * Makes sure that we keep the phone from sleeping.
   * See if there is a current track. Acquire a wake lock if there is no
   * current track.
   */
  private void aquireLocksAndImport() {
    SharedPreferences prefs =
        activity.getSharedPreferences(MyTracksSettings.SETTINGS_NAME, 0);
    long recordingTrackId = -1;
    if (prefs != null) {
      recordingTrackId =
    	  prefs.getLong(activity.getString(R.string.recording_track_key), -1);
    }
    if (recordingTrackId != -1) {
      acquireWakeLock();
    }

    // Now we can safely import everything.
    importAll();

    // Release the wake lock if we recorded one.
    // TODO check what happens if we started recording after getting this lock.
    if (wakeLock != null && wakeLock.isHeld()) {
      wakeLock.release();
      Log.i(MyTracksConstants.TAG, "ImportAllTracks: Releasing wake lock.");
    }
    Log.i(MyTracksConstants.TAG, "ImportAllTracks: Done");
    Toast.makeText(activity, R.string.import_done, Toast.LENGTH_SHORT).show();
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
   * Actually import the tracks.
   * This should be called after the wake locks have been acquired.
   */
  private void importAll() {
    try {
      MyTracksProviderUtils providerUtils =
          MyTracksProviderUtils.Factory.get(activity);

      LinkedList<File> gpxFiles = new LinkedList<File>();
      File[] gpxFileCandidates = new File("/sdcard/" + MyTracksConstants.SDCARD_TOP_DIR + "/gpx").listFiles(); 
      if(gpxFileCandidates == null || gpxFileCandidates.length == 0) {
    	  Toast.makeText(activity, activity.getString(R.string.import_empty), Toast.LENGTH_LONG).show();
    	  return;
      }
      for (File file : gpxFileCandidates) {
          if (!file.isDirectory() && file.getName().endsWith(".gpx")) {
              gpxFiles.add(file);
          }
      }
      
      final int trackCount = gpxFiles.size();
      
      Log.i(MyTracksConstants.TAG,
          "ImportAllTracks: Importing: " + trackCount + " tracks.");
      activity.runOnUiThread(new Runnable() {
        public void run() {
          makeProgressDialog(trackCount);
        }
      });

      Iterator<File> gpxFilesIterator = gpxFiles.iterator();
      File currentFile;
      int currentFileNumber = 0;
      while(gpxFilesIterator.hasNext()) {
    	currentFile = gpxFilesIterator.next();
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

        Log.i(MyTracksConstants.TAG, "ImportAllTracks: importing: " + currentFile.getName());
        try {
			GpxImporter.importGPXFile(new FileInputStream(currentFile), providerUtils);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
            Toast.makeText(activity, "FileNotFoundException", Toast.LENGTH_LONG).show();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
            Toast.makeText(activity, "ParserConfigurationException", Toast.LENGTH_LONG).show();
		} catch (SAXException e) {
			e.printStackTrace();
            Toast.makeText(activity, "SAXException", Toast.LENGTH_LONG).show();
		} catch (IOException e) {
			e.printStackTrace();
            Toast.makeText(activity, "IOException", Toast.LENGTH_LONG).show();
		}
		
		currentFileNumber++;
      }
    } finally {
      if (progress != null) {
        synchronized (this) {
          progress.dismiss();
          progress = null;
        }
      }
    }
  }

  /**
   * Tries to acquire a partial wake lock if not already acquired. Logs errors
   * and gives up trying in case the wake lock cannot be acquired.
   */
  private void acquireWakeLock() {
    Log.i(MyTracksConstants.TAG, "ImportAllTracks: Acquiring wake lock.");
    try {
      PowerManager pm =
          (PowerManager) activity.getSystemService(Context.POWER_SERVICE);
      if (pm == null) {
        Log.e(MyTracksConstants.TAG,
            "ImportAllTracks: Power manager not found!");
        return;
      }
      if (wakeLock == null) {
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
            MyTracksConstants.TAG);
        if (wakeLock == null) {
          Log.e(MyTracksConstants.TAG,
              "ImportAllTracks: Could not create wake lock (null).");
          return;
        }
      }
      if (!wakeLock.isHeld()) {
        wakeLock.acquire();
        if (!wakeLock.isHeld()) {
          Log.e(MyTracksConstants.TAG,
              "ImportAllTracks: Could not acquire wake lock.");
        }
      }
    } catch (RuntimeException e) {
      Log.e(MyTracksConstants.TAG,
          "ImportAllTracks: Caught unexpected exception: " + e.getMessage(), e);
    }
  }
}
