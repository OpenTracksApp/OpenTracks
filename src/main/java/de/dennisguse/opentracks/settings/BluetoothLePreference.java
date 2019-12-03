package de.dennisguse.opentracks.settings;

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

import java.util.ArrayList;
import java.util.List;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.util.BluetoothUtils;
import de.dennisguse.opentracks.util.PreferencesUtils;

/**
 * Preference to select a discoverable Bluetooth LE device.
 * Based upon ListPreference.
 */
public class BluetoothLePreference extends DialogPreference {

    private final static String TAG = BluetoothLePreference.class.getSimpleName();

    private final static int DEVICE_NONE_RESOURCEID = R.string.value_none;

    public BluetoothLePreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public BluetoothLePreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public BluetoothLePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BluetoothLePreference(Context context) {
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
        if (getValue() == null || PreferencesUtils.isBluetoothHeartRateSensorAddressDefault(getContext(), getValue())) {
            return getContext().getString(DEVICE_NONE_RESOURCEID);
        }

        return getValue();
    }

    public static class BluetoothLePreferenceDialog extends PreferenceDialogFragmentCompat {

        private int selectedEntryIndex;
        private BluetoothLeAdapter listAdapter = new BluetoothLeAdapter();

        private BluetoothLeScanner scanner = null;
        private ScanCallback scanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);
                Log.d(TAG, "Found device " + result.getDevice().getName() + " " + result);

                listAdapter.add(result.getDevice());
            }

            @Override
            public void onBatchScanResults(List<ScanResult> results) {
                for (ScanResult scanResult : results) {
                    listAdapter.add(scanResult.getDevice());
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

        static BluetoothLePreferenceDialog newInstance(String key) {
            final BluetoothLePreferenceDialog fragment = new BluetoothLePreferenceDialog();
            final Bundle b = new Bundle(1);
            b.putString(ARG_KEY, key);
            fragment.setArguments(b);
            return fragment;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            BluetoothAdapter bluetoothAdapter = BluetoothUtils.getDefaultBluetoothAdapter(TAG);
            if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
                Log.w(TAG, "Bluetooth adapter is present or not enabled.");
                Toast.makeText(getContext(), R.string.bluetooth_disabled, Toast.LENGTH_SHORT).show();
                dismiss();
                return;
            }

            if (bluetoothAdapter.isDiscovering()) {
                Log.i(TAG, "Cancelling ongoing bluetooth discovery.");
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

            BluetoothLePreference preference = (BluetoothLePreference) getPreference();
            String deviceSelected = preference.getValue();
            if (deviceSelected != null && !deviceNone.equals(deviceSelected)) {
                listAdapter.add(preference.getValue(), preference.getValue());
                selectedEntryIndex = 1;
            }

            ScanFilter.Builder scanFilterBuilder = new ScanFilter.Builder().setServiceUuid(new ParcelUuid(BluetoothUtils.HEART_RATE_SERVICE_UUID));
            List<ScanFilter> scanFilter = new ArrayList<>();
            scanFilter.add(scanFilterBuilder.build());

            ScanSettings.Builder scanSettingsBuilder = new ScanSettings.Builder();
            scanSettingsBuilder.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY);

            scanner.startScan(scanFilter, scanSettingsBuilder.build(), scanCallback);
        }

        //Behave like ListPreferenceDialogFragmentCompat, but uses a custom listAdapter.
        @Override
        protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
            super.onPrepareDialogBuilder(builder);

            builder.setSingleChoiceItems(listAdapter, selectedEntryIndex,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            selectedEntryIndex = which;

                            BluetoothLePreferenceDialog.this.onClick(dialog, DialogInterface.BUTTON_POSITIVE);
                            dialog.dismiss();
                        }
                    });
            builder.setIcon(android.R.drawable.stat_sys_data_bluetooth);
            builder.setPositiveButton(null, null);
        }

        @Override
        public void onDialogClosed(boolean positiveResult) {
            if (scanner != null) {
                scanner.stopScan(scanCallback);
            }

            if (positiveResult && selectedEntryIndex >= 0) {
                String value = listAdapter.get(selectedEntryIndex).getAddress();
                BluetoothLePreference preference = (BluetoothLePreference) getPreference();
                if (preference.callChangeListener(value)) {
                    preference.setValue(value);
                }
            }
        }
    }
}
