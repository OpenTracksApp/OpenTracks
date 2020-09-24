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

package de.dennisguse.opentracks.fragments;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import de.dennisguse.opentracks.R;

public class ExportFinishedDialogFragment extends DialogFragment {

    private static final String TAG = ExportFinishedDialogFragment.class.getSimpleName();

    private static final String EXTRA_DIRECTORY_NAME_KEY = "directory_name";
    private static final String EXTRA_EXPORTED_TRACK_COUNT = "exported_track_count";
    private static final String EXTRA_TOTAL_TRACK_COUNT = "";

    private static final String FINISHED_DIALOG_TAG = "finished_dialog_tag";

    private DismissCallback caller;

    public static void showDialog(FragmentManager fragmentManager, String directoryDisplayName, int successCount, int fileCount) {
        Bundle bundle = new Bundle();
        bundle.putString(EXTRA_DIRECTORY_NAME_KEY, directoryDisplayName);
        bundle.putInt(EXTRA_EXPORTED_TRACK_COUNT, successCount);
        bundle.putInt(EXTRA_TOTAL_TRACK_COUNT, fileCount);

        ExportFinishedDialogFragment dialogFragment = new ExportFinishedDialogFragment();
        dialogFragment.setArguments(bundle);
        dialogFragment.show(fragmentManager, FINISHED_DIALOG_TAG);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            caller = (DismissCallback) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context + " must implement " + DismissCallback.class.getSimpleName());
        }
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        String directoryDisplayName = getArguments().getString(EXTRA_DIRECTORY_NAME_KEY);
        int exportedTracksCount = getArguments().getInt(EXTRA_EXPORTED_TRACK_COUNT);
        int totalTracksCount = getArguments().getInt(EXTRA_TOTAL_TRACK_COUNT);

        int iconId;
        int titleId;
        String message;
        String totalTracks = getResources().getQuantityString(R.plurals.tracks, totalTracksCount, totalTracksCount);
        if (exportedTracksCount == totalTracksCount) {
            iconId = R.drawable.ic_dialog_success_24dp;
            titleId = R.string.generic_success_title;
            message = getString(
                    R.string.export_success, totalTracks, directoryDisplayName);
        } else {
            iconId = R.drawable.ic_dialog_error_24dp;
            titleId = R.string.generic_error_title;
            message = getString(R.string.export_error, exportedTracksCount, totalTracks, directoryDisplayName);
        }
        Dialog alertDialog = new AlertDialog.Builder(getContext()).setCancelable(true)
                .setIcon(iconId).setMessage(message)
                .setPositiveButton(R.string.generic_ok, (dialog, arg1) -> {
                    dialog.dismiss();
                    caller.onDismissed();
                }).setTitle(titleId)
                .create();

        alertDialog.setCanceledOnTouchOutside(false);
        return alertDialog;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        caller = null;
    }

    public interface DismissCallback {
        void onDismissed();
    }
}