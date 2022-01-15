package de.dennisguse.opentracks.ui.customRecordingLayout;

import android.content.res.Resources;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.settings.PreferencesUtils;

public class Layout implements Parcelable {
    private static final String TAG = Layout.class.getSimpleName();

    // User-generated layout's name.
    private final String name;
    private int columnsPerRow;
    private final List<DataField> dataFields = new ArrayList<>();

    public static Layout fromCsv(@NonNull String csvLine, @NonNull Resources resources) {
        List<String> csvParts = CsvLayoutUtils.getCsvLineParts(csvLine);
        if (csvParts == null) {
            Log.e(TAG, "Invalid CSV layout. It shouldn't happen: " + csvLine);
            return new Layout(PreferencesUtils.getDefaultLayoutName());
        }

        Layout layout = new Layout(csvParts.get(0), Integer.parseInt(csvParts.get(1)));
        for (int i = 2; i < csvParts.size(); i++) {
            String[] fieldParts = CsvLayoutUtils.getCsvFieldParts(csvParts.get(i));
            if (fieldParts == null) {
                Log.e(TAG, "Invalid CSV layout. It shouldn't happen: " + csvLine);
                return layout;
            }
            layout.addField(fieldParts[0], DataField.getTitleByKey(resources, fieldParts[0]), fieldParts[1].equals(DataField.YES_VALUE), fieldParts[2].equals(DataField.YES_VALUE), fieldParts[0].equals(resources.getString(R.string.stats_custom_layout_coordinates_key)));
        }
        return layout;
    }

    public Layout(String name) {
        this.name = name;
        this.columnsPerRow = PreferencesUtils.getLayoutColumnsByDefault();
    }

    public Layout(String name, int columnsPerRow) {
        this.name = name;
        this.columnsPerRow = columnsPerRow;
    }

    protected Layout(Parcel in) {
        name = in.readString();
        columnsPerRow = in.readInt();
        in.readList(dataFields, DataField.class.getClassLoader());
    }

    public static final Creator<Layout> CREATOR = new Creator<>() {
        @Override
        public Layout createFromParcel(Parcel in) {
            return new Layout(in);
        }

        @Override
        public Layout[] newArray(int size) {
            return new Layout[size];
        }
    };

    public void addField(String key, String title, boolean visible, boolean primary, boolean isWide) {
        dataFields.add(new DataField(key, title, visible, primary, isWide));
    }

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

    public boolean sameName(Layout layout) {
        return this.name.equalsIgnoreCase(layout.getName());
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
                + fields.stream().map(DataField::toCsv).collect(Collectors.joining(CsvLayoutUtils.ITEM_SEPARATOR))
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
}
