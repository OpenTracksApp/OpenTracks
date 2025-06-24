package de.dennisguse.opentracks.data.models;

import android.content.Context;
import android.content.res.Resources;

import androidx.annotation.NonNull;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import de.dennisguse.opentracks.R;

public enum ActivityType {

    AIRPLANE("airplane", R.string.activity_type_airplane, R.drawable.ic_activity_flight_24dp, true),
    ATV("ATV", R.string.activity_type_atv,  R.drawable.ic_activity_drive_24dp, true),
    BIKING("biking", R.string.activity_type_biking, R.drawable.ic_activity_bike_24dp, true),
    BLIMP("blimp", R.string.activity_type_blimp, ActivityIcon.ICON_UNKNOWN, true),
    BOAT("boat", R.string.activity_type_boat, R.drawable.ic_activity_boat_24dp, true),
    CLIMBING("climbing", R.string.activity_type_climbing, R.drawable.ic_activity_climbing_24dp, false),
    COMMERCIAL_AIRPLANE("commercial airplane", R.string.activity_type_commercial_airplane, R.drawable.ic_activity_flight_24dp, true),
    CROSS_COUNTRY_SKIING("cross-country skiing", R.string.activity_type_cross_country_skiing, R.drawable.ic_activity_skiing_24dp, true),
    CYCLING("cycling", R.string.activity_type_cycling, R.drawable.ic_activity_bike_24dp, true),
    DIRT_BIKE("dirt bike", R.string.activity_type_dirt_bike, R.drawable.ic_activity_mtb_24dp, true),
    DONKEY_BACK_RIDING("donkey back riding", R.string.activity_type_donkey_back_riding, ActivityIcon.ICON_UNKNOWN, true),
    DRIVING("driving", R.string.activity_type_driving,  R.drawable.ic_activity_drive_24dp, true),
    DRIVING_BUS("driving bus", R.string.activity_type_driving_bus,  R.drawable.ic_activity_drive_24dp, true),
    DRIVING_CAR("driving car", R.string.activity_type_driving_car,  R.drawable.ic_activity_drive_24dp, true),
    ESCOOTER("escooter", R.string.activity_type_escooter, R.drawable.ic_activity_escooter_24dp, true),
    FERRY("ferry", R.string.activity_type_ferry, R.drawable.ic_activity_boat_24dp, true),
    FRISBEE("frisbee", R.string.activity_type_frisbee, ActivityIcon.ICON_UNKNOWN, true),
    GLIDING("gliding", R.string.activity_type_gliding, ActivityIcon.ICON_UNKNOWN, true),
    HANG_GLIDING("hang gliding", R.string.activity_type_hang_gliding, ActivityIcon.ICON_UNKNOWN, true),
    HELICOPTER("helicopter", R.string.activity_type_helicopter, ActivityIcon.ICON_UNKNOWN, true),
    HIKING("hiking", R.string.activity_type_hiking, R.drawable.ic_activity_walk_24dp, false),
    HORSE_BACK_RIDING("horse back riding", R.string.activity_type_horse_back_riding, ActivityIcon.ICON_UNKNOWN, true),
    HOT_AIR_BALLOON("hot air balloon", R.string.activity_type_hot_air_balloon, ActivityIcon.ICON_UNKNOWN, true),
    ICE_SAILING("ice sailing", R.string.activity_type_ice_sailing, ActivityIcon.ICON_UNKNOWN, true),
    INLINE_SKATING("inline skating", R.string.activity_type_inline_skating, R.drawable.ic_activity_inline_skating_24dp, true),
    KAYAKING("kayaking", R.string.activity_type_kayaking, R.drawable.ic_activity_kayaking_24dp, true),
    KITE_SURFING("kite surfing", R.string.activity_type_kite_surfing, ActivityIcon.ICON_UNKNOWN, true),
    LAND_SAILING("land sailing", R.string.activity_type_land_sailing, ActivityIcon.ICON_UNKNOWN, true),
    MIXED_TYPE("mixed type", R.string.activity_type_mixed_type, ActivityIcon.ICON_UNKNOWN, true),
    MOTOR_BIKE("motor bike", R.string.activity_type_motor_bike, R.drawable.ic_activity_mtb_24dp, true),
    MOTOR_BOATING("motor boating", R.string.activity_type_motor_boating, R.drawable.ic_activity_boat_24dp, true),
    MOUNTAIN_BIKING("mountain biking", R.string.activity_type_mountain_biking, R.drawable.ic_activity_mtb_24dp, true),
    OFF_TRAIL_HIKING("off trail hiking", R.string.activity_type_off_trail_hiking, R.drawable.ic_activity_walk_24dp, false),
    OTHER("other", R.string.activity_type_other, ActivityIcon.ICON_UNKNOWN, true),
    PADDLING("paddling", R.string.activity_type_paddling, ActivityIcon.ICON_UNKNOWN, true),
    PARA_GLIDING("para gliding", R.string.activity_type_para_gliding, ActivityIcon.ICON_UNKNOWN, true),
    RC_AIRPLANE("RC airplane", R.string.activity_type_rc_airplane, R.drawable.ic_activity_flight_24dp, true),
    RC_BOAT("RC boat", R.string.activity_type_rc_boat, R.drawable.ic_activity_boat_24dp, true),
    RC_HELICOPTER("RC helicopter", R.string.activity_type_rc_helicopter, ActivityIcon.ICON_UNKNOWN, true),
    RIDING("riding", R.string.activity_type_horse_back_riding, ActivityIcon.ICON_UNKNOWN, true),
    ROAD_BIKING("road biking", R.string.activity_type_road_biking, R.drawable.ic_activity_bike_24dp, true),
    ROLLER_SKIING("roller skiing", R.string.activity_type_roller_skiing, ActivityIcon.ICON_UNKNOWN, true),
    ROWING("rowing", R.string.activity_type_rowing, ActivityIcon.ICON_UNKNOWN, true),
    RUNNING("running", R.string.activity_type_running, R.drawable.ic_activity_run_24dp, false),
    SAILING("sailing", R.string.activity_type_sailing, R.drawable.ic_activity_sailing_24dp, true),
    KICKSCOOTER("kickscooter", R.string.activity_type_kickscooter, R.drawable.ic_activity_scooter_24dp, true),
    SEAPLANE("seaplane", R.string.activity_type_seaplane, R.drawable.ic_activity_flight_24dp, true),
    SKATE_BOARDING("skateboarding", R.string.activity_type_skate_boarding, R.drawable.ic_activity_skateboarding_24dp, true),
    SKATING("skating", R.string.activity_type_skating, ActivityIcon.ICON_UNKNOWN, true),
    SKIING("skiing", R.string.activity_type_skiing, R.drawable.ic_activity_skiing_24dp, true),
    SKY_JUMPING("sky jumping", R.string.activity_type_sky_jumping, ActivityIcon.ICON_UNKNOWN, true),
    SLED("sled", R.string.activity_type_sled, ActivityIcon.ICON_UNKNOWN, true),
    SNOW_BOARDING("snowboarding", R.string.activity_type_snow_boarding, R.drawable.ic_activity_snowboarding_24dp, true),
    SNOW_SHOEING("snow shoeing", R.string.activity_type_snow_shoeing, ActivityIcon.ICON_UNKNOWN, true),
    SPEED_WALKING("speed walking", R.string.activity_type_speed_walking, R.drawable.ic_activity_walk_24dp, false),
    STREET_RUNNING("street running", R.string.activity_type_street_running, R.drawable.ic_activity_run_24dp, false),
    SURFING("surfing", R.string.activity_type_surfing, ActivityIcon.ICON_UNKNOWN, true),
    TRACK_CYCLING("track cycling", R.string.activity_type_track_cycling, R.drawable.ic_activity_bike_24dp, true),
    TRACK_RUNNING("track running", R.string.activity_type_trail_running, R.drawable.ic_activity_run_24dp, false),
    TRAIL_HIKING("trail hiking", R.string.activity_type_trail_hiking, R.drawable.ic_activity_walk_24dp, false),
    TRAIL_RUNNING("trail running", R.string.activity_type_trail_running, R.drawable.ic_activity_run_24dp, false),
    TRAIN("train", R.string.activity_type_train, ActivityIcon.ICON_UNKNOWN, true),
    ULTIMATE_FRISBEE("ultimate frisbee", R.string.activity_type_ultimate_frisbee, ActivityIcon.ICON_UNKNOWN, true),
    WAKEBOARDING("wakeboarding", R.string.activity_type_wakeboarding, ActivityIcon.ICON_UNKNOWN, true),
    WALKING("walking", R.string.activity_type_walking, R.drawable.ic_activity_walk_24dp, false),
    WATER_SKIING("water skiing", R.string.activity_type_water_skiing, ActivityIcon.ICON_UNKNOWN, true),
    WIND_SURFING("wind surfing", R.string.activity_type_wind_surfing, ActivityIcon.ICON_UNKNOWN, true),
    SWIMMING("swimming", R.string.activity_type_swimming, R.drawable.ic_activity_swimming_24dp, false),
    SWIMMING_OPEN("swimming in open water", R.string.activity_type_swimming_open, R.drawable.ic_activity_swimming_open_24dp, false),
    WORKOUT("workout", R.string.activity_type_workout, R.drawable.ic_activity_workout_24dp, false),
    UNKNOWN("unknown", R.string.activity_type_unknown, ActivityIcon.ICON_UNKNOWN, true);

    final String id;

    final int iconDrawableId;
    final boolean showSpeedPreferred;
    final int localizedStringId;

    ActivityType(String id, int localizedStringId, int iconDrawableId, boolean showSpeedPreferred) {
        this.id = id;
        this.localizedStringId = localizedStringId;
        this.iconDrawableId = iconDrawableId;
        this.showSpeedPreferred = showSpeedPreferred;
    }

    public String getId() {
        return id;
    }

    public int getIconDrawableId() {
        return iconDrawableId;
    }

    public int getLocalizedStringId() {
        return localizedStringId;
    }

    public boolean isShowSpeedPreferred() {
        return showSpeedPreferred;
    }

    public static List<String> getLocalizedStrings(Context context) {
        return Arrays.stream(values())
                .map(ActivityType::getLocalizedStringId)
                .map(context::getString)
                .collect(Collectors.toList());
    }

    @NonNull
    public static ActivityType findBy(String activityTypeId) {
        return Arrays.stream(ActivityType.values()).filter(
                        it -> it.id.equals(activityTypeId)
                ).findFirst()
                .orElse(ActivityType.UNKNOWN);
    }

    @NonNull
    public static ActivityType findByLocalizedString(Context context, String localizedActivityType) {
        return findByLocalizedString(context.getResources(), localizedActivityType);
    }

    @NonNull
    public static ActivityType findByLocalizedString(Resources resources, String localizedActivityType) {
        return Arrays.stream(ActivityType.values())
                .filter(it -> resources.getString(it.getLocalizedStringId()).equals(localizedActivityType))
                .findFirst()
                .orElse(ActivityType.UNKNOWN);
    }
}
