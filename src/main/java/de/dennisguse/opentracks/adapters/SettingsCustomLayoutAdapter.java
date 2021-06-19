package de.dennisguse.opentracks.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Optional;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.content.data.DataField;
import de.dennisguse.opentracks.content.data.Layout;
import de.dennisguse.opentracks.util.StatisticsUtils;

public class SettingsCustomLayoutAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private Layout layout;
    private final Context context;
    private final SettingsCustomLayoutItemClickListener itemClickListener;

    public SettingsCustomLayoutAdapter(Context context, SettingsCustomLayoutItemClickListener itemClickListener, Layout layout) {
        this.context = context;
        this.itemClickListener = itemClickListener;
        this.layout = layout;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.custom_stats_item, parent, false);
        return new SettingsCustomLayoutAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        SettingsCustomLayoutAdapter.ViewHolder viewHolder = (SettingsCustomLayoutAdapter.ViewHolder) holder;
        DataField field = layout.getFields().get(position);
        viewHolder.itemView.setTag(field.getKey());
        viewHolder.title.setText(field.getTitle());
        viewHolder.value.setText(StatisticsUtils.emptyValue(context, field.getKey()));

        viewHolder.title.setTextAppearance(context, field.isVisible() ? (field.isPrimary() ? R.style.StatsPrimaryLabel : R.style.StatsSecondaryLabel) : R.style.StatsLabelHidden);
        viewHolder.value.setTextAppearance(context, field.isVisible() ? (field.isPrimary() ? R.style.StatsPrimaryValue : R.style.StatsSecondaryValue) : R.style.StatsValueHidden);
        viewHolder.statusIcon.setVisibility(field.isVisible() ? View.GONE : View.VISIBLE);
        viewHolder.statusIcon.setImageDrawable(AppCompatResources.getDrawable(context, R.drawable.ic_baseline_visibility_off_24));
        viewHolder.moveIcon.setVisibility(field.isVisible() ? View.VISIBLE : View.GONE);
    }

    @Override
    public int getItemCount() {
        if (layout == null) {
            return 0;
        } else {
            return layout.getFields().size();
        }
    }

    @Override
    public int getItemViewType(int position) {
        return layout.getFields().get(position).isWide() ? CustomLayoutFieldType.WIDE.value() : CustomLayoutFieldType.SHORT.value();
    }

    public boolean isItemWide(int position) {
        return getItemViewType(position) == CustomLayoutFieldType.WIDE.value();
    }

    public DataField getItem(int position) {
        return layout.getFields().get(position);
    }

    public void swapValues(Layout data) {
        this.layout = data;
        if (this.layout != null) {
            this.notifyDataSetChanged();
        }
    }

    public Layout move(int fromPosition, int toPosition) {
        layout.moveField(fromPosition, toPosition);
        notifyItemMoved(fromPosition, toPosition);
        return layout;
    }

    private class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        final TextView title;
        final TextView value;
        final TextView unit;
        final ImageView statusIcon;
        final ImageView moveIcon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.stats_description_main);
            value = itemView.findViewById(R.id.stats_value);
            unit = itemView.findViewById(R.id.stats_unit);
            statusIcon = itemView.findViewById(R.id.stats_icon_show_status);
            moveIcon = itemView.findViewById(R.id.stats_iv_drag_indicator);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            String statTitle = (String) view.getTag();
            Optional<DataField> optionalField = layout.getFields().stream().filter(f -> f.getKey().equals(statTitle)).findFirst();
            optionalField.ifPresent(itemClickListener::onSettingsCustomLayoutItemClicked);
        }
    }

    public interface SettingsCustomLayoutItemClickListener {
        void onSettingsCustomLayoutItemClicked(@NonNull DataField field);
    }
}
