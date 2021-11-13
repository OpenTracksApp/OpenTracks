package de.dennisguse.opentracks.content.data;

public enum CustomLayoutFieldType {
    GENERIC(1),
    CLOCK(2);

    private final int value;

    CustomLayoutFieldType(int value) {
        this.value = value;
    }

    public int value() {
        return this.value;
    }
}
