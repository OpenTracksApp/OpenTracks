package de.dennisguse.opentracks.data.models;

import android.content.res.Resources;
import androidx.annotation.Nullable;
import android.content.Context;


import de.dennisguse.opentracks.settings.UnitSystem;

public class CaloriesFormatter {

    private final Resources resources;

    private final int decimalCount;

    private final UnitSystem unitSystem;

    private CaloriesFormatter(Resources resources, int decimalCount, UnitSystem unitSystem) {
        this.resources = resources;
        this.decimalCount = decimalCount;
        this.unitSystem = unitSystem;
        assert unitSystem != null;
    }

    public static Builder Builder() {
        return new Builder();
    }

    public static class Builder {

        private int decimalCount;

        private UnitSystem unitSystem;

        public Builder() {
            decimalCount = 2;
        }

        public Builder setDecimalCount(int decimalCount) {
            this.decimalCount = decimalCount;
            return this;
        }

        public Builder setUnit(@Nullable UnitSystem unitSystem) {
            this.unitSystem = unitSystem;
            return this;
        }
    }
}