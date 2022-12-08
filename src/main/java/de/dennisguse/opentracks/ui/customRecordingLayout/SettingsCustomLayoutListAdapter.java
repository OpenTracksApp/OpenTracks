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

    private List<RecordingLayout> recordingLayoutList;
    private final Context context;
    private final SettingsCustomLayoutProfileClickListener itemClickListener;

    public SettingsCustomLayoutListAdapter(Context context, SettingsCustomLayoutProfileClickListener itemClickListener) {
        this.context = context;
        this.itemClickListener = itemClickListener;
        recordingLayoutList = PreferencesUtils.getAllCustomLayouts();
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
        RecordingLayout recordingLayout = recordingLayoutList.get(position);
        viewHolder.itemView.setTag(recordingLayout.getName());
        viewHolder.title.setText(recordingLayout.getName());
    }

    @Override
    public int getItemCount() {
        if (recordingLayoutList == null) {
            return 0;
        }
        return recordingLayoutList.size();
    }

    public List<RecordingLayout> getLayouts() {
        return recordingLayoutList;
    }

    public void reloadLayouts() {
        recordingLayoutList = PreferencesUtils.getAllCustomLayouts();
        notifyDataSetChanged();
    }

    public void removeLayout(int position) {
        recordingLayoutList.remove(position);
        PreferencesUtils.updateCustomLayouts(recordingLayoutList);
        notifyDataSetChanged();
    }

    public void restoreItem(RecordingLayout recordingLayout, int position) {
        recordingLayoutList.add(position, recordingLayout);
        PreferencesUtils.updateCustomLayouts(recordingLayoutList);
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
            Optional<RecordingLayout> optionalLayout = recordingLayoutList.stream().filter(layout -> layout.sameName(new RecordingLayout(profile))).findFirst();
            optionalLayout.ifPresent(itemClickListener::onSettingsCustomLayoutProfileClicked);
        }
    }

    public interface SettingsCustomLayoutProfileClickListener {
        void onSettingsCustomLayoutProfileClicked(@NonNull RecordingLayout recordingLayout);
    }
}
