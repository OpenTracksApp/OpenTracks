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

package de.dennisguse.opentracks.io.file.exporter;

import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import de.dennisguse.opentracks.fragments.ExportFinishedDialogFragment;
import de.dennisguse.opentracks.fragments.ExportProgressDialogFragment;

/**
 * An activity for saving tracks to the external storage.
 *
 * @author Rodrigo Damazio
 */
public class ExportActivity extends FragmentActivity implements ExportProgressDialogFragment.DismissCallback, ExportFinishedDialogFragment.DismissCallback {

    public static final String EXTRA_DIRECTORY_URI_KEY = "directory_uri";

    public static final String EXTRA_TRACKFILEFORMAT_KEY = "trackfileformat";

    private static final String BUNDLE_ACTIVITY_RECREATED = "activity_recreated";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null || !savedInstanceState.getBoolean(BUNDLE_ACTIVITY_RECREATED, false)) {
            Uri directoryUri = getIntent().getParcelableExtra(EXTRA_DIRECTORY_URI_KEY);
            String trackfileformat = getIntent().getStringExtra(EXTRA_TRACKFILEFORMAT_KEY);
            ExportProgressDialogFragment.showDialog(getSupportFragmentManager(), trackfileformat, directoryUri);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(BUNDLE_ACTIVITY_RECREATED, true);
    }

    @Override
    public void onExportCanceled(String directoryDisplayName, int successCount, int fileCount) {
        onExportFinished(directoryDisplayName, successCount, fileCount);
    }

    @Override
    public void onExportFinished(final String directoryDisplayName, final int successCount, final int fileCount) {
        runOnUiThread(() -> {
            ExportFinishedDialogFragment.showDialog(getSupportFragmentManager(), directoryDisplayName, successCount, fileCount);
        });
    }

    @Override
    public void onDismissed() {
        finish();
    }
}
