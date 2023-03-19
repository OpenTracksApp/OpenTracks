package de.dennisguse.opentracks.data.models;

import androidx.annotation.NonNull;

import java.util.Objects;

/**
 * A class representing a calorie value.
 */
public class Calories implements Comparable<Calories> {

    private final double value;


    public Calories(double value) {
        this.value = value;
    }

    /**
     * Gets the calorie value.
     *
     * @return the calorie value.
     */
    public double getValue() {
        return value;
    }

    /**
     * Compares this calorie value with another.
     *
     * @param other the other calorie value to compare with.
     * @return a negative integer if this value is less than the other, zero if they are equal,
     * or a positive integer if this value is greater than the other.
     */
    @Override
    public int compareTo(@NonNull Calories other) {
        return Double.compare(this.value, other.value);
    }

}