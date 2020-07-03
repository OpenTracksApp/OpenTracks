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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/**
 * An activity for deleting tracks.
 *
 * @author Jimmy Shih
 */
public class TrackDeleteActivity extends Activity {

    public static final String EXTRA_TRACK_IDS = "track_ids";

    private DeleteAsyncTask deleteAsyncTask;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.track_delete);

        setResult(RESULT_CANCELED);

        Intent intent = getIntent();
        long[] trackIds = intent.getLongArrayExtra(EXTRA_TRACK_IDS);

        Object retained = getLastNonConfigurationInstance();
        if (retained instanceof DeleteAsyncTask) {
            deleteAsyncTask = (DeleteAsyncTask) retained;
            deleteAsyncTask.setActivity(this);
        } else {
            deleteAsyncTask = new DeleteAsyncTask(this, trackIds);
            deleteAsyncTask.execute();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        deleteAsyncTask.cancel(true);
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        deleteAsyncTask.setActivity(null);
        return deleteAsyncTask;
    }

    public void onAsyncTaskCompleted() {
        setResult(RESULT_OK);
        finish();
    }
}
