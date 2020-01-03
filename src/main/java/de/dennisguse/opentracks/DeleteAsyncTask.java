package de.dennisguse.opentracks;

/*
 * Copyright 2013 Google Inc.
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

import android.content.Context;
import android.os.AsyncTask;

import de.dennisguse.opentracks.content.provider.ContentProviderUtils;

/**
 * Async Task to delete tracks.
 *
 * @author Jimmy Shih
 */
public class DeleteAsyncTask extends AsyncTask<Void, Integer, Boolean> {

    private final long[] trackIds;
    private final Context context;
    private TrackDeleteActivity deleteActivity;
    // true if the AsyncTask has completed
    private boolean completed;

    /**
     * Creates an AsyncTask.
     *
     * @param deleteActivity the activity currently associated with this task
     * @param trackIds       the track ids to delete. To delete all, set to size 1 with
     *                       trackIds[0] == -1L
     */
    public DeleteAsyncTask(TrackDeleteActivity deleteActivity, long[] trackIds) {
        this.deleteActivity = deleteActivity;
        this.trackIds = trackIds;
        context = deleteActivity.getApplicationContext();
        completed = false;
    }

    /**
     * Sets the current activity associated with this AyncTask.
     *
     * @param deleteActivity the current activity, can be null
     */
    public void setActivity(TrackDeleteActivity deleteActivity) {
        this.deleteActivity = deleteActivity;
        if (completed && deleteActivity != null) {
            deleteActivity.onAsyncTaskCompleted();
        }
    }

    @Override
    protected void onPreExecute() {
        if (deleteActivity != null) {
            deleteActivity.showProgressDialog();
        }
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        ContentProviderUtils contentProviderUtils = new ContentProviderUtils(context);

        if (trackIds.length == 1 && trackIds[0] == -1L) {
            contentProviderUtils.deleteAllTracks(context);
        } else {
            for (long id : trackIds) {
                if (isCancelled()) {
                    return false;
                }
                contentProviderUtils.deleteTrack(context, id);
            }
        }
        return true;
    }

    @Override
    protected void onPostExecute(Boolean result) {
        completed = true;
        if (deleteActivity != null) {
            deleteActivity.onAsyncTaskCompleted();
        }
    }
}
