package de.dennisguse.opentracks.services;

import de.dennisguse.opentracks.R;

public enum GpsStatusValue {
    GPS_NONE(R.drawable.ic_gps_off_24dp),
    GPS_ENABLED(R.drawable.ic_gps_not_fixed_24dp),
    GPS_DISABLED(R.drawable.ic_gps_off_24dp),
    GPS_FIRST_FIX(R.drawable.ic_gps_fixed_24dp),
    GPS_SIGNAL_BAD(R.drawable.ic_gps_fixed_24dp),
    GPS_SIGNAL_LOST(R.drawable.ic_gps_not_fixed_24dp),
    GPS_SIGNAL_OKAY(R.drawable.ic_gps_fixed_24dp);

    public final int icon;

    GpsStatusValue(int icon) {
        this.icon = icon;
    }
}
