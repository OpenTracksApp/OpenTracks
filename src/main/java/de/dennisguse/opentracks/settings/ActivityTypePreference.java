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

package de.dennisguse.opentracks.settings;

import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;

import androidx.preference.DialogPreference;
import androidx.preference.PreferenceDialogFragmentCompat;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.data.models.ActivityType;
import de.dennisguse.opentracks.fragments.ChooseActivityTypeDialogFragment;

/**
 * For entering the default activity type.
 *
 * @author Jimmy Shih
 */
public class ActivityTypePreference extends DialogPreference {

    public ActivityTypePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setDialogLayoutResource(R.layout.preference_activity_type);
        setPositiveButtonText(android.R.string.ok);
        setNegativeButtonText(android.R.string.cancel);
        setDialogIcon(null);
        setPersistent(true);

        SummaryProvider<DialogPreference> summaryProvider = preference -> PreferencesUtils.getDefaultActivityTypeLocalized();
        setSummaryProvider(summaryProvider);
    }

    @Override
    public int getDialogLayoutResource() {
        // TODO PreferenceActivityTypeBinding
        return R.layout.preference_activity_type;
    }

    public static class ActivityPreferenceDialog extends PreferenceDialogFragmentCompat {

        private AutoCompleteTextView textView;
        private ImageView iconView;

        static ActivityPreferenceDialog newInstance(String preferenceKey) {
            ActivityTypePreference.ActivityPreferenceDialog dialog = new ActivityTypePreference.ActivityPreferenceDialog();
            final Bundle bundle = new Bundle(1);
            bundle.putString(PreferenceDialogFragmentCompat.ARG_KEY, preferenceKey);
            dialog.setArguments(bundle);

            return dialog;
        }

        @Override
        protected void onBindDialogView(View view) {
            super.onBindDialogView(view);

            final Context context = getActivity();

            textView = view.findViewById(R.id.activity_type_preference_text_view);
            String activityTypeLocalized = PreferencesUtils.getDefaultActivityTypeLocalized();
            textView.setText(activityTypeLocalized);
            ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_dropdown_item_1line, ActivityType.getLocalizedStrings(context));
            textView.setAdapter(adapter);
            textView.setOnItemClickListener((parent, v, position, id) -> {
                String localizedActivityType = (String) textView.getAdapter().getItem(position);
                ActivityType activityType = ActivityType.findByLocalizedString(context, localizedActivityType);
                updateIcon(activityType);
            });
            textView.setOnFocusChangeListener((v, hasFocus) -> {
                if (!hasFocus) {
                    String localizedActivityType = textView.getText().toString();
                    ActivityType activityType = ActivityType.findByLocalizedString(context, localizedActivityType);
                    updateIcon(activityType);
                }
            });

            iconView = view.findViewById(R.id.activity_type_preference_spinner);
            iconView.setOnClickListener((it) -> showIconSelectDialog());

            updateIcon(ActivityType.findByLocalizedString(context, activityTypeLocalized));
        }

        private void showIconSelectDialog() {
            String category = PreferencesUtils.getDefaultActivityTypeLocalized();
            ChooseActivityTypeDialogFragment.showDialog(getActivity().getSupportFragmentManager(), getContext(), category);
        }

        @Override
        public void onDialogClosed(boolean positiveResult) {
            if (positiveResult) {
                String newDefaultActivity = textView.getText().toString();
                if (getPreference().callChangeListener(newDefaultActivity)) {
                    PreferencesUtils.setDefaultActivityLocalized(newDefaultActivity);
                    HackUtils.invalidatePreference(getPreference());
                }
            }
        }

        public void updateUI(ActivityType activityType) {
            updateIcon(activityType);
            textView.setText(getActivity().getString(activityType.getLocalizedStringId()));
            textView.clearFocus();
        }

        private void updateIcon(ActivityType activityType) {
            iconView.setImageResource(activityType.getIconDrawableId());
        }
    }
}
