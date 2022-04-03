package de.dennisguse.opentracks.services.handlers;

import android.content.Context;
import android.location.Location;
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
import de.dennisguse.opentracks.sensors.AltitudeSumManager;
import de.dennisguse.opentracks.sensors.BluetoothRemoteSensorManager;
import de.dennisguse.opentracks.sensors.sensorData.SensorDataSet;

/**
 * Creates TrackPoints while recording by fusing data from different sensors (e.g., GNSS, barometer, BLE sensors).
 */
public class TrackPointCreator implements BluetoothRemoteSensorManager.SensorDataSetChangeObserver {

    private static final String TAG = TrackPointCreator.class.getSimpleName();

    private Context context;

    private final Callback service;

    @NonNull
    private Clock clock = new MonotonicClock();

    private final GPSHandler gpsHandler;
    private BluetoothRemoteSensorManager remoteSensorManager;
    private AltitudeSumManager altitudeSumManager;

    public TrackPointCreator(Callback service) {
        this.service = service;
        this.gpsHandler = new GPSHandler(this);
    }

    @VisibleForTesting
    TrackPointCreator(GPSHandler gpsHandler, Callback service) {
        this.service = service;
        this.gpsHandler = gpsHandler;
    }

    public synchronized void start(@NonNull Context context) {
        this.context = context;

        gpsHandler.onStart(context);

        remoteSensorManager = new BluetoothRemoteSensorManager(context, this);
        altitudeSumManager = new AltitudeSumManager();

        remoteSensorManager.start();
        altitudeSumManager.start(context);

    }

    private boolean isStarted() {
        return context != null;
    }

    @Deprecated
    //There should be a cooler way to do this; we want to send fake locations without getting affected by real GPS data.
    @VisibleForTesting
    public void stopGPS() {
        gpsHandler.onStop();
    }

    public synchronized void reset() {
        if (remoteSensorManager == null || altitudeSumManager == null) {
            Log.d(TAG, "No recording running and no reset necessary.");
            return;
        }
        remoteSensorManager.reset();
        altitudeSumManager.reset();
    }

    private SensorDataSet addSensorData(TrackPoint trackPoint) {
        if (!isStarted()) {
            Log.w(TAG, "Not started, should not be called.");
            return null;
        }
        SensorDataSet sensorDataSet = null;
        BluetoothRemoteSensorManager localRemoteSensorManager = remoteSensorManager;
        if (localRemoteSensorManager != null) {
            sensorDataSet = localRemoteSensorManager.fill(trackPoint);
        }
        AltitudeSumManager localAltitudeSumManager = altitudeSumManager;
        if (localAltitudeSumManager != null) {
            localAltitudeSumManager.fill(trackPoint);
        }

        return sensorDataSet;
    }

    public synchronized void stop() {
        gpsHandler.onStop();

        if (remoteSensorManager != null) {
            remoteSensorManager.stop();
            remoteSensorManager = null;
        }

        if (altitudeSumManager != null) {
            altitudeSumManager.stop(context);
            altitudeSumManager = null;
        }

        this.context = null;
    }

    public synchronized void onChange(@NonNull Location location) {
        onNewTrackPoint(new TrackPoint(location, createNow()));
    }

    /**
     * Got a new TrackPoint from Bluetooth only; contains no GPS location.
     */
    @Override
    public synchronized void onChange(@NonNull SensorDataSet unused) {
        onNewTrackPoint(new TrackPoint(TrackPoint.Type.SENSORPOINT, createNow()));
    }

    @VisibleForTesting
    public void onNewTrackPoint(@NonNull TrackPoint trackPoint) {
        addSensorData(trackPoint);

        boolean stored = service.newTrackPoint(trackPoint, gpsHandler.getThresholdHorizontalAccuracy());
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
    public AltitudeSumManager getAltitudeSumManager() {
        return altitudeSumManager;
    }

    @Deprecated
    @VisibleForTesting
    public void setAltitudeSumManager(AltitudeSumManager altitudeSumManager) {
        this.altitudeSumManager = altitudeSumManager;
    }

    @Deprecated
    @VisibleForTesting
    public BluetoothRemoteSensorManager getRemoteSensorManager() {
        return remoteSensorManager;
    }

    @Deprecated
    @VisibleForTesting
    public void setRemoteSensorManager(BluetoothRemoteSensorManager remoteSensorManager) {
        this.remoteSensorManager = remoteSensorManager;
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
    public GPSHandler getGpsHandler() {
        return gpsHandler;
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
