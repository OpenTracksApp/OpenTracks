package de.dennisguse.opentracks.data.models;

public record AtmosphericPressure(float value) {

    public static AtmosphericPressure ofPA(float value_Pa) {
        return new AtmosphericPressure(value_Pa / 100);
    }

    public static AtmosphericPressure ofHPA(float value_hPa) {
        return new AtmosphericPressure(value_hPa);
    }

    public float getPA() {
        return value * 100;
    }

    public float getHPA() {
        return value;
    }
}
