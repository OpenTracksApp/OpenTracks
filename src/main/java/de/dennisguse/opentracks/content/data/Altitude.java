package de.dennisguse.opentracks.content.data;

public abstract class Altitude {

    private final double altitude_m;

    private Altitude(double altitude_m) {
        this.altitude_m = altitude_m;
    }

    public double toM() {
        return altitude_m;
    }

    public static class WGS84 extends Altitude {

        private WGS84(double altitude_m) {
            super(altitude_m);
        }


        public static Altitude of(double altitude_m) {
            return new WGS84(altitude_m);
        }
    }

    public static class EGM2008 extends Altitude {

        private EGM2008(double altitude_m) {
            super(altitude_m);
        }


        public static Altitude of(double altitude_m) {
            return new EGM2008(altitude_m);
        }
    }
}

