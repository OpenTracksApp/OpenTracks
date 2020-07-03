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

package de.dennisguse.opentracks;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;

import de.dennisguse.opentracks.content.provider.ContentProviderUtils;

/**
 * An activity for deleting tracks.
 *
 * @author Jimmy Shih
 */
public class TrackDeleteActivity extends AbstractActivity {

    public static final String EXTRA_TRACK_IDS = "track_ids";

    private long[] trackIds;

    private DeleteAsyncTask deleteAsyncTask;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setResult(RESULT_CANCELED);

        Intent intent = getIntent();
        trackIds = intent.getLongArrayExtra(EXTRA_TRACK_IDS);

        deleteAsyncTask = new DeleteAsyncTask();
    }

    @Override
    protected void onStart() {
        super.onStart();
        deleteAsyncTask.execute();
    }

    @Override
    protected void onStop() {
        super.onStop();
        deleteAsyncTask.cancel(true);
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.track_delete;
    }

    public void onAsyncTaskCompleted() {
        setResult(RESULT_OK);
        finish();
    }

    class DeleteAsyncTask extends AsyncTask<Void, Integer, Boolean> {

        @Override
        protected Boolean doInBackground(Void... params) {
            ContentProviderUtils contentProviderUtils = new ContentProviderUtils(TrackDeleteActivity.this);

            for (long id : trackIds) {
                if (isCancelled()) {
                    return false;
                }
                contentProviderUtils.deleteTrack(TrackDeleteActivity.this, id);
            }

            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            TrackDeleteActivity.this.onAsyncTaskCompleted();
        }
    }
}
