package de.dennisguse.opentracks.sensors.sensorData;


import androidx.annotation.NonNull;

import java.time.Instant;

public record Raw<T extends Record>(
        @NonNull T value,

        @NonNull Instant time
) {
    public Raw(@NonNull T value) {
        this(value, Instant.now()); //TODO We should be using the MonotonicClock
    }
}
