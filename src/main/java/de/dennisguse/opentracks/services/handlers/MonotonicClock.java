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
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == this){
            return true;
        }
        if(!(obj instanceof MonotonicClock)){
            return false;
        }
        MonotonicClock monotonicClock = (MonotonicClock) obj;
        return this.instant().compareTo(monotonicClock.instant()) >=0 && this.getZone().equals(monotonicClock.getZone());
    }

    @Override
    public Instant instant() {
        long current = (SystemClock.elapsedRealtime() - elapsedRealtimeAtCreation);
        return Instant.ofEpochMilli(epochAtCreation + current);
    }

    @Override
    public ZoneId getZone(){
        return Clock.systemDefaultZone().getZone();
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return Clock.systemDefaultZone().withZone(zone);
    }
}
