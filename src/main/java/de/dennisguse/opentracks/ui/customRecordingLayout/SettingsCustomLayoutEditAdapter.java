package de.dennisguse.opentracks.ui.customRecordingLayout;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.util.StatisticsUtils;
import de.dennisguse.opentracks.viewmodels.Mapping;
import de.dennisguse.opentracks.viewmodels.StatisticViewHolder;

public class SettingsCustomLayoutEditAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private Layout layout;
    private final Context context;
    private final SettingsCustomLayoutItemClickListener itemClickListener;
    private final Map<String, Callable<StatisticViewHolder<?>>> mapping;


    public SettingsCustomLayoutEditAdapter(Context context, SettingsCustomLayoutItemClickListener itemClickListener, Layout layout) {
        this.context = context;
        this.itemClickListener = itemClickListener;
        this.layout = layout;

        mapping = Mapping.create(context);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.custom_stats_item, parent, false); //TODO CustomStatsItemBinding or rather use mapping.get().call() directly
        return new SettingsCustomLayoutEditAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        SettingsCustomLayoutEditAdapter.ViewHolder viewHolder = (SettingsCustomLayoutEditAdapter.ViewHolder) holder;
        DataField field = layout.getFields().get(position);
        viewHolder.itemView.setTag(field.getKey());
        try {
            viewHolder.title.setText(mapping.get(field.getKey()).call().getTitleId());
        } catch (Exception e) {
            //Ignored.
        }
        viewHolder.value.setText(StatisticsUtils.emptyValue(context, field.getKey()));

        viewHolder.title.setTextAppearance(context, field.isVisible() ? (field.isPrimary() ? R.style.TextAppearance_OpenTracks_PrimaryHeader : R.style.TextAppearance_OpenTracks_SecondaryHeader) : R.style.TextAppearance_OpenTracks_HiddenHeader);
        viewHolder.value.setTextAppearance(context, field.isVisible() ? (field.isPrimary() ? R.style.TextAppearance_OpenTracks_PrimaryValue : R.style.TextAppearance_OpenTracks_SecondaryValue) : R.style.TextAppearance_OpenTracks_HiddenValue);
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

    public boolean isItemWide(int position) {
        return layout.getFields().get(position).isWide();
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
