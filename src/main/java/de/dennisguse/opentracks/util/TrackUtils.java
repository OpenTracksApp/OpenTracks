/*
 * Copyright 2013 Google Inc.
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

import de.dennisguse.opentracks.data.ContentProviderUtils;
import de.dennisguse.opentracks.data.models.ActivityType;
import de.dennisguse.opentracks.data.models.Track;

/**
 * Utilities for updating track.
 *
 * @author Jimmy Shih
 */
@Deprecated //TODO Refactor: all this should happen somewhere else (ContentProviderUtils?)
public class TrackUtils {

    private TrackUtils() {
    }

    public static void updateTrack(Context context, Track track, String name, String activityType, String description, ContentProviderUtils contentProviderUtils) {
        updateTrack(context, track, name, activityType, ActivityType.findByLocalizedString(context, activityType)
                .getId(), description, contentProviderUtils);
    }

    public static void updateTrack(Context context, Track track, String name, String activityType, String iconValue, String description, ContentProviderUtils contentProviderUtils) {
        boolean update = false;
        if (name != null) {
            track.setName(name);
            update = true;
        }
        if (activityType != null) {
            track.setActivityType(activityType);
            update = true;
        }
        if (iconValue != null) {
            track.setActivityTypeId(iconValue);
        } else if (activityType != null) {
            track.setActivityTypeId(ActivityType.findByLocalizedString(context, activityType)
                    .getId());
        }
        if (description != null) {
            track.setDescription(description);
            update = true;
        }
        if (update) {
            contentProviderUtils.updateTrack(track);
        }
    }
}
