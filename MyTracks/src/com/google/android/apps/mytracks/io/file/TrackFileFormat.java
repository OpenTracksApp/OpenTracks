package com.google.android.apps.mytracks.io.file;

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
    TrackFormatWriter newFormatWriter(Context context) {
      return new KmlTrackWriter(context);
    }
  },
  GPX {
  @Override
    TrackFormatWriter newFormatWriter(Context context) {
      return new GpxTrackWriter(context);
    }
  },
  CSV {
  @Override
    public TrackFormatWriter newFormatWriter(Context context) {
      return new CsvTrackWriter(context);
    }
  },
  TCX {
  @Override
    public TrackFormatWriter newFormatWriter(Context context) {
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
   * Creates and returns a new format writer for each format.
   */
  abstract TrackFormatWriter newFormatWriter(Context context);

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