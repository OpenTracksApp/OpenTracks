package de.dennisguse.opentracks.data.models;

import android.content.Context;
import android.content.res.Resources;
import android.util.Pair;

import androidx.annotation.Nullable;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.settings.UnitSystem;
import de.dennisguse.opentracks.util.StringUtils;

public class DistanceFormatter {

    private final Resources resources;

    private final int decimalCount;

    private final UnitSystem unitSystem;

    private DistanceFormatter(Resources resources, int decimalCount, UnitSystem unitSystem) {
        this.resources = resources;
        this.decimalCount = decimalCount;
        this.unitSystem = unitSystem;
        assert unitSystem != null;
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
     * @return the formatted distance (or null) and it's unit as {@link Pair}
     */
    public Pair<String, String> getDistanceParts(Distance distance) {
        if (distance.isInvalid()) {
            switch (unitSystem) {
                case METRIC:
                    return new Pair<>(null, resources.getString(R.string.unit_meter));
                case IMPERIAL:
                case NAUTICAL_IMPERIAL:
                    return new Pair<>(null, resources.getString(R.string.unit_feet));
                default:
                    throw new RuntimeException("Not implemented");
            }
        }

        switch (unitSystem) {
            case METRIC:
                if (distance.greaterThan(Distance.of(500))) {
                    return new Pair<>(StringUtils.formatDecimal(distance.toKM(), decimalCount), resources.getString(R.string.unit_kilometer));
                } else {
                    return new Pair<>(StringUtils.formatDecimal(distance.toM(), decimalCount), resources.getString(R.string.unit_meter));
                }
            case IMPERIAL:
                if (distance.greaterThan(Distance.ofMile(0.5))) {
                    return new Pair<>(StringUtils.formatDecimal(distance.toMI(), decimalCount), resources.getString(R.string.unit_mile));
                } else {
                    return new Pair<>(StringUtils.formatDecimal(distance.toFT(), decimalCount), resources.getString(R.string.unit_feet));
                }
            case NAUTICAL_IMPERIAL:
                if (distance.greaterThan(Distance.ofNauticalMile(0.5))) {
                    return new Pair<>(StringUtils.formatDecimal(distance.toNauticalMiles(), decimalCount), resources.getString(R.string.unit_nautical_mile));
                } else {
                    return new Pair<>(StringUtils.formatDecimal(distance.toFT(), decimalCount), resources.getString(R.string.unit_feet));
                }
            default:
                throw new RuntimeException("Not implemented");
        }
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

        public DistanceFormatter build(Resources resource) {
            return new DistanceFormatter(resource, decimalCount, unitSystem);
        }

        public DistanceFormatter build(Context context) {
            return build(context.getResources());
        }
    }
}
