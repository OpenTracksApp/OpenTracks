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
// changed name of value_rpm to valueRpm
    public static Cadence of(float valueRpm) {
        return new Cadence(valueRpm);
    }

    public static Cadence zero() {
        return of(0.0f);
    }

    private final float valueRpm;

    private Cadence(float value) {
        this.valueRpm = value;
    }

    public float getRPM() {
        return valueRpm;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Cadence cadence = (Cadence) o;
        return Float.compare(cadence.valueRpm, valueRpm) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(valueRpm);
    }

    @NonNull
    @Override
    public String toString() {
        return "Cadence{" +
                "value=" + valueRpm + " rpm" +
                '}';
    }
}
