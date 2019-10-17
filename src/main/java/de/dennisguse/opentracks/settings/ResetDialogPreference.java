package de.dennisguse.opentracks.settings;

import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;
import android.widget.Toast;

import androidx.preference.DialogPreference;
import androidx.preference.PreferenceDialogFragmentCompat;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.util.PreferencesUtils;

public class ResetDialogPreference extends DialogPreference {

    public ResetDialogPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public static class ResetPreferenceDialog extends PreferenceDialogFragmentCompat {

        static PreferenceDialogFragmentCompat newInstance(String preferenceKey) {
            ResetPreferenceDialog dialog = new ResetPreferenceDialog();
            final Bundle bundle = new Bundle(1);
            bundle.putString(PreferenceDialogFragmentCompat.ARG_KEY, preferenceKey);
            dialog.setArguments(bundle);

            return dialog;
        }

        @Override
        public void onDialogClosed(boolean positiveResult) {
            if (positiveResult) {
                PreferencesUtils.resetPreferences(getActivity(), true);

                Toast.makeText(getActivity(), R.string.settings_reset_done, Toast.LENGTH_SHORT).show();
            }
        }
    }
}
