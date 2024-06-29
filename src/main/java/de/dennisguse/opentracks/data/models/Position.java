package de.dennisguse.opentracks.data.models;

import android.location.Location;

import java.time.Instant;

//TODO Use everywhere instead of android.location.Location.
public record Position(
        Instant time,
        Double latitude,
        Double longitude,
        Distance horizontalAccuracy,
        Altitude altitude,
        Distance verticalAccuracy,
        Float bearing,
        Speed speed
) {
    public static Position empty() {
        return new Position(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    public static Position of(Location location) {
        return new Position(
                Instant.ofEpochMilli(location.getTime()),
                location.getLatitude(),
                location.getLongitude(),
                location.hasAccuracy() ? Distance.of(location.getAccuracy()) : null,
                location.hasAltitude() ? Altitude.WGS84.of(location.getAltitude()) : null,
                location.hasVerticalAccuracy() ? Distance.of(location.getVerticalAccuracyMeters()) : null,
                location.hasBearing() ? location.getBearing() : null,
                location.hasSpeed() ? Speed.of(location.getSpeed()) : null
        );
    }

    public boolean hasLocation() {
        return latitude != null && longitude != null;
    }

    public boolean hasHorizontalAccuracy() {
        return horizontalAccuracy != null;
    }

    public boolean hasAltitude() {
        return altitude != null;
    }

    public boolean hasBearing() {
        return bearing != null;
    }

    public boolean hasSpeed() {
        return speed != null;
    }

    public Location toLocation() {
        if (!hasLocation()) {
            throw new RuntimeException("Cannot convert to Location.");
        }

        Location location = new Location("");
        location.setTime(time.toEpochMilli());
        location.setLatitude(latitude);
        location.setLongitude(longitude);
        if (hasBearing()) {
            location.setBearing(bearing);
        }
        if (hasHorizontalAccuracy()) {
            location.setAccuracy((float) horizontalAccuracy.toM());
        }
        if (hasAltitude()) {
            location.setAltitude(altitude.toM());
        }
        if (hasSpeed()) {
            location.setSpeed((float) speed.toMPS());
        }

        return location;
    }
}
