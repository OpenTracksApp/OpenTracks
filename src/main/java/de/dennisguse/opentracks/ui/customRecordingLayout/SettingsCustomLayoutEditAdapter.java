package de.dennisguse.opentracks.ui.customRecordingLayout;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.recyclerview.widget.RecyclerView;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.data.models.Altitude;
import de.dennisguse.opentracks.data.models.Distance;
import de.dennisguse.opentracks.data.models.Speed;
import de.dennisguse.opentracks.data.models.Track;
import de.dennisguse.opentracks.data.models.TrackPoint;
import de.dennisguse.opentracks.databinding.CustomStatsItemBinding;
import de.dennisguse.opentracks.services.RecordingData;
import de.dennisguse.opentracks.settings.UnitSystem;
import de.dennisguse.opentracks.stats.TrackStatistics;
import de.dennisguse.opentracks.viewmodels.Mapping;
import de.dennisguse.opentracks.viewmodels.StatisticViewHolder;

public class SettingsCustomLayoutEditAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = SettingsCustomLayoutEditAdapter.class.getSimpleName();

    private static final RecordingData demoData;

    static {
        TrackStatistics trackStatistics = new TrackStatistics();
        trackStatistics.setStartTime(Instant.ofEpochMilli(0));
        trackStatistics.setMovingTime(Duration.ofMinutes(0));
        trackStatistics.setTotalTime(Duration.ofMinutes(0));

        trackStatistics.setTotalDistance(Distance.of(0));

        trackStatistics.setTotalAltitudeGain(0f);
        trackStatistics.setTotalAltitudeLoss(0f);
        Track track = new Track(ZoneOffset.UTC);
        track.setTrackStatistics(trackStatistics);

        TrackPoint lastTrackPoint = new TrackPoint(TrackPoint.Type.TRACKPOINT, Instant.ofEpochMilli(0));
        lastTrackPoint.setLatitude(0);
        lastTrackPoint.setLongitude(0);
        lastTrackPoint.setAltitude(Altitude.EGM2008.of(0));
        lastTrackPoint.setSpeed(Speed.of(0));

        demoData = new RecordingData(track, lastTrackPoint, null);
    }

    private RecordingLayout recordingLayout;
    private final Context context;
    private final SettingsCustomLayoutItemClickListener itemClickListener;
    private final Map<String, Callable<StatisticViewHolder<?>>> mapping;


    public SettingsCustomLayoutEditAdapter(Context context, SettingsCustomLayoutItemClickListener itemClickListener, RecordingLayout recordingLayout) {
        this.context = context;
        this.itemClickListener = itemClickListener;
        this.recordingLayout = recordingLayout;

        mapping = Mapping.create(context);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        CustomStatsItemBinding binding = CustomStatsItemBinding.inflate(LayoutInflater.from(context), parent, false);
        return new SettingsCustomLayoutEditAdapter.ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        SettingsCustomLayoutEditAdapter.ViewHolder viewHolder = (SettingsCustomLayoutEditAdapter.ViewHolder) holder;
        DataField field = recordingLayout.getFields().get(position);
        viewHolder.itemView.setTag(field.getKey());
        try {
            StatisticViewHolder<?> m = mapping.get(field.getKey()).call();
            m.initialize(context, LayoutInflater.from(context));
            m.configureUI(field);
            m.onChanged(UnitSystem.METRIC, demoData);

            viewHolder.viewBinding.statsLayout.removeAllViews(); //TODO this is not really performant
            viewHolder.viewBinding.statsLayout.addView(m.getView());
        } catch (Exception e) {
            Log.e(TAG, "Couldn't to instantiate UI for DataField with key " + field.getKey() + " " + e.getMessage());
            throw new RuntimeException(e);
        }
        viewHolder.viewBinding.statsIconShowStatus.setVisibility(field.isVisible() ? View.GONE : View.VISIBLE);
        viewHolder.viewBinding.statsIconShowStatus.setImageDrawable(AppCompatResources.getDrawable(context, R.drawable.ic_baseline_visibility_off_24));
        viewHolder.viewBinding.statsIvDragIndicator.setVisibility(field.isVisible() ? View.VISIBLE : View.GONE);
    }

    @Override
    public int getItemCount() {
        if (recordingLayout == null) {
            return 0;
        } else {
            return recordingLayout.getFields().size();
        }
    }

    public boolean isItemWide(int position) {
        return recordingLayout.getFields().get(position).isWide();
    }

    public DataField getItem(int position) {
        return recordingLayout.getFields().get(position);
    }

    public void swapValues(RecordingLayout data) {
        this.recordingLayout = data;
        if (this.recordingLayout != null) {
            this.notifyDataSetChanged();
        }
    }

    public RecordingLayout move(int fromPosition, int toPosition) {
        recordingLayout.moveField(fromPosition, toPosition);
        notifyItemMoved(fromPosition, toPosition);
        return recordingLayout;
    }

    private class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        final CustomStatsItemBinding viewBinding;

        public ViewHolder(@NonNull CustomStatsItemBinding viewBinding) {
            super(viewBinding.getRoot());
            this.viewBinding = viewBinding;

            viewBinding.getRoot().setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            String statTitle = (String) view.getTag();
            Optional<DataField> optionalField = recordingLayout.getFields().stream().filter(f -> f.getKey().equals(statTitle)).findFirst();
            optionalField.ifPresent(itemClickListener::onSettingsCustomLayoutItemClicked);
        }
    }

    public interface SettingsCustomLayoutItemClickListener {
        void onSettingsCustomLayoutItemClicked(@NonNull DataField field);
    }
}
