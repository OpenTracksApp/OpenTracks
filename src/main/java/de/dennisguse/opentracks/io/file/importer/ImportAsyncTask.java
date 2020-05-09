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

import android.net.Uri;
import android.os.AsyncTask;
import android.os.PowerManager.WakeLock;
import android.util.Log;

import androidx.documentfile.provider.DocumentFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.content.provider.ContentProviderUtils;
import de.dennisguse.opentracks.io.file.TrackFileFormat;
import de.dennisguse.opentracks.util.FileUtils;
import de.dennisguse.opentracks.util.PreferencesUtils;
import de.dennisguse.opentracks.util.SystemUtils;

/**
 * AsyncTask to import files from the external storage.
 *
 * @author Jimmy Shih
 */
class ImportAsyncTask extends AsyncTask<Void, Integer, Boolean> {

    private static final String TAG = ImportAsyncTask.class.getSimpleName();
    private final TrackFileFormat trackFileFormat;
    private final DocumentFile directory;

    // TODO Use weak reference
    private ImportActivity importActivity;
    private WakeLock wakeLock;

    private int importTrackCount;
    private int totalTrackCount;
    private long lastSuccessfulTrackId;

    public ImportAsyncTask(ImportActivity importActivity, TrackFileFormat trackFileFormat, DocumentFile directory) {
        this.importActivity = importActivity;
        this.trackFileFormat = trackFileFormat;
        this.directory = directory;

        importTrackCount = 0;
        totalTrackCount = 0;
        lastSuccessfulTrackId = -1L;
    }

    @Override
    protected void onPreExecute() {
        if (importActivity != null) {
            importActivity.showProgressDialog();
        }
    }

    /**
     * Gets a list of files.
     */
    private static List<DocumentFile> getFiles(DocumentFile file, TrackFileFormat trackFileFormat) {
        List<DocumentFile> files = new ArrayList<>();

        for (DocumentFile candidate : file.listFiles()) {
            if (!candidate.isDirectory()) {
                String extension = FileUtils.getExtension(candidate.getName());
                if (extension != null && trackFileFormat.getExtension().equals(extension)) {
                    files.add(candidate);
                }
            } else {
                files.addAll(getFiles(candidate, trackFileFormat));
            }
        }

        return files;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        if (importActivity != null) {
            importActivity.setProgressDialogValue(values[0], values[1]);
        }
    }

    @Override
    protected void onPostExecute(Boolean result) {
        if (importActivity != null) {
            importActivity.onAsyncTaskCompleted(importTrackCount, totalTrackCount);
        }
    }

    @Override
    protected void onCancelled() {
        if (importActivity != null) {
            importActivity.onAsyncTaskCompleted(importTrackCount, totalTrackCount);
            importActivity = null;
        }
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        try {
            Thread.currentThread().setPriority(Thread.MAX_PRIORITY); // TODO Should be set to previous level at the end of this method.
            // Get the wake lock if not recording or paused
            boolean isRecording = PreferencesUtils.isRecording(importActivity);
            boolean isPaused = PreferencesUtils.isRecordingTrackPaused(importActivity);
            if (!isRecording || isPaused) {
                wakeLock = SystemUtils.acquireWakeLock(importActivity, wakeLock);
            }

            List<DocumentFile> files = getFiles(directory, trackFileFormat);
            totalTrackCount = files.size();

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

    /**
     * Imports a file.
     *
     * @param file the file
     */
    private boolean importFile(final DocumentFile file) {
        TrackImporter trackImporter;
        if (trackFileFormat == TrackFileFormat.GPX) {
            trackImporter = new GpxFileTrackImporter(importActivity);
        } else {
            //KML or KMZ
            String extension = FileUtils.getExtension(file.getName());
            if (TrackFileFormat.KML_ONLY_TRACK.getExtension().equals(extension)) {
                trackImporter = new KmlFileTrackImporter(importActivity, -1L);
            } else {
                ContentProviderUtils contentProviderUtils = new ContentProviderUtils(importActivity);
                Uri uri = contentProviderUtils.insertTrack(new Track());
                long newId = Long.parseLong(uri.getLastPathSegment());

                trackImporter = new KmzTrackImporter(importActivity, newId, file.getUri());
            }
        }

        try (InputStream inputStream = importActivity.getContentResolver().openInputStream(file.getUri())) {
            lastSuccessfulTrackId = trackImporter.importFile(inputStream);
            return lastSuccessfulTrackId != -1L;
        } catch (IOException e) {
            Log.e(TAG, "Unable to import file", e);
            return false;
        }
    }
}
