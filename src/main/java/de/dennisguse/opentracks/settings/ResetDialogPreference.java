package de.dennisguse.opentracks.settings;

import android.content.Context;
import android.content.DialogInterface;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.widget.Toast;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.util.PreferencesUtils;

public class ResetDialogPreference extends DialogPreference {

    public ResetDialogPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        super.onClick(dialog, which);
        if (which == DialogInterface.BUTTON_POSITIVE) {
            PreferencesUtils.resetPreferences(getContext(), true);

            Toast.makeText(getContext(), R.string.settings_reset_done, Toast.LENGTH_SHORT).show();
        }
    }
}
