package de.dennisguse.opentracks.settings.bluetooth;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
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
import java.util.List;
import java.util.stream.Collectors;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.sensors.BluetoothUtils;
import de.dennisguse.opentracks.sensors.SensorType;
import de.dennisguse.opentracks.sensors.ServiceMeasurementUUID;
import de.dennisguse.opentracks.settings.PreferencesUtils;
import de.dennisguse.opentracks.util.PermissionRequester;

/**
 * Preference to select a discoverable Bluetooth LE device.
 * Based upon ListPreference.
 */
@SuppressLint("MissingPermission")
public abstract class BluetoothLeSensorPreference extends DialogPreference {

    private static final String TAG = BluetoothLeSensorPreference.class.getSimpleName();

    private static final String ARG_BLE_SERVICE_UUIDS = "bluetoothUUID";
    private static final String ARG_INCLUDE_INTERNAL = "supportsInternal";

    private static final int DEVICE_NONE_RESOURCEID = R.string.value_none;

    private static final int SENSOR_INTERNAL_RESOURCEID = R.string.value_internal_sensor;

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

    private void setValue(String value) {
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
        if (value == null || SensorType.NONE.getPreferenceValue().equals(value)) {
            return getContext().getString(DEVICE_NONE_RESOURCEID);
        }
        if (SensorType.INTERNAL.getPreferenceValue().equals(value)) {
            return getContext().getString(SENSOR_INTERNAL_RESOURCEID);
        }

        BluetoothAdapter bluetoothAdapter = BluetoothUtils.getAdapter(getContext());
        if (bluetoothAdapter == null) {
            Log.w(TAG, "No Bluetooth adapter present");
            return value;
        }

        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(value);
        if (device != null && device.getName() != null) {
            return getContext().getString(R.string.bluetooth_sensor_summary, device.getAddress(), device.getName());
        }
        return value;
    }

    public abstract PreferenceDialogFragmentCompat createInstance();

    public static class BluetoothLeSensorPreferenceDialog extends PreferenceDialogFragmentCompat {

        private AnimatedVectorDrawableCompat bluetoothIcon;

        private int selectedEntryIndex;
        private final BluetoothLeAdapter listAdapter = new BluetoothLeAdapter();

        private BluetoothLeScanner scanner = null;
        private final ScanCallback scanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                Log.d(TAG, "Found device " + result.getDevice().getName() + " " + result);
                onBatchScanResults(List.of(result));
            }

            @Override
            public void onBatchScanResults(List<ScanResult> results) {
                listAdapter.addAll(results.stream().map(ScanResult::getDevice).collect(Collectors.toList()));
            }

            @Override
            public void onScanFailed(int errorCode) {
                super.onScanFailed(errorCode);
                Log.e(TAG, "Bluetooth scan failed with errorCode " + errorCode);
                Toast.makeText(getContext(), R.string.sensor_could_not_scan, Toast.LENGTH_LONG).show();
                dismiss();
            }
        };

        public static BluetoothLeSensorPreferenceDialog newInstance(String preferenceKey, List<ServiceMeasurementUUID> sensorUUIDs) {
            return newInstance(preferenceKey, sensorUUIDs, false);
        }

        public static BluetoothLeSensorPreferenceDialog newInstance(String preferenceKey, List<ServiceMeasurementUUID> sensorUUIDs, boolean includeInternalSensor) {
            final BluetoothLeSensorPreferenceDialog fragment = new BluetoothLeSensorPreferenceDialog();
            final Bundle b = new Bundle(3);
            b.putString(ARG_KEY, preferenceKey);
            b.putParcelableArrayList(ARG_BLE_SERVICE_UUIDS, new ArrayList<>(sensorUUIDs.stream()
                    .map(ServiceMeasurementUUID::serviceUUID)
                    .map(ParcelUuid::new)
                    .collect(Collectors.toList())));
            b.putBoolean(ARG_INCLUDE_INTERNAL, includeInternalSensor);

            fragment.setArguments(b);
            return fragment;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Don't know why: need to load the drawable _twice_, so that animation is actually started.
            bluetoothIcon = AnimatedVectorDrawableCompat.create(getContext(), R.drawable.ic_bluetooth_searching_animated_24dp);
            bluetoothIcon = AnimatedVectorDrawableCompat.create(getContext(), R.drawable.ic_bluetooth_searching_animated_24dp);
            bluetoothIcon.start();

            PermissionRequester.BLUETOOTH.requestPermissionsIfNeeded(getContext(), this,
                    this::startBluetoothScan,
                    (requester) -> {
                        if (requester.shouldShowRequestPermissionRationale(this)) {
                            Toast.makeText(getContext(), R.string.permission_bluetooth_failed_rejected, Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(getContext(), R.string.permission_bluetooth_failed, Toast.LENGTH_SHORT).show();
                        }
                        dismiss();
                    });

            startBluetoothScan();
        }

        private void startBluetoothScan() {
            List<ParcelUuid> serviceUUIDs = getArguments().getParcelableArrayList(ARG_BLE_SERVICE_UUIDS);
            boolean includeInternalSensor = getArguments().getBoolean(ARG_INCLUDE_INTERNAL);

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

            listAdapter.add(SensorType.NONE.getPreferenceValue(), getContext().getString(DEVICE_NONE_RESOURCEID));
            selectedEntryIndex = 0;

            BluetoothLeSensorPreference preference = (BluetoothLeSensorPreference) getPreference();
            String deviceSelected = preference.value;
            if (includeInternalSensor) {
                listAdapter.add(SensorType.INTERNAL.getPreferenceValue(), getString(SENSOR_INTERNAL_RESOURCEID));
                if (SensorType.INTERNAL.getPreferenceValue().equals(deviceSelected)) {
                    selectedEntryIndex = 1;
                }
            }

            if (deviceSelected != null && SensorType.REMOTE.equals(PreferencesUtils.getSensorType(deviceSelected))) {
                listAdapter.add(preference.value, preference.value);
                selectedEntryIndex = !includeInternalSensor ? 1 : 2;
            }

            List<ScanFilter> scanFilter = null;
            if (PreferencesUtils.getBluetoothFilterEnabled()) {
                scanFilter = serviceUUIDs.stream()
                        .map(it -> new ScanFilter.Builder().setServiceUuid(it).build())
                        .collect(Collectors.toList());
            }

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
