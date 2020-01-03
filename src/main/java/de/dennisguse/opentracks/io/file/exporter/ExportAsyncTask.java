/*
 * Copyright 2012 Google Inc.
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

package de.dennisguse.opentracks.io.file.exporter;

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.PowerManager.WakeLock;
import android.util.Log;

import androidx.documentfile.provider.DocumentFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;

import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.content.data.TracksColumns;
import de.dennisguse.opentracks.content.provider.ContentProviderUtils;
import de.dennisguse.opentracks.io.file.TrackFileFormat;
import de.dennisguse.opentracks.util.PreferencesUtils;
import de.dennisguse.opentracks.util.SystemUtils;

/**
 * Async Task to save tracks to the external storage.
 *
 * @author Jimmy Shih
 */
//TODO Make independent from ExportActivity?
public class ExportAsyncTask extends AsyncTask<Void, Integer, Boolean> {

    private static final String TAG = ExportAsyncTask.class.getSimpleName();
    private final TrackFileFormat trackFileFormat;
    private final DocumentFile directory;
    private final Context context;
    private final ContentProviderUtils contentProviderUtils;
    private ExportActivity exportActivity;
    private WakeLock wakeLock;

    private boolean completed;

    private int processedTrackCount;
    private int totalTrackCount;

    /**
     * Creates an AsyncTask.
     *
     * @param exportActivity  the activity currently associated with this task
     * @param trackFileFormat the track file format
     * @param directory       the directory to write the file
     */
    public ExportAsyncTask(ExportActivity exportActivity, TrackFileFormat trackFileFormat, DocumentFile directory) {
        this.exportActivity = exportActivity;
        this.trackFileFormat = trackFileFormat;
        this.directory = directory;
        context = exportActivity.getApplicationContext();
        contentProviderUtils = new ContentProviderUtils(context);

        completed = false;
        processedTrackCount = 0;
        totalTrackCount = 0;
    }

    /**
     * Sets the current activity associated with this AsyncTask.
     *
     * @param exportActivity the current activity, can be null
     */
    public void setActivity(ExportActivity exportActivity) {
        this.exportActivity = exportActivity;
        if (completed && exportActivity != null) {
            exportActivity.onAsyncTaskCompleted(processedTrackCount, totalTrackCount);
        }
    }

    @Override
    protected void onPreExecute() {
        if (exportActivity != null) {
            exportActivity.showProgressDialog();
        }
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        try {
            Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
            boolean isRecording = PreferencesUtils.isRecording(exportActivity);
            boolean isPaused = PreferencesUtils.isRecordingTrackPaused(exportActivity);
            // Get the wake lock if not recording or paused
            if (!isRecording || isPaused) {
                wakeLock = SystemUtils.acquireWakeLock(exportActivity, wakeLock);
            }
            return saveAllTracks();
        } finally {
            if (wakeLock != null && wakeLock.isHeld()) {
                wakeLock.release();
            }
        }
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        if (exportActivity != null) {
            exportActivity.setProgressDialogValue(values[0], values[1]);
        }
    }

    @Override
    protected void onPostExecute(Boolean result) {
        completed = true;
        if (exportActivity != null) {
            exportActivity.onAsyncTaskCompleted(processedTrackCount, totalTrackCount);
        }
    }

    @Override
    protected void onCancelled() {
        completed = true;
        if (exportActivity != null) {
            exportActivity.onAsyncTaskCompleted(processedTrackCount, totalTrackCount);
        }
    }

    /**
     * Saves tracks to one file (uses first track to determine the filename).
     *
     * @param tracks the tracks
     */
    private Boolean saveTracks(Track[] tracks) {
        if (tracks.length == 0) {
            return false;
        }

        TrackExporterListener trackExporterListener = new TrackExporterListener() {
            @Override
            public void onProgressUpdate(int number, int max) {
                //Update the progress dialog once every 500 points.
                if (number % 500 == 0) {
                    publishProgress(number, max);
                }
            }
        };

        TrackExporter trackExporter = trackFileFormat.newTrackExporter(context, tracks, trackExporterListener);
        Track track = tracks[0];

        String fileName = track.getId() + "." + trackFileFormat.getExtension();
        DocumentFile file = directory.createFile(trackFileFormat.getMimeType(), fileName);

        try (OutputStream outputStream = context.getContentResolver().openOutputStream(file.getUri())) {
            if (trackExporter.writeTrack(context, outputStream)) {
                return true;
            } else {
                if (!file.delete()) {
                    Log.d(TAG, "Unable to delete file");
                }
                Log.e(TAG, "Unable to export track");
                return false;
            }
        } catch (FileNotFoundException e) {
            Log.e(TAG, "Unable to open file " + file.getName(), e);
            return false;
        } catch (IOException e) {
            Log.e(TAG, "Unable to close file output stream", e);
            return false;
        }
    }

    /**
     * Saves all the tracks.
     */
    private Boolean saveAllTracks() {
        try (Cursor cursor = contentProviderUtils.getTrackCursor(null, null, TracksColumns._ID)) {
            if (cursor == null) {
                return false;
            }
            totalTrackCount = cursor.getCount();
            for (int i = 0; i < totalTrackCount; i++) {
                if (isCancelled()) {
                    return false;
                }
                cursor.moveToPosition(i);
                Track track = contentProviderUtils.createTrack(cursor);
                if (track != null && saveTracks(new Track[]{track})) {
                    processedTrackCount++;
                }
                publishProgress(i + 1, totalTrackCount);
            }
            return true;
        }
    }
}
