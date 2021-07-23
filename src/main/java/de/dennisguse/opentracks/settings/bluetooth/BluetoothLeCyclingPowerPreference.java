package de.dennisguse.opentracks.settings.bluetooth;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;

import androidx.annotation.RequiresApi;

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
public class BluetoothLeCyclingPowerPreference extends BluetoothLeSensorPreference {

    public BluetoothLeCyclingPowerPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public BluetoothLeCyclingPowerPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public BluetoothLeCyclingPowerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BluetoothLeCyclingPowerPreference(Context context) {
        super(context);
    }
}