package de.dennisguse.opentracks.data.models;

import androidx.annotation.NonNull;

import java.util.Objects;

public class Power {

    public static Power of(float value) {
        return new Power(value);
    }

    private final float value;

    private Power(float value) {
        this.value = value;
    }

    public float getW() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Power power = (Power) o;
        return Float.compare(power.value, value) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @NonNull
    @Override
    public String toString() {
        return "Power{" +
                "value=" + value + " W" +
                '}';
    }
}
