package de.dennisguse.opentracks.data.models;

import androidx.annotation.NonNull;

import java.util.Objects;

public record Power(float value) {

    public static Power of(float value) {
        return new Power(value);
    }

    public float getW() {
        return value;
    }
}
