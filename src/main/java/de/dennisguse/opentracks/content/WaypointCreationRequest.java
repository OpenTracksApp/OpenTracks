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


/**
 * A request for the service to create a waypoint at the current location.
 *
 * @author Sandor Dornbush
 */
public class WaypointCreationRequest {

    private String name;
    private String category;
    private String description;
    private String iconUrl;
    private String photoUrl;

    public WaypointCreationRequest(String name, String category, String description, String iconUrl, String photoUrl) {
        this.name = name;
        this.category = category;
        this.description = description;
        this.iconUrl = iconUrl;
        this.photoUrl = photoUrl;
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