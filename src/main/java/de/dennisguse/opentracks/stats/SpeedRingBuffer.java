package de.dennisguse.opentracks.stats;

import androidx.annotation.Nullable;

import de.dennisguse.opentracks.content.data.Speed;

public class SpeedRingBuffer extends RingBuffer<Speed> {

    SpeedRingBuffer(int size) {
        super(size);
    }

    SpeedRingBuffer(RingBuffer<Speed> toCopy) {
        super(toCopy);
    }

    @Nullable
    @Override
    protected Number from(Speed object) {
        return object.toMPS();
    }

    @Override
    protected Speed to(double object) {
        return Speed.of(object);
    }
}
