package de.dennisguse.opentracks.data.models;

import android.content.Context;
import android.content.res.Resources;
import android.util.Pair;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.util.StringUtils;

public class SpeedFormatter {

    private final Resources resources;

    private final int decimalCount;

    private final boolean metricUnits;

    private final boolean reportSpeedOrPace;

    private SpeedFormatter(Resources resources, int decimalCount, boolean metricUnits, boolean reportSpeedOrPace) {
        this.resources = resources;
        this.decimalCount = decimalCount;
        this.metricUnits = metricUnits;
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
        int unitId;
        if (metricUnits) {
            unitId = reportSpeedOrPace ? R.string.unit_kilometer_per_hour : R.string.unit_minute_per_kilometer;
        } else {
            unitId = reportSpeedOrPace ? R.string.unit_mile_per_hour : R.string.unit_minute_per_mile;
        }
        String unitString = resources.getString(unitId);

        if (speed == null) {
            speed = Speed.zero();
        }

        if (reportSpeedOrPace) {
            return new Pair<>(StringUtils.formatDecimal(speed.to(metricUnits), 1), unitString);
        }

        int pace = (int) speed.toPace(metricUnits).getSeconds();

        int minutes = pace / 60;
        int seconds = pace % 60;
        return new Pair<>(resources.getString(R.string.time, minutes, seconds), unitString);
    }

    public static Builder Builder() {
        return new Builder();
    }

    public static class Builder {

        private int decimalCount;

        private boolean metricUnits;

        private boolean reportSpeedOrPace;

        public Builder() {
            decimalCount = 2;
            metricUnits = true;
            reportSpeedOrPace = true;
        }

        public Builder setDecimalCount(int decimalCount) {
            this.decimalCount = decimalCount;
            return this;
        }

        public Builder setMetricUnits(boolean metricUnits) {
            this.metricUnits = metricUnits;
            return this;
        }

        public Builder setReportSpeedOrPace(boolean reportSpeedOrPace) {
            this.reportSpeedOrPace = reportSpeedOrPace;
            return this;
        }

        public SpeedFormatter build(Resources resource) {
            return new SpeedFormatter(resource, decimalCount, metricUnits, reportSpeedOrPace);
        }

        public SpeedFormatter build(Context context) {
            return build(context.getResources());
        }
    }
}
