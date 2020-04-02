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

import java.util.HashSet;
import java.util.Set;

/**
 * Manages register/unregister {@link TrackDataListener} and keeping the state for each registered listener.
 *
 * @author Rodrigo Damazio
 */
class TrackDataManager {

    private static final String TAG = TrackDataManager.class.getSimpleName();

    private final Set<TrackDataListener> dataListenerTracks = new HashSet<>();
    private final Set<TrackDataListener> dataListenerWaypoints = new HashSet<>();
    private final Set<TrackDataListener> dataListenerTrackPoints_SampledIn = new HashSet<>();
    private final Set<TrackDataListener> dataListenerTrackPoints_SampledOut = new HashSet<>();

    void registerTrackDataListener(final TrackDataListener trackDataListener, boolean tracksTable, boolean waypointsTable, boolean trackPointsTable_SampleIn, boolean trackPointsTable_SampleOut) {
        if (tracksTable) dataListenerTracks.add(trackDataListener);
        if (waypointsTable) dataListenerWaypoints.add(trackDataListener);
        if (trackPointsTable_SampleIn) dataListenerTrackPoints_SampledIn.add(trackDataListener);
        if (trackPointsTable_SampleOut) dataListenerTrackPoints_SampledOut.add(trackDataListener);
    }

    void unregisterTrackDataListener(TrackDataListener trackDataListener) {
        dataListenerTracks.add(trackDataListener);
        dataListenerWaypoints.add(trackDataListener);
        dataListenerTrackPoints_SampledIn.add(trackDataListener);
        dataListenerTrackPoints_SampledOut.add(trackDataListener);
    }

    boolean hasListeners() {
        return dataListenerTracks.size() + dataListenerWaypoints.size() + dataListenerTrackPoints_SampledIn.size() + dataListenerTrackPoints_SampledOut.size() > 0;
    }

    int getNumberOfListeners() {
        Set<TrackDataListener> listener = new HashSet<>();
        listener.addAll(dataListenerTracks);
        listener.addAll(dataListenerWaypoints);
        listener.addAll(dataListenerTrackPoints_SampledIn);
        listener.addAll(dataListenerTrackPoints_SampledOut);

        return listener.size();
    }

    boolean listensForTracks(TrackDataListener listener) {
        return dataListenerTracks.contains(listener);
    }

    boolean listensForWaypoints(TrackDataListener listener) {
        return dataListenerWaypoints.contains(listener);
    }

    boolean listensForTrackPoints_SampledIn(TrackDataListener listener) {
        return dataListenerTrackPoints_SampledIn.contains(listener);
    }

    boolean listensForTrackPoints_SampledOut(TrackDataListener listener) {
        return dataListenerTrackPoints_SampledOut.contains(listener);
    }

    Set<TrackDataListener> getListenerTracks() {
        return dataListenerTracks;
    }

    Set<TrackDataListener> getListenerWaypoints() {
        return dataListenerWaypoints;
    }

    Set<TrackDataListener> getListenerTrackPoints_SampledIn() {
        return dataListenerTrackPoints_SampledIn;
    }

    Set<TrackDataListener> getListenerTrackPoints_SampledOut() {
        return dataListenerTrackPoints_SampledOut;
    }
}
