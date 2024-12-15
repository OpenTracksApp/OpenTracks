package de.dennisguse.opentracks.sensors.sensorData;


import androidx.annotation.NonNull;

import java.time.Instant;

public record Raw<T>(
        @NonNull Instant time,
        @NonNull T value
) {
    @Deprecated
    public Raw(@NonNull T value) {
        this(Instant.now(), value); //TODO We should be using the MonotonicClock
    }
}
