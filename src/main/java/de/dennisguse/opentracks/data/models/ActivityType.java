package de.dennisguse.opentracks.data.models;

import android.content.Context;
import android.content.res.Resources;

import androidx.annotation.NonNull;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import de.dennisguse.opentracks.R;

public enum ActivityType {

    AIRPLANE("airplane", R.string.activity_type_airplane, ActivityIcon.AIRPLANE, true),
    ATV("ATV", R.string.activity_type_atv, ActivityIcon.DRIVE, true),
    BIKING("biking", R.string.activity_type_biking, ActivityIcon.BIKE, true),
    BLIMP("blimp", R.string.activity_type_blimp, ActivityIcon.UNKNOWN, true),
    BOAT("boat", R.string.activity_type_boat, ActivityIcon.BOAT, true),
    CLIMBING("climbing", R.string.activity_type_climbing, ActivityIcon.CLIMBING, false),
    COMMERCIAL_AIRPLANE("commercial airplane", R.string.activity_type_commercial_airplane, ActivityIcon.AIRPLANE, true),
    CROSS_COUNTRY_SKIING("cross-country skiing", R.string.activity_type_cross_country_skiing, ActivityIcon.SKI, true),
    CYCLING("cycling", R.string.activity_type_cycling, ActivityIcon.BIKE, true),
    DIRT_BIKE("dirt bike", R.string.activity_type_dirt_bike, ActivityIcon.MOTOR_BIKE, true),
    DONKEY_BACK_RIDING("donkey back riding", R.string.activity_type_donkey_back_riding, ActivityIcon.UNKNOWN, true),
    DRIVING("driving", R.string.activity_type_driving, ActivityIcon.DRIVE, true),
    DRIVING_BUS("driving bus", R.string.activity_type_driving_bus, ActivityIcon.DRIVE, true),
    DRIVING_CAR("driving car", R.string.activity_type_driving_car, ActivityIcon.DRIVE, true),
    ESCOOTER("escooter", R.string.activity_type_escooter, ActivityIcon.ESCOOTER, true),
    FERRY("ferry", R.string.activity_type_ferry, ActivityIcon.BOAT, true),
    FRISBEE("frisbee", R.string.activity_type_frisbee, ActivityIcon.UNKNOWN, true),
    GLIDING("gliding", R.string.activity_type_gliding, ActivityIcon.UNKNOWN, true),
    HANG_GLIDING("hang gliding", R.string.activity_type_hang_gliding, ActivityIcon.UNKNOWN, true),
    HELICOPTER("helicopter", R.string.activity_type_helicopter, ActivityIcon.UNKNOWN, true),
    HIKING("hiking", R.string.activity_type_hiking, ActivityIcon.WALK, false),
    HORSE_BACK_RIDING("horse back riding", R.string.activity_type_horse_back_riding, ActivityIcon.UNKNOWN, true),
    HOT_AIR_BALLOON("hot air balloon", R.string.activity_type_hot_air_balloon, ActivityIcon.UNKNOWN, true),
    ICE_SAILING("ice sailing", R.string.activity_type_ice_sailing, ActivityIcon.UNKNOWN, true),
    INLINE_SKATING("inline skating", R.string.activity_type_inline_skating, ActivityIcon.INLINE_SKATING, true),
    KAYAKING("kayaking", R.string.activity_type_kayaking, ActivityIcon.KAYAK, true),
    KITE_SURFING("kite surfing", R.string.activity_type_kite_surfing, ActivityIcon.UNKNOWN, true),
    LAND_SAILING("land sailing", R.string.activity_type_land_sailing, ActivityIcon.UNKNOWN, true),
    MIXED_TYPE("mixed type", R.string.activity_type_mixed_type, ActivityIcon.UNKNOWN, true),
    MOTOR_BIKE("motor bike", R.string.activity_type_motor_bike, ActivityIcon.MOTOR_BIKE, true),
    MOTOR_BOATING("motor boating", R.string.activity_type_motor_boating, ActivityIcon.BOAT, true),
    MOUNTAIN_BIKING("mountain biking", R.string.activity_type_mountain_biking, ActivityIcon.MOUNTAIN_BIKE, true),
    OFF_TRAIL_HIKING("off trail hiking", R.string.activity_type_hiking, ActivityIcon.WALK, false),
    OTHER("other", R.string.activity_type_other, ActivityIcon.UNKNOWN, true),
    PADDLING("paddling", R.string.activity_type_paddling, ActivityIcon.UNKNOWN, true),
    PARA_GLIDING("para gliding", R.string.activity_type_para_gliding, ActivityIcon.UNKNOWN, true),
    RC_AIRPLANE("RC airplane", R.string.activity_type_rc_airplane, ActivityIcon.AIRPLANE, true),
    RC_BOAT("RC boat", R.string.activity_type_rc_boat, ActivityIcon.BOAT, true),
    RC_HELICOPTER("RC helicopter", R.string.activity_type_rc_helicopter, ActivityIcon.UNKNOWN, true),
    RIDING("riding", R.string.activity_type_horse_back_riding, ActivityIcon.UNKNOWN, true),
    ROAD_BIKING("road biking", R.string.activity_type_road_biking, ActivityIcon.BIKE, true),
    ROLLER_SKIING("roller skiing", R.string.activity_type_roller_skiing, ActivityIcon.UNKNOWN, true),
    ROWING("rowing", R.string.activity_type_rowing, ActivityIcon.UNKNOWN, true),
    RUNNING("running", R.string.activity_type_running, ActivityIcon.RUN, false),
    SAILING("sailing", R.string.activity_type_sailing, ActivityIcon.SAILING, true),
    SEAPLANE("seaplane", R.string.activity_type_seaplane, ActivityIcon.AIRPLANE, true),
    SKATE_BOARDING("skate boarding", R.string.activity_type_skate_boarding, ActivityIcon.SKATE_BOARDING, true),
    SKATING("skating", R.string.activity_type_skating, ActivityIcon.UNKNOWN, true),
    SKIING("skiing", R.string.activity_type_skiing, ActivityIcon.SKI, true),
    SKY_JUMPING("sky jumping", R.string.activity_type_sky_jumping, ActivityIcon.UNKNOWN, true),
    SLED("sled", R.string.activity_type_sled, ActivityIcon.UNKNOWN, true),
    SNOW_BOARDING("snow boarding", R.string.activity_type_snow_boarding, ActivityIcon.SNOW_BOARDING, true),
    SNOW_SHOEING("snow shoeing", R.string.activity_type_snow_shoeing, ActivityIcon.UNKNOWN, true),
    SPEED_WALKING("speed walking", R.string.activity_type_speed_walking, ActivityIcon.WALK, false),
    STREET_RUNNING("street running", R.string.activity_type_street_running, ActivityIcon.RUN, false),
    SURFING("surfing", R.string.activity_type_surfing, ActivityIcon.UNKNOWN, true),
    TRACK_CYCLING("track cycling", R.string.activity_type_track_cycling, ActivityIcon.BIKE, true),
    TRACK_RUNNING("track running", R.string.activity_type_trail_running, ActivityIcon.RUN, false),
    TRAIL_HIKING("trail hiking", R.string.activity_type_trail_hiking, ActivityIcon.WALK, false),
    TRAIL_RUNNING("trail running", R.string.activity_type_trail_running, ActivityIcon.RUN, false),
    TRAIN("train", R.string.activity_type_train, ActivityIcon.UNKNOWN, true),
    ULTIMATE_FRISBEE("ultimate frisbee", R.string.activity_type_ultimate_frisbee, ActivityIcon.UNKNOWN, true),
    WAKEBOARDING("wakeboarding", R.string.activity_type_wakeboarding, ActivityIcon.UNKNOWN, true),
    WALKING("walking", R.string.activity_type_walking, ActivityIcon.WALK, false),
    WATER_SKIING("water skiing", R.string.activity_type_water_skiing, ActivityIcon.UNKNOWN, true),
    WIND_SURFING("wind surfing", R.string.activity_type_wind_surfing, ActivityIcon.UNKNOWN, true),
    SWIMMING("swimming", R.string.activity_type_swimming, ActivityIcon.SWIMMING, false),
    SWIMMING_OPEN("swimming in open water", R.string.activity_type_swimming_open, ActivityIcon.SWIMMING_OPEN, false),
    WORKOUT("workout", R.string.activity_type_workout, ActivityIcon.WORKOUT, false),
    UNKNOWN("unknown", R.string.activity_type_unknown, ActivityIcon.UNKNOWN, true);

    final String id;

    final ActivityIcon icon;
    final boolean showSpeedPreferred;
    final int localizedStringId;

    ActivityType(String id, int localizedStringId, ActivityIcon icon, boolean showSpeedPreferred) {
        this.id = id;
        this.localizedStringId = localizedStringId;
        this.icon = icon;
        this.showSpeedPreferred = showSpeedPreferred;
    }

    public String getId() {
        return id;
    }

    @Deprecated
    public String getIconId() {
        return icon.iconId;
    }

    public int getIconDrawableId() {
        return icon.iconDrawableId;
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
    public static ActivityType findBy(String iconId) {
        if (ActivityIcon.UNKNOWN.iconId.equals(iconId)) {
            return ActivityType.UNKNOWN;
        }
        return Arrays.stream(ActivityType.values()).filter(
                        it -> it.icon.iconId.equals(iconId)
                ).findFirst()
                .orElse(ActivityType.UNKNOWN);
    }

    public static ActivityType findByLocalizedString(Context context, String localizedActivityType) {
        return findByLocalizedString(context.getResources(), localizedActivityType);
    }

    public static ActivityType findByLocalizedString(Resources resources, String localizedActivityType) {
        return Arrays.stream(ActivityType.values())
                .filter(it -> resources.getString(it.getLocalizedStringId()).equals(localizedActivityType))
                .findFirst()
                .orElse(ActivityType.UNKNOWN);
    }
}
