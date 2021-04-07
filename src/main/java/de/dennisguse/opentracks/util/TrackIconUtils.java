/*
 * Copyright 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package de.dennisguse.opentracks.util;

import android.content.Context;
import android.util.Pair;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import de.dennisguse.opentracks.R;

/**
 * Utilities for track icon.
 *
 * @author Jimmy Shih
 */
public class TrackIconUtils {

    private static final String AIRPLANE = "AIRPLANE";
    private static final String BIKE = "BIKE";
    private static final String MOUNTAIN_BIKE = "MOUNTAIN_BIKE";
    private static final String MOTOR_BIKE = "MOTOR_BIKE";
    private static final String KAYAK = "KAYAK";
    private static final String BOAT = "BOAT";
    private static final String SAILING = "SAILING";
    private static final String DRIVE = "DRIVE";
    private static final String RUN = "RUN";
    private static final String SKI = "SKI";
    private static final String SNOW_BOARDING = "SNOW_BOARDING";
    private static final String UNKNOWN = "UNKNOWN";
    private static final String WALK = "WALK";
    private static final String ESCOOTER = "ESCOOTER";
    private static final String INLINE_SKATING = "INLINES_SKATING";
    private static final String SKATE_BOARDING = "SKATE_BOARDING";
    private static final String CLIMBING = "CLIMBING";

    private static final int ACTIVITY_UNKNOWN_LOGO = R.drawable.ic_logo_24dp;

    private static final int[] AIRPLANE_LIST = new int[]{R.string.activity_type_airplane, R.string.activity_type_commercial_airplane, R.string.activity_type_rc_airplane};
    private static final int[] BIKE_LIST = new int[]{R.string.activity_type_biking, R.string.activity_type_cycling, R.string.activity_type_dirt_bike, R.string.activity_type_road_biking, R.string.activity_type_track_cycling};
    private static final int[] BOAT_LIST = new int[]{R.string.activity_type_boat, R.string.activity_type_ferry, R.string.activity_type_motor_boating, R.string.activity_type_rc_boat};
    private static final int[] CLIMBING_LIST = new int[]{R.string.activity_type_climbing};
    private static final int[] DRIVE_LIST = new int[]{R.string.activity_type_atv, R.string.activity_type_driving, R.string.activity_type_driving_bus, R.string.activity_type_driving_car};
    private static final int[] MOTOR_BIKE_LIST = new int[]{R.string.activity_type_motor_bike};
    private static final int[] MOUNTAIN_BIKE_LIST = new int[]{R.string.activity_type_mountain_biking};
    private static final int[] ESCOOTER_LIST = new int[]{R.string.activity_type_escooter};
    private static final int[] INLINE_SKATING_LIST = new int[]{R.string.activity_type_inline_skating};
    private static final int[] RUN_LIST = new int[]{R.string.activity_type_running, R.string.activity_type_street_running, R.string.activity_type_track_running, R.string.activity_type_trail_running};
    private static final int[] SAILING_LIST = new int[]{R.string.activity_type_sailing};
    private static final int[] SKI_LIST = new int[]{R.string.activity_type_cross_country_skiing, R.string.activity_type_skiing};
    private static final int[] SNOW_BOARDING_LIST = new int[]{R.string.activity_type_snow_boarding};
    private static final int[] SKATE_BOARDING_LIST = new int[]{R.string.activity_type_skate_boarding};
    private static final int[] KAYAKING_LIST = new int[]{R.string.activity_type_kayaking};
    private static final int[] WALK_LIST = new int[]{R.string.activity_type_hiking, R.string.activity_type_off_trail_hiking, R.string.activity_type_speed_walking, R.string.activity_type_trail_hiking, R.string.activity_type_walking};

    // List of icons whose sports associated use speed (in km/h or mi/h).
    private static final int[] SPEED_ICON = {
            // Unknown.
            R.string.activity_type_unknown,
            // All airplane categories.
            R.string.activity_type_airplane, R.string.activity_type_commercial_airplane, R.string.activity_type_rc_airplane,
            // All bike categories.
            R.string.activity_type_biking, R.string.activity_type_cycling, R.string.activity_type_dirt_bike, R.string.activity_type_motor_bike, R.string.activity_type_mountain_biking, R.string.activity_type_road_biking, R.string.activity_type_track_cycling, R.string.activity_type_inline_skating,
            // All boat categories.
            R.string.activity_type_boat, R.string.activity_type_ferry, R.string.activity_type_motor_boating, R.string.activity_type_rc_boat, R.string.activity_type_sailing, R.string.activity_type_kayaking,
            // All drive categories.
            R.string.activity_type_atv, R.string.activity_type_driving, R.string.activity_type_driving_bus, R.string.activity_type_driving_car, R.string.activity_type_escooter, R.string.activity_type_skate_boarding,
            // All wintersport categories
            R.string.activity_type_skiing, R.string.activity_type_snow_boarding

    };

    private static final LinkedHashMap<String, Pair<Integer, Integer>> MAP = new LinkedHashMap<>();

    static {
        //Reflects order in ChooseActivityTypeDialogFragmentActivity
        MAP.put(UNKNOWN, new Pair<>(R.string.activity_type_unknown, ACTIVITY_UNKNOWN_LOGO));
        MAP.put(RUN, new Pair<>(R.string.activity_type_running, R.drawable.ic_activity_run_24dp));
        MAP.put(WALK, new Pair<>(R.string.activity_type_walking, R.drawable.ic_activity_walk_24dp));
        MAP.put(CLIMBING, new Pair<>(R.string.activity_type_climbing, R.drawable.ic_activity_climbing_24dp));
        MAP.put(SKATE_BOARDING, new Pair<>(R.string.activity_type_skate_boarding, R.drawable.ic_activity_skateboarding_24dp));
        MAP.put(INLINE_SKATING, new Pair<>(R.string.activity_type_inline_skating, R.drawable.ic_activity_inline_skating_24dp));
        MAP.put(SNOW_BOARDING, new Pair<>(R.string.activity_type_snow_boarding, R.drawable.ic_activity_snowboarding_24dp));
        MAP.put(SKI, new Pair<>(R.string.activity_type_skiing, R.drawable.ic_activity_skiing_24dp));
        MAP.put(ESCOOTER, new Pair<>(R.string.activity_type_escooter, R.drawable.ic_activity_escooter_24dp));
        MAP.put(BIKE, new Pair<>(R.string.activity_type_biking, R.drawable.ic_activity_bike_24dp));
        MAP.put(MOUNTAIN_BIKE, new Pair<>(R.string.activity_type_mountain_biking, R.drawable.ic_activity_mtb_24dp));
        MAP.put(MOTOR_BIKE, new Pair<>(R.string.activity_type_motor_bike, R.drawable.ic_activity_motorbike_24dp));
        MAP.put(DRIVE, new Pair<>(R.string.activity_type_driving, R.drawable.ic_activity_drive_24dp));
        MAP.put(AIRPLANE, new Pair<>(R.string.activity_type_airplane, R.drawable.ic_activity_flight_24dp));
        MAP.put(KAYAK, new Pair<>(R.string.activity_type_kayaking, R.drawable.ic_activity_kayaking_24dp));
        MAP.put(BOAT, new Pair<>(R.string.activity_type_boat, R.drawable.ic_activity_boat_24dp));
        MAP.put(SAILING, new Pair<>(R.string.activity_type_sailing, R.drawable.ic_activity_sailing_24dp));
    }

    private TrackIconUtils() {
    }

    /**
     * Gets the icon drawable.
     *
     * @param iconValue the icon value
     */
    public static int getIconDrawable(String iconValue) {
        if (iconValue == null || iconValue.equals("")) {
            return ACTIVITY_UNKNOWN_LOGO;
        }
        Pair<Integer, Integer> pair = MAP.get(iconValue);
        return pair == null ? ACTIVITY_UNKNOWN_LOGO : pair.second;
    }

    /**
     * Gets the icon activity type.
     *
     * @param iconValue the icon value
     */
    public static int getIconActivityType(String iconValue) {
        if (iconValue == null || iconValue.equals("")) {
            return R.string.activity_type_unknown;
        }
        Pair<Integer, Integer> pair = MAP.get(iconValue);
        return pair == null ? R.string.activity_type_unknown : pair.first;
    }

    /**
     * Gets all icon values.
     */
    public static List<String> getAllIconValues() {
        return new ArrayList<>(MAP.keySet());
    }

    /**
     * Gets the icon value.
     *
     * @param context      the context
     * @param activityType the activity type
     */
    @NonNull
    public static String getIconValue(Context context, String activityType) {
        if (activityType == null || activityType.equals("")) {
            return UNKNOWN;
        }
        if (inList(context, activityType, AIRPLANE_LIST)) {
            return AIRPLANE;
        }
        if (inList(context, activityType, BIKE_LIST)) {
            return BIKE;
        }
        if (inList(context, activityType, MOUNTAIN_BIKE_LIST)) {
            return MOUNTAIN_BIKE;
        }
        if (inList(context, activityType, CLIMBING_LIST)) {
            return CLIMBING;
        }
        if (inList(context, activityType, MOTOR_BIKE_LIST)) {
            return MOTOR_BIKE;
        }
        if (inList(context, activityType, KAYAKING_LIST)) {
            return KAYAK;
        }
        if (inList(context, activityType, BOAT_LIST)) {
            return BOAT;
        }
        if (inList(context, activityType, SAILING_LIST)) {
            return SAILING;
        }
        if (inList(context, activityType, DRIVE_LIST)) {
            return DRIVE;
        }
        if (inList(context, activityType, INLINE_SKATING_LIST)) {
            return INLINE_SKATING;
        }
        if (inList(context, activityType, ESCOOTER_LIST)) {
            return ESCOOTER;
        }
        if (inList(context, activityType, RUN_LIST)) {
            return RUN;
        }
        if (inList(context, activityType, SKI_LIST)) {
            return SKI;
        }
        if (inList(context, activityType, SNOW_BOARDING_LIST)) {
            return SNOW_BOARDING;
        }
        if (inList(context, activityType, SKATE_BOARDING_LIST)) {
            return SKATE_BOARDING;
        }
        if (inList(context, activityType, WALK_LIST)) {
            return WALK;
        }
        return UNKNOWN;
    }

    /**
     * Returns true if the activity type is in the list.
     *
     * @param context      the context
     * @param activityType the activity type
     * @param list         the list
     */
    private static boolean inList(Context context, String activityType, int[] list) {
        for (int i : list) {
            if (context.getString(i).equals(activityType)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if category is in the SPEED_ICON array. Otherwise returns false.
     *
     * @param context  the context.
     * @param category the name of the category, activity type.
     */
    public static boolean isSpeedIcon(Context context, String category) {
        return inList(context, category, SPEED_ICON);
    }
}
