package de.dennisguse.opentracks.data.models;

import java.time.Duration;

import de.dennisguse.opentracks.settings.UnitSystem;

public record Speed(double speed_mps) {

    public static Speed of(Distance distance, Duration duration) {
        if (duration.isZero()) {
            return zero();
        }

        return new Speed(distance.toM() / (duration.toMillis() / (double) Duration.ofSeconds(1).toMillis()));
    }

    public static Speed of(double speed_mps) {
        return new Speed(speed_mps);
    }

    public static Speed of(String speed_mps) {
        return of(Float.parseFloat(speed_mps));
    }

    public static Speed ofKMH(double speed_kmh) {
        return of(Distance.ofKilometer(speed_kmh), Duration.ofHours(1));
    }

    public static Speed zero() {
        return of(0.0);
    }

    public static Speed max(Speed speed1, Speed speed2) {
        if (speed1.greaterThan(speed2)) {
            return speed1;
        }

        return speed2;
    }

    public boolean isZero() {
        return speed_mps == 0;
    }

    public boolean isInvalid() {
        return Double.isNaN(speed_mps) || Double.isInfinite(speed_mps);
    }

    public boolean lessThan(Speed speed) {
        return !greaterThan(speed);
    }

    public boolean greaterThan(Speed speed) {
        return speed_mps > speed.speed_mps;
    }

    public boolean greaterOrEqualThan(Speed speed) {
        return speed_mps >= speed.speed_mps;
    }

    public double toMPS() {
        return speed_mps;
    }

    /**
     * We interpret {@link Speed} here as a {@link Distance} over 1h.
     */
    private Distance toH() {
        return Distance.of(speed_mps * Duration.ofHours(1).toSeconds());
    }

    public double toKMH() {
        return toH().toKM();
    }

    public double toMPH() {
        return toH().toMI();
    }

    public double toKnots() {
        return toH().toNauticalMiles();
    }

    public Duration toPace(UnitSystem unitSystem) {
        if (isZero()) {
            return Duration.ofSeconds(0);
        }

        double distance = Distance.of(speed_mps).toKM_Miles(unitSystem);

        return Duration.ofSeconds(Math.round(1 / distance));
    }

    public double to(UnitSystem unitSystem) {
        return switch (unitSystem) {
            case METRIC -> toKMH();
            case IMPERIAL_FEET, IMPERIAL_METER -> toMPH();
            case NAUTICAL_IMPERIAL -> toKnots();
        };
    }
}
