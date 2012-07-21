package com.google.android.apps.mytracks;

import com.google.android.apps.mytracks.util.PreferencesUtils;
import com.google.android.maps.mytracks.R;

import android.content.Context;
import android.preference.EditTextPreference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

/**
 * The {@link AutoCompleteTextPreference} class is a preference that allows for
 * string input using auto complete . It is a subclass of
 * {@link EditTextPreference} and shows the {@link AutoCompleteTextView} in a
 * dialog.
 * <p>
 * This preference will store a string into the SharedPreferences.
 * 
 * @author Rimas Trumpa (with Matt Levan)
 */
public class AutoCompleteTextPreference extends EditTextPreference {

  private AutoCompleteTextView mEditText = null;

  public AutoCompleteTextPreference(Context context, AttributeSet attrs) {
    super(context, attrs);
    mEditText = new AutoCompleteTextView(context, attrs);
    mEditText.setThreshold(0);

    // Gets autocomplete values for 'Default Activity' preference
    if (PreferencesUtils.getKey(context, R.string.default_activity_key).equals(getKey())) {
      ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
          context, R.array.activity_types, android.R.layout.simple_dropdown_item_1line);
      mEditText.setAdapter(adapter);
    }
  }

  @Override
  protected void onBindDialogView(View view) {
    AutoCompleteTextView editText = mEditText;
    editText.setText(getText());

    ViewParent oldParent = editText.getParent();
    if (oldParent != view) {
      if (oldParent != null) {
        ((ViewGroup) oldParent).removeView(editText);
      }
      onAddEditTextToDialogView(view, editText);
    }
  }

  @Override
  protected void onDialogClosed(boolean positiveResult) {
    if (positiveResult) {
      String value = mEditText.getText().toString();
      if (callChangeListener(value)) {
        setText(value);
      }
    }
  }
}
