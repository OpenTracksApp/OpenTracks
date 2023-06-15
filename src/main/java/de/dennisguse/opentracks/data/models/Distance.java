package de.dennisguse.opentracks.data.models;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import de.dennisguse.opentracks.settings.UnitSystem;

public record Distance(double distance_m) {

    public static Distance of(double distance_m) {
        return new Distance(distance_m);
    }
    public static Distance of(Double distance_m) {
        if (distance_m == null) {
            return Distance.of(Double.NaN);
        } else {
            return Distance.of((double) distance_m);
        }
    }
    public static Distance of(String distance_m) {
        return of(Float.parseFloat(distance_m));
    }
    @Nullable
    public static Distance ofOrNull(Double distance_m) {
        if (distance_m == null) {
            return null;
        }
        return of(distance_m);
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
        return switch (unitSystem) {
            case METRIC -> Distance.ofKilometer(1);
            case IMPERIAL_FEET, IMPERIAL_METER -> Distance.ofMile(1);
            case NAUTICAL_IMPERIAL -> Distance.ofNauticalMile(1);
        };
    }

    public Distance plus(@NonNull Distance distance) {
        return new Distance(distance_m + distance.distance_m);
    }

    public Distance minus(@NonNull Distance distance) {
        return new Distance(distance_m - distance.distance_m);
    }

    public Distance multipliedBy(double factor) {
        return new Distance(factor * distance_m);
    }

    public double dividedBy(@NonNull Distance divisor) {
        return distance_m / divisor.distance_m;
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
        return distance_m * M_TO_KM;
    }

    public double toFT() {
        return distance_m * M_TO_FT;
    }

    public double toMI() {
        return distance_m * M_TO_MI;
    }

    public double toNauticalMiles() {
        return distance_m * M_TO_NAUTICAL_MILE;
    }

    public double toKM_Miles(UnitSystem unitSystem) {
        return switch (unitSystem) {
            case METRIC -> toKM();
            case IMPERIAL_FEET, IMPERIAL_METER -> toMI();
            case NAUTICAL_IMPERIAL -> toNauticalMiles();
        };
    }

    public double toM_FT(UnitSystem unitSystem) {
        return switch (unitSystem) {
            case METRIC, IMPERIAL_METER -> toM();
            case NAUTICAL_IMPERIAL, IMPERIAL_FEET -> toFT();
        };
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
