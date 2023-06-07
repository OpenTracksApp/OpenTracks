package de.dennisguse.opentracks.data.models;

public record BatteryLevel(int percentage) {

    public static BatteryLevel of(int percentage) {
        return new BatteryLevel(percentage);
    }
}
