package de.dennisguse.opentracks.io.file.exporter;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;
import java.util.Objects;

import de.dennisguse.opentracks.data.models.Track;
import de.dennisguse.opentracks.io.file.TrackFileFormat;

public class ExportTask implements Parcelable {

    private final String filename; //TODO Name will only be used when exporting multiple tracks

    private final TrackFileFormat trackFileFormat;

    private final List<Track.Id> trackIds;

    public ExportTask(@Nullable String filename, @NonNull TrackFileFormat trackFileFormat, @NonNull List<Track.Id> trackIds) {
        this.filename = filename;
        this.trackFileFormat = trackFileFormat;
        this.trackIds = trackIds;
    }

    protected ExportTask(Parcel in) {
        filename = in.readString();
        trackFileFormat = TrackFileFormat.valueOf(in.readString());
        trackIds = in.createTypedArrayList(Track.Id.CREATOR);
    }

    public String getFilename() {
        return filename;
    }

    public TrackFileFormat getTrackFileFormat() {
        return trackFileFormat;
    }

    public List<Track.Id> getTrackIds() {
        return trackIds;
    }

    public boolean isMultiExport() {
        return trackIds.size() > 1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExportTask that = (ExportTask) o;
        return Objects.equals(filename, that.filename) && trackFileFormat == that.trackFileFormat && Objects.equals(trackIds, that.trackIds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(filename, trackFileFormat, trackIds);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(filename);
        dest.writeString(trackFileFormat.name());
        dest.writeTypedList(trackIds);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<ExportTask> CREATOR = new Creator<>() {
        @Override
        public ExportTask createFromParcel(Parcel in) {
            return new ExportTask(in);
        }

        @Override
        public ExportTask[] newArray(int size) {
            return new ExportTask[size];
        }
    };
}
