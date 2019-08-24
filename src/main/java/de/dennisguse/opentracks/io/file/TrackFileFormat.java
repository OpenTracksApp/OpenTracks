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
    KML {
        @Override
        public TrackWriter newTrackWriter(Context context, boolean multiple) {
            return new KmlTrackWriter(context, multiple, false);
        }

        @Override
        public String getMimeType() {
            return "application/vnd.google-earth.kml+xml";
        }
    },
    KMZ {
        @Override
        public TrackWriter newTrackWriter(Context context, boolean multiple) {
            return new KmlTrackWriter(context, multiple, true);
        }

        @Override
        public String getMimeType() {
            return "application/vnd.google-earth.kmz";
        }

        public TrackExporter newTrackExporter(Context context, Track[] tracks, TrackExporterListener trackExporterListener) {
            ContentProviderUtils contentProviderUtils = ContentProviderUtils.Factory.get(context);
            TrackWriter trackWriter = this.newTrackWriter(context, tracks.length > 1);

            FileTrackExporter fileTrackExporter = new FileTrackExporter(contentProviderUtils, trackWriter, tracks, trackExporterListener);

            return new KmzTrackExporter(contentProviderUtils, fileTrackExporter, tracks);
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
    };

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

    /**
     * Creates a new track writer for the format.
     *
     * @param context   the context
     * @param multiple  true for writing multiple tracks
     */
    public abstract TrackWriter newTrackWriter(Context context, boolean multiple);

    public TrackExporter newTrackExporter(Context context, Track[] tracks, TrackExporterListener trackExporterListener) {
        ContentProviderUtils contentProviderUtils = ContentProviderUtils.Factory.get(context);
        TrackWriter trackWriter = this.newTrackWriter(context, tracks.length > 1);
        return new FileTrackExporter(contentProviderUtils, trackWriter, tracks, trackExporterListener);
    }

    /**
     * Returns the mime type for each format.
     */
    public abstract String getMimeType();

    /**
     * Returns the file extension for each format.
     */
    public String getExtension() {
        return this.name().toLowerCase(Locale.US);
    }
}