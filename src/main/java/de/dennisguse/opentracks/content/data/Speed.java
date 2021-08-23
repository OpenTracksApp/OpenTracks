package de.dennisguse.opentracks.content.data;

import androidx.annotation.NonNull;

import java.time.Duration;
import java.util.Objects;

import de.dennisguse.opentracks.util.UnitConversions;

public class Speed {

    public static Speed of(Distance distance, Duration duration) {
        if (duration.isZero()) {
            return zero();
        }

        return new Speed(distance.toM() / (duration.toMillis() * UnitConversions.MS_TO_S));
    }

    public static Speed of(double speed_mps) {
        return new Speed(speed_mps);
    }

    public static Speed of(String speed_mps) {
        return of(Float.parseFloat(speed_mps));
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


    // Anything faster than that (in meters per second) will be considered moving.
    private static final double MAX_NO_MOVEMENT_SPEED = 0.224;

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
        return !isInvalid() && speed_mps >= MAX_NO_MOVEMENT_SPEED;
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

    public double toKMH() {
        return speed_mps * UnitConversions.MPS_TO_KMH;
    }

    public double toMPH() {
        return toKMH() * UnitConversions.KM_TO_MI;
    }

    public Duration toPace(boolean metricUnit) {
        if (isZero()) {
            return Duration.ofSeconds(0);
        }

        double distance = speed_mps * (metricUnit ? UnitConversions.M_TO_KM : UnitConversions.M_TO_MI);
        return Duration.ofSeconds(Math.round(1 / distance));
    }

    public double to(boolean metricUnit) {
        return to(metricUnit ? Unit.KMH : Unit.MPH);
    }

    public double to(Unit unit) {
        switch (unit) {
            case KMH:
                return toKMH();
            case MPH:
                return toMPH();
            default:
                throw new RuntimeException("Not implemented");
        }
    }

    public enum Unit {
        KMH,
        MPH,
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
