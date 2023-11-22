package de.dennisguse.opentracks.settings.bluetooth;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.util.AttributeSet;

import androidx.preference.PreferenceDialogFragmentCompat;

import java.util.List;

import de.dennisguse.opentracks.sensors.BluetoothHandlerBarometricPressure;

public class BluetoothLePressurePreference extends BluetoothLeSensorPreference {

    public BluetoothLePressurePreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public BluetoothLePressurePreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public BluetoothLePressurePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BluetoothLePressurePreference(Context context) {
        super(context);
    }

    @Override
    public PreferenceDialogFragmentCompat createInstance() {
        SensorManager sensorManager = (SensorManager) getContext().getSystemService(Context.SENSOR_SERVICE);
        Sensor pressureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
        boolean hasInternalSensor = pressureSensor != null;

        return BluetoothLeSensorPreference.BluetoothLeSensorPreferenceDialog
                .newInstance(getKey(), List.of(BluetoothHandlerBarometricPressure.BAROMETRIC_PRESSURE), hasInternalSensor);
    }
}