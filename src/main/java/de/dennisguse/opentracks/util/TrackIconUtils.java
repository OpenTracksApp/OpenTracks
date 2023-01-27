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
import android.content.res.Resources;

import androidx.annotation.NonNull;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.data.models.ActivityType;

/**
 * Utilities for track icon.
 *
 * @author Jimmy Shih
 */
public class TrackIconUtils {

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

    private static final List<ActivityType> MAP = List.of(
            //Reflects order in ChooseActivityTypeDialogFragmentActivity
            ActivityType.UNKNOWN,
            ActivityType.RUN,
            ActivityType.WALK,
            ActivityType.CLIMBING,
            ActivityType.SKATE_BOARDING,
            ActivityType.INLINE_SKATING,
            ActivityType.SNOW_BOARDING,
            ActivityType.SKI,
            ActivityType.ESCOOTER,
            ActivityType.BIKE,
            ActivityType.MOUNTAIN_BIKE,
            ActivityType.MOTOR_BIKE,
            ActivityType.DRIVE,
            ActivityType.AIRPLANE,
            ActivityType.KAYAK,
            ActivityType.BOAT,
            ActivityType.SAILING,
            ActivityType.SWIMMING,
            ActivityType.SWIMMING_OPEN,
            ActivityType.WORKOUT
    );

    private TrackIconUtils() {
    }

    public static int getIconDrawable(String activityTypeId) {
        Optional<ActivityType> found = Arrays.stream(ActivityType.values()).filter(
                it -> it.getId().equals(activityTypeId)
        ).findFirst();

        if (found.isEmpty()) {
            return ActivityType.UNKNOWN.getIconId();
        }

        return found.get().getIconId();
    }

    public static int getIconActivityType(String activityTypeId) {
        Optional<ActivityType> found = Arrays.stream(ActivityType.values()).filter(
                it -> it.getId().equals(activityTypeId)
        ).findFirst();

        if (found.isEmpty()) {
            return ActivityType.UNKNOWN.getFirstLocalizedStringId();
        }

        return found.get().getFirstLocalizedStringId();
    }

    /**
     * Gets all icon values.
     */
    public static List<String> getAllIconValues() {
        return Arrays.stream(ActivityType.values())
                .map(ActivityType::getId)
                .collect(Collectors.toList());
    }

    /**
     * Gets the icon value.
     *
     * @param context        the context
     * @param activityTypeId the activity type
     */
    @NonNull
    public static String getIconValue(Context context, String activityTypeId) {
        Optional<ActivityType> selected = Arrays.stream(ActivityType.values())
                .filter(
                        it -> Arrays.stream(it.getLocalizedStringIds())
                                .anyMatch(id -> context.getString(id).equals(activityTypeId))
                )
                .findFirst();
        if (selected.isEmpty()) {
            return ActivityType.UNKNOWN.getId();
        }
        return selected.get().getId();
    }

    /**
     * Returns true if the activity type is in the list.
     *
     * @param context      the context
     * @param activityType the activity type
     * @param list         the list
     */
    private static boolean inList(Resources context, String activityType, int[] list) {
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
     * @param category the name of the category, activity type.
     */
    public static boolean isSpeedIcon(Resources resources, String category) {
        return inList(resources, category, SPEED_ICON);
    }
}
