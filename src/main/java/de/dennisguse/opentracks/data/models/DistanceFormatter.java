package de.dennisguse.opentracks.data.models;

import android.content.Context;
import android.content.res.Resources;
import android.util.Pair;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.util.StringUtils;

public class DistanceFormatter {

    private final Resources resources;

    private final int decimalCount;

    private final boolean metricUnits;

    private DistanceFormatter(Resources resources, int decimalCount, boolean metricUnits) {
        this.resources = resources;
        this.decimalCount = decimalCount;
        this.metricUnits = metricUnits;
    }

    public String formatDistance(Distance distance) {
        if (distance.isInvalid()) {
            return resources.getString(R.string.value_unknown);
        }

        Pair<String, String> distanceParts = getDistanceParts(distance);

        return resources.getString(R.string.distance_with_unit, distanceParts.first, distanceParts.second);
    }

    /**
     * Get the formatted distance with unit.
     *
     * @param metricUnits true to use metric unit
     * @return the formatted distance (or null) and it's unit as {@link Pair}
     */
    public Pair<String, String> getDistanceParts(Distance distance) {
        if (distance.isInvalid()) {
            return new Pair<>(null, resources.getString(metricUnits ? R.string.unit_meter : R.string.unit_feet));
        }

        if (metricUnits) {
            if (distance.greaterThan(Distance.of(500))) {
                return new Pair<>(StringUtils.formatDecimal(distance.toKM(), decimalCount), resources.getString(R.string.unit_kilometer));
            } else {
                return new Pair<>(StringUtils.formatDecimal(distance.toM(), decimalCount), resources.getString(R.string.unit_meter));
            }
        } else {
            if (distance.greaterThan(Distance.ofMile(0.5))) {
                return new Pair<>(StringUtils.formatDecimal(distance.toMI(), decimalCount), resources.getString(R.string.unit_mile));
            } else {
                return new Pair<>(StringUtils.formatDecimal(distance.toFT(), decimalCount), resources.getString(R.string.unit_feet));
            }
        }
    }

    public static Builder Builder() {
        return new Builder();
    }

    public static class Builder {

        private int decimalCount;

        private boolean metricUnits;

        public Builder() {
            decimalCount = 2;
            metricUnits = true;
        }

        public Builder setDecimalCount(int decimalCount) {
            this.decimalCount = decimalCount;
            return this;
        }

        public Builder setMetricUnits(boolean metricUnits) {
            this.metricUnits = metricUnits;
            return this;
        }

        public DistanceFormatter build(Resources resource) {
            return new DistanceFormatter(resource, decimalCount, metricUnits);
        }

        public DistanceFormatter build(Context context) {
            return build(context.getResources());
        }
    }
}
