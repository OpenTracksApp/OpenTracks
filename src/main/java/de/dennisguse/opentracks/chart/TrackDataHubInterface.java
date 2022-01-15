package de.dennisguse.opentracks.chart;

import de.dennisguse.opentracks.content.TrackDataHub;

/**
 * Interface for communication between activities that use {@link de.dennisguse.opentracks.content.TrackDataHub} and their fragments that need thi data hub.
 */
public interface TrackDataHubInterface {
    TrackDataHub getTrackDataHub();
}
