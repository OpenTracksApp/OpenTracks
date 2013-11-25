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

package com.google.android.apps.mytracks.settings;

import com.google.android.apps.mytracks.util.DialogUtils;
import com.google.android.apps.mytracks.util.PreferencesUtils;
import com.google.android.apps.mytracks.util.TrackIconUtils;
import com.google.android.maps.mytracks.R;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Spinner;

/**
 * For entering the default activity type.
 * 
 * @author Jimmy Shih
 */
public class ActivityTypePreference extends DialogPreference {

  private RecordingSettingsActivity recordingSettingsActivity;
  private AutoCompleteTextView textView;
  private Spinner spinner;

  public ActivityTypePreference(Context context, AttributeSet attrs) {
    super(context, attrs);
    setDialogLayoutResource(R.layout.activity_type_preference);
    setPositiveButtonText(android.R.string.ok);
    setNegativeButtonText(android.R.string.cancel);
    setDialogIcon(null);
    setPersistent(false);
  }

  @Override
  protected View onCreateDialogView() {
    View view = super.onCreateDialogView();
    textView = (AutoCompleteTextView) view.findViewById(R.id.activity_type_preference_text_view);
    spinner = (Spinner) view.findViewById(R.id.activity_type_preference_spinner);

    String category = PreferencesUtils.getString(
        getContext(), R.string.default_activity_key, PreferencesUtils.DEFAULT_ACTIVITY_DEFAULT);
    textView.setText(category);

    ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
        getContext(), R.array.activity_types, android.R.layout.simple_dropdown_item_1line);
    textView.setAdapter(adapter);
    textView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
        @Override
      public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
        String iconValue = TrackIconUtils.getIconValue(
            getContext(), (String) textView.getAdapter().getItem(position));
        TrackIconUtils.setIconSpinner(spinner, iconValue);
      }
    });
    textView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
        @Override
      public void onFocusChange(View v, boolean hasFocus) {
        if (!hasFocus) {
          String iconValue = TrackIconUtils.getIconValue(
              getContext(), textView.getText().toString());
          TrackIconUtils.setIconSpinner(spinner, iconValue);
        }
      }
    });

    String iconValue = TrackIconUtils.getIconValue(getContext(), category);
    spinner.setAdapter(TrackIconUtils.getIconSpinnerAdapter(getContext(), iconValue));
    spinner.setOnTouchListener(new View.OnTouchListener() {
        @Override
      public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
          recordingSettingsActivity.showChooseActivityTypeDialog();
        }
        return true;
      }
    });
    spinner.setOnKeyListener(new View.OnKeyListener() {
        @Override
      public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
          recordingSettingsActivity.showChooseActivityTypeDialog();
        }
        return true;
      }
    });
    return view;
  }

  @Override
  protected void showDialog(Bundle state) {
    super.showDialog(state);
    DialogUtils.setDialogTitleDivider(getContext(), getDialog());
  }

  @Override
  protected void onDialogClosed(boolean positiveResult) {
    if (positiveResult) {
      String value = textView.getText().toString();
      if (callChangeListener(value)) {
        PreferencesUtils.setString(getContext(), R.string.default_activity_key, value);
      }
    }
  }

  @Override
  protected void onRestoreInstanceState(Parcelable state) {
    super.onRestoreInstanceState(state);
    String iconValue = TrackIconUtils.getIconValue(getContext(), textView.getText().toString());
    TrackIconUtils.setIconSpinner(spinner, iconValue);
    textView.clearFocus();
  }

  /**
   * Sets the recording settings activity.
   * 
   * @param activity the activity
   */
  public void setRecordingSettingsActivity(RecordingSettingsActivity activity) {
    recordingSettingsActivity = activity;
  }

  /**
   * Updates the value of the dialog.
   * 
   * @param iconValue the icon value
   */
  public void updateValue(String iconValue) {
    TrackIconUtils.setIconSpinner(spinner, iconValue);
    textView.setText(
        recordingSettingsActivity.getString(TrackIconUtils.getIconActivityType(iconValue)));
    textView.clearFocus();
  }
}
