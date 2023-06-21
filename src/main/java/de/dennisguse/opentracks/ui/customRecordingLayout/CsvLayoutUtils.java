package de.dennisguse.opentracks.ui.customRecordingLayout;

import androidx.annotation.Nullable;

import java.util.Arrays;
import java.util.List;

public final class CsvLayoutUtils {
    public static final String LINE_SEPARATOR = "\n";
    public static final String ITEM_SEPARATOR = ";";
    public static final String PROPERTY_SEPARATOR = ",";

    private CsvLayoutUtils() {

    }

    /**
     * @param csvLine Layout description in a CSV format.
     * @return All CSV parts from the csvLine or null if it's malformed.
     */
    @Nullable
    public static List<String> getCsvLineParts(String csvLine) {
        if (csvLine == null) {
            return null;
        }

        // The line must have three items, at least: layout's name, number of columns and one field.
        List<String> csvParts = Arrays.asList(csvLine.split(ITEM_SEPARATOR));
        if (csvParts.size() < 3 || !hasValue(csvParts.get(0)) || !isInt(csvParts.get(1))) {
            return null;
        }

        return csvParts;
    }

    /**
     * @param csvField Layout's field in a CSV format.
     */
    @Nullable
    public static String[] getCsvFieldParts(@Nullable String csvField) {
        if (csvField == null) {
            return null;
        }

        // Field must have three items: key, is visible (0 or 1), is primary (0 or 1).
        String[] fieldParts = csvField.split(CsvLayoutUtils.PROPERTY_SEPARATOR);
        if (fieldParts.length < 3 || !hasValue(fieldParts[0]) || !hasZeroOneValue(fieldParts[1]) || !hasZeroOneValue(fieldParts[2])) {
            return null;
        }

        return fieldParts;
    }

    private static boolean hasValue(String value) {
        return value != null && !value.isEmpty();
    }

    private static boolean isInt(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }

        try {
            Integer.parseInt(value);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }

    private static boolean hasZeroOneValue(String value) {
        return isInt(value) && (Integer.parseInt(value) == 0 || Integer.parseInt(value) == 1);
    }
}
