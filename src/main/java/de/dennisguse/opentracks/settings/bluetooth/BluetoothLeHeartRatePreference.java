package de.dennisguse.opentracks.settings.bluetooth;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;

import androidx.annotation.RequiresApi;

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
public class BluetoothLeHeartRatePreference extends BluetoothLeSensorPreference {

    public BluetoothLeHeartRatePreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public BluetoothLeHeartRatePreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public BluetoothLeHeartRatePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BluetoothLeHeartRatePreference(Context context) {
        super(context);
    }
}