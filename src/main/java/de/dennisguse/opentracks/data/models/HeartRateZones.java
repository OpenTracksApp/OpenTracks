package de.dennisguse.opentracks.data.models;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.ui.util.ThemeUtils;

public class HeartRateZones {

    private final HeartRate max;

    public HeartRateZones(@NonNull HeartRate max) {
        this.max = max;
    }

    public int getTextColorForZone(Context context, HeartRate current) {
        if (current != null) {
            if (current.getBPM() >= max.getBPM() * 0.9) {
                return ContextCompat.getColor(context, R.color.heart_rate_zone_textcolor_5);
            }
            if (current.getBPM() >= max.getBPM() * 0.8) {
                return ContextCompat.getColor(context, R.color.heart_rate_zone_textcolor_4);
            }
            if (current.getBPM() >= max.getBPM() * 0.7) {
                return ContextCompat.getColor(context, R.color.heart_rate_zone_textcolor_3);
            }
            if (current.getBPM() >= max.getBPM() * 0.6) {
                return ContextCompat.getColor(context, R.color.heart_rate_zone_textcolor_2);
            }
        }

        // Zone 1
        return ThemeUtils.getTextColorPrimary(context);
    }
}
