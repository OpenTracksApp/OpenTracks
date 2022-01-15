package de.dennisguse.opentracks.ui.customRecordingLayout;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Optional;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.settings.PreferencesUtils;

public class SettingsCustomLayoutListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<Layout> layoutList;
    private final Context context;
    private final SettingsCustomLayoutProfileClickListener itemClickListener;

    public SettingsCustomLayoutListAdapter(Context context, SettingsCustomLayoutProfileClickListener itemClickListener) {
        this.context = context;
        this.itemClickListener = itemClickListener;
        layoutList = PreferencesUtils.getAllCustomLayouts();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.custom_layout_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        SettingsCustomLayoutListAdapter.ViewHolder viewHolder = (SettingsCustomLayoutListAdapter.ViewHolder) holder;
        Layout layout = layoutList.get(position);
        viewHolder.itemView.setTag(layout.getName());
        viewHolder.title.setText(layout.getName());
    }

    @Override
    public int getItemCount() {
        if (layoutList == null) {
            return 0;
        }
        return layoutList.size();
    }

    public List<Layout> getLayouts() {
        return layoutList;
    }

    public void reloadLayouts() {
        layoutList = PreferencesUtils.getAllCustomLayouts();
        notifyDataSetChanged();
    }

    public void removeLayout(int position) {
        layoutList.remove(position);
        PreferencesUtils.updateCustomLayouts(layoutList);
        notifyDataSetChanged();
    }

    public void restoreItem(Layout layout, int position) {
        layoutList.add(position, layout);
        PreferencesUtils.updateCustomLayouts(layoutList);
        notifyDataSetChanged();
    }

    private class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        final TextView title;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.custom_layout_title);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            String profile = (String) view.getTag();
            Optional<Layout> optionalLayout = layoutList.stream().filter(layout -> layout.sameName(new Layout(profile))).findFirst();
            optionalLayout.ifPresent(itemClickListener::onSettingsCustomLayoutProfileClicked);
        }
    }

    public interface SettingsCustomLayoutProfileClickListener {
        void onSettingsCustomLayoutProfileClicked(@NonNull Layout layout);
    }
}
