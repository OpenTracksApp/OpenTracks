package de.dennisguse.opentracks.sensors;

import android.content.Context;
import android.os.Handler;

public interface SensorConnector {

    void start(Context context, Handler handler);

    void stop(Context context);
}
