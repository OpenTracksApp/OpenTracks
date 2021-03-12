package de.dennisguse.opentracks.services.handlers;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.dennisguse.opentracks.content.data.TrackPoint;
import de.dennisguse.opentracks.util.PreferencesUtils;

public class HandlerServer {

    private static final String TAG = HandlerServer.class.getSimpleName();

    private final LocationHandler locationHandler;
    private final HandlerServerInterface service;
    private ExecutorService serviceExecutor;

    public HandlerServer(HandlerServerInterface service) {
        this.locationHandler = new LocationHandler(this);
        this.service = service;
    }

    @VisibleForTesting
    HandlerServer(LocationHandler locationHandler, HandlerServerInterface service) {
        this.locationHandler = locationHandler;
        this.service = service;
    }

    public void start(Context context) {
        serviceExecutor = Executors.newSingleThreadExecutor();

        SharedPreferences sharedPreferences = PreferencesUtils.getSharedPreferences(context);
        locationHandler.onStart(context);
        locationHandler.onSharedPreferenceChanged(context, sharedPreferences, null);
    }

    public void stop(Context context) {
        locationHandler.onStop(context);

        if (serviceExecutor != null) {
            serviceExecutor.shutdownNow();
        }
        serviceExecutor = null;
    }

    public void onSharedPreferenceChanged(@NonNull Context context, @NonNull SharedPreferences preferences, String key) {
        locationHandler.onSharedPreferenceChanged(context, preferences, key);
    }

    public void sendTrackPoint(TrackPoint trackPoint, int recordingGpsAccuracy) {
        if (serviceExecutor == null || serviceExecutor.isTerminated() || serviceExecutor.isShutdown()) {
            return;
        }
        serviceExecutor.execute(() -> service.newTrackPoint(trackPoint, recordingGpsAccuracy));
    }

    void sendGpsStatus(GpsStatusValue gpsStatusValue) {
        service.newGpsStatus(gpsStatusValue);
    }

    public GpsStatusValue getGpsStatus() {
        return locationHandler.getGpsStatus();
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
