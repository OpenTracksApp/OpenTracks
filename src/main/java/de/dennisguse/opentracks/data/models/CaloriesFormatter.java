package de.dennisguse.opentracks.data.models;
import android.content.res.Resources;

import de.dennisguse.opentracks.settings.UnitSystem;

public class CaloriesFormatter {

    private final Resources resources;

    private final int decimalCount;

    private final UnitSystem unitSystem;

//# ------ Create Unit Formatter ----------------------

    private CaloriesFormatter(Resources resources, int decimalCount, UnitSystem unitSystem) {
        this.resources = resources;
        this.decimalCount = decimalCount;
        this.unitSystem = unitSystem;
        assert unitSystem != null;
    }


}