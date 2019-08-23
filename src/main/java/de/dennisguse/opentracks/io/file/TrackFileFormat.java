package de.dennisguse.opentracks.io.file;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Locale;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.io.file.exporter.GpxTrackWriter;
import de.dennisguse.opentracks.io.file.exporter.KmlTrackWriter;
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