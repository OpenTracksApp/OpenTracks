package com.google.android.apps.mytracks.io.file;

import com.google.android.apps.mytracks.io.file.export.CsvTrackWriter;
import com.google.android.apps.mytracks.io.file.export.GpxTrackWriter;
import com.google.android.apps.mytracks.io.file.export.KmlTrackWriter;
import com.google.android.apps.mytracks.io.file.export.TcxTrackWriter;
import com.google.android.apps.mytracks.io.file.export.TrackWriter;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Locale;

/**
 * Definition of all possible track formats.
 */
public enum TrackFileFormat implements Parcelable {
  KML {
  @Override
    public TrackWriter newTrackWriter(Context context, boolean inZip) {
      return new KmlTrackWriter(context, inZip);
    }
  },
  GPX {
  @Override
    public TrackWriter newTrackWriter(Context context, boolean inZip) {
      return new GpxTrackWriter(context);
    }
  },
  CSV {
  @Override
    public TrackWriter newTrackWriter(Context context, boolean inZip) {
      return new CsvTrackWriter(context);
    }
  },
  TCX {
  @Override
    public TrackWriter newTrackWriter(Context context, boolean inZip) {
      return new TcxTrackWriter(context);
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

  /**
   * Creates a new track writer for the format.
   */
  public abstract TrackWriter newTrackWriter(Context context, boolean inZip);

  /**
   * Returns the mime type for each format.
   */
  public String getMimeType() {
    return "application/" + getExtension() + "+xml";
  }

  /**
   * Returns the file extension for each format.
   */
  public String getExtension() {
    return this.name().toLowerCase(Locale.US);
  }
}