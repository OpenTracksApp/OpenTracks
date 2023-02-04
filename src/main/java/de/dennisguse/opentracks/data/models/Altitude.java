package de.dennisguse.opentracks.data.models;

import androidx.annotation.NonNull;

import de.dennisguse.opentracks.R;

public abstract class Altitude {

    private final double altitudeM;

    private Altitude(double altitudeM) {
        this.altitudeM = altitudeM;
    }

    public double toM() {
        return altitudeM;
    }

    public abstract Altitude replace(double altitudeM);

    public abstract int getLabelId();

    public static class WGS84 extends Altitude {

        private WGS84(double altitudeM) {
            super(altitudeM);
        }

        @Override
        public int getLabelId() {
            return R.string.wgs84;
        }

        public static Altitude of(double altitudeM) {
            return new WGS84(altitudeM);
        }

        @Override
        public Altitude replace(double altitudeM) {
            return new WGS84(altitudeM);
        }
    }

    public static class EGM2008 extends Altitude {

        private EGM2008(double altitudeM) {
            super(altitudeM);
        }

        @Override
        public int getLabelId() {
            return R.string.egm2008;
        }

        public static Altitude of(double altitudeM) {
            return new EGM2008(altitudeM);
        }

        @Override
        public Altitude replace(double altitudeM) {
            return new EGM2008(altitudeM);
        }
    }

    @NonNull
    @Override
    public String toString() {
        return "Altitude{" +
                "altitudeM=" + altitudeM + this.getClass().getSimpleName() +
                '}';
    }
}
