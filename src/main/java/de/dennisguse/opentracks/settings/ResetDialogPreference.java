package de.dennisguse.opentracks.settings;

import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;
import androidx.preference.DialogPreference;
import androidx.preference.PreferenceDialogFragmentCompat;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.util.PreferencesUtils;

public class ResetDialogPreference extends DialogPreference {

    public ResetDialogPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    interface ResetCallback {
        void onReset();
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
            if (!positiveResult) {
                return;
            }

            FragmentActivity activity = getActivity();

            String preferenceKey = getArguments().getString(PreferenceDialogFragmentCompat.ARG_KEY);
            if (preferenceKey.equals(getString(R.string.settings_reset_key))) {
                PreferencesUtils.resetPreferences(activity, true);
                Toast.makeText(activity, R.string.settings_reset_done, Toast.LENGTH_SHORT).show();
            } else if (preferenceKey.equals(getString(R.string.settings_layout_reset_key))) {
                PreferencesUtils.resetCustomLayoutPreferences(activity);
                Toast.makeText(activity, R.string.settings_layout_reset_done, Toast.LENGTH_SHORT).show();
            }

            if (activity instanceof ResetCallback) {
                ((ResetCallback) activity).onReset();
            }
        }
    }
}
