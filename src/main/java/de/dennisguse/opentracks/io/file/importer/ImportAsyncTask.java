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

package de.dennisguse.opentracks.io.file.importer;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.PowerManager.WakeLock;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import de.dennisguse.opentracks.content.ContentProviderUtils;
import de.dennisguse.opentracks.content.Track;
import de.dennisguse.opentracks.io.file.TrackFileFormat;
import de.dennisguse.opentracks.util.FileUtils;
import de.dennisguse.opentracks.util.PreferencesUtils;
import de.dennisguse.opentracks.util.SystemUtils;

/**
 * AsyncTask to import files from the external storage.
 *
 * @author Jimmy Shih
 */
public class ImportAsyncTask extends AsyncTask<Void, Integer, Boolean> {

    private static final String TAG = ImportAsyncTask.class.getSimpleName();
    private final TrackFileFormat trackFileFormat;
    private final String path;
    private final Context context;
    private ImportActivity importActivity;
    private WakeLock wakeLock;

    // true if the AsyncTask has completed
    private boolean completed;

    private int importTrackCount;

    private int totalTrackCount;

    // the last successfully imported track id
    private long trackId;

    /**
     * Creates an AsyncTask.
     *
     * @param importActivity  the activity currently associated with this AsyncTask
     * @param trackFileFormat the track file format
     * @param path            path to import GPX files
     */
    public ImportAsyncTask(ImportActivity importActivity, TrackFileFormat trackFileFormat, String path) {
        this.importActivity = importActivity;
        this.trackFileFormat = trackFileFormat;
        this.path = path;
        context = importActivity.getApplicationContext();

        completed = false;
        importTrackCount = 0;
        totalTrackCount = 0;
        trackId = -1L;
    }

    /**
     * Sets the current {@link ImportActivity} associated with this AyncTask.
     *
     * @param importActivity the current {@link ImportActivity}, can be null
     */
    public void setActivity(ImportActivity importActivity) {
        this.importActivity = importActivity;
        if (completed && importActivity != null) {
            importActivity.onAsyncTaskCompleted(importTrackCount, totalTrackCount);
        }
    }

    @Override
    protected void onPreExecute() {
        if (importActivity != null) {
            importActivity.showProgressDialog();
        }
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        try {
            Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
            // Get the wake lock if not recording or paused
            boolean isRecording = PreferencesUtils.isRecording(importActivity);
            boolean isPaused = PreferencesUtils.isRecordingTrackPaused(importActivity);
            if (!isRecording || isPaused) {
                wakeLock = SystemUtils.acquireWakeLock(importActivity, wakeLock);
            }

            List<File> files = getFiles();
            totalTrackCount = files.size();
            if (totalTrackCount == 0) {
                return true;
            }

            for (int i = 0; i < totalTrackCount; i++) {
                if (isCancelled()) {
                    // If cancelled, return true to show the number of files imported
                    return true;
                }
                if (importFile(files.get(i))) {
                    importTrackCount++;
                }
                publishProgress(i + 1, totalTrackCount);
            }
            return true;
        } finally {
            if (wakeLock != null && wakeLock.isHeld()) {
                wakeLock.release();
            }
        }
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        if (importActivity != null) {
            importActivity.setProgressDialogValue(values[0], values[1]);
        }
    }

    @Override
    protected void onPostExecute(Boolean result) {
        completed = true;
        if (importActivity != null) {
            importActivity.onAsyncTaskCompleted(importTrackCount, totalTrackCount);
        }
    }

    @Override
    protected void onCancelled() {
        completed = true;
        if (importActivity != null) {
            importActivity.onAsyncTaskCompleted(importTrackCount, totalTrackCount);
        }
    }

    /**
     * Imports a file.
     *
     * @param file the file
     */
    private boolean importFile(final File file) {
        TrackImporter trackImporter;
        if (trackFileFormat == TrackFileFormat.GPX) {
            trackImporter = new GpxFileTrackImporter(context);
        } else { //KML or KMZ
            String extension = FileUtils.getExtension(file.getName());
            if (TrackFileFormat.KML_ONLY_TRACK.getExtension().equals(extension)) {
                trackImporter = new KmlFileTrackImporter(context, -1L);
            } else {
                ContentProviderUtils contentProviderUtils = new ContentProviderUtils(context);
                ;
                Uri uri = contentProviderUtils.insertTrack(new Track());
                long newId = Long.parseLong(uri.getLastPathSegment());

                trackImporter = new KmzTrackImporter(context, newId);
            }
        }

        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            trackId = trackImporter.importFile(fileInputStream);
            return trackId != -1L;
        } catch (IOException e) {
            Log.e(TAG, "Unable to import file", e);
            return false;
        }
    }

    /**
     * Gets a list of files.
     * If importAll is true, returns a list of the files under the path directory.
     * If importAll is false, returns a list containing just the path file.
     */
    private List<File> getFiles() {
        List<File> files = new ArrayList<>();
        File file = new File(path);

        File[] candidates = file.listFiles();
        if (candidates == null) {
            return files;
        }
        for (File candidate : candidates) {
            if (!FileUtils.isDirectory(candidate)) {
                String extension = FileUtils.getExtension(candidate.getName());
                if (trackFileFormat.getExtension().equals(extension)) {
                    files.add(candidate);
                }
            }
        }

        return files;
    }
}
