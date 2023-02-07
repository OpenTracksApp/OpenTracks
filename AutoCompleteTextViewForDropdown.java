package de.dennisguse.opentracks.ui.util;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.textfield.MaterialAutoCompleteTextView;

// https://github.com/material-components/material-components-android/issues/1464
public class AutoCompleteTextViewForDropdown extends MaterialAutoCompleteTextView {

    public AutoCompleteTextViewForDropdown(@NonNull final Context context, @Nullable final AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    @Override
    public boolean getFreezesText() {
        return false;
    }
}
