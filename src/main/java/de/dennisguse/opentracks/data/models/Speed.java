package de.dennisguse.opentracks.data.models;

import androidx.annotation.NonNull;

import java.time.Duration;
import java.util.Objects;

import de.dennisguse.opentracks.settings.PreferencesUtils;
import de.dennisguse.opentracks.settings.UnitSystem;

public class Speed {

    public static Speed of(Distance distance, Duration duration) {
        if (duration.isZero()) {
            return zero();
        }

        return new Speed(distance.toM() / (duration.toMillis() / (double) Duration.ofSeconds(1).toMillis()));
    }

    public static Speed of(double speedMps) {
        return new Speed(speedMps);
    }

    public static Speed of(String speedMps) {
        return of(Float.parseFloat(speedMps));
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

    public static Speed absDiff(Speed speed1, Speed speed2) {
        //TODO Why Math.abs? Seems to be a leftover.
        return Speed.of(Math.abs(speed1.speed_mps - speed2.speed_mps));
    }

    private final double speed_mps;

    private Speed(double speed_mps) {
        this.speed_mps = speed_mps;
    }

    public Speed mul(double factor) {
        return new Speed(factor * speed_mps);
    }

    public boolean isZero() {
        return speed_mps == 0;
    }

    public boolean isInvalid() {
        return Double.isNaN(speed_mps) || Double.isInfinite(speed_mps);
    }

    public boolean isMoving() {
        return !isInvalid() && greaterThan(PreferencesUtils.getIdleSpeed());
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
        switch (unitSystem) {
            case METRIC:
                return toKMH();
            case IMPERIAL:
                return toMPH();
            case NAUTICAL_IMPERIAL:
                return toKnots();
            default:
                throw new RuntimeException("Not implemented");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Speed speed = (Speed) o;
        return Double.compare(speed.speed_mps, speed_mps) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(speed_mps);
    }

    @NonNull
    @Override
    public String toString() {
        return "Speed{" +
                "speed_mps=" + speed_mps +
                '}';
    }
}
