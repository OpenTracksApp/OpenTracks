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
 * A DialogFragment to confirm an action.
 * 
 * @author Jimmy Shih
 */
public class ConfirmDialogFragment extends DialogFragment {

  /**
   * Interface for caller of this dialog fragment.
   * 
   * @author Jimmy Shih
   */
  public interface ConfirmCaller {

    /**
     * Called when confirm is done.
     */
    public void onConfirmDone(int confirmId, long trackId);
  }

  public static final String CONFIRM_DIALOG_TAG = "confirmDialog";

  private static final String KEY_CONFIRM_ID = "confirmId";
  private static final String KEY_DEFAULT_VALUE = "defaultValue";
  private static final String KEY_MESSAGE = "message";
  private static final String KEY_TRACK_ID = "trackId";

  private CheckBox checkBox;

  public static ConfirmDialogFragment newInstance(
      int confirmId, boolean defaultValue, CharSequence message, long trackId) {
    Bundle bundle = new Bundle();
    bundle.putInt(KEY_CONFIRM_ID, confirmId);
    bundle.putBoolean(KEY_DEFAULT_VALUE, defaultValue);
    bundle.putCharSequence(KEY_MESSAGE, message);
    bundle.putLong(KEY_TRACK_ID, trackId);

    ConfirmDialogFragment confirmDialogFragment = new ConfirmDialogFragment();
    confirmDialogFragment.setArguments(bundle);
    return confirmDialogFragment;
  }

  private ConfirmCaller caller;
  private FragmentActivity fragmentActivity;

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    try {
      caller = (ConfirmCaller) activity;
    } catch (ClassCastException e) {
      throw new ClassCastException(
          activity.toString() + " must implement " + ConfirmCaller.class.getSimpleName());
    }
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    int confirmId = getArguments().getInt(KEY_CONFIRM_ID);
    boolean defaultValue = getArguments().getBoolean(KEY_DEFAULT_VALUE);
    fragmentActivity = getActivity();
    if (!PreferencesUtils.getBoolean(fragmentActivity, confirmId, defaultValue)) {
      long trackId = getArguments().getLong(KEY_TRACK_ID);
      dismiss();
      caller.onConfirmDone(confirmId, trackId);
    }
  }

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    View view = fragmentActivity.getLayoutInflater().inflate(R.layout.confirm_dialog, null);
    TextView textView = (TextView) view.findViewById(R.id.confirm_dialog_message);
    textView.setText(getArguments().getCharSequence(KEY_MESSAGE));
    checkBox = (CheckBox) view.findViewById(R.id.confirm_dialog_check_box);

    return new AlertDialog.Builder(fragmentActivity).setNegativeButton(
        R.string.generic_no, null)
        .setPositiveButton(R.string.generic_yes, new DialogInterface.OnClickListener() {
            @Override
          public void onClick(DialogInterface dialog, int which) {
            int confirmId = getArguments().getInt(KEY_CONFIRM_ID);
            long trackId = getArguments().getLong(KEY_TRACK_ID);
            PreferencesUtils.setBoolean(fragmentActivity, confirmId, !checkBox.isChecked());
            caller.onConfirmDone(confirmId, trackId);
          }
        }).setTitle(R.string.generic_confirm_title).setView(view).create();
  }
}