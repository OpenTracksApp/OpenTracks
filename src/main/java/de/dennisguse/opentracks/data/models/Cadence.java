package de.dennisguse.opentracks.data.models;

import androidx.annotation.NonNull;

import java.util.Objects;

public class Cadence {

    public static Cadence of(float value) {
        return new Cadence(value);
    }

    private final float value;

    private Cadence(float value) {
        this.value = value;
    }

    public float getRPM() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Cadence cadence = (Cadence) o;
        return Float.compare(cadence.value, value) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @NonNull
    @Override
    public String toString() {
        return "Cadence{" +
                "value=" + value + " rpm" +
                '}';
    }
}
