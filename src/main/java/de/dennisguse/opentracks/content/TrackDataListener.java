/*
 * Copyright 2011 Google Inc.
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

import androidx.annotation.Nullable;

import de.dennisguse.opentracks.content.data.Marker;
import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.content.data.TrackPoint;

/**
 * Listener for track data changes.
 *
 * @author Rodrigo Damazio
 */
public interface TrackDataListener {

    /**
     * Called when the track or its statistics has been updated.
     *
     * @param track the track
     */
    //TODO Could be @NonNull
    void onTrackUpdated(Track track);

    /**
     * Called to clear previously-sent track points.
     */
    void clearTrackPoints();

    /**
     * Called when a sampled in track point is read.
     *
     * @param trackPoint the trackPoint
     */
    void onSampledInTrackPoint(TrackPoint trackPoint);

    /**
     * Called when a sampled out track point is read.
     *
     * @param trackPoint the trackPoint
     */
    void onSampledOutTrackPoint(TrackPoint trackPoint);

    /**
     * Called when finish sending new track points.
     * This gets called after every batch of calls to {@link #onSampledInTrackPoint(TrackPoint)} and {@link #onSampledOutTrackPoint(TrackPoint)}.
     */
    void onNewTrackPointsDone(@Nullable TrackPoint lastTrackPoint);

    /**
     * Called to clear previously sent markers.
     */
    void clearMarkers();

    /**
     * Called when a new marker is read.
     *
     * @param marker the marker
     */
    void onNewMarker(Marker marker);

    /**
     * Called when finish sending new markers.
     * This gets called after every batch of calls to {@link #clearMarkers()} and {@link #onNewMarker(Marker)}.
     */
    void onNewMarkersDone();
}
