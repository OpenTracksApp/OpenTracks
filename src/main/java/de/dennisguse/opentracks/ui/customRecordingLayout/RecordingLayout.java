package de.dennisguse.opentracks.ui.customRecordingLayout;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import de.dennisguse.opentracks.settings.PreferencesUtils;

public class RecordingLayout implements Parcelable {

    // User-generated layout's name.
    private final String name;
    private int columnsPerRow;
    private final List<DataField> dataFields = new ArrayList<>();

    public RecordingLayout(String name) {
        this.name = name;
        this.columnsPerRow = PreferencesUtils.getLayoutColumnsByDefault();
    }

    public RecordingLayout(String name, int columnsPerRow) {
        this.name = name;
        this.columnsPerRow = columnsPerRow;
    }

    protected RecordingLayout(Parcel in) {
        name = in.readString();
        columnsPerRow = in.readInt();
        in.readList(dataFields, DataField.class.getClassLoader());
    }

    public static final Creator<RecordingLayout> CREATOR = new Creator<>() {
        @Override
        public RecordingLayout createFromParcel(Parcel in) {
            return new RecordingLayout(in);
        }

        @Override
        public RecordingLayout[] newArray(int size) {
            return new RecordingLayout[size];
        }
    };

    public void addField(DataField dataField) {
        dataFields.add(dataField);
    }

    public void addFields(List<DataField> dataFields) {
        this.dataFields.addAll(dataFields);
    }

    public void removeField(DataField dataField) {
        dataFields.remove(dataField);
    }

    public void replaceAllFields(List<DataField> newFields) {
        dataFields.clear();
        addFields(newFields);
    }

    public List<DataField> getFields() {
        return new ArrayList<>(dataFields);
    }

    public RecordingLayout toRecordingLayout(boolean visibility) {
        RecordingLayout result = new RecordingLayout(this.getName());
        result.addFields(dataFields.stream().filter(f -> f.isVisible() == visibility).toList());
        return result;
    }

    public void moveField(int from, int to) {
        DataField dataFieldToMove = dataFields.remove(from);
        dataFields.add(to, dataFieldToMove);
    }

    public String getName() {
        return name;
    }

    public int getColumnsPerRow() {
        return columnsPerRow;
    }

    public void setColumnsPerRow(int columnsPerRow) {
        this.columnsPerRow = columnsPerRow;
    }

    public boolean sameName(RecordingLayout recordingLayout) {
        return this.name.equalsIgnoreCase(recordingLayout.getName());
    }

    public boolean sameName(String name) {
        return this.name.equalsIgnoreCase(name);
    }

    public String toCsv() {
        List<DataField> fields = getFields();
        if (fields.isEmpty()) {
            return "";
        }

        return getName() + CsvLayoutUtils.ITEM_SEPARATOR + getColumnsPerRow() + CsvLayoutUtils.ITEM_SEPARATOR
                + fields.stream().map(RecordingLayoutIO::toCsv).collect(Collectors.joining(CsvLayoutUtils.ITEM_SEPARATOR))
                + CsvLayoutUtils.ITEM_SEPARATOR;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(name);
        parcel.writeInt(columnsPerRow);
        parcel.writeList(dataFields);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RecordingLayout recordingLayout = (RecordingLayout) o;
        return columnsPerRow == recordingLayout.columnsPerRow && Objects.equals(name, recordingLayout.name) && Objects.equals(dataFields, recordingLayout.dataFields);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, columnsPerRow, dataFields);
    }
}
