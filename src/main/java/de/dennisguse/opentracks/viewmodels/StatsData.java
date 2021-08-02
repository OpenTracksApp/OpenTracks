package de.dennisguse.opentracks.viewmodels;


import android.util.Pair;

import androidx.annotation.NonNull;

public class StatsData {
    private final String value;
    private final String unit;
    private final String descMain;
    private final String descSecondary;
    private final boolean isPrimary;
    private final boolean isWide;

    public StatsData(@NonNull Pair<String, String> valueAndUnit, String descMain, String descSecondary, boolean isPrimary, boolean isWide) {
        this.value = valueAndUnit.first;
        this.unit = valueAndUnit.second;
        this.descMain = descMain;
        this.descSecondary = descSecondary;
        this.isPrimary = isPrimary;
        this.isWide = isWide;
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

    public boolean isWide() {
        return isWide;
    }
}
