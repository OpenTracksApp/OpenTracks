package de.dennisguse.opentracks.services.handlers;

import android.content.Context;
import android.content.SharedPreferences;
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
import de.dennisguse.opentracks.sensors.GpsStatusValue;
import de.dennisguse.opentracks.sensors.SensorManager;
import de.dennisguse.opentracks.sensors.sensorData.SensorDataSet;
import de.dennisguse.opentracks.settings.PreferencesUtils;

/**
 * Creates TrackPoints while recording by fusing data from different sensors (e.g., GNSS, barometer, BLE sensors).
 */
public class TrackPointCreator implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = TrackPointCreator.class.getSimpleName();

    private Context context;

    private final Callback service;

    @NonNull
    private Clock clock = new MonotonicClock();
    private final SensorManager sensorManager;

    public TrackPointCreator(Callback service) {
        this.service = service;
        this.sensorManager = new SensorManager(this);
    }

    public synchronized void start(@NonNull Context context, @NonNull Handler handler) {
        this.context = context;

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

    public void stop() {
        sensorManager.stop(context);
        this.context = null;
    }

    /**
     * Got a new TrackPoint from Bluetooth only; contains no GPS location.
     */
    public synchronized void onChange(@NonNull SensorDataSet unused) {
        onNewTrackPoint(new TrackPoint(TrackPoint.Type.TRACKPOINT, createNow()));
    }

    @VisibleForTesting
    public void onNewTrackPoint(@NonNull TrackPoint trackPoint) {
        addSensorData(trackPoint);

        boolean stored = service.newTrackPoint(trackPoint, PreferencesUtils.getThresholdHorizontalAccuracy());  //TODO Cache preference for performance
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

    public synchronized TrackPoint createIdle() {
        TrackPoint idle = new TrackPoint(TrackPoint.Type.IDLE, createNow());
        addSensorData(idle);
        reset();
        return idle;
    }

    public Pair<TrackPoint, SensorDataSet> createCurrentTrackPoint(@Nullable TrackPoint lastTrackPointUISpeed, @Nullable TrackPoint lastTrackPointUIAltitude, @Nullable TrackPoint lastStoredTrackPointWithLocation) {
        TrackPoint currentTrackPoint = new TrackPoint(TrackPoint.Type.TRACKPOINT, createNow());

        SensorDataSet sensorDataSet = addSensorData(currentTrackPoint);

        if (!currentTrackPoint.hasLocation() && lastStoredTrackPointWithLocation != null && lastStoredTrackPointWithLocation.hasLocation()) {
            //We are taking the coordinates from the last stored TrackPoint, so the distance is monotonously increasing.
            currentTrackPoint.setPosition(lastStoredTrackPointWithLocation.getPosition());
        }

        if (lastTrackPointUISpeed != null) {
            currentTrackPoint.setSpeed(lastTrackPointUISpeed.getSpeed());
        }

        if (lastTrackPointUIAltitude != null) {
            currentTrackPoint.setAltitude(lastTrackPointUIAltitude.getAltitude());
        }

        return new Pair<>(currentTrackPoint, sensorDataSet);
    }

    public Instant createNow() {
        return Instant.now(clock);
    }

    @VisibleForTesting
    public SensorManager getSensorManager() {
        return sensorManager;
    }

    @VisibleForTesting
    public void setClock(@NonNull String time) {
        this.clock = Clock.fixed(Instant.parse(time), ZoneId.of("CET"));
    }

    @Deprecated //TODO This should be refactored. Can we use a SensorDataSet for this?
    public void sendGpsStatus(GpsStatusValue gpsStatusValue) {
        service.newGpsStatus(gpsStatusValue);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, @Nullable String key) {
        sensorManager.onSharedPreferenceChanged(sharedPreferences, key);
    }

    public interface Callback {
        /**
         * @return Was TrackPoint stored (not discarded)?
         */
        boolean newTrackPoint(TrackPoint trackPoint, Distance thresholdHorizontalAccuracy);

        void newGpsStatus(GpsStatusValue gpsStatusValue);
    }
}
