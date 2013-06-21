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
import com.google.android.apps.mytracks.util.FileUtils;
import com.google.android.maps.mytracks.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

/**
 * A DialogFragment to select a file type, gpx, kml, etc.
 * 
 * @author Jimmy Shih
 */
public class FileTypeDialogFragment extends DialogFragment {

  /**
   * Interface for caller of this dialog fragment.
   * 
   * @author Jimmy Shih
   */
  public interface FileTypeCaller {

    /**
     * Called when file type selection is done.
     */
    public void onFileTypeDone(int menuId, TrackFileFormat trackFileFormat);
  }

  public static final String FILE_TYPE_DIALOG_TAG = "fileType";

  private static final String KEY_MENU_ID = "menuId";
  private static final String KEY_TITLE_ID = "titleId";
  private static final String KEY_OPTION_ID = "optionId";
  private static final String KEY_SIZE = "size";

  public static FileTypeDialogFragment newInstance(
      int menuId, int titleId, int optionId, int size) {
    Bundle bundle = new Bundle();
    bundle.putInt(KEY_MENU_ID, menuId);
    bundle.putInt(KEY_TITLE_ID, titleId);
    bundle.putInt(KEY_OPTION_ID, optionId);
    bundle.putInt(KEY_SIZE, size);

    FileTypeDialogFragment fileTypeDialogFragment = new FileTypeDialogFragment();
    fileTypeDialogFragment.setArguments(bundle);
    return fileTypeDialogFragment;
  }

  private FileTypeCaller caller;

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    try {
      caller = (FileTypeCaller) activity;
    } catch (ClassCastException e) {
      throw new ClassCastException(
          activity.toString() + " must implement " + FileTypeCaller.class.getSimpleName());
    }
  }

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    int size = getArguments().getInt(KEY_SIZE);
    int optionId = getArguments().getInt(KEY_OPTION_ID);
    final int titleId = getArguments().getInt(KEY_TITLE_ID);
    final int menuId = getArguments().getInt(KEY_MENU_ID);
    String[] choices = new String[size];
    TrackFileFormat[] trackFileFormats = TrackFileFormat.values();
    for (int i = 0; i < choices.length; i++) {
      TrackFileFormat trackFileFormat = trackFileFormats[i];
      choices[i] = getString(optionId, trackFileFormat.name(),
          FileUtils.getDirectoryDisplayName(trackFileFormat.getExtension()));
    }
    return new AlertDialog.Builder(getActivity()).setNegativeButton(R.string.generic_cancel, null)
        .setPositiveButton(R.string.generic_ok, new OnClickListener() {
            @Override
          public void onClick(DialogInterface dialog, int which) {
            int position = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
            caller.onFileTypeDone(menuId, TrackFileFormat.values()[position]);
          }
        }).setSingleChoiceItems(choices, 0, null).setTitle(titleId).create();
  }
}