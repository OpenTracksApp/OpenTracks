package de.dennisguse.opentracks.io.file;

import android.content.Context;
import android.content.res.Resources;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.data.ContentProviderUtils;
import de.dennisguse.opentracks.io.file.exporter.CSVTrackExporter;
import de.dennisguse.opentracks.io.file.exporter.GPXTrackExporter;
import de.dennisguse.opentracks.io.file.exporter.KMLTrackExporter;
import de.dennisguse.opentracks.io.file.exporter.KmzTrackExporter;
import de.dennisguse.opentracks.io.file.exporter.TrackExporter;

/**
 * Definition of all possible track formats.
 * <p>
 * NOTE: The names of the entries are used in the user's settings.
 */
public enum TrackFileFormat {

    KML_WITH_TRACKDETAIL_AND_SENSORDATA("KML_WITH_TRACKDETAIL_AND_SENSORDATA") {
        @Override
        public TrackExporter createTrackExporter(Context context) {
            return new KMLTrackExporter(context, false);
        }

        @Override
        public String getMimeType() {
            return MIME_KML;
        }

        public String getExtension() {
            return "kml";
        }
    },

    @Deprecated //TODO Check if we really need this
    KMZ_WITH_TRACKDETAIL_AND_SENSORDATA("KMZ_WITH_TRACKDETAIL_AND_SENSORDATA") {

        private static final boolean EXPORT_PHOTOS = false;

        @Override
        public TrackExporter createTrackExporter(Context context) {
            KMLTrackExporter exporter = new KMLTrackExporter(context, EXPORT_PHOTOS);
            return new KmzTrackExporter(context, new ContentProviderUtils(context), exporter, EXPORT_PHOTOS);
        }

        @Override
        public String getMimeType() {
            return MIME_KMZ;
        }

        public String getExtension() {
            return "kmz";
        }

        @Override
        public boolean includesPhotos() {
            return EXPORT_PHOTOS;
        }
    },

    KMZ_WITH_TRACKDETAIL_AND_SENSORDATA_AND_PICTURES("KMZ_WITH_TRACKDETAIL_AND_SENSORDATA_AND_PICTURES") {

        private static final boolean EXPORT_PHOTOS = true;

        @Override
        public TrackExporter createTrackExporter(Context context) {
            KMLTrackExporter exporter = new KMLTrackExporter(context, EXPORT_PHOTOS);
            return new KmzTrackExporter(context, new ContentProviderUtils(context), exporter, EXPORT_PHOTOS);
        }

        @Override
        public String getMimeType() {
            return MIME_KMZ;
        }

        public String getExtension() {
            return "kmz";
        }

        @Override
        public boolean includesPhotos() {
            return EXPORT_PHOTOS;
        }

    },

    GPX("GPX") {
        @Override
        public TrackExporter createTrackExporter(Context context) {
            return new GPXTrackExporter(new ContentProviderUtils(context), context.getString(R.string.app_name));
        }

        @Override
        public String getMimeType() {
            return "application/gpx+xml";
        }

        public String getExtension() {
            return "gpx";
        }
    },

    CSV("CSV") {
        @Override
        public TrackExporter createTrackExporter(Context context) {
            return new CSVTrackExporter(new ContentProviderUtils(context));
        }

        @Override
        public String getMimeType() {
            return "text/csv";
        }

        @Override
        public String getExtension() {
            return "csv";
        }
    };

    private static final String MIME_KMZ = "application/vnd.google-earth.kmz";

    private static final String MIME_KML = "application/vnd.google-earth.kml+xml";

    private final String preferenceId;

    TrackFileFormat(String preferenceId) {
        this.preferenceId = preferenceId;
    }

    public static Map<String, String> toPreferenceIdLabelMap(final Resources resources, final TrackFileFormat ... trackFileFormats) {
        Map<String, String> preferenceIdLabelMap = new LinkedHashMap<>(trackFileFormats.length);
        for (TrackFileFormat trackFileFormat : trackFileFormats) {
            String trackFileFormatUpperCase = trackFileFormat.getExtension().toUpperCase(Locale.US); //ASCII upper case
            int photoMessageId = trackFileFormat.includesPhotos() ? R.string.export_with_photos : R.string.export_without_photos;
            preferenceIdLabelMap.put(trackFileFormat.getPreferenceId(), String.format("%s (%s)", trackFileFormatUpperCase, resources.getString(photoMessageId)));
        }
        return preferenceIdLabelMap;
    }

    public static TrackFileFormat valueOfPreferenceId(final String preferenceId) {
        return Arrays.stream(values())
                .filter(trackFileFormat -> trackFileFormat.getPreferenceId().equals(preferenceId))
                .findFirst()
                .orElse(null);
    }

    /**
     * Returns the mime type for each format.
     */
    public abstract String getMimeType();

    /**
     * Creates a new track writer for the format.
     *
     * @param context the context
     */
    public abstract TrackExporter createTrackExporter(Context context);

    /**
     * Returns the file extension for each format.
     */
    public abstract String getExtension();

    /**
     * Returns whether the format supports photos.
     */
    public boolean includesPhotos() {
        return false;
    }

    /**
     * The identifier to be stored in the preferences.
     */
    public String getPreferenceId() {
        return preferenceId;
    }
}