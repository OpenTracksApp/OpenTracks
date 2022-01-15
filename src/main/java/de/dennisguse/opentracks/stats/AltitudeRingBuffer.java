package de.dennisguse.opentracks.stats;

import androidx.annotation.Nullable;

import de.dennisguse.opentracks.data.models.Altitude;

public class AltitudeRingBuffer extends RingBuffer<Altitude> {

    private Altitude firstAltitude;

    AltitudeRingBuffer(int size) {
        super(size);
    }

    AltitudeRingBuffer(RingBuffer<Altitude> toCopy) {
        super(toCopy);
    }

    @Nullable
    @Override
    protected Number from(Altitude object) {
        if (firstAltitude == null) {
            firstAltitude = object;
        }
        return object.toM();
    }

    @Override
    protected Altitude to(double object) {
        return firstAltitude.replace(object);
    }
}
