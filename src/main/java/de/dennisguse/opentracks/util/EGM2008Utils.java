package de.dennisguse.opentracks.util;

import android.content.Context;
import android.location.Location;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import java.io.DataInputStream;
import java.io.IOException;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.data.models.Altitude;

/**
 * Converts WGS84 altitude to EGM2008 (should be close to height above sea level).
 * <p>
 * Uses <a href="https://geographiclib.sourceforge.io/">GeographicLib</a>] EGM2008 5minute undulation data.
 * <a href="https://geographiclib.sourceforge.io/html/geoid.html">...</a>
 * <p>
 * File starts at 90N, 0E (North pole) and is encoded in parallel bands as unsigned shorts.
 */
public class EGM2008Utils {

    static final int EGM2008_5_DATA = R.raw.egm2008_5;

    private static final int HEADER_LENGTH = 404;

    private static final int RESOLUTION_IN_MINUTES = 60 / 5;

    private static final int LATITUDE_CORRECTION = 360 * RESOLUTION_IN_MINUTES;

    private EGM2008Utils() {
    }

    public static EGM2008Correction createCorrection(Context context, Location location) throws IOException {
        Indices indices = getIndices(location);

        try (DataInputStream dataInputStream = new DataInputStream(context.getResources().openRawResource(EGM2008_5_DATA))) {
            return new EGM2008Correction(indices, dataInputStream);
        }
    }

    @VisibleForTesting
    static int getUndulationRaw(DataInputStream dataInputStream, Indices indices) throws IOException {
        dataInputStream.reset();
        int absoluteIndex = indices.getAbsoluteIndex();
        return getUndulationRaw(dataInputStream, absoluteIndex);
    }

    private static int getUndulationRaw(DataInputStream dataInputStream, int undulationIndex) throws IOException {
        dataInputStream.reset();
        int index = HEADER_LENGTH + undulationIndex * 2;  //byte size is 2
        long ignored = dataInputStream.skip(index);

        return dataInputStream.readUnsignedShort();
    }

    @VisibleForTesting
    static Indices getIndices(Location location) {
        double latitude = -location.getLatitude() + 90;
        int latitudeIndex = (int) (latitude * RESOLUTION_IN_MINUTES);

        double longitude;
        if (location.getLongitude() >= 0) {
            longitude = location.getLongitude();
        } else {
            longitude = 360 + location.getLongitude();
        }
        int longitudeIndex = (int) (longitude * RESOLUTION_IN_MINUTES);

        if (longitudeIndex >= 360 * RESOLUTION_IN_MINUTES) {
            longitudeIndex = 0;
        }
        return new Indices(latitudeIndex, longitudeIndex);
    }

    public static class EGM2008Correction {

        protected final Indices indices;

        protected final int v00;
        protected final int v10;
        protected final int v01;
        protected final int v11;

        private EGM2008Correction(Indices indices, DataInputStream dataInputStream) throws IOException {
            this.indices = indices;

            v00 = getUndulationRaw(dataInputStream, indices);
            if (!isSouthPole()) {
                v10 = getUndulationRaw(dataInputStream, indices.offset(0, 1));
                v01 = getUndulationRaw(dataInputStream, indices.offset(1, 0));
                v11 = getUndulationRaw(dataInputStream, indices.offset(1, 1));
            } else {
                v10 = 0;
                v01 = 0;
                v11 = 0;
            }
        }

        public boolean canCorrect(@NonNull Location location) {
            return indices.getAbsoluteIndex() == getIndices(location).getAbsoluteIndex();
        }

        public Altitude.EGM2008 correctAltitude(@NonNull Location location) {
            if (!canCorrect(location))
                throw new RuntimeException("Undulation data not loaded for this location.");
            if (!location.hasAltitude())
                throw new RuntimeException("Location has no altitude");

            double undulationRaw;
            if (isSouthPole()) {
                // No bilinear interpolation on South Pole (not worth the time)
                undulationRaw = v00;
            } else {
                double fLongitude = location.getLongitude() * RESOLUTION_IN_MINUTES -
                        (int) (location.getLongitude() * RESOLUTION_IN_MINUTES);

                double fLatitude = (-location.getLatitude() + 90) * RESOLUTION_IN_MINUTES
                        - (int) ((-location.getLatitude() + 90) * RESOLUTION_IN_MINUTES);

                // Bilinear interpolation (optimized; taken from GeopgrahicLib/Geoid.cpp)
                double
                        a = (1 - fLongitude) * v00 + fLongitude * v01,
                        b = (1 - fLongitude) * v10 + fLongitude * v11;
                undulationRaw = (1 - fLatitude) * a + fLatitude * b;
            }
            // Bilinear interpolation (not optimized)
            //undulationRaw = v00 * (1 - fLongitude) * (1 - fLatitude)
            //        + v10 * fLongitude * (1 - fLatitude)
            //        + v01 * (1 - fLongitude) * fLatitude
            //        + v11 * fLongitude * fLatitude;

            double h = 0.003 * undulationRaw - 108;

            return Altitude.EGM2008.of(location.getAltitude() - h);
        }

        private boolean isSouthPole() {
            return indices.latitudeIndex == 2160;
        }
    }

    @VisibleForTesting
    record Indices(int latitudeIndex, int longitudeIndex) {

        Indices offset(int latitudeOffset, int longitudeOffset) {
                return new Indices(latitudeIndex + latitudeOffset, longitudeIndex + longitudeOffset);
            }

            int getAbsoluteIndex() {
                return latitudeIndex * LATITUDE_CORRECTION + longitudeIndex;
            }
        }
}
