package de.dennisguse.opentracks.sensors.sensorData;


import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import java.time.Clock;
import java.time.Instant;

public record Raw<T>(
        @NonNull Instant time,
        @NonNull T value
) {
    public Raw(@NonNull Clock clock, @NonNull T value) {
        this(clock.instant(), value);
    }

    @VisibleForTesting
    public Raw(@NonNull String time, @NonNull T value) {
        this(Instant.parse(time), value);
    }
}
