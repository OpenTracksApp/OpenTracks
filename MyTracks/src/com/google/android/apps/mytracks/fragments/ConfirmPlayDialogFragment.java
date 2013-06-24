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

package com.google.android.apps.mytracks.fragments;

import com.google.android.apps.mytracks.util.PreferencesUtils;
import com.google.android.maps.mytracks.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

/**
 * A DialogFragment to confirm playing a track.
 * 
 * @author Jimmy Shih
 */
public class ConfirmPlayDialogFragment extends DialogFragment {

  /**
   * Interface for caller of this dialog fragment.
   * 
   * @author Jimmy Shih
   */
  public interface ConfirmPlayCaller {

    /**
     * Called when confirm play is done.
     */
    public void onConfirmPlayDone(long[] trackIds);
  }

  public static final String CONFIRM_PLAY_DIALOG_TAG = "confirmPlayDialog";

  private static final String KEY_TRACK_IDS = "trackIds";

  private CheckBox checkBox;

  public static ConfirmPlayDialogFragment newInstance(long[] trackIds) {
    Bundle bundle = new Bundle();
    bundle.putLongArray(KEY_TRACK_IDS, trackIds);

    ConfirmPlayDialogFragment confirmDialogFragment = new ConfirmPlayDialogFragment();
    confirmDialogFragment.setArguments(bundle);
    return confirmDialogFragment;
  }

  private ConfirmPlayCaller caller;
  private FragmentActivity fragmentActivity;

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    try {
      caller = (ConfirmPlayCaller) activity;
    } catch (ClassCastException e) {
      throw new ClassCastException(
          activity.toString() + " must implement " + ConfirmPlayCaller.class.getSimpleName());
    }
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    fragmentActivity = getActivity();
  }

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    View view = fragmentActivity.getLayoutInflater().inflate(R.layout.confirm_dialog, null);
    TextView textView = (TextView) view.findViewById(R.id.confirm_dialog_message);
    textView.setText(R.string.track_detail_play_confirm_message);
    checkBox = (CheckBox) view.findViewById(R.id.confirm_dialog_check_box);

    return new AlertDialog.Builder(fragmentActivity).setNegativeButton(R.string.generic_no, null)
        .setPositiveButton(R.string.generic_yes, new DialogInterface.OnClickListener() {
            @Override
          public void onClick(DialogInterface dialog, int which) {
            long[] trackIds = getArguments().getLongArray(KEY_TRACK_IDS);
            PreferencesUtils.setBoolean(
                fragmentActivity, R.string.confirm_play_earth_key, !checkBox.isChecked());
            caller.onConfirmPlayDone(trackIds);
          }
        }).setTitle(R.string.generic_confirm_title).setView(view).create();
  }
}