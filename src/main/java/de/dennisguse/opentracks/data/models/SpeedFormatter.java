package de.dennisguse.opentracks.data.models;

import android.content.Context;
import android.content.res.Resources;
import android.util.Pair;

import androidx.annotation.NonNull;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.settings.UnitSystem;
import de.dennisguse.opentracks.util.StringUtils;

public class SpeedFormatter {

    private final Resources resources;

    private final int decimalCount;

    private final UnitSystem unitSystem;

    private final boolean reportSpeedOrPace;

    private SpeedFormatter(Resources resources, int decimalCount, UnitSystem unitSystem, boolean reportSpeedOrPace) {
        this.resources = resources;
        this.decimalCount = decimalCount;
        this.unitSystem = unitSystem;
        this.reportSpeedOrPace = reportSpeedOrPace;
    }

    public String formatSpeed(Speed speed) {
        Pair<String, String> distanceParts = getSpeedParts(speed);

        return resources.getString(R.string.speed_with_unit, distanceParts.first, distanceParts.second);
    }

    /**
     * Gets the formatted speed with unit.
     *
     * @return the formatted speed (or null) and it's unit as {@link Pair}
     */
    public Pair<String, String> getSpeedParts(Speed speed) {
        int unitId = switch (unitSystem) {
            case METRIC ->
                    reportSpeedOrPace ? R.string.unit_kilometer_per_hour : R.string.unit_minute_per_kilometer;
            case IMPERIAL_FEET, IMPERIAL_METER ->
                    reportSpeedOrPace ? R.string.unit_mile_per_hour : R.string.unit_minute_per_mile;
            case NAUTICAL_IMPERIAL ->
                    reportSpeedOrPace ? R.string.unit_knots : R.string.unit_minute_per_nautical_mile;
        };

        String unitString = resources.getString(unitId);

        if (speed == null) {
            speed = Speed.zero();
        }

        if (reportSpeedOrPace) {
            return new Pair<>(StringUtils.formatDecimal(speed.to(unitSystem), 1), unitString);
        }

        int pace = (int) speed.toPace(unitSystem).getSeconds();

        int minutes = pace / 60;
        int seconds = pace % 60;
        return new Pair<>(resources.getString(R.string.time, minutes, seconds), unitString);
    }

    public static Builder Builder() {
        return new Builder();
    }

    public static class Builder {

        private int decimalCount;

        private UnitSystem unitSystem;

        private boolean reportSpeedOrPace;

        public Builder() {
            decimalCount = 2;
            reportSpeedOrPace = true;
        }

        public Builder setDecimalCount(int decimalCount) {
            this.decimalCount = decimalCount;
            return this;
        }

        public Builder setUnit(@NonNull UnitSystem unitSystem) {
            this.unitSystem = unitSystem;
            return this;
        }

        public Builder setReportSpeedOrPace(boolean reportSpeedOrPace) {
            this.reportSpeedOrPace = reportSpeedOrPace;
            return this;
        }

        public SpeedFormatter build(Resources resource) {
            return new SpeedFormatter(resource, decimalCount, unitSystem, reportSpeedOrPace);
        }

        public SpeedFormatter build(Context context) {
            return build(context.getResources());
        }
    }
}
