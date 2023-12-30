package de.dennisguse.opentracks.settings.bluetooth;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@SuppressLint("MissingPermission")
public class BluetoothLeAdapter extends BaseAdapter {

    /**
     * Contains a unique list (by address) of devices.
     */
    private final List<Device> devices = new ArrayList<>();

    @Override
    public int getCount() {
        return devices.size();
    }

    @Override
    public Object getItem(int position) {
        return devices.get(position);
    }

    @Override
    public long getItemId(int position) {
        return devices.get(position).hashCode();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View currentView = convertView;
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            //TODO Check if there is a better way to achieve identical look and feel to ListPreference.
            //Use material design single choice; for old style use: android.R.layout.select_dialog_singlechoice
            currentView = inflater.inflate(androidx.appcompat.R.layout.select_dialog_singlechoice_material, null);
        }

        Device device = devices.get(position);
        TextView textView = currentView.findViewById(android.R.id.text1);
        textView.setText(device.getNameOrAddress());

        return currentView;
    }

    /**
     * @return Data changed?
     */
    public boolean add(String address, String name) {
        Device device = new Device(address, name);
        if (!devices.contains(device)) {
            devices.add(device);
            return true;
        } else {
            for (Device currentDevice : devices) {
                if (currentDevice.address.equals(address)) {
                    currentDevice.name = name;
                    return true;
                }
            }
        }
        return false;
    }

    public void addAll(List<BluetoothDevice> bluetoothDevices) {
        boolean dataSetChanged = bluetoothDevices.stream()
                .anyMatch(bluetoothDevice -> add(bluetoothDevice.getAddress(), bluetoothDevice.getName()));

        if (dataSetChanged) notifyDataSetChanged();
    }

    public Device get(int index) {
        return devices.get(index);
    }

    public static class Device {

        @NonNull
        private final String address;
        private String name;

        Device(@NonNull String address, String name) {
            Objects.requireNonNull(address);
            this.address = address;
            this.name = name;
        }

        public String getAddress() {
            return address;
        }

        public String getNameOrAddress() {
            return name != null ? name : address;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Device device)) return false;

            return address.equals(device.address);
        }

        @Override
        public int hashCode() {
            return Objects.hash(address);
        }
    }
}
