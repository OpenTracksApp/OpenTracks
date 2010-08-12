/*
 * Copyright 2009 Google Inc.
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
import com.google.android.apps.mytracks.content.TracksColumns;
import com.google.android.apps.mytracks.io.TrackWriterFactory;
import com.google.android.apps.mytracks.io.TrackWriterFactory.TrackFileFormat;
import com.google.android.maps.mytracks.R;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.widget.Toast;

/**
 * A class that will export all tracks to the sd card.
 *
 * @author Sandor Dornbush
 */
public class ExportAllTracks {
  // These must line up with the index in the array.
  public static final int GPX_OPTION_INDEX = 0;
  public static final int KML_OPTION_INDEX = 1;
  public static final int CSV_OPTION_INDEX = 2;

  private final Context context;
  private WakeLock wakeLock;
  private ProgressDialog progress;

  private TrackFileFormat format = TrackFileFormat.GPX;

  private final DialogInterface.OnClickListener itemClick =
      new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
          switch (which) {
            case GPX_OPTION_INDEX:
              format = TrackFileFormat.GPX;
              break;
            case KML_OPTION_INDEX:
              format = TrackFileFormat.KML;
              break;
            case CSV_OPTION_INDEX:
              format = TrackFileFormat.CSV;
              break;
          }
        }
      };

  public ExportAllTracks(Context c) {
    this.context = c;
    Log.i(MyTracksConstants.TAG, "ExportAllTracks: Starting");

    AlertDialog.Builder builder = new AlertDialog.Builder(c);
    builder.setSingleChoiceItems(R.array.export_formats, 0, itemClick);
    builder.setPositiveButton(R.string.ok, positiveClick);
    builder.setNegativeButton(R.string.cancel, null);
    builder.show();
  }

  private final DialogInterface.OnClickListener positiveClick =
      new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
          HandlerThread handlerThread;
          Handler handler;
          handlerThread = new HandlerThread("SendToMyMaps");
          handlerThread.start();
          handler = new Handler(handlerThread.getLooper());
          handler.post(runner);
        }
      };

  private final Runnable runner = new Runnable() {
    public void run() {
      aquireLocksAndExport();
    }
  };

  /**
   * Makes sure that we keep the phone from sleeping.
   * See if there is a current track. Aquire a wake lock if there is no
   * current track.
   */
  private void aquireLocksAndExport() {
    SharedPreferences prefs =
        context.getSharedPreferences(MyTracksSettings.SETTINGS_NAME, 0);
    long recordingTrackId = -1;
    if (prefs != null) {
      recordingTrackId =
    	  prefs.getLong(context.getString(R.string.recording_track_key), -1);
    }
    if (recordingTrackId != -1) {
      acquireWakeLock();
    }

    // Now we can safely export everything.
    exportAll();

    // Release the wake lock if we recorded one.
    // TODO check what happens if we started recording after getting this lock.
    if (wakeLock != null && wakeLock.isHeld()) {
      wakeLock.release();
      Log.i(MyTracksConstants.TAG, "ExportAllTracks: Releasing wake lock.");
    }
    Log.i(MyTracksConstants.TAG, "ExportAllTracks: Done");
    Toast.makeText(context, R.string.export_done, Toast.LENGTH_SHORT).show();
  }

  private void makeProgressDialog(final int trackCount) {
    String exportMsg = context.getString(R.string.tracklist_btn_export_all);
    progress = new ProgressDialog(context);
    progress.setIcon(android.R.drawable.ic_dialog_info);
    progress.setTitle(exportMsg);
    progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
    progress.setMax(trackCount);
    progress.setProgress(0);
    progress.show();
  }

  /**
   * Actually export the tracks.
   * This should be called after the wake locks have been aquired.
   */
  private void exportAll() {
    // Get a cursor over all tracks.
    Cursor cursor = null;
    try {
      MyTracksProviderUtils providerUtils =
          MyTracksProviderUtils.Factory.get(context);
      cursor = providerUtils.getTracksCursor("");
      if (cursor == null) {
        return;
      }
      final int trackCount = cursor.getCount();
      Log.i(MyTracksConstants.TAG,
          "ExportAllTracks: Exporting: " + cursor.getCount() + " tracks.");
      int idxTrackId = cursor.getColumnIndexOrThrow(TracksColumns._ID);
      MyTracks.getInstance().runOnUiThread(new Runnable() {
        public void run() {
          makeProgressDialog(trackCount);
        }});
      for (int i = 0; cursor.moveToNext(); i++) {
        final int status = i;
        MyTracks.getInstance().runOnUiThread(new Runnable() {
          public void run() {
            synchronized (this) {
              if (progress == null) {
                return;
              }
              progress.setProgress(status);
            }
          }
        });

        long id = cursor.getLong(idxTrackId);
        Log.i(MyTracksConstants.TAG, "ExportAllTracks: exporting: " + id);
        TrackWriterFactory.newWriter(context, providerUtils, id, format)
            .writeTrack();
      }
    } finally {
      if (cursor != null) {
        cursor.close();
      }
    }
    if (progress != null) {
      progress.dismiss();
    }
  }

  /**
   * Tries to acquire a partial wake lock if not already acquired. Logs errors
   * and gives up trying in case the wake lock cannot be acquired.
   */
  private void acquireWakeLock() {
    Log.i(MyTracksConstants.TAG, "ExportAllTracks: Aquiring wake lock.");
    try {
      PowerManager pm =
          (PowerManager) context.getSystemService(Context.POWER_SERVICE);
      if (pm == null) {
        Log.e(MyTracksConstants.TAG,
            "ExportAllTracks: Power manager not found!");
        return;
      }
      if (wakeLock == null) {
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
            MyTracksConstants.TAG);
        if (wakeLock == null) {
          Log.e(MyTracksConstants.TAG,
              "ExportAllTracks: Could not create wake lock (null).");
          return;
        }
      }
      if (!wakeLock.isHeld()) {
        wakeLock.acquire();
        if (!wakeLock.isHeld()) {
          Log.e(MyTracksConstants.TAG,
              "ExportAllTracks: Could not acquire wake lock.");
        }
      }
    } catch (RuntimeException e) {
      Log.e(MyTracksConstants.TAG,
          "ExportAllTracks: Caught unexpected exception: " + e.getMessage(), e);
    }
  }
}
