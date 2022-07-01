package de.dennisguse.opentracks.data.models;

import androidx.annotation.NonNull;

import java.time.Duration;
import java.util.Objects;

public class Cadence {

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

    private final float value_rpm;

    private Cadence(float value) {
        this.value_rpm = value;
    }

    public float getRPM() {
        return value_rpm;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Cadence cadence = (Cadence) o;
        return Float.compare(cadence.value_rpm, value_rpm) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value_rpm);
    }

    @NonNull
    @Override
    public String toString() {
        return "Cadence{" +
                "value=" + value_rpm + " rpm" +
                '}';
    }
}
