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

import com.google.android.apps.mytracks.util.TrackIconUtils;
import com.google.android.maps.mytracks.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;

import java.util.ArrayList;
import java.util.List;

/**
 * A DialogFragment to choose an activity type.
 *
 * @author apoorvn
 */
public class ChooseActivityTypeDialogFragment extends DialogFragment {

  /**
   * Interface for caller of this dialog fragment.
   *
   * @author apoorvn
   */
  public interface ChooseActivityTypeCaller {

    /**
     * Called when choose activity type is done.
     */
    public void onChooseActivityTypeDone(String iconValue);
  }

  public static final String CHOOSE_ACTIVITY_TYPE_DIALOG_TAG = "chooseActivityType";

  private ChooseActivityTypeCaller caller;

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    try {
      caller = (ChooseActivityTypeCaller) activity;
    } catch (ClassCastException e) {
      throw new ClassCastException(activity.toString() + " must implement "
          + ChooseActivityTypeCaller.class.getSimpleName());
    }
  }

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    GridView gridView =
        (GridView) getActivity().getLayoutInflater().inflate(R.layout.choose_activity_type, null);

    final List<String> iconValues = TrackIconUtils.getAllIconValues();
    List<Integer> imageIds = new ArrayList<Integer>();
    for (String iconValue : iconValues) {
      imageIds.add(TrackIconUtils.getIconDrawable(iconValue));
    }

    Options options = new BitmapFactory.Options();
    options.inJustDecodeBounds = true;
    BitmapFactory.decodeResource(getResources(), R.drawable.ic_track_airplane, options);
    int padding = 32;
    int width = options.outWidth + 2 * padding;
    int height = options.outHeight + 2 * padding;
    gridView.setColumnWidth(width);

    ChooseActivityTypeImageAdapter imageAdapter =
        new ChooseActivityTypeImageAdapter(getActivity(), imageIds, width, height, padding);
    gridView.setAdapter(imageAdapter);
    gridView.setOnItemClickListener(new OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
        dismiss();
        caller.onChooseActivityTypeDone(iconValues.get(position));
      }
    });

    return new AlertDialog.Builder(getActivity()).setNegativeButton(R.string.generic_cancel, null)
        .setTitle(R.string.track_edit_activity_type_hint).setView(gridView).create();
  }
}
