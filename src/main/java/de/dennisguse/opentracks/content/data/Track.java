/*
 * Copyright 2008 Google Inc.
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

package de.dennisguse.opentracks.content.data;

import java.util.UUID;

import de.dennisguse.opentracks.stats.TrackStatistics;

/**
 * A track.
 *
 * @author Leif Hendrik Wilden
 * @author Rodrigo Damazio
 */
public class Track {

    private long id = -1L;
    private UUID uuid = UUID.randomUUID();

    private String name = "";
    private String description = "";
    private String category = "";

    private String icon = "";

    private TrackStatistics trackStatistics = new TrackStatistics();

    public Track() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public TrackStatistics getTrackStatistics() {
        return trackStatistics;
    }

    public void setTrackStatistics(TrackStatistics trackStatistics) {
        this.trackStatistics = trackStatistics;
    }
}
