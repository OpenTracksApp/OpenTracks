package de.dennisguse.opentracks.data.models;

import java.time.Duration;

public record Cadence(float value_rpm) {

    public static Cadence of(float value, Duration duration) {
        if (duration.isZero()) {
            return zero();
        }

        return new Cadence(value / (duration.toMillis() / (float) Duration.ofMinutes(1).toMillis()));
    }

    public static Cadence of(float value_rpm) {
        return new Cadence(value_rpm);
    }

    public static Cadence zero() {
        return of(0.0f);
    }

    public float getRPM() {
        return value_rpm;
    }
}
