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

import de.dennisguse.opentracks.data.models.ActivityType;

/**
 * Utilities for track icon.
 *
 * @author Jimmy Shih
 */
public class TrackIconUtils {

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

    public static boolean isSpeedIcon(Resources resources, String activityTypeId) {
        Optional<ActivityType> selected = Arrays.stream(ActivityType.values())
                .filter(
                        it -> Arrays.stream(it.getLocalizedStringIds())
                                .anyMatch(id -> resources.getString(id).equals(activityTypeId))
                )
                .findFirst();

        if (selected.isEmpty()) {
            return false;
        }

        return selected.get().isShowSpeedPreferred();
    }
}
