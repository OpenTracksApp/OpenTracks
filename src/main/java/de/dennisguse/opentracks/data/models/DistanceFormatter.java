package de.dennisguse.opentracks.data.models;

import android.content.Context;
import android.content.res.Resources;
import android.util.Pair;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.util.StringUtils;

public class DistanceFormatter {

    private final Resources resources;

    private final int decimalCount;

    private DistanceFormatter(Resources resources, int decimalCount) {
        this.resources = resources;
        this.decimalCount = decimalCount;
    }

    public String formatDistance(Distance distance, boolean metricUnits) {
        if (distance.isInvalid()) {
            return resources.getString(R.string.value_unknown);
        }

        Pair<String, String> distanceParts = getDistanceParts(distance, metricUnits);

        return resources.getString(R.string.distance_with_unit, distanceParts.first, distanceParts.second);
    }

    /**
     * Get the formatted distance with unit.
     *
     * @param metricUnits true to use metric unit
     * @return the formatted distance (or null) and it's unit as {@link Pair}
     */
    public Pair<String, String> getDistanceParts(Distance distance, boolean metricUnits) {
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

        public Builder() {
            decimalCount = 2;
        }

        public Builder setDecimalCount(int decimalCount) {
            this.decimalCount = decimalCount;
            return this;
        }

        public DistanceFormatter build(Resources resource) {
            return new DistanceFormatter(resource, decimalCount);
        }

        public DistanceFormatter build(Context context) {
            return build(context.getResources());
        }
    }
}
