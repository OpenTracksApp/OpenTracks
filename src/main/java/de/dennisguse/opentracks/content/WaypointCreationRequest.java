/*
 * Copyright 2010 Google Inc.
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
package de.dennisguse.opentracks.content;

import de.dennisguse.opentracks.content.Waypoint.WaypointType;

/**
 * A request for the service to create a waypoint at the current location.
 *
 * @author Sandor Dornbush
 */
public class WaypointCreationRequest {

    public final static WaypointCreationRequest DEFAULT_WAYPOINT = new WaypointCreationRequest(WaypointType.WAYPOINT, false);

    public final static WaypointCreationRequest DEFAULT_STATISTICS = new WaypointCreationRequest(WaypointType.STATISTICS, false);

    public final static WaypointCreationRequest DEFAULT_START_TRACK = new WaypointCreationRequest(WaypointType.STATISTICS, true);

    private WaypointType type;

    // true if this marker contains the track statistics
    private boolean isTrackStatistics;

    private String name;
    private String category;
    private String description;
    private String iconUrl;
    private String photoUrl;

    private WaypointCreationRequest(WaypointType type, boolean isTrackStatistics) {
        this(type, isTrackStatistics, null, null, null, null, null);
    }

    public WaypointCreationRequest(WaypointType type, boolean isTrackStatistics, String name, String category, String description, String iconUrl, String photoUrl) {
        this.type = type;
        this.isTrackStatistics = isTrackStatistics;
        this.name = name;
        this.category = category;
        this.description = description;
        this.iconUrl = iconUrl;
        this.photoUrl = photoUrl;
    }

    public WaypointType getType() {
        return type;
    }

    public boolean isTrackStatistics() {
        return isTrackStatistics;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }
}