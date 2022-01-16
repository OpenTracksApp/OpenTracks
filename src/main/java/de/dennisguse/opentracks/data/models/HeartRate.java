package de.dennisguse.opentracks.data.models;

import androidx.annotation.NonNull;

import java.util.Objects;

public class HeartRate {

    public static HeartRate of(float value) {
        return new HeartRate(value);
    }

    private final float value;

    private HeartRate(float value) {
        this.value = value;
    }

    public float getBPM() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HeartRate heartRate = (HeartRate) o;
        return Float.compare(heartRate.value, value) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @NonNull
    @Override
    public String toString() {
        return "HeartRate{" +
                "value=" + value + " bpm" +
                '}';
    }
}
