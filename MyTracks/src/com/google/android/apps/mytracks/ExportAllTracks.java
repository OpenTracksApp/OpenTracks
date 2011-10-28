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
import com.google.android.apps.mytracks.io.file.TrackWriter;
import com.google.android.apps.mytracks.io.file.TrackWriterFactory;
import com.google.android.apps.mytracks.io.file.TrackWriterFactory.TrackFileFormat;
import com.google.android.apps.mytracks.util.SystemUtils;
import com.google.android.maps.mytracks.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
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
  public static final int TCX_OPTION_INDEX = 3;

  private final Activity activity;
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
            case TCX_OPTION_INDEX:
              format = TrackFileFormat.TCX;
              break;
            default:
              Log.w(Constants.TAG, "Unknown export format: " + which);
          }
        }
      };

  public ExportAllTracks(Activity activity) {
    this.activity = activity;
    Log.i(Constants.TAG, "ExportAllTracks: Starting");

    AlertDialog.Builder builder = new AlertDialog.Builder(activity);
    builder.setSingleChoiceItems(R.array.export_formats, 0, itemClick);
    builder.setPositiveButton(R.string.ok, positiveClick);
    builder.setNegativeButton(R.string.cancel, null);
    builder.show();
  }

  private final DialogInterface.OnClickListener positiveClick =
      new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
          new Thread(runner, "SendToMyMaps").start();
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
        activity.getSharedPreferences(Constants.SETTINGS_NAME, 0);
    long recordingTrackId = -1;
    if (prefs != null) {
      recordingTrackId =
          prefs.getLong(activity.getString(R.string.recording_track_key), -1);
    }
    if (recordingTrackId != -1) {
      wakeLock = SystemUtils.acquireWakeLock(activity, wakeLock);
    }

    // Now we can safely export everything.
    exportAll();

    // Release the wake lock if we recorded one.
    // TODO check what happens if we started recording after getting this lock.
    if (wakeLock != null && wakeLock.isHeld()) {
      wakeLock.release();
      Log.i(Constants.TAG, "ExportAllTracks: Releasing wake lock.");
    }
    Log.i(Constants.TAG, "ExportAllTracks: Done");
    showToast(R.string.export_done, Toast.LENGTH_SHORT);
  }

  private void makeProgressDialog(final int trackCount) {
    String exportMsg = activity.getString(R.string.tracklist_btn_export_all);
    progress = new ProgressDialog(activity);
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
          MyTracksProviderUtils.Factory.get(activity);
      cursor = providerUtils.getTracksCursor("");
      if (cursor == null) {
        return;
      }

      final int trackCount = cursor.getCount();
      Log.i(Constants.TAG,
          "ExportAllTracks: Exporting: " + cursor.getCount() + " tracks.");
      int idxTrackId = cursor.getColumnIndexOrThrow(TracksColumns._ID);
      activity.runOnUiThread(new Runnable() {
        public void run() {
          makeProgressDialog(trackCount);
        }
      });

      for (int i = 0; cursor.moveToNext(); i++) {
        final int status = i;
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

        long id = cursor.getLong(idxTrackId);
        Log.i(Constants.TAG, "ExportAllTracks: exporting: " + id);
        TrackWriter writer =
            TrackWriterFactory.newWriter(activity, providerUtils, id, format);
        if (writer == null) {
          showToast(R.string.error_export_generic, Toast.LENGTH_LONG);
          return;
        }

        writer.writeTrack();

        if (!writer.wasSuccess()) {
          // Abort the whole export on the first error.
          showToast(writer.getErrorMessage(), Toast.LENGTH_LONG);
          return;
        }
      }
    } finally {
      if (cursor != null) {
        cursor.close();
      }
      if (progress != null) {
        synchronized (this) {
          progress.dismiss();
          progress = null;
        }
      }
    }
  }

  private void showToast(final int messageId, final int length) {
    activity.runOnUiThread(new Runnable() {
      @Override
      public void run() {
        Toast.makeText(activity, messageId, length).show();
      }
    });
  }
}
