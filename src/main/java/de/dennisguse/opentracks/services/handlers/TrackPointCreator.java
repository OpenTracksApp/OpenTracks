package de.dennisguse.opentracks.services.handlers;

import android.content.Context;
import android.location.Location;
import android.os.Handler;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import de.dennisguse.opentracks.data.models.Distance;
import de.dennisguse.opentracks.data.models.TrackPoint;
import de.dennisguse.opentracks.sensors.SensorManager;
import de.dennisguse.opentracks.sensors.sensorData.SensorDataSet;

/**
 * Creates TrackPoints while recording by fusing data from different sensors (e.g., GNSS, barometer, BLE sensors).
 */
public class TrackPointCreator {

    private static final String TAG = TrackPointCreator.class.getSimpleName();

    private Context context;

    private final Callback service;

    @NonNull
    private Clock clock = new MonotonicClock();

    private final GPSManager gpsManager;
    private SensorManager sensorManager;

    public TrackPointCreator(Callback service, Context context, Handler handler) {
        this.service = service;
        this.gpsManager = new GPSManager(this);
        this.sensorManager = new SensorManager(context, handler, this);
    }

    @VisibleForTesting
    TrackPointCreator(GPSManager gpsManager, Callback service) {
        this.service = service;
        this.gpsManager = gpsManager;
    }

    public synchronized void start(@NonNull Context context, @NonNull Handler handler) {
        this.context = context;

        gpsManager.start(context, handler);
        sensorManager.start(context, handler);
    }

    private boolean isStarted() {
        return context != null;
    }

    private synchronized void reset() {
        sensorManager.reset();
    }

    private SensorDataSet addSensorData(TrackPoint trackPoint) {
        if (!isStarted()) {
            Log.w(TAG, "Not started, should not be called.");
            return null;
        }

        return sensorManager.fill(trackPoint);
    }

    public synchronized void stop() {
        gpsManager.stop(context);

        this.context = null;
    }

    public synchronized void onChange(@NonNull Location location) {
        onNewTrackPoint(new TrackPoint(location, createNow()));
    }

    /**
     * Got a new TrackPoint from Bluetooth only; contains no GPS location.
     */
    public synchronized void onChange(@NonNull SensorDataSet unused) {
        onNewTrackPoint(new TrackPoint(TrackPoint.Type.SENSORPOINT, createNow()));
    }

    @VisibleForTesting
    public void onNewTrackPoint(@NonNull TrackPoint trackPoint) {
        addSensorData(trackPoint);

        boolean stored = service.newTrackPoint(trackPoint, gpsManager.getThresholdHorizontalAccuracy());
        if (stored) {
            reset();
        }
    }

    public synchronized TrackPoint createSegmentStartManual() {
        return TrackPoint.createSegmentStartManualWithTime(createNow());
    }

    public synchronized TrackPoint createSegmentEnd() {
        TrackPoint segmentEnd = TrackPoint.createSegmentEndWithTime(createNow());
        addSensorData(segmentEnd);
        reset();
        return segmentEnd;
    }

    public Pair<TrackPoint, SensorDataSet> createCurrentTrackPoint(@Nullable TrackPoint lastTrackPointUISpeed, @Nullable TrackPoint lastTrackPointUIAltitude, @Nullable TrackPoint lastStoredTrackPointWithLocation) {
        TrackPoint currentTrackPoint = new TrackPoint(TrackPoint.Type.TRACKPOINT, createNow());

        if (lastTrackPointUISpeed != null) {
            currentTrackPoint.setSpeed(lastTrackPointUISpeed.getSpeed());
        }
        if (lastTrackPointUIAltitude != null) {
            currentTrackPoint.setAltitude(lastTrackPointUIAltitude.getAltitude());
        }

        if (lastStoredTrackPointWithLocation != null && lastStoredTrackPointWithLocation.hasLocation()) {
            //We are taking the coordinates from the last stored TrackPoint, so the distance is monotonously increasing.
            currentTrackPoint.setLongitude(lastStoredTrackPointWithLocation.getLongitude());
            currentTrackPoint.setLatitude(lastStoredTrackPointWithLocation.getLatitude());
        }

        SensorDataSet sensorDataSet = addSensorData(currentTrackPoint);

        return new Pair<>(currentTrackPoint, sensorDataSet);
    }

    @VisibleForTesting
    Instant createNow() {
        return Instant.now(clock);
    }

    @VisibleForTesting
    public SensorManager getSensorManager() {
        return sensorManager;
    }

    @VisibleForTesting
    public void setSensorManager(SensorManager sensorManager) {
        this.sensorManager = sensorManager;
    }

    @VisibleForTesting
    public void setClock(@NonNull String time) {
        this.clock = Clock.fixed(Instant.parse(time), ZoneId.of("CET"));
    }

    @VisibleForTesting
    public void setClock(@NonNull Clock clock) {
        this.clock = clock;
    }

    @VisibleForTesting
    public GPSManager getGpsHandler() {
        return gpsManager;
    }

    void sendGpsStatus(GpsStatusValue gpsStatusValue) {
        service.newGpsStatus(gpsStatusValue);
    }

    public interface Callback {
        /**
         * @return Was TrackPoint stored (not discarded)?
         */
        boolean newTrackPoint(TrackPoint trackPoint, Distance thresholdHorizontalAccuracy);

        void newGpsStatus(GpsStatusValue gpsStatusValue);
    }
}
