package de.dennisguse.opentracks.data.models;

import android.location.Location;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.Instant;

//TODO Use everywhere instead of android.location.Location.
public record Position(
        @Nullable Instant time,
        @Nullable Double latitude,
        @Nullable Double longitude,
        @Nullable Distance horizontalAccuracy,
        @Nullable Altitude altitude,
        @Nullable Distance verticalAccuracy,
        @Nullable Float bearing,
        @Nullable Speed speed
) {
    public static Position empty() {
        return of((Instant) null);
    }

    public static Position of(@NonNull Instant time) {
        return new Position(
                time,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    public static Position of(@NonNull Location location) {
        return of(location, Instant.ofEpochMilli(location.getTime()));
    }

    public static Position of(@NonNull Location location, @NonNull Instant time) {
        return new Position(
                time,
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

    public boolean hasVerticalAccuracy() {
        return verticalAccuracy != null;
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
        if (time != null) {
            location.setTime(time.toEpochMilli());
        }
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

    /**
     * Checks if a given location is a valid (i.e. physically possible) location on Earth.
     *
     * @return true if the location is a valid location.
     */
    public boolean hasValidLocation() {
        return hasLocation()
                && Math.abs(latitude) <= 90
                && Math.abs(longitude) <= 180;
    }

    public boolean fulfillsAccuracy(Distance thresholdHorizontalAccuracy) {
        return hasHorizontalAccuracy() &&
                horizontalAccuracy
                        .lessThan(thresholdHorizontalAccuracy);

    }

    public Position with(Instant time) {
        return new Position(time, latitude, longitude, horizontalAccuracy, altitude, verticalAccuracy, bearing, speed);
    }

    public Position with(Altitude altitude) {
        return new Position(time, latitude, longitude, horizontalAccuracy, altitude, verticalAccuracy, bearing, speed);
    }

    public Position with(Speed speed) {
        return new Position(time, latitude, longitude, horizontalAccuracy, altitude, verticalAccuracy, bearing, speed);
    }

    //TODO Use double
    public Position withCoordinates(Double latitude, Double longitude) {
        return new Position(time, latitude, longitude, horizontalAccuracy, altitude, verticalAccuracy, bearing, speed);
    }

    //TODO Use float
    public Position withBearing(Float bearing) {
        return new Position(time, latitude, longitude, horizontalAccuracy, altitude, verticalAccuracy, bearing, speed);
    }

    public Position withHorizontalAccuracy(Distance horizontalAccuracy) {
        return new Position(time, latitude, longitude, horizontalAccuracy, altitude, verticalAccuracy, bearing, speed);
    }

    public Position withVerticalAccuracy(Distance verticalAccuracy) {
        return new Position(time, latitude, longitude, horizontalAccuracy, altitude, verticalAccuracy, bearing, speed);
    }
}
