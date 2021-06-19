package de.dennisguse.opentracks.content.data;

import java.util.ArrayList;
import java.util.List;

public class Layout {
    private final String profile;
    private final List<DataField> dataFields = new ArrayList<>();

    public Layout(String profile) {
        this.profile = profile;
    }

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

    public List<DataField> getFields() {
        return new ArrayList<>(dataFields);
    }

    public void moveField(int from, int to) {
        DataField dataFieldToMove = dataFields.remove(from);
        dataFields.add(to, dataFieldToMove);
    }

    public String getProfile() {
        return profile;
    }
}
