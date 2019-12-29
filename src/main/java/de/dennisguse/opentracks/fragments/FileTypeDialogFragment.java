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

package de.dennisguse.opentracks.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import java.util.Locale;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.io.file.TrackFileFormat;

/**
 * A DialogFragment to select a file type, gpx, kml, etc.
 *
 * @author Jimmy Shih
 */
public class FileTypeDialogFragment extends DialogFragment {

    private static final String FILE_TYPE_DIALOG_TAG = "fileType";
    private int titleId;

    private FileTypeCaller caller;
    private int optionId;

    public FileTypeDialogFragment(int titleId, int optionId) {
        this.titleId = titleId;
        this.optionId = optionId;
    }

    public static void showDialog(FragmentManager fragmentManager, int titleId, int optionId) {
        new FileTypeDialogFragment(titleId, optionId).show(fragmentManager, FILE_TYPE_DIALOG_TAG);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            caller = (FileTypeCaller) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context + " must implement " + FileTypeCaller.class.getSimpleName());
        }
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final TrackFileFormat[] trackFileFormats = {TrackFileFormat.KMZ_WITH_TRACKDETAIL_AND_SENSORDATA_AND_PICTURES, TrackFileFormat.KML_WITH_TRACKDETAIL_AND_SENSORDATA, TrackFileFormat.GPX};
        String[] choices = new String[trackFileFormats.length];
        for (int i = 0; i < choices.length; i++) {
            TrackFileFormat trackFileFormat = trackFileFormats[i];
            String trackFileFormatUpperCase = trackFileFormat.getExtension().toUpperCase(Locale.US); //ASCII upper case
            choices[i] = getString(optionId, trackFileFormatUpperCase);
        }
        return new AlertDialog.Builder(getActivity())
                .setNegativeButton(R.string.generic_cancel, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        onDismissed();
                    }
                })
                .setPositiveButton(R.string.generic_ok, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        int position = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                        caller.onFileTypeDone(trackFileFormats[position]);
                    }
                })
                .setSingleChoiceItems(choices, 0, null)
                .setTitle(titleId)
                .create();
    }

    @Override
    public void onCancel(@NonNull DialogInterface dialog) {
        onDismissed();
    }

    private void onDismissed() {
        dismiss();
        caller.onDismissed();
    }

    /**
     * Interface for caller of this dialog fragment.
     *
     * @author Jimmy Shih
     */
    public interface FileTypeCaller {

        /**
         * Called when file type selection is done.
         */
        void onFileTypeDone(TrackFileFormat trackFileFormat);

        void onDismissed();
    }
}