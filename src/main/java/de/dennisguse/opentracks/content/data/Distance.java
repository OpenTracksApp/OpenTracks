package de.dennisguse.opentracks.content.data;

import androidx.annotation.NonNull;

import java.util.Objects;

import de.dennisguse.opentracks.util.UnitConversions;

public class Distance {

    public static Distance of(double distance_m) {
        return new Distance(distance_m);
    }

    public static Distance of(String distance_m) {
        return of(Float.parseFloat(distance_m));
    }

    public static Distance ofMile(double distance_mile) {
        return of(distance_mile * UnitConversions.MI_TO_M);
    }

    public static Distance invalid() {
        return of(Double.NaN);
    }

    private final double distance_m;

    private Distance(double distance_m) {
        this.distance_m = distance_m;
    }

    public Distance plus(@NonNull Distance distance) {
        return new Distance(distance_m + distance.distance_m);
    }

    public Distance multipliedBy(double factor) {
        return new Distance(factor * distance_m);
    }

    public boolean isZero() {
        return distance_m == 0;
    }

    public boolean isInvalid() {
        return Double.isNaN(distance_m) || Double.isInfinite(distance_m);
    }

    public boolean lessThan(@NonNull Distance distance) {
        return !greaterThan(distance);
    }

    public boolean greaterThan(@NonNull Distance distance) {
        return distance_m > distance.distance_m;
    }

    public boolean greaterOrEqualThan(@NonNull Distance distance) {
        return distance_m >= distance.distance_m;
    }

    public double toM() {
        return distance_m;
    }

    public double toKM() {
        return distance_m * UnitConversions.M_TO_KM;
    }

    public double toFT() {
        return distance_m * UnitConversions.M_TO_FT;
    }

    public double toMI() {
        return toKM() * UnitConversions.KM_TO_MI;
    }

    public double to(boolean metricUnit) {
        return to(metricUnit ? Unit.KM : Unit.MILES);
    }

    public double to(Unit unit) {
        switch (unit) {
            case KM:
                return toKM();
            case MILES:
                return toMI();
            default:
                throw new RuntimeException("Not implemented");
        }
    }

    public enum Unit {
        KM,
        MILES
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Distance distance = (Distance) o;
        return Double.compare(distance.distance_m, distance_m) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(distance_m);
    }

    @Override
    public String toString() {
        return "Distance{" +
                "distance_m=" + distance_m +
                '}';
    }
}
