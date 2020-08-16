package de.dennisguse.opentracks.io.file;

import android.content.Context;

import java.util.Locale;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.content.provider.ContentProviderUtils;
import de.dennisguse.opentracks.io.file.exporter.FileTrackExporter;
import de.dennisguse.opentracks.io.file.exporter.GpxTrackWriter;
import de.dennisguse.opentracks.io.file.exporter.KmlTrackWriter;
import de.dennisguse.opentracks.io.file.exporter.KmzTrackExporter;
import de.dennisguse.opentracks.io.file.exporter.TrackExporter;
import de.dennisguse.opentracks.io.file.exporter.TrackWriter;

/**
 * Definition of all possible track formats.
 */
public enum TrackFileFormat {

    KML_ONLY_TRACK {
        @Override
        public TrackWriter newTrackWriter(Context context) {
            return new KmlTrackWriter(context, false, false, false);
        }

        @Override
        public String getMimeType() {
            return MIME_KML;
        }

        public String getExtension() {
            return "kml";
        }
    },
    KML_WITH_TRACKDETAIL {
        @Override
        public TrackWriter newTrackWriter(Context context) {
            return new KmlTrackWriter(context, true, false, false);
        }

        @Override
        public String getMimeType() {
            return MIME_KML;
        }

        public String getExtension() {
            return "kml";
        }
    },
    KML_WITH_TRACKDETAIL_AND_SENSORDATA {
        @Override
        public TrackWriter newTrackWriter(Context context) {
            return new KmlTrackWriter(context, true, true, false);
        }

        @Override
        public String getMimeType() {
            return MIME_KML;
        }

        public String getExtension() {
            return "kml";
        }
    },
    KMZ_ONLY_TRACK {

        private static final boolean exportPhotos = false;

        @Override
        public TrackWriter newTrackWriter(Context context) {
            return new KmlTrackWriter(context, false, false, exportPhotos);
        }

        public TrackExporter newTrackExporter(Context context) {
            return newKmzTrackExporter(context, this.newTrackWriter(context), exportPhotos);
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
    KMZ_WITH_TRACKDETAIL {

        private static final boolean exportPhotos = false;

        @Override
        public TrackWriter newTrackWriter(Context context) {
            return new KmlTrackWriter(context, true, false, exportPhotos);
        }

        public TrackExporter newTrackExporter(Context context) {
            return newKmzTrackExporter(context, this.newTrackWriter(context), exportPhotos);
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
    KMZ_WITH_TRACKDETAIL_AND_SENSORDATA {

        private static final boolean exportPhotos = false;

        @Override
        public TrackWriter newTrackWriter(Context context) {
            return new KmlTrackWriter(context, true, true, exportPhotos);
        }

        @Override
        public String getMimeType() {
            return MIME_KMZ;
        }

        public TrackExporter newTrackExporter(Context context) {
            return newKmzTrackExporter(context, this.newTrackWriter(context), exportPhotos);
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
        public TrackWriter newTrackWriter(Context context) {
            return new KmlTrackWriter(context, true, true, exportPhotos);
        }

        @Override
        public String getMimeType() {
            return MIME_KMZ;
        }

        public TrackExporter newTrackExporter(Context context) {
            return newKmzTrackExporter(context, newTrackWriter(context), exportPhotos);
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
        public TrackWriter newTrackWriter(Context context) {
            return new GpxTrackWriter(context.getString(R.string.app_name));
        }

        @Override
        public String getMimeType() {
            return "application/gpx+xml";
        }

        public String getExtension() {
            return "gpx";
        }
    };

    private static final String MIME_KMZ = "application/vnd.google-earth.kmz";

    private static final String MIME_KML = "application/vnd.google-earth.kml+xml";

    private static TrackExporter newKmzTrackExporter(Context context, TrackWriter trackWriter, boolean exportPhotos) {
        ContentProviderUtils contentProviderUtils = new ContentProviderUtils(context);

        FileTrackExporter fileTrackExporter = new FileTrackExporter(contentProviderUtils, trackWriter);

        return new KmzTrackExporter(context, contentProviderUtils, fileTrackExporter, exportPhotos);
    }

    /**
     * Returns the mime type for each format.
     */
    public abstract String getMimeType();

    public TrackExporter newTrackExporter(Context context) {
        ContentProviderUtils contentProviderUtils = new ContentProviderUtils(context);
        TrackWriter trackWriter = newTrackWriter(context);
        return new FileTrackExporter(contentProviderUtils, trackWriter);
    }

    /**
     * Creates a new track writer for the format.
     *
     * @param context the context
     */
    public abstract TrackWriter newTrackWriter(Context context);

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
     * Returns the name of for each format.
     */
    public String getName() {
        return this.name().toLowerCase(Locale.US);
    }
}