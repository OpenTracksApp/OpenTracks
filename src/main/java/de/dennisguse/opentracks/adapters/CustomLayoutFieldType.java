package de.dennisguse.opentracks.adapters;

enum CustomLayoutFieldType {
    SHORT(0),
    WIDE(1);

    private final int value;

    CustomLayoutFieldType(int value) {
        this.value = value;
    }

    public int value() {
        return this.value;
    }
}
