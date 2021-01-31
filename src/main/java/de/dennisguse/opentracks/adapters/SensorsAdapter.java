package de.dennisguse.opentracks.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.viewmodels.SensorDataModel;

public class SensorsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<SensorDataModel> sensorDataList;
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
        SensorDataModel sensorDataModel = sensorDataList.get(position);
        viewHolder.setData(sensorDataModel);
    }

    @Override
    public int getItemCount() {
        if (sensorDataList == null) {
            return 0;
        } else {
            return sensorDataList.size();
        }
    }

    public List<SensorDataModel> swapData(List<SensorDataModel> data) {
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
        final TextView label;
        final TextView sensorValue;
        final TextView value;
        final TextView unit;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            label = itemView.findViewById(R.id.stats_sensor_label);
            sensorValue = itemView.findViewById(R.id.stats_sensor_sensor_value);
            value = itemView.findViewById(R.id.stats_sensor_value);
            unit = itemView.findViewById(R.id.stats_sensor_unit);
        }

        public void setData(SensorDataModel sensorDataModel) {
            String sensorName = sensorDataModel.getSensorName();
            String sensorValue = sensorDataModel.hasValue() ? sensorDataModel.getSensorValue() : context.getString(R.string.value_unknown);

            this.label.setText(context.getString(sensorDataModel.getLabelId()));
            if (sensorName == null) {
                this.sensorValue.setVisibility(View.GONE);
            } else {
                this.sensorValue.setText(sensorName);
            }
            this.value.setText(sensorValue);
            this.unit.setText(context.getString(sensorDataModel.getUnitId()));
        }
    }
}
