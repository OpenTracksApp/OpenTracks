package de.dennisguse.opentracks.data.models;

import androidx.annotation.NonNull;

import java.util.Objects;

public record HeartRate(float value) {

    public static HeartRate of(float value) {
        return new HeartRate(value);
    }

    public float getBPM() {
        return value;
    }
}
