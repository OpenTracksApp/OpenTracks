package de.dennisguse.opentracks.content.provider;

import android.location.Location;
import android.location.LocationManager;

import de.dennisguse.opentracks.content.data.TrackPoint;

/**
 * Creates a new {@link TrackPoint}.
 * An implementation can create new instances or reuse existing instances for optimization.
 */
public class TrackPointFactory {

    /**
     * The default {@link TrackPointFactory} which creates a location each time.
     */
    public static final TrackPointFactory DEFAULT_LOCATION_FACTORY = new TrackPointFactory();

    public TrackPoint create() {
        return new TrackPoint(new Location(LocationManager.GPS_PROVIDER));
    }
}
