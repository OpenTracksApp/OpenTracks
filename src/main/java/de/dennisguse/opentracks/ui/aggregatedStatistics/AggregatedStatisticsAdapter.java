package de.dennisguse.opentracks.ui.aggregatedStatistics;

import android.content.Context;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.data.models.ActivityType;
import de.dennisguse.opentracks.data.models.DistanceFormatter;
import de.dennisguse.opentracks.data.models.SpeedFormatter;
import de.dennisguse.opentracks.settings.PreferencesUtils;
import de.dennisguse.opentracks.settings.UnitSystem;
import de.dennisguse.opentracks.util.StringUtils;

public class AggregatedStatisticsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private AggregatedStatistics aggregatedStatistics;
    private final Context context;

    public AggregatedStatisticsAdapter(Context context, AggregatedStatistics aggregatedStatistics) {
        this.context = context;
        this.aggregatedStatistics = aggregatedStatistics;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.aggregated_stats_list_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ViewHolder viewHolder = (ViewHolder) holder;

        AggregatedStatistics.AggregatedStatistic aggregatedStatistic = aggregatedStatistics.getItem(position);

        String type = aggregatedStatistic.getActivityTypeLocalized();
        if (ActivityType.findByLocalizedString(context, type).isShowSpeedPreferred()) {
            viewHolder.setSpeed(aggregatedStatistic);
        } else {
            viewHolder.setPace(aggregatedStatistic);
        }
    }

    @Override
    public int getItemCount() {
        if (aggregatedStatistics == null) {
            return 0;
        }
        return aggregatedStatistics.getCount();
    }

    public void swapData(AggregatedStatistics aggregatedStatistics) {
        this.aggregatedStatistics = aggregatedStatistics;
        this.notifyDataSetChanged();
    }

    public List<String> getCategories() {
        List<String> categories = new ArrayList<>();
        for (int i = 0; i < aggregatedStatistics.getCount(); i++) {
            categories.add(aggregatedStatistics.getItem(i).getActivityTypeLocalized());
        }
        return categories;
    }

    private class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView sportIcon;
        private final TextView typeLabel;
        private final TextView numTracks;
        private final TextView distance;
        private final TextView distanceUnit;
        private final TextView time;

        private final TextView avgSpeed;
        private final TextView avgSpeedUnit;
        private final TextView avgSpeedLabel;
        private final TextView maxSpeed;
        private final TextView maxSpeedUnit;
        private final TextView maxSpeedLabel;

        private UnitSystem unitSystem = UnitSystem.defaultUnitSystem();
        private boolean reportSpeed;

        public ViewHolder(View view) {
            super(view);
            sportIcon = view.findViewById(R.id.aggregated_stats_sport_icon);
            typeLabel = view.findViewById(R.id.aggregated_stats_type_label);
            numTracks = view.findViewById(R.id.aggregated_stats_num_tracks);
            distance = view.findViewById(R.id.aggregated_stats_distance);
            distanceUnit = view.findViewById(R.id.aggregated_stats_distance_unit);
            time = view.findViewById(R.id.aggregated_stats_time);

            avgSpeed = view.findViewById(R.id.aggregated_stats_avg_rate);
            avgSpeedUnit = view.findViewById(R.id.aggregated_stats_avg_rate_unit);
            avgSpeedLabel = view.findViewById(R.id.aggregated_stats_avg_rate_label);
            maxSpeed = view.findViewById(R.id.aggregated_stats_max_rate);
            maxSpeedUnit = view.findViewById(R.id.aggregated_stats_max_rate_unit);
            maxSpeedLabel = view.findViewById(R.id.aggregated_stats_max_rate_label);
        }

        public void setSpeed(AggregatedStatistics.AggregatedStatistic aggregatedStatistic) {
            setCommonValues(aggregatedStatistic);

            SpeedFormatter formatter = SpeedFormatter.Builder().setUnit(unitSystem).setReportSpeedOrPace(reportSpeed).build(context);
            {
                Pair<String, String> parts = formatter.getSpeedParts(aggregatedStatistic.getTrackStatistics().getAverageMovingSpeed());
                avgSpeed.setText(parts.first);
                avgSpeedUnit.setText(parts.second);
                avgSpeedLabel.setText(context.getString(R.string.stats_average_moving_speed));
            }

            {
                Pair<String, String> parts = formatter.getSpeedParts(aggregatedStatistic.getTrackStatistics().getMaxSpeed());
                maxSpeed.setText(parts.first);
                maxSpeedUnit.setText(parts.second);
                maxSpeedLabel.setText(context.getString(R.string.stats_max_speed));
            }
        }

        public void setPace(AggregatedStatistics.AggregatedStatistic aggregatedStatistic) {
            setCommonValues(aggregatedStatistic);

            SpeedFormatter formatter = SpeedFormatter.Builder().setUnit(unitSystem).setReportSpeedOrPace(reportSpeed).build(context);
            {
                Pair<String, String> parts = formatter.getSpeedParts(aggregatedStatistic.getTrackStatistics().getAverageMovingSpeed());
                avgSpeed.setText(parts.first);
                avgSpeedUnit.setText(parts.second);
                avgSpeedLabel.setText(context.getString(R.string.stats_average_moving_pace));
            }

            {
                Pair<String, String> parts = formatter.getSpeedParts(aggregatedStatistic.getTrackStatistics().getMaxSpeed());
                maxSpeed.setText(parts.first);
                maxSpeedUnit.setText(parts.second);
                maxSpeedLabel.setText(R.string.stats_fastest_pace);
            }
        }

        //TODO Check preference handling.
        private void setCommonValues(AggregatedStatistics.AggregatedStatistic aggregatedStatistic) {
            String activityType = aggregatedStatistic.getActivityTypeLocalized();

            reportSpeed = PreferencesUtils.isReportSpeed(activityType);
            unitSystem = PreferencesUtils.getUnitSystem();

            sportIcon.setImageResource(getIcon(aggregatedStatistic));
            typeLabel.setText(activityType);
            numTracks.setText(StringUtils.valueInParentheses(String.valueOf(aggregatedStatistic.getCountTracks())));

            Pair<String, String> parts = DistanceFormatter.Builder()
                    .setUnit(unitSystem)
                    .build(context).getDistanceParts(aggregatedStatistic.getTrackStatistics().getTotalDistance());
            distance.setText(parts.first);
            distanceUnit.setText(parts.second);

            time.setText(StringUtils.formatElapsedTime(aggregatedStatistic.getTrackStatistics().getMovingTime()));
        }

        private int getIcon(AggregatedStatistics.AggregatedStatistic aggregatedStatistic) {
            String localizedActivityType = aggregatedStatistic.getActivityTypeLocalized();
            return ActivityType.findByLocalizedString(context, localizedActivityType)
                    .getIconDrawableId();
        }
    }
}
