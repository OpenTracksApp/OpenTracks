package de.dennisguse.opentracks.sensors.driver;

import android.content.Context;
import android.os.Handler;

public interface Driver {
    void connect(Context context, Handler handler, String address);

    boolean isConnected();

    void disconnect();
}
