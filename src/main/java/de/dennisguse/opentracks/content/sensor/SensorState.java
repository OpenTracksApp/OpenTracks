package de.dennisguse.opentracks.content.sensor;

import android.content.Context;

import de.dennisguse.opentracks.R;

public enum SensorState {
    NONE,
    CONNECTING,
    CONNECTED,
    DISCONNECTING,
    DISCONNECTED;

    public static String getStateAsString(SensorState state, Context c) {
        switch (state) {
            case NONE:
                return c.getString(R.string.value_none);
            case CONNECTING:
                return c.getString(R.string.sensor_state_connecting);
            case CONNECTED:
                return c.getString(R.string.sensor_state_connected);
            case DISCONNECTING:
            case DISCONNECTED:
                return c.getString(R.string.sensor_state_disconnected);
            default:
                return "";
        }
    }
}
