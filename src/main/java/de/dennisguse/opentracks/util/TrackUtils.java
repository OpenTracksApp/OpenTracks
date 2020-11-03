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

import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.content.provider.ContentProviderUtils;

/**
 * Utilities for updating track.
 *
 * @author Jimmy Shih
 */
public class TrackUtils {

    private TrackUtils() {
    }

    public static void updateTrack(Context context, Track track, String name, String category, String description, ContentProviderUtils contentProviderUtils) {
        boolean update = false;
        if (name != null) {
            track.setName(name);
            update = true;
        }
        if (category != null) {
            track.setCategory(category);
            track.setIcon(TrackIconUtils.getIconValue(context, category));
            update = true;
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
