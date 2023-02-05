package de.dennisguse.opentracks.ui.aggregatedStatistics;

import android.content.Context;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.data.models.DistanceFormatter;
import de.dennisguse.opentracks.data.models.SpeedFormatter;
import de.dennisguse.opentracks.settings.PreferencesUtils;
import de.dennisguse.opentracks.settings.UnitSystem;
import de.dennisguse.opentracks.util.StringUtils;
import de.dennisguse.opentracks.util.TrackIconUtils;

public class AggregatedStatisticsAdapter extends BaseAdapter {

    private AggregatedStatistics aggregatedStatistics;
    private final Context context;

    public AggregatedStatisticsAdapter(Context context, AggregatedStatistics aggregatedStatistics) {
        this.context = context;
        this.aggregatedStatistics = aggregatedStatistics;
    }

    @Override
    public int getCount() {
        if (aggregatedStatistics != null) {
            return aggregatedStatistics.getCount();
        }
        return 0;
    }

    @Override
    public AggregatedStatistics.AggregatedStatistic getItem(int position) {
        return aggregatedStatistics.getItem(position);
    }

    @Override
    public long getItemId(int position) {
        // Not important because data are not database register but cooked data.
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        AggregatedStatistics.AggregatedStatistic aggregatedStatistic = getItem(position);

        ViewHolder viewHolder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.aggregated_stats_list_item, parent, false);
            viewHolder = new ViewHolder(convertView);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        if (TrackIconUtils.isSpeedIcon(context.getResources(), aggregatedStatistic.getCategory())) {
            viewHolder.setSpeed(aggregatedStatistic);
        } else {
            viewHolder.setPace(aggregatedStatistic);
        }

        return convertView;
    }

    public AggregatedStatistics swapData(AggregatedStatistics aggregatedStatistics) {
        this.aggregatedStatistics = aggregatedStatistics;
        this.notifyDataSetChanged();
        return aggregatedStatistics;
    }

    public List<String> getCategories() {
        List<String> categories = new ArrayList<>();
        for (int i = 0; i < aggregatedStatistics.getCount(); i++) {
            categories.add(aggregatedStatistics.getItem(i).getCategory());
        }
        return categories;
    }

    private class ViewHolder {
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
            SpeedFormatter formatter = SpeedFormatter.Builder().setUnit(unitSystem).setReportSpeedOrPace(reportSpeed).build(context);

            setCommonValues(aggregatedStatistic);
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
            String category = aggregatedStatistic.getCategory();

            reportSpeed = PreferencesUtils.isReportSpeed(category);
            unitSystem = PreferencesUtils.getUnitSystem();

            sportIcon.setImageResource(getIcon(aggregatedStatistic));
            typeLabel.setText(category);
            numTracks.setText(StringUtils.valueInParentheses(String.valueOf(aggregatedStatistic.getCountTracks())));

            Pair<String, String> parts = DistanceFormatter.Builder()
                    .setUnit(unitSystem)
                    .build(context).getDistanceParts(aggregatedStatistic.getTrackStatistics().getTotalDistance());
            distance.setText(parts.first);
            distanceUnit.setText(parts.second);

            time.setText(StringUtils.formatElapsedTime(aggregatedStatistic.getTrackStatistics().getMovingTime()));
        }

        private int getIcon(AggregatedStatistics.AggregatedStatistic aggregatedStatistic) {
            String iconValue = TrackIconUtils.getIconValue(context, aggregatedStatistic.getCategory());
            return TrackIconUtils.getIconDrawable(iconValue);
        }
    }
}
