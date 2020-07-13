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
import android.os.Bundle;
import android.os.PowerManager;
import android.view.View;

import de.dennisguse.opentracks.content.provider.ContentProviderUtils;
import de.dennisguse.opentracks.util.SystemUtils;

/**
 * An activity for deleting tracks.
 *
 * @author Jimmy Shih
 */
public class TrackDeleteActivity extends AbstractActivity {

    public static final String EXTRA_TRACK_IDS = "track_ids";

    private long[] trackIds;

    private Thread deleteThread;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setResult(RESULT_CANCELED);

        Intent intent = getIntent();
        trackIds = intent.getLongArrayExtra(EXTRA_TRACK_IDS);
        deleteThread = new Thread(() -> {
            ContentProviderUtils contentProviderUtils = new ContentProviderUtils(TrackDeleteActivity.this);

            PowerManager.WakeLock wakeLock = SystemUtils.acquireWakeLock(TrackDeleteActivity.this, null);

            for (long id : trackIds) {
                if (Thread.interrupted()) {
                    break;
                }
                contentProviderUtils.deleteTrack(TrackDeleteActivity.this, id);
            }

            wakeLock = SystemUtils.releaseWakeLock(wakeLock);
            if (Thread.interrupted()) {
                return;
            }

            runOnUiThread(TrackDeleteActivity.this::onAsyncTaskCompleted);
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        deleteThread.start();
    }

    @Override
    protected void onStop() {
        super.onStop();
        deleteThread.interrupt();
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.track_delete;
    }

    public void onAsyncTaskCompleted() {
        findViewById(R.id.progressbar).setVisibility(View.INVISIBLE);
        setResult(RESULT_OK);
        finish();
    }
}
