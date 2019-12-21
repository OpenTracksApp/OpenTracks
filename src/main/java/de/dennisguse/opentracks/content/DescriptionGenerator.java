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

import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.stats.TripStatistics;

/**
 * An interface for an object that can generate descriptions of track and waypoint.
 *
 * @author Sandor Dornbush
 */
public interface DescriptionGenerator {

    /**
     * Generates a track description.
     *
     * @param track      the track
     * @param html       true to output html, false to output plain text
     */
    String generateTrackDescription(Track track, boolean html);

    /**
     * Generate a waypoint description from a trip statistics.
     *
     * @param tripStatistics the trip statistics
     */
    String generateWaypointDescription(TripStatistics tripStatistics);
}
