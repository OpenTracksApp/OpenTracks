package de.dennisguse.opentracks.data.models;

import androidx.annotation.NonNull;

import java.util.Objects;

/**
 * A class representing a calorie value.
 */
public class Calories implements Comparable<Calories> {

    private final double value;

    /**
     * Creates a new instance of the Calories class.
     *
     * @param value the calorie value.
     */

//# ----------------- Create Arithmetic Model & Getter Setter or Divide this part into two  ------------
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

    @Override
    public int compareTo(@NonNull Calories other) {
        return Double.compare(this.value, other.value);
    }

}