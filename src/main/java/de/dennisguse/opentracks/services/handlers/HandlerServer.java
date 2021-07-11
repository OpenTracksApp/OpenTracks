package de.dennisguse.opentracks.services.handlers;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import de.dennisguse.opentracks.content.data.TrackPoint;
import de.dennisguse.opentracks.util.PreferencesUtils;

public class HandlerServer {

    private static final String TAG = HandlerServer.class.getSimpleName();

    private Context context;

    private final HandlerServerInterface service;
// Disabled to simplify testing and implementation of #822
//    private ExecutorService serviceExecutor;

    private final LocationHandler locationHandler;
    private final EGM2008CorrectionManager egm2008CorrectionManager = new EGM2008CorrectionManager();

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
        locationHandler.onStart(context);
        locationHandler.onSharedPreferenceChanged(context, sharedPreferences, null);
    }

    public void stop() {
        locationHandler.onStop(context);

//        if (serviceExecutor != null) {
//            serviceExecutor.shutdownNow();
//        }
//        serviceExecutor = null;

        this.context = null;
    }

    public void onSharedPreferenceChanged(@NonNull Context context, @NonNull SharedPreferences preferences, String key) {
        locationHandler.onSharedPreferenceChanged(context, preferences, key);
    }

    public void onNewTrackPoint(TrackPoint trackPoint, int recordingGpsAccuracy) {
//        if (serviceExecutor == null || serviceExecutor.isTerminated() || serviceExecutor.isShutdown()) {
//            return;
//        }

        egm2008CorrectionManager.correctAltitude(context, trackPoint);

//        serviceExecutor.execute(() -> service.newTrackPoint(trackPoint, recordingGpsAccuracy));
        service.newTrackPoint(trackPoint, recordingGpsAccuracy);
    }

    void sendGpsStatus(GpsStatusValue gpsStatusValue) {
        service.newGpsStatus(gpsStatusValue);
    }

    public interface HandlerServerInterface {
        void newTrackPoint(TrackPoint trackPoint, int gpsAccuracy);
        void newGpsStatus(GpsStatusValue gpsStatusValue);
    }

    public interface Handler {
        void onStart(@NonNull Context context);

        void onStop(@NonNull Context context);

        void onSharedPreferenceChanged(@NonNull Context context, @NonNull SharedPreferences preferences, String key);
    }
}
