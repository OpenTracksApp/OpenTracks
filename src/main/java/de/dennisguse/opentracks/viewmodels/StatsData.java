package de.dennisguse.opentracks.viewmodels;

import android.util.Pair;

public class StatsData {
    private final String value;
    private String unit;
    private final String descMain;
    private String descSecondary;
    private final boolean isPrimary;
    private boolean isLong = false;

    public StatsData(String value, String descMain, boolean isPrimary) {
        this.value = value;
        this.descMain = descMain;
        this.isPrimary = isPrimary;
    }

    public StatsData(Pair<String, String> valueAndUnit, String descMain, boolean isPrimary) {
        this.value = valueAndUnit.first;
        this.unit = valueAndUnit.second;
        this.descMain = descMain;
        this.isPrimary = isPrimary;
    }

    public StatsData(String value, String unit, String descMain, String descSecondary, boolean isPrimary) {
        this.value = value;
        this.unit = unit;
        this.descMain = descMain;
        this.descSecondary = descSecondary;
        this.isPrimary = isPrimary;
    }

    public StatsData setLong() {
        this.isLong = true;
        return this;
    }

    public String getDescMain() {
        return descMain;
    }

    public String getUnit() {
        return unit;
    }

    public String getDescSecondary() {
        return descSecondary;
    }

    public boolean hasDescSecondary() {
        return descSecondary != null;
    }

    public String getValue() {
        return value;
    }

    public boolean hasValue() {
        return value != null;
    }

    public boolean isPrimary() {
        return isPrimary;
    }

    public boolean isLong() {
        return isLong;
    }
}
