package de.dennisguse.opentracks.data.models;

public record Power(float value) {

    public static Power of(float value) {
        return new Power(value);
    }

    public float getW() {
        return value;
    }
}
