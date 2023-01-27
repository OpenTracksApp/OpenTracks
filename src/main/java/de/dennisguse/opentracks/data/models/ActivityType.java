package de.dennisguse.opentracks.data.models;

import de.dennisguse.opentracks.R;

public enum ActivityType {
    AIRPLANE("AIRPLANE", R.drawable.ic_activity_flight_24dp, R.string.activity_type_airplane, R.string.activity_type_commercial_airplane, R.string.activity_type_rc_airplane),
    BIKE("BIKE", R.drawable.ic_activity_bike_24dp, R.string.activity_type_biking, R.string.activity_type_cycling, R.string.activity_type_dirt_bike, R.string.activity_type_road_biking, R.string.activity_type_track_cycling),
    MOUNTAIN_BIKE("MOUNTAIN_BIKE", R.drawable.ic_activity_mtb_24dp, R.string.activity_type_mountain_biking),
    MOTOR_BIKE("MOTOR_BIKE", R.drawable.ic_activity_motorbike_24dp, R.string.activity_type_motor_bike),
    KAYAK("KAYAK", R.drawable.ic_activity_kayaking_24dp, R.string.activity_type_kayaking),
    BOAT("BOAT", R.drawable.ic_activity_boat_24dp, R.string.activity_type_boat, R.string.activity_type_ferry, R.string.activity_type_motor_boating, R.string.activity_type_rc_boat),
    SAILING("SAILING", R.drawable.ic_activity_sailing_24dp, R.string.activity_type_sailing),
    DRIVE("DRIVE", R.drawable.ic_activity_drive_24dp, R.string.activity_type_atv, R.string.activity_type_driving, R.string.activity_type_driving_bus, R.string.activity_type_driving_car),
    RUN("RUN", R.drawable.ic_activity_run_24dp, R.string.activity_type_running, R.string.activity_type_street_running, R.string.activity_type_track_running, R.string.activity_type_trail_running),
    SKI("SKI", R.drawable.ic_activity_skiing_24dp, R.string.activity_type_cross_country_skiing, R.string.activity_type_skiing),
    SNOW_BOARDING("SNOW_BOARDING", R.drawable.ic_activity_snowboarding_24dp, R.string.activity_type_snow_boarding),
    UNKNOWN("UNKNOWN", R.drawable.ic_logo_24dp, R.string.activity_type_unknown),
    WALK("WALK", R.drawable.ic_activity_walk_24dp, R.string.activity_type_hiking, R.string.activity_type_off_trail_hiking, R.string.activity_type_speed_walking, R.string.activity_type_trail_hiking, R.string.activity_type_walking),
    ESCOOTER("ESCOOTER", R.drawable.ic_activity_escooter_24dp, R.string.activity_type_escooter),
    INLINE_SKATING("INLINES_SKATING", R.drawable.ic_activity_inline_skating_24dp, R.string.activity_type_inline_skating),
    SKATE_BOARDING("SKATE_BOARDING", R.drawable.ic_activity_skateboarding_24dp, R.string.activity_type_skate_boarding),
    CLIMBING("CLIMBING", R.drawable.ic_activity_climbing_24dp, R.string.activity_type_climbing),
    SWIMMING("SWIMMING", R.drawable.ic_activity_swimming_24dp, R.string.activity_type_swimming),
    SWIMMING_OPEN("SWIMMING_OPEN", R.drawable.ic_activity_swimming_open_24dp, R.string.activity_type_swimming_open),
    WORKOUT("WORKOUT", R.drawable.ic_activity_workout_24dp, R.string.activity_type_workout);

    final String id;
    final int[] localizedStringIds;
    final int iconId;

    //isSpeed?

    ActivityType(java.lang.String id, int iconId, int... localizedStringIds) {
        this.id = id;
        this.localizedStringIds = localizedStringIds;
        this.iconId = iconId;
    }

    public String getId() {
        return id;
    }

    public int getIconId() {
        return iconId;
    }

    public int[] getLocalizedStringIds() {
        return localizedStringIds;
    }

    public int getFirstLocalizedStringId() {
        return localizedStringIds[0];
    }
}
