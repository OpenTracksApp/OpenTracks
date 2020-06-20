package de.dennisguse.opentracks.services;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class HandlerServer {
    private String TAG = HandlerServer.class.getSimpleName();

    private List<HandlerSubscriber> subscriberList;

    private static HandlerServer handlerServerInstance;
    private final LocationHandler locationHandler;
    private final Context context;

    public static HandlerServer getInstance(Context context) {
        if (handlerServerInstance == null) {
            handlerServerInstance = new HandlerServer(context);
        }
        return handlerServerInstance;
    }

    private HandlerServer(Context context) {
        this.subscriberList = new ArrayList<>();
        this.locationHandler = new LocationHandler();
        this.context = context;
    }

    public void subscribe(HandlerSubscriber s) {
        Log.e(TAG + " probando", "subscribe: " + s);
        if (subscriberList.isEmpty()) {
            Log.e(TAG + " probando", "es empty así que inicia el LocationHandler.");
            locationHandler.onStart(context);
        }
        if (!subscriberList.contains(s)) {
            Log.e(TAG + " probando", "como es nuevo lo inserta.");
            subscriberList.add(s);
        }
    }

    public void unsubscribe(HandlerSubscriber s) {
        Log.e(TAG + " probando", "unsubscribe: " + s);
        subscriberList.remove(s);
        if (subscriberList.isEmpty()) {
            Log.e(TAG + " probando", "se queda empty así que para el LocationHandler.");
            locationHandler.onStop();
        }
    }

    public void sendLocation(Location location) {
        for (HandlerSubscriber s : subscriberList) {
            s.newLocation(location);
        }
    }

    public interface HandlerSubscriber {
        void newLocation(Location location);
    }
}
