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

import de.dennisguse.opentracks.data.models.ActivityType;

/**
 * Utilities for track icon.
 *
 * @author Jimmy Shih
 */
public class TrackIconUtils {

    public static int getIconDrawableId(String activityTypeId) {
        return ActivityType.findByActivityTypeId(activityTypeId)
                .getIconDrawableId();
    }

    public static int getIconActivityType(String activityTypeId) {
        return ActivityType.findByActivityTypeId(activityTypeId)
                .getFirstLocalizedStringId();
    }

    @NonNull
    public static String getActivityTypeId(Context context, String localizedActivityType) {
        return ActivityType.findByLocalizedString(context, localizedActivityType)
                .getId();
    }

    public static boolean isSpeedIcon(Resources resources, String localizedActivityType) {
        return ActivityType.findByLocalizedString(resources, localizedActivityType)
                .isShowSpeedPreferred();
    }
}
