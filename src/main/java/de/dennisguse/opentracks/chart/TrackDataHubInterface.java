package de.dennisguse.opentracks.chart;

import de.dennisguse.opentracks.data.TrackDataHub;

/**
 * Interface for communication between activities that use {@link TrackDataHub} and their fragments that need thi data hub.
 */
public interface TrackDataHubInterface {
    TrackDataHub getTrackDataHub();
}
