package de.dennisguse.opentracks.adapters;

import android.content.Context;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.content.sensor.SensorData;
import de.dennisguse.opentracks.content.sensor.SensorDataCycling;
import de.dennisguse.opentracks.content.sensor.SensorDataCyclingPower;
import de.dennisguse.opentracks.content.sensor.SensorDataHeartRate;
import de.dennisguse.opentracks.util.StringUtils;

public class SensorsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final int HEART_RATE_TYPE = 0;
    public static final int CADENCE_TYPE = 1;
    public static final int POWER_TYPE = 2;

    private List<Pair<Integer, SensorData>> sensorDataList;
    private final Context context;

    public SensorsAdapter(Context context) {
        this.context = context;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.sensor_item, parent, false);
        return new SensorsAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        SensorsAdapter.ViewHolder viewHolder = (SensorsAdapter.ViewHolder) holder;
        int type = sensorDataList.get(position).first;
        SensorData sensorData = sensorDataList.get(position).second;
        viewHolder.setData(sensorData, type);
    }

    @Override
    public int getItemCount() {
        if (sensorDataList == null) {
            return 0;
        } else {
            return sensorDataList.size();
        }
    }

    @Override
    public int getItemViewType(int position) {
        return sensorDataList.get(position).first;
    }

    public List<Pair<Integer, SensorData>> swapData(List<Pair<Integer, SensorData>> data) {
        if (sensorDataList == data) {
            return null;
        }

        sensorDataList = data;

        if (data != null) {
            this.notifyDataSetChanged();
        }

        return data;
    }

    private class ViewHolder extends RecyclerView.ViewHolder {
        TextView label;
        TextView sensorValue;
        TextView value;
        TextView unit;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            label = itemView.findViewById(R.id.stats_sensor_label);
            sensorValue = itemView.findViewById(R.id.stats_sensor_sensor_value);
            value = itemView.findViewById(R.id.stats_sensor_value);
            unit = itemView.findViewById(R.id.stats_sensor_unit);
        }

        public void setData(SensorData sensorData, int type) {
            switch (type) {
                case HEART_RATE_TYPE:
                    setHeartRateSensorData((SensorDataHeartRate) sensorData);
                    break;
                case CADENCE_TYPE:
                    setCadenceSensorData((SensorDataCycling.Cadence) sensorData);
                    break;
                case POWER_TYPE:
                    setPowerSensorData((SensorDataCyclingPower) sensorData);
                    break;
                default:
                    throw new RuntimeException("Unknown sensor type");
            }
        }

        private void setHeartRateSensorData(SensorDataHeartRate data) {
            String sensorValue = context.getString(R.string.value_unknown);
            String sensorName = context.getString(R.string.value_unknown);
            if (data != null) {
                sensorName = data.getSensorNameOrAddress();
                if (data.hasHeartRate_bpm() && data.isRecent()) {
                    sensorValue = StringUtils.formatDecimal(data.getHeartRate_bpm(), 0);
                }
            }

            this.label.setText(context.getString(R.string.sensor_state_heart_rate));
            this.sensorValue.setText(sensorName);
            this.value.setText(sensorValue);
        }

        private void setCadenceSensorData(SensorDataCycling.Cadence data) {
            String sensorValue = context.getString(R.string.value_unknown);
            String sensorName = context.getString(R.string.value_unknown);
            if (data != null) {
                sensorName = data.getSensorNameOrAddress();

                if (data.hasCadence_rpm() && data.isRecent()) {
                    sensorValue = StringUtils.formatDecimal(data.getCadence_rpm(), 0);
                }
            }

            this.label.setText(context.getString(R.string.sensor_state_cadence));
            this.sensorValue.setText(sensorName);
            this.value.setText(sensorValue);
        }

        private void setPowerSensorData(SensorDataCyclingPower data) {
            String sensorValue = context.getString(R.string.value_unknown);
            String sensorName = context.getString(R.string.value_unknown);
            if (data != null) {
                sensorName = data.getSensorNameOrAddress();

                if (data.hasPower_w() && data.isRecent()) {
                    sensorValue = StringUtils.formatDecimal(data.getPower_w(), 0);
                }
            }

            this.label.setText(context.getString(R.string.sensor_state_power));
            this.sensorValue.setText(sensorName);
            this.value.setText(sensorValue);
        }
    }
}
