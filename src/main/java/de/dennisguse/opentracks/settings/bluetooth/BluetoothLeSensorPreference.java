package de.dennisguse.opentracks.settings.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.DialogPreference;
import androidx.preference.PreferenceDialogFragmentCompat;
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.settings.BluetoothLeAdapter;
import de.dennisguse.opentracks.util.BluetoothUtils;
import de.dennisguse.opentracks.util.PreferencesUtils;

/**
 * Preference to select a discoverable Bluetooth LE device.
 * Based upon ListPreference.
 */
public abstract class BluetoothLeSensorPreference extends DialogPreference {

    private static final String TAG = BluetoothLeSensorPreference.class.getSimpleName();

    private static final String ARG_BLE_SERVICE_UUIDS = "bluetoothUUID";

    private static final int DEVICE_NONE_RESOURCEID = R.string.value_none;

    public BluetoothLeSensorPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public BluetoothLeSensorPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public BluetoothLeSensorPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BluetoothLeSensorPreference(Context context) {
        super(context);
    }

    private String value;
    private boolean valueSet = false;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        final boolean changed = !TextUtils.equals(this.value, value);
        if (changed || !valueSet) {
            this.value = value;
            valueSet = true;
            persistString(value);
            if (changed) {
                notifyChanged();
            }
        }
    }

    @Override
    protected void onSetInitialValue(Object defaultValue) {
        setValue(getPersistedString((String) defaultValue));
    }

    @Override
    public CharSequence getSummary() {
        if (getValue() == null || PreferencesUtils.isBluetoothSensorAddressNone(getContext(), getValue())) {
            return getContext().getString(DEVICE_NONE_RESOURCEID);
        }

        return getValue();
    }

    public static class BluetoothLeSensorPreferenceDialog extends PreferenceDialogFragmentCompat {

        private AnimatedVectorDrawableCompat bluetoothIcon;

        private int selectedEntryIndex;
        private final BluetoothLeAdapter listAdapter = new BluetoothLeAdapter();

        private BluetoothLeScanner scanner = null;
        private final ScanCallback scanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);
                Log.d(TAG, "Found device " + result.getDevice().getName() + " " + result);

                listAdapter.add(result.getDevice());
            }

            @Override
            public void onBatchScanResults(List<ScanResult> results) {
                for (ScanResult result : results) {
                    Log.d(TAG, "Found device " + result.getDevice().getName() + " " + result);
                    listAdapter.add(result.getDevice());
                }
            }

            @Override
            public void onScanFailed(int errorCode) {
                super.onScanFailed(errorCode);
                Log.e(TAG, "Bluetooth scan failed with errorCode " + errorCode);
                Toast.makeText(getContext(), R.string.sensor_could_not_scan, Toast.LENGTH_LONG).show();
                dismiss();
            }
        };

        public static BluetoothLeSensorPreferenceDialog newInstance(String preferenceKey, UUID sensorUUID) {
            return newInstance(preferenceKey, Collections.singletonList(sensorUUID));
        }

        public static BluetoothLeSensorPreferenceDialog newInstance(String preferenceKey, List<UUID> sensorUUIDs) {
            final BluetoothLeSensorPreferenceDialog fragment = new BluetoothLeSensorPreferenceDialog();
            final Bundle b = new Bundle(1);
            b.putString(ARG_KEY, preferenceKey);
            b.putParcelableArrayList(ARG_BLE_SERVICE_UUIDS, new ArrayList<>(sensorUUIDs.stream().map(ParcelUuid::new).collect(Collectors.toList())));
            fragment.setArguments(b);
            return fragment;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            List<ParcelUuid> serviceUUIDs = getArguments().getParcelableArrayList(ARG_BLE_SERVICE_UUIDS);

            // Don't know why: need to load the drawable _twice_, so that animation is actually started.
            bluetoothIcon = AnimatedVectorDrawableCompat.create(getContext(), R.drawable.ic_bluetooth_searching_animated_24dp);
            bluetoothIcon = AnimatedVectorDrawableCompat.create(getContext(), R.drawable.ic_bluetooth_searching_animated_24dp);
            bluetoothIcon.start();

            BluetoothAdapter bluetoothAdapter = BluetoothUtils.getAdapter(getContext());
            if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
                Log.w(TAG, "Bluetooth adapter is present or not enabled.");
                Toast.makeText(getContext(), R.string.bluetooth_disabled, Toast.LENGTH_SHORT).show();
                dismiss();
                return;
            }

            if (bluetoothAdapter.isDiscovering()) {
                Log.i(TAG, "Cancelling ongoing Bluetooth discovery.");
                bluetoothAdapter.cancelDiscovery();
            }

            scanner = bluetoothAdapter.getBluetoothLeScanner();
            if (scanner == null) {
                Log.e(TAG, "BluetoothLeScanner is null.");
                dismiss();
                return;
            }

            String deviceNone = getContext().getString(R.string.sensor_type_value_none);
            listAdapter.add(getContext().getString(DEVICE_NONE_RESOURCEID), deviceNone);
            selectedEntryIndex = 0;

            BluetoothLeSensorPreference preference = (BluetoothLeSensorPreference) getPreference();
            String deviceSelected = preference.getValue();
            if (deviceSelected != null && !deviceNone.equals(deviceSelected)) {
                listAdapter.add(preference.getValue(), preference.getValue());
                selectedEntryIndex = 1;
            }

            List<ScanFilter> scanFilter = serviceUUIDs.stream().map(it -> new ScanFilter.Builder().setServiceUuid(it).build()).collect(Collectors.toList());

            ScanSettings.Builder scanSettingsBuilder = new ScanSettings.Builder();
            scanSettingsBuilder.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY);

            scanner.startScan(scanFilter, scanSettingsBuilder.build(), scanCallback);
        }

        //Behave like ListPreferenceDialogFragmentCompat, but uses a custom listAdapter.
        @Override
        protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
            super.onPrepareDialogBuilder(builder);

            builder.setSingleChoiceItems(listAdapter, selectedEntryIndex,
                    (dialog, which) -> {
                        selectedEntryIndex = which;

                        BluetoothLeSensorPreferenceDialog.this.onClick(dialog, DialogInterface.BUTTON_POSITIVE);
                        dialog.dismiss();
                    });

            builder.setIcon(bluetoothIcon);

            builder.setPositiveButton(null, null);
        }

        @Override
        public void onDialogClosed(boolean positiveResult) {
            if (scanner != null) {
                scanner.stopScan(scanCallback);
            }

            if (positiveResult && selectedEntryIndex >= 0) {
                String value = listAdapter.get(selectedEntryIndex).getAddress();
                BluetoothLeSensorPreference preference = (BluetoothLeSensorPreference) getPreference();
                if (preference.callChangeListener(value)) {
                    preference.setValue(value);
                }
            }
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            bluetoothIcon = null;
            scanner = null;
        }
    }
}
