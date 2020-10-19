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

package de.dennisguse.opentracks.io.file.importer;

import android.content.ClipData;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import de.dennisguse.opentracks.fragments.ImportFinishedDialogFragment;
import de.dennisguse.opentracks.fragments.ImportProgressDialogFragment;

/**
 * An activity to import files from the external storage.
 *
 * @author Rodrigo Damazio
 */
public class ImportActivity extends FragmentActivity implements ImportProgressDialogFragment.DismissCallback, ImportFinishedDialogFragment.DismissCallback {

    private static final String TAG = ImportActivity.class.getSimpleName();

    public static final String EXTRA_DIRECTORY_URI_KEY = "directory_uri";

    private static final String BUNDLE_ACTIVITY_RECREATED = "activity_recreated";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null || !savedInstanceState.getBoolean(BUNDLE_ACTIVITY_RECREATED, false)) {
            if (getIntent().getData() != null) {
                ImportProgressDialogFragment.showDialog(getSupportFragmentManager(), getIntent().getData(), false);
            } else if (getIntent().getClipData().getItemCount() > 0) {
                ImportProgressDialogFragment.showDialog(getSupportFragmentManager(), getIntent().getClipData().getItemAt(0).getUri(), false);
            } else {
                // Started from DirectoryChooserActivity
                ImportProgressDialogFragment.showDialog(getSupportFragmentManager(), getIntent().getParcelableExtra(EXTRA_DIRECTORY_URI_KEY), true);
            }
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(BUNDLE_ACTIVITY_RECREATED, true);
    }

    @Override
    public void onImportCanceled(String directoryDisplayName, int successCount, int fileCount) {
        onImportFinished(directoryDisplayName, successCount, fileCount);
    }

    @Override
    public void onImportFinished(final String directoryDisplayName, final int successCount, final int fileCount) {
        runOnUiThread(() -> ImportFinishedDialogFragment.showDialog(getSupportFragmentManager(), directoryDisplayName, successCount, fileCount));
    }

    @Override
    public void onDismissed() {
        finish();
    }
}
