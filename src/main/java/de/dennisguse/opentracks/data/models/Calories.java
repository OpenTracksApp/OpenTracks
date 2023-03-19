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
    public double getCalories() {
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

    /**
     * Checks if this calorie value is equal to another object.
     *
     * @param obj the other object to check for equality.
     * @return true if the other object is a calorie value and has the same value, false otherwise.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        Calories calories = (Calories) obj;
        return Double.compare(calories.value, value) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    /**
     * Returns the string representation of the calorie value.
     *
     * @return the string representation of the calorie value.
     */
    @Override
    public String toString() {
        return "Calories{" +
                "value=" + value + " Kcal" +
                '}';
    }


}