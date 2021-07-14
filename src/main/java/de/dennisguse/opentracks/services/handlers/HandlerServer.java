package de.dennisguse.opentracks.services.handlers;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import java.time.Clock;
import java.time.Instant;

import de.dennisguse.opentracks.content.data.TrackPoint;
import de.dennisguse.opentracks.content.sensor.SensorDataSet;
import de.dennisguse.opentracks.services.sensors.AltitudeSumManager;
import de.dennisguse.opentracks.services.sensors.BluetoothRemoteSensorManager;
import de.dennisguse.opentracks.util.PreferencesUtils;

public class HandlerServer {

    private static final String TAG = HandlerServer.class.getSimpleName();

    private Context context;

    private final HandlerServerInterface service;
// Disabled to simplify testing and implementation of #822
//    private ExecutorService serviceExecutor;

    @NonNull
    private Clock clock = Clock.systemUTC();

    private final LocationHandler locationHandler;
    private final EGM2008CorrectionManager egm2008CorrectionManager = new EGM2008CorrectionManager();
    private BluetoothRemoteSensorManager remoteSensorManager;
    private AltitudeSumManager altitudeSumManager;

    public HandlerServer(HandlerServerInterface service) {
        this.service = service;
        this.locationHandler = new LocationHandler(this);
    }

    @VisibleForTesting
    HandlerServer(LocationHandler locationHandler, HandlerServerInterface service) {
        this.service = service;
        this.locationHandler = locationHandler;
    }

    public void start(@NonNull Context context) {
        this.context = context;
//        serviceExecutor = Executors.newSingleThreadExecutor();

        SharedPreferences sharedPreferences = PreferencesUtils.getSharedPreferences(context);
        locationHandler.onStart(context, sharedPreferences);

        remoteSensorManager = new BluetoothRemoteSensorManager(context);
        remoteSensorManager.start();

        altitudeSumManager = new AltitudeSumManager();
        altitudeSumManager.start(context);
    }

    @Deprecated
    //There should be a cooler way to do this; we want to send fake locations without getting affected by real GPS data.
    @VisibleForTesting
    public void stopGPS() {
        locationHandler.onStop();
    }

    public void resetSensorData() {
        remoteSensorManager.reset();
        altitudeSumManager.reset();
    }

    //TODO TrackPoint should be created by HandlerServer; instead of in the TrackRecordingService.
    @Deprecated
    public SensorDataSet fill(TrackPoint trackPoint) {
        SensorDataSet sensorDataSet = remoteSensorManager.fill(trackPoint);
        altitudeSumManager.fill(trackPoint);

        return sensorDataSet;
    }

    public SensorDataSet fillAndReset(TrackPoint trackPoint) {
        SensorDataSet sensorDataSet = fill(trackPoint);
        resetSensorData();

        return sensorDataSet;
    }


    public void stop() {
        locationHandler.onStop();

//        if (serviceExecutor != null) {
//            serviceExecutor.shutdownNow();
//        }
//        serviceExecutor = null;

        this.context = null;
    }

    public void onSharedPreferenceChanged(@NonNull SharedPreferences preferences, String key) {
        if (context == null) {
            Log.w(TAG, "not started yet.");
            return;
        }

        locationHandler.onSharedPreferenceChanged(context, preferences, key);
    }

    public void onNewTrackPoint(TrackPoint trackPoint, int recordingGpsAccuracy) {
//        if (serviceExecutor == null || serviceExecutor.isTerminated() || serviceExecutor.isShutdown()) {
//            return;
//        }

        egm2008CorrectionManager.correctAltitude(context, trackPoint);
        fillAndReset(trackPoint);


//        serviceExecutor.execute(() -> service.newTrackPoint(trackPoint, recordingGpsAccuracy));
        service.newTrackPoint(trackPoint, recordingGpsAccuracy);
    }

    public TrackPoint createSegmentStartManual() {
        return TrackPoint.createSegmentStartManualWithTime(createNow());
    }

    public TrackPoint createSegmentEnd() {
        TrackPoint segmentEnd = TrackPoint.createSegmentEndWithTime(createNow());
        fillAndReset(segmentEnd);
        return segmentEnd;
    }

    //TODO get lastTrackPoint from LocationHandler
    public Pair<TrackPoint, SensorDataSet> createCurrentTrackPoint(@Nullable TrackPoint lastTrackPoint) {
        TrackPoint currentTrackPoint = new TrackPoint(TrackPoint.Type.TRACKPOINT, createNow());

        if (lastTrackPoint != null && lastTrackPoint.hasLocation()) {
            //TODO Should happen in TrackPoint? via constructor
            currentTrackPoint.setSpeed(lastTrackPoint.getSpeed());
            currentTrackPoint.setAltitude(lastTrackPoint.getAltitude());
            currentTrackPoint.setLongitude(lastTrackPoint.getLongitude());
            currentTrackPoint.setLatitude(lastTrackPoint.getLatitude());
        }
        SensorDataSet sensorDataSet = fill(currentTrackPoint);

        return new Pair<>(currentTrackPoint, sensorDataSet);
    }

    //TODO Limit visibility
    public Instant createNow() {
        return Instant.now(clock);
    }

    @Deprecated
    @VisibleForTesting
    public void setAltitudeSumManager(AltitudeSumManager altitudeSumManager) {
        this.altitudeSumManager = altitudeSumManager;
    }

    @Deprecated
    @VisibleForTesting
    public void setRemoteSensorManager(BluetoothRemoteSensorManager remoteSensorManager) {
        this.remoteSensorManager = remoteSensorManager;
    }

    @VisibleForTesting
    public void setClock(@NonNull Clock clock) {
        this.clock = clock;
    }

    void sendGpsStatus(GpsStatusValue gpsStatusValue) {
        service.newGpsStatus(gpsStatusValue);
    }

    public interface HandlerServerInterface {
        void newTrackPoint(TrackPoint trackPoint, int gpsAccuracy);

        void newGpsStatus(GpsStatusValue gpsStatusValue);
    }
}
