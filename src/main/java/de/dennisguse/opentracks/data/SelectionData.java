package de.dennisguse.opentracks.data;

public record SelectionData(
        String selection,
        String[] selectionArgs
) {

    public SelectionData() {
        this(null, null);
    }
}
