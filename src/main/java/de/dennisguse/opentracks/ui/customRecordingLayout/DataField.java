package de.dennisguse.opentracks.ui.customRecordingLayout;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

public class DataField implements Parcelable {

    private final String key;
    private boolean isVisible;
    private boolean isPrimary;
    private final boolean isWide;

    public DataField(String key, boolean isVisible, boolean isPrimary, boolean isWide) {
        this.key = key;
        this.isVisible = isVisible;
        this.isPrimary = isPrimary;
        this.isWide = isWide;
    }

    protected DataField(Parcel in) {
        key = in.readString();
        isVisible = in.readByte() != 0;
        isPrimary = in.readByte() != 0;
        isWide = in.readByte() != 0;
    }

    public static final Creator<DataField> CREATOR = new Creator<>() {
        @Override
        public DataField createFromParcel(Parcel in) {
            return new DataField(in);
        }

        @Override
        public DataField[] newArray(int size) {
            return new DataField[size];
        }
    };

    public String getKey() {
        return key;
    }

    public boolean isVisible() {
        return isVisible;
    }

    public void toggleVisibility() {
        isVisible = !isVisible;
    }

    public boolean isPrimary() {
        return isPrimary;
    }

    public void togglePrimary() {
        isPrimary = !isPrimary;
    }

    public boolean isWide() {
        return isWide;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(key);
        parcel.writeByte((byte) (isVisible ? 1 : 0));
        parcel.writeByte((byte) (isPrimary ? 1 : 0));
        parcel.writeByte((byte) (isWide ? 1 : 0));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DataField dataField = (DataField) o;
        return isVisible == dataField.isVisible && isPrimary == dataField.isPrimary && isWide == dataField.isWide && Objects.equals(key, dataField.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, isVisible, isPrimary, isWide);
    }

    @Override
    public String toString() {
        return "DataField{" +
                "key='" + key + '\'' +
                ", isVisible=" + isVisible +
                ", isPrimary=" + isPrimary +
                ", isWide=" + isWide +
                '}';
    }
}
