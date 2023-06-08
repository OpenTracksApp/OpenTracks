package de.dennisguse.opentracks.data.models;

import java.util.Objects;

public class AtmosphericPressure {

    public static AtmosphericPressure ofPA(float value_Pa) {
        return new AtmosphericPressure(value_Pa * 100);
    }

    public static AtmosphericPressure ofHPA(float value_hPa) {
        return new AtmosphericPressure(value_hPa);
    }

    private final float value;

    private AtmosphericPressure(float value) {
        this.value = value;
    }

    public float getPA() {
        return value * 100;
    }

    public float getHPA() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AtmosphericPressure that = (AtmosphericPressure) o;
        return Float.compare(that.value, value) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return "AtmosphericPressure{" +
                "value=" + value +
                '}';
    }
}
