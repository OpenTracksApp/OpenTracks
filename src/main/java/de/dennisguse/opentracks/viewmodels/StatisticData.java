package de.dennisguse.opentracks.viewmodels;

import android.content.Context;
import android.util.Pair;

import androidx.annotation.NonNull;

import de.dennisguse.opentracks.ui.customRecordingLayout.CustomLayoutFieldType;
import de.dennisguse.opentracks.ui.customRecordingLayout.DataField;

public class StatisticData {
    private final DataField dataField;
    private final String value;
    private final String unit;
    private final String description;

    public StatisticData(@NonNull DataField dataField, @NonNull Pair<String, String> valueAndUnit, String description) {
        this.dataField = dataField;
        this.value = valueAndUnit.first;
        this.unit = valueAndUnit.second;
        this.description = description;
    }

    public CustomLayoutFieldType getType(Context context) {
        return dataField.getType(context);
    }

    public String getTitle() {
        return dataField.getTitle();
    }

    public boolean isWide() {
        return dataField.isWide();
    }

    public boolean isPrimary() {
        return dataField.isPrimary();
    }

    public boolean isVisible() {
        return dataField.isVisible();
    }

    public String getValue() {
        return value;
    }

    public boolean hasValue() {
        return value != null;
    }

    public String getUnit() {
        return unit;
    }

    public String getDescription() {
        return description;
    }

    public boolean hasDescription() {
        return description != null;
    }
}
