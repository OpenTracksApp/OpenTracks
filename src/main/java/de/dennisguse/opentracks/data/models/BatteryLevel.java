package de.dennisguse.opentracks.data.models;

public class BatteryLevel {

    public static BatteryLevel of(int percentage) {
        return new BatteryLevel(percentage);
    }

    private final int percentage;

    private BatteryLevel(int percentage) {
        this.percentage = percentage;
    }

    public int getPercentage() {
        return percentage;
    }
}
