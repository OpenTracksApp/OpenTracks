package de.dennisguse.opentracks.data.models;

import de.dennisguse.opentracks.R;

public enum ActivityIcon {
    AIRPLANE("AIRPLANE", R.drawable.ic_activity_flight_24dp),
    BIKE("BIKE", R.drawable.ic_activity_bike_24dp),
    MOUNTAIN_BIKE("MOUNTAIN_BIKE", R.drawable.ic_activity_mtb_24dp),
    MOTOR_BIKE("MOTOR_BIKE", R.drawable.ic_activity_motorbike_24dp),
    KAYAK("KAYAK", R.drawable.ic_activity_kayaking_24dp),
    BOAT("BOAT", R.drawable.ic_activity_boat_24dp),
    SAILING("SAILING", R.drawable.ic_activity_sailing_24dp),
    DRIVE("DRIVE", R.drawable.ic_activity_drive_24dp),
    RUN("RUN", R.drawable.ic_activity_run_24dp),
    SKI("SKI", R.drawable.ic_activity_skiing_24dp),
    SNOW_BOARDING("SNOW_BOARDING", R.drawable.ic_activity_snowboarding_24dp),
    UNKNOWN("UNKNOWN", R.drawable.ic_logo_24dp),
    WALK("WALK", R.drawable.ic_activity_walk_24dp),
    ESCOOTER("ESCOOTER", R.drawable.ic_activity_escooter_24dp),
    KICKSCOOTER("KICKSCOOTER", R.drawable.ic_activity_scooter_24dp),
    INLINE_SKATING("INLINES_SKATING", R.drawable.ic_activity_inline_skating_24dp),
    SKATE_BOARDING("SKATE_BOARDING", R.drawable.ic_activity_skateboarding_24dp),
    CLIMBING("CLIMBING", R.drawable.ic_activity_climbing_24dp),
    SWIMMING("SWIMMING", R.drawable.ic_activity_swimming_24dp),
    SWIMMING_OPEN("SWIMMING_OPEN", R.drawable.ic_activity_swimming_open_24dp),
    WORKOUT("WORKOUT", R.drawable.ic_activity_workout_24dp);

    @Deprecated //TODO should be removed.
    final String iconId;
    final int iconDrawableId;

    ActivityIcon(String iconId, int iconDrawableId) {
        this.iconId = iconId;
        this.iconDrawableId = iconDrawableId;
    }
}
