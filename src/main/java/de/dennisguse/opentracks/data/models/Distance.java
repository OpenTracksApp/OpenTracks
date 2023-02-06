package de.dennisguse.opentracks.data.models;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

import de.dennisguse.opentracks.settings.UnitSystem;

public class Distance {

    public static Distance of(double distanceM) {
        return new Distance(distanceM);
    }

    public static Distance of(Double distanceM) {
        if (distanceM == null) {
            return Distance.of(Double.NaN);
        } else {
            return Distance.of((double) distanceM);
        }
    }

    public static Distance of(String distanceM) {
        return of(Float.parseFloat(distanceM));
    }

    @Nullable
    public static Distance ofOrNull(Double distanceM) {
        if (distanceM == null) {
            return null;
        }
        return of(distanceM);
    }

    public static Distance ofMile(double distance_mile) {
        return of(distance_mile * MI_TO_M);
    }

    public static Distance ofNauticalMile(double distance_mile) {
        return of(distance_mile * NAUTICAL_MILE_TO_M);
    }

    public static Distance ofKilometer(double distance_km) {
        return of(distance_km * KM_TO_M);
    }

    public static Distance ofMM(double distance_mm) {
        return of(0.001 * distance_mm);
    }

    public static Distance ofCM(double distance_cm) {
        return of(0.01 * distance_cm);
    }

    public static Distance ofDM(double distance_dm) {
        return of(0.1 * distance_dm);
    }

    public static Distance one(UnitSystem unitSystem) {
        switch (unitSystem) {
            case METRIC:
                return Distance.ofKilometer(1);
            case IMPERIAL:
                return Distance.ofMile(1);
            case NAUTICAL_IMPERIAL:
                return Distance.ofNauticalMile(1);
            default:
                throw new RuntimeException("Not implemented");
        }
    }

    private final double distanceM;

    private Distance(double distanceM) {
        this.distanceM = distanceM;
    }

    public Distance plus(@NonNull Distance distance) {
        return new Distance(distanceM + distance.distanceM);
    }

    public Distance minus(@NonNull Distance distance) {
        return new Distance(distanceM - distance.distanceM);
    }

    public Distance multipliedBy(double factor) {
        return new Distance(factor * distanceM);
    }

    public double dividedBy(@NonNull Distance divisor) {
        return distanceM / divisor.distanceM;
    }

    public boolean isZero() {
        return distanceM == 0;
    }

    public boolean isInvalid() {
        return Double.isNaN(distanceM) || Double.isInfinite(distanceM);
    }

    public boolean lessThan(@NonNull Distance distance) {
        return !greaterThan(distance);
    }

    public boolean greaterThan(@NonNull Distance distance) {
        return distanceM > distance.distanceM;
    }

    public boolean greaterOrEqualThan(@NonNull Distance distance) {
        return distanceM >= distance.distanceM;
    }

    public double toM() {
        return distanceM;
    }

    public double toKM() {
        return distanceM * M_TO_KM;
    }

    public double toFT() {
        return distanceM * M_TO_FT;
    }

    public double toMI() {
        return distanceM * M_TO_MI;
    }

    public double toNauticalMiles() {
        return distanceM * M_TO_NAUTICAL_MILE;
    }

    public double toKM_Miles(UnitSystem unitSystem) {
        switch (unitSystem) {
            case METRIC:
                return toKM();
            case IMPERIAL:
                return toMI();
            case NAUTICAL_IMPERIAL:
                return toNauticalMiles();
            default:
                throw new RuntimeException("Not implemented");
        }
    }

    public double toM_FT(UnitSystem unitSystem) {
        switch (unitSystem) {
            case METRIC:
                return toM();
            case NAUTICAL_IMPERIAL:
            case IMPERIAL:
                return toFT();
            default:
                throw new RuntimeException("Not implemented");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Distance distance = (Distance) o;
        return Double.compare(distance.distanceM, distanceM) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(distanceM);
    }

    @NonNull
    @Override
    public String toString() {
        return "Distance{" +
                "distance_m=" + distanceM +
                '}';
    }

    // multiplication factors for conversion
    private static final double KM_TO_M = 1000.0;
    private static final double M_TO_KM = 1 / KM_TO_M;

    public static final double MI_TO_M = 1609.344;
    public static final double M_TO_MI = 1 / MI_TO_M;

    private static final double MI_TO_FT = 5280.0;
    public static final double M_TO_FT = M_TO_MI * MI_TO_FT;

    private static final double NAUTICAL_MILE_TO_M = 1852.0;
    private static final double M_TO_NAUTICAL_MILE = 1 / NAUTICAL_MILE_TO_M;
}
