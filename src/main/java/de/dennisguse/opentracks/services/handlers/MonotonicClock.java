package de.dennisguse.opentracks.services.handlers;

import android.os.SystemClock;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

/**
 * A monotonic clock relative to the device's clock at time of the instantiation.
 * Uses device time since startup.
 * Replacement for java.time.Clock as it may not be monotonic (i.e., can jump back and forwards).
 */
public class MonotonicClock extends Clock {

    private final long epochAtCreation;

    private final long elapsedRealtimeAtCreation;

    public MonotonicClock() {
        epochAtCreation = Instant.now().toEpochMilli();
        elapsedRealtimeAtCreation = SystemClock.elapsedRealtime();
    }

    @Override
    public Instant instant() {
        long current = (SystemClock.elapsedRealtime() - elapsedRealtimeAtCreation);
        return Instant.ofEpochMilli(epochAtCreation + current);
    }

    @Override
    public ZoneId getZone() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public Clock withZone(ZoneId zone) {
        throw new RuntimeException("Not implemented");
    }
}
