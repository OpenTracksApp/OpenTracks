package de.dennisguse.opentracks.io.file;

import android.content.Context;
import android.graphics.Bitmap;

import java.util.Locale;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.content.provider.ContentProviderUtils;
import de.dennisguse.opentracks.io.file.exporter.GPXTrackExporter;
import de.dennisguse.opentracks.io.file.exporter.KMLTrackExporter;
import de.dennisguse.opentracks.io.file.exporter.KmzTrackExporter;
import de.dennisguse.opentracks.io.file.exporter.SharePictureTrackExporter;
import de.dennisguse.opentracks.io.file.exporter.TrackExporter;

/**
 * Definition of all possible track formats.
 * <p>
 * NOTE: The names of the entries are used in the user's settings.
 */
public enum TrackFileFormat {

    KML_WITH_TRACKDETAIL_AND_SENSORDATA {
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
    KMZ_WITH_TRACKDETAIL_AND_SENSORDATA {

        private static final boolean exportPhotos = false;

        @Override
        public TrackExporter createTrackExporter(Context context) {
            KMLTrackExporter exporter = new KMLTrackExporter(context, exportPhotos);
            return new KmzTrackExporter(context, new ContentProviderUtils(context), exporter, exportPhotos);
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
            return exportPhotos;
        }
    },

    KMZ_WITH_TRACKDETAIL_AND_SENSORDATA_AND_PICTURES {

        private static final boolean exportPhotos = true;

        @Override
        public TrackExporter createTrackExporter(Context context) {
            KMLTrackExporter exporter = new KMLTrackExporter(context, exportPhotos);
            return new KmzTrackExporter(context, new ContentProviderUtils(context), exporter, exportPhotos);
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
            return exportPhotos;
        }

    },

    GPX {
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

    SHARE_PICTURE_PNG {
        @Override
        public TrackExporter createTrackExporter(Context context) {
            return new SharePictureTrackExporter(context, Bitmap.CompressFormat.PNG);
        }

        @Override
        public String getMimeType() {
            return "image/png";
        }

        public String getExtension() {
            return "png";
        }
    };

    private static final String MIME_KMZ = "application/vnd.google-earth.kmz";

    private static final String MIME_KML = "application/vnd.google-earth.kml+xml";

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
     * Returns the name for each format.
     */
    public String getName() {
        return this.name().toLowerCase(Locale.US);
    }
}