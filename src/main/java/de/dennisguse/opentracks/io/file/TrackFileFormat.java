package de.dennisguse.opentracks.io.file;

import android.content.Context;

import java.util.Locale;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.content.data.Track;
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
        public TrackWriter newTrackWriter(Context context, boolean multiple) {
            return new KmlTrackWriter(context, multiple, false, false, false);
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
        public TrackWriter newTrackWriter(Context context, boolean multiple) {
            return new KmlTrackWriter(context, multiple, true, false, false);
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
        public TrackWriter newTrackWriter(Context context, boolean multiple) {
            return new KmlTrackWriter(context, multiple, true, true, false);
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
        public TrackWriter newTrackWriter(Context context, boolean multiple) {
            return new KmlTrackWriter(context, multiple, false, false, exportPhotos);
        }

        public TrackExporter newTrackExporter(Context context, Track[] tracks) {
            return newKmzTrackExporter(context, this.newTrackWriter(context, tracks.length > 1), tracks, exportPhotos);
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
        public TrackWriter newTrackWriter(Context context, boolean multiple) {
            return new KmlTrackWriter(context, multiple, true, false, exportPhotos);
        }

        public TrackExporter newTrackExporter(Context context, Track[] tracks) {
            return newKmzTrackExporter(context, this.newTrackWriter(context, tracks.length > 1), tracks, exportPhotos);
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
        public TrackWriter newTrackWriter(Context context, boolean multiple) {
            return new KmlTrackWriter(context, multiple, true, true, exportPhotos);
        }

        @Override
        public String getMimeType() {
            return MIME_KMZ;
        }

        public TrackExporter newTrackExporter(Context context, Track[] tracks) {
            return newKmzTrackExporter(context, this.newTrackWriter(context, tracks.length > 1), tracks, exportPhotos);
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
        public TrackWriter newTrackWriter(Context context, boolean multiple) {
            return new KmlTrackWriter(context, multiple, true, true, exportPhotos);
        }

        @Override
        public String getMimeType() {
            return MIME_KMZ;
        }

        public TrackExporter newTrackExporter(Context context, Track[] tracks) {
            return newKmzTrackExporter(context, newTrackWriter(context, tracks.length > 1), tracks, exportPhotos);
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
        public TrackWriter newTrackWriter(Context context, boolean multiple) {
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

    private static TrackExporter newKmzTrackExporter(Context context, TrackWriter trackWriter, Track[] tracks, boolean exportPhotos) {
        ContentProviderUtils contentProviderUtils = new ContentProviderUtils(context);

        FileTrackExporter fileTrackExporter = new FileTrackExporter(contentProviderUtils, trackWriter, tracks);

        return new KmzTrackExporter(context, contentProviderUtils, fileTrackExporter, tracks, exportPhotos);
    }

    /**
     * Returns the mime type for each format.
     */
    public abstract String getMimeType();

    public TrackExporter newTrackExporter(Context context, Track[] tracks) {
        ContentProviderUtils contentProviderUtils = new ContentProviderUtils(context);
        TrackWriter trackWriter = newTrackWriter(context, tracks.length > 1);
        return new FileTrackExporter(contentProviderUtils, trackWriter, tracks);
    }

    /**
     * Creates a new track writer for the format.
     *
     * @param context  the context
     * @param multiple true for writing multiple tracks
     */
    public abstract TrackWriter newTrackWriter(Context context, boolean multiple);

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