package de.dennisguse.opentracks.sensors;

public enum SensorType {
    NONE("NONE"),
    INTERNAL("INTERNAL"),
    REMOTE("*");

    private final String preferenceValue;

    SensorType(String preferenceValue) {
        this.preferenceValue = preferenceValue;
    }

    public String getPreferenceValue() {
        return preferenceValue;
    }
}
