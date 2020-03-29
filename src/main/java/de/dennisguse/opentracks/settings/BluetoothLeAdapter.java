package de.dennisguse.opentracks.settings;

import android.bluetooth.BluetoothDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
        textView.setText(device.getName());

        return currentView;
    }

    public void add(String name, String address) {
        Device device = new Device(name, address);
        if (!devices.contains(device)) {
            devices.add(new Device(name, address));
        } else {
            for (Device currentDevice : devices) {
                if (currentDevice.getAddress().equals(address)) {
                    currentDevice.setName(name);
                }
            }
        }
        notifyDataSetChanged();
    }

    public void add(BluetoothDevice bluetoothDevice) {
        add(bluetoothDevice.getName(), bluetoothDevice.getAddress());
    }

    public Device get(int index) {
        return devices.get(index);
    }

    public static class Device {
        private String name;
        private final String address;

        public Device(String name, String address) {
            this.name = name;
            this.address = address;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getAddress() {
            return address;
        }

        /**
         * Check if the address is identical.
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Device)) return false;
            Device device = (Device) o;
            return address.equals(device.address);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, address);
        }
    }
}
