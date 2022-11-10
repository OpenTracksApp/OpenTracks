package de.dennisguse.opentracks.ui.customRecordingLayout;

import android.content.res.Resources;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.List;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.settings.PreferencesUtils;

public class LayoutIO {

    private static final String TAG = Layout.class.getSimpleName();

    private static final String YES_VALUE = "1";
    private static final String NOT_VALUE = "0";

    public static String toCsv(DataField datafield) {
        String visible = datafield.isVisible() ? YES_VALUE : NOT_VALUE;
        String primary = datafield.isPrimary() ? YES_VALUE : NOT_VALUE;
        String wide = datafield.isWide() ? YES_VALUE : NOT_VALUE;
        return datafield.getKey() + CsvLayoutUtils.PROPERTY_SEPARATOR + visible + CsvLayoutUtils.PROPERTY_SEPARATOR + primary + CsvLayoutUtils.PROPERTY_SEPARATOR + wide;
    }

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
            layout.addField(fromCSV(fieldParts, resources));
        }
        return layout;
    }

    private static DataField fromCSV(String[] fieldParts, @NonNull Resources resources) {
        return new DataField(
                fieldParts[0],
                fieldParts[1].equals(YES_VALUE),
                fieldParts[2].equals(YES_VALUE),
                fieldParts[0].equals(resources.getString(R.string.stats_custom_layout_coordinates_key)));
    }
}
