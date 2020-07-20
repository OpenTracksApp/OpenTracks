package de.dennisguse.opentracks.services.handlers;

import android.content.Context;
import android.content.SharedPreferences;

import de.dennisguse.opentracks.content.data.TrackPoint;

public class HandlerServer {
    private String TAG = HandlerServer.class.getSimpleName();

    private final LocationHandler locationHandler;
    private final HandlerServerInterface service;

    public HandlerServer(HandlerServerInterface service) {
        this.locationHandler = new LocationHandler(this);
        this.service = service;
    }

    public void start(Context context) {
        locationHandler.onStart(context);
        locationHandler.onSharedPreferenceChanged(context, null, null);
    }

    public void stop(Context context) {
        locationHandler.onStop(context);
    }

    public void onSharedPreferenceChanged(Context context, SharedPreferences preferences, String key) {
        locationHandler.onSharedPreferenceChanged(context, preferences, key);
    }

    public void sendTrackPoint(TrackPoint trackPoint, int recordingGpsAccuracy) {
        service.newTrackPoint(trackPoint, recordingGpsAccuracy);
    }

    public interface HandlerServerInterface {
        void newTrackPoint(TrackPoint trackPoint, int gpsAccuracy);
    }

    public interface Handler {
        void onStart(Context context);

        void onStop(Context context);

        void onSharedPreferenceChanged(Context context, SharedPreferences preferences, String key);
    }
}
