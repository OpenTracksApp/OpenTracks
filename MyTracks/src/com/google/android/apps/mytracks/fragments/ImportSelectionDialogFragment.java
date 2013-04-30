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

package com.google.android.apps.mytracks.fragments;

import com.google.android.apps.mytracks.io.file.TrackFileFormat;
import com.google.android.maps.mytracks.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

/**
 * A DialogFragment to select an import type, gpx, kml, etc.
 * 
 * @author Jimmy Shih
 */
public class ImportSelectionDialogFragment extends DialogFragment {

  /**
   * Interface for caller of this dialog fragment.
   * 
   * @author Jimmy Shih
   */
  public interface ImportSelectionCaller {

    /**
     * Called when import selection is done.
     */
    public void onImportSelectionDone(TrackFileFormat trackFileFormat);
  }

  public static final String IMPORT_SELECTION_DIALOG_TAG = "importSelection";

  private ImportSelectionCaller caller;

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    try {
      caller = (ImportSelectionCaller) activity;
    } catch (ClassCastException e) {
      throw new ClassCastException(
          activity.toString() + " must implement " + ImportSelectionCaller.class.getSimpleName());
    }
  }

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    String[] choices = new String[2];
    String fileTypes[] = getResources().getStringArray(R.array.file_types);
    choices[0] = getString(R.string.import_selection_option, fileTypes[0]);
    choices[1] = getString(R.string.import_selection_option, fileTypes[1]);

    return new AlertDialog.Builder(getActivity()).setNegativeButton(R.string.generic_cancel, null)
        .setPositiveButton(R.string.generic_ok, new OnClickListener() {
            @Override
          public void onClick(DialogInterface dialog, int which) {
            int position = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
            caller.onImportSelectionDone(position == 0 ? TrackFileFormat.GPX : TrackFileFormat.KML);
          }
        }).setSingleChoiceItems(choices, 0, null).setTitle(R.string.import_selection_title)
        .create();
  }
}