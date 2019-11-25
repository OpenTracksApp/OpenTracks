package de.dennisguse.opentracks.content;

import android.location.Location;
import android.location.LocationManager;

/**
 * Creates a new {@link SensorDataSetLocation}.
 * An implementation can create new instances or reuse existing instances for optimization.
 */
public class LocationFactory {

    /**
     * The default {@link LocationFactory} which creates a location each time.
     */
    public static LocationFactory DEFAULT_LOCATION_FACTORY = new LocationFactory();

    public Location createLocation() {
        return new SensorDataSetLocation(LocationManager.GPS_PROVIDER);
    }
}
