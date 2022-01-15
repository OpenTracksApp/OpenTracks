package de.dennisguse.opentracks.data;

public class SelectionData {
    private final String selection;
    private final String[] selectionArgs;

    public SelectionData() {
        selection = null;
        selectionArgs = null;
    }

    public SelectionData(String selection, String[] selectionArgs) {
        this.selection = selection;
        this.selectionArgs = selectionArgs;
    }

    public String getSelection() {
        return selection;
    }

    public String[] getSelectionArgs() {
        return selectionArgs;
    }
}
