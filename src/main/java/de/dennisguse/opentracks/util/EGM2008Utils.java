package de.dennisguse.opentracks.util;

import android.content.Context;
import android.location.Location;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Objects;

import de.dennisguse.opentracks.R;

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

    /**
     * Returns the latitude index corresponding to the given location, based on the EGM2008 dataset.
     *
     * @param location the location to calculate the latitude index for
     * @return the latitude index
     */
    public static int getLatitudeIndex(Location location) {
        double latitude = -location.getLatitude() + 90;
        return (int) (latitude * RESOLUTION_IN_MINUTES);
    }

    /**
     * Returns the longitude index corresponding to the given location, based on the EGM2008 dataset.
     *
     * @param location the location to calculate the longitude index for
     * @return the longitude index
     */
    public static int getLongitudeIndex(Location location) {
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
        return longitudeIndex;
    }

    /**
     * Calculates the correction for the EGM2008 geoid model at the given location.
     *
     * @param context  the Android context
     * @param location the location to calculate the correction for
     * @return an EGM2008Correction object representing the correction at the given location
     * @throws IOException if an I/O error occurs
     */
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

    /**
     * Retrieves the raw undulation at the given index from the EGM2008 data input stream.
     *
     * @param dataInputStream the EGM2008 data input stream
     * @param undulationIndex the index of the undulation to retrieve
     * @return the raw undulation value at the given index
     * @throws IOException if an I/O error occurs
     */
    @VisibleForTesting
    static int getUndulationRaw(DataInputStream dataInputStream, int undulationIndex) throws IOException {
        int index = HEADER_LENGTH + undulationIndex * 2; //byte size is 2
        long ignored = dataInputStream.skip(index);
        return dataInputStream.readUnsignedShort();
    }

    /**
     * Calculates the indices into the EGM2008 data array for the given location.
     *
     * @param location the location to calculate the indices for
     * @return an Indices object representing the calculated indices
     */
    @VisibleForTesting
    static Indices getIndices(Location location) {
        double latitude = -location.getLatitude() + 90;
        int latitudeIndex = (int) (latitude * RESOLUTION_IN_MINUTES);

        double longitude = location.getLongitude();
        if (longitude < 0) {
            longitude += 360;
        }
        int longitudeIndex = (int) (longitude * RESOLUTION_IN_MINUTES) % (360 * RESOLUTION_IN_MINUTES);

        return new Indices(latitudeIndex, longitudeIndex);
    }

    /**
     * A class that stores the latitude and longitude indices for EGM2008 correction.
     */
    @VisibleForTesting
    static class Indices {
        /**
         * The latitude index.
         */
        final int latitudeIndex;

        /**
         * The longitude index.
         */
        final int longitudeIndex;

        /**
         * Constructs indices with the given latitude and longitude indices.
         *
         * @param latitudeIndex  the latitude index
         * @param longitudeIndex the longitude index
         */
        Indices(int latitudeIndex, int longitudeIndex) {
            this.latitudeIndex = latitudeIndex;
            this.longitudeIndex = longitudeIndex;
        }

        /**
         * Returns indices that are offset from the current indices by the given latitude and longitude offset.
         *
         * @param latitudeOffset  the latitude offset
         * @param longitudeOffset the longitude offset
         * @return the new indices with the offset
         */
        Indices offset(int latitudeOffset, int longitudeOffset) {
            return new Indices(latitudeIndex + latitudeOffset, longitudeIndex + longitudeOffset);
        }

        /**
         * Returns the absolute index of the current indices.
         *
         * @return the absolute index
         */
        int getAbsoluteIndex() {
            return latitudeIndex * LATITUDE_CORRECTION + longitudeIndex;
        }

        /**
         * Returns true if the given object is equal to the current object.
         *
         * @param o the object to compare
         * @return true if the objects are equal, false otherwise
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Indices indices = (Indices) o;
            return latitudeIndex == indices.latitudeIndex && longitudeIndex == indices.longitudeIndex;
        }

        /**
         * Returns the hash code of the current object.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return Objects.hash(latitudeIndex, longitudeIndex);
        }

        /**
         * Returns a string representation of the current indices.
         *
         * @return the string representation
         */
        @Override
        public String toString() {
            return "Indices{" +
                    "latitudeIndex=" + latitudeIndex +
                    ", longitudeIndex=" + longitudeIndex +
                    '}';
        }
    }


    /**
     * This class provides corrections for the EGM2008 geoid model.
     */
    public static final class EGM2008Correction {

        /**
         * The number of arc-minute per degree.
         */
        private static final double MINUTES_PER_DEGREE = 60.0;

        /**
         * The geoid undulation data in integer format.
         */
        private final int v00;
        private final int v10;
        private final int v01;
        private final int v11;

        /**
         * The indices of the geoid undulation data.
         */
        private final Indices indices;

        /**
         * Constructs an instance of the EGM2008Correction class.
         *
         * @param indices         the indices of the geoid undulation data
         * @param dataInputStream the input stream of the geoid undulation data
         * @throws IOException if there is an I/O error
         */
        public EGM2008Correction(final Indices indices, final DataInputStream dataInputStream) throws IOException {
            this.indices = indices;
            v00 = getUndulationRaw(dataInputStream, indices);
            if (isSouthPole()) {
                v10 = 0;
                v01 = 0;
                v11 = 0;
            } else {
                v10 = getUndulationRaw(dataInputStream, indices.offset(0, 1));
                v01 = getUndulationRaw(dataInputStream, indices.offset(1, 0));
                v11 = getUndulationRaw(dataInputStream, indices.offset(1, 1));
            }
        }

        /**
         * Returns the latitude and longitude of the location for which the correction is being applied.
         *
         * @param location the location for which the correction is being applied
         * @return a double array containing the latitude and longitude in degrees
         */
        public static double[] getLatitudeLongitude(@NonNull Location location) {
            double latitude = location.getLatitude();
            double longitude = location.getLongitude();

            if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
                throw new IllegalArgumentException("Invalid latitude or longitude");
            }

            return new double[]{latitude, longitude};
        }

        /**
         * Calculates the undulation correction for a given latitude and longitude index using the EGM2008 data from the specified DataInputStream.
         *
         * @param dataInputStream the DataInputStream containing the EGM2008 data
         * @param latitudeIndex   the index of the latitude in the EGM2008 data
         * @param longitudeIndex  the index of the longitude in the EGM2008 data
         * @return the undulation correction at the given latitude and longitude
         * @throws IOException if there is an error reading from the DataInputStream
         */
        public static double getUndulationCorrection(DataInputStream dataInputStream, int latitudeIndex, int longitudeIndex) throws IOException {
            int undulationIndex = latitudeIndex * 3601 + longitudeIndex;
            int index = HEADER_LENGTH + undulationIndex * 2;
            long ignored = dataInputStream.skip(index);
            int undulationRaw = dataInputStream.readUnsignedShort();
            return (double) undulationRaw * 0.1;
        }

        /**
         * Returns a value indicating whether this instance can correct the altitude of a location.
         *
         * @param location the location to be corrected
         * @return true if the altitude can be corrected, otherwise false
         */
        public boolean canCorrect(final Location location) {
            return indices.getAbsoluteIndex() == getIndices(location).getAbsoluteIndex();
        }

        /**
         * Corrects the altitude of a location using the EGM2008 geoid model.
         *
         * @param location the location to be corrected
         * @return the corrected altitude
         * @throws RuntimeException if the altitude of the location is not available or the geoid undulation data is not loaded for this location
         */
        public double correctAltitude(final Location location) {
            if (!location.hasAltitude()) {
                throw new RuntimeException("Location has no altitude");
            }

            if (!canCorrect(location)) {
                throw new RuntimeException("Undulation data not loaded for this location.");
            }

            double undulationRaw = v00;
            if (!isSouthPole()) {
                final double fLongitude = (location.getLongitude() * MINUTES_PER_DEGREE) - Math.floor(location.getLongitude() * MINUTES_PER_DEGREE);
                final double fLatitude = (Math.abs(location.getLatitude()) * MINUTES_PER_DEGREE) - Math.floor(Math.abs(location.getLatitude()) * MINUTES_PER_DEGREE);
                final double a = (1 - fLongitude) * v00 + fLongitude * v01;
                final double b = (1 - fLongitude) * v10 + fLongitude * v11;
                undulationRaw = (1 - fLatitude) * a + fLatitude * b;
            }
            return location.getAltitude() - (0.003 * undulationRaw - 108);
        }

        /**
         * Returns a value indicating whether this instance is for the South Pole.
         *
         * @return true if this instance is for the South Pole, otherwise false
         */
        private boolean isSouthPole() {
            return indices.latitudeIndex == 2160;
        }


    }
}
