package de.dennisguse.opentracks.settings;

import androidx.annotation.NonNull;
import androidx.preference.Preference;

public class HackUtils {

    private HackUtils() {
    }

    /**
     * Triggers a redraw of the summary of a preference if it was set programmatically.
     * Need to trigger androidx.preference.preferences.notifyChanged() to trigger a redraw, but method is protected.
     * This workaround also works when this preference has not changed, but it's entries (see R.string.stats_rate_key).
     * TODO
     */
    public static void invalidatePreference(@NonNull Preference preference) {
        boolean isEnabled = preference.isEnabled();
        preference.setVisible(!isEnabled);
        preference.setVisible(isEnabled);
    }
}
