package de.dennisguse.opentracks.io.file;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Locale;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.content.ContentProviderUtils;
import de.dennisguse.opentracks.content.Track;
import de.dennisguse.opentracks.io.file.exporter.FileTrackExporter;
import de.dennisguse.opentracks.io.file.exporter.GpxTrackWriter;
import de.dennisguse.opentracks.io.file.exporter.KmlTrackWriter;
import de.dennisguse.opentracks.io.file.exporter.KmzTrackExporter;
import de.dennisguse.opentracks.io.file.exporter.TrackExporter;
import de.dennisguse.opentracks.io.file.exporter.TrackExporterListener;
import de.dennisguse.opentracks.io.file.exporter.TrackWriter;

/**
 * Definition of all possible track formats.
 */
public enum TrackFileFormat implements Parcelable {

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
    KML_WITH_SENSORDATA {
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
        @Override
        public TrackWriter newTrackWriter(Context context, boolean multiple) {
            return new KmlTrackWriter(context, multiple, false, false, false);
        }

        @Override
        public String getMimeType() {
            return MIME_KMZ;
        }

        public String getExtension() {
            return "kmz";
        }
    },
    KMZ_WITH_SENSORDATA {
        @Override
        public TrackWriter newTrackWriter(Context context, boolean multiple) {
            return new KmlTrackWriter(context, multiple, true, true, false);
        }

        @Override
        public String getMimeType() {
            return MIME_KMZ;
        }

        public TrackExporter newTrackExporter(Context context, Track[] tracks, TrackExporterListener trackExporterListener) {
            return newKmzTrackExporter(context, this.newTrackWriter(context, tracks.length > 1), tracks, trackExporterListener);
        }

        public String getExtension() {
            return "kmz";
        }
    },
    KMZ_WITH_SENSORDATA_AND_PICTURES {
        @Override
        public TrackWriter newTrackWriter(Context context, boolean multiple) {
            return new KmlTrackWriter(context, multiple, true, true, true);
        }

        @Override
        public String getMimeType() {
            return MIME_KMZ;
        }

        public TrackExporter newTrackExporter(Context context, Track[] tracks, TrackExporterListener trackExporterListener) {
            return newKmzTrackExporter(context, this.newTrackWriter(context, tracks.length > 1), tracks, trackExporterListener);
        }

        public String getExtension() {
            return "kmz";
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

    @Deprecated
    public static final Creator<TrackFileFormat> CREATOR = new Creator<TrackFileFormat>() {
        @Override
        public TrackFileFormat createFromParcel(final Parcel source) {
            return TrackFileFormat.values()[source.readInt()];
        }

        @Override
        public TrackFileFormat[] newArray(final int size) {
            return new TrackFileFormat[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        dest.writeInt(ordinal());
    }

    private static final String MIME_KMZ = "application/vnd.google-earth.kmz";

    private static final String MIME_KML = "application/vnd.google-earth.kml+xml";

    public TrackExporter newTrackExporter(Context context, Track[] tracks, TrackExporterListener trackExporterListener) {
        ContentProviderUtils contentProviderUtils = ContentProviderUtils.Factory.get(context);
        TrackWriter trackWriter = this.newTrackWriter(context, tracks.length > 1);
        return new FileTrackExporter(contentProviderUtils, trackWriter, tracks, trackExporterListener);
    }

    /**
     * Returns the mime type for each format.
     */
    public abstract String getMimeType();

    private static TrackExporter newKmzTrackExporter(Context context, TrackWriter trackWriter, Track[] tracks, TrackExporterListener trackExporterListener) {
        ContentProviderUtils contentProviderUtils = ContentProviderUtils.Factory.get(context);

        FileTrackExporter fileTrackExporter = new FileTrackExporter(contentProviderUtils, trackWriter, tracks, trackExporterListener);

        return new KmzTrackExporter(contentProviderUtils, fileTrackExporter, tracks);
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
     * Returns the name of for each format.
     */
    public String getName() {
        return this.name().toLowerCase(Locale.US);
    }
}