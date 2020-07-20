package de.dennisguse.opentracks.adapters;

import android.content.Context;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.util.PreferencesUtils;
import de.dennisguse.opentracks.util.StringUtils;
import de.dennisguse.opentracks.util.TrackIconUtils;
import de.dennisguse.opentracks.viewmodels.AggregatedStatistics;

public class AggregatedStatisticsAdapter extends BaseAdapter {

    private final AggregatedStatistics aggregatedStatistics;
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
        AggregatedStatistics.AggregatedStatistic aggregatedStats = getItem(position);
        ViewHolder viewHolder;

        if (isSpeedSport(position)) {
            viewHolder = new ViewSpeedHolder();
        } else {
            viewHolder = new ViewPaceHolder();
        }

        convertView = LayoutInflater.from(context).inflate(R.layout.aggregated_stats_list_item, parent, false);
        viewHolder.setValues(convertView, getIcon(position), getName(position), aggregatedStats);

        return convertView;
    }

    private boolean isSpeedSport(int position) {
        String category = aggregatedStatistics.getSportName(position);
        return TrackIconUtils.isSpeedIcon(context, category);
    }

    private int getIcon(int position) {
        String iconValue = TrackIconUtils.getIconValue(context, aggregatedStatistics.getSportName(position));
        return TrackIconUtils.getIconDrawable(iconValue);
    }

    private String getName(int position) {
        return aggregatedStatistics.getSportName(position);
    }

    private class ViewHolder {
        protected ImageView sportIcon;
        protected TextView typeLabel;
        protected TextView numTracks;
        protected TextView distance;
        protected TextView distanceUnit;
        protected TextView time;
        protected boolean metricsUnits;
        protected boolean reportSpeed;

        public void setValues(View view, int iconDrawable, String name, AggregatedStatistics.AggregatedStatistic aggregatedStats) {
            reportSpeed = PreferencesUtils.isReportSpeed(context, name);
            metricsUnits = PreferencesUtils.isMetricUnits(context);

            sportIcon = view.findViewById(R.id.aggregated_stats_sport_icon);
            typeLabel = view.findViewById(R.id.aggregated_stats_type_label);
            numTracks = view.findViewById(R.id.aggregated_stats_num_tracks);
            distance = view.findViewById(R.id.aggregated_stats_distance);
            distanceUnit = view.findViewById(R.id.aggregated_stats_distance_unit);
            time = view.findViewById(R.id.aggregated_stats_time);

            sportIcon.setImageResource(iconDrawable);
            typeLabel.setText(name);
            numTracks.setText(StringUtils.valueInParentheses(String.valueOf(aggregatedStats.getCountTracks())));

            Pair<String, String> parts = StringUtils.getDistanceParts(context, aggregatedStats.getTrackStatistics().getTotalDistance(), metricsUnits);
            distance.setText(parts.first);
            distanceUnit.setText(parts.second);

            time.setText(StringUtils.formatElapsedTime(aggregatedStats.getTrackStatistics().getMovingTime()));
        }
    }

    private class ViewSpeedHolder extends ViewHolder {
        private TextView avgSpeed;
        private TextView avgSpeedUnit;
        private TextView avgSpeedLabel;
        private TextView maxSpeed;
        private TextView maxSpeedUnit;
        private TextView maxSpeedLabel;

        @Override
        public void setValues(View view, int iconDrawable, String name, AggregatedStatistics.AggregatedStatistic aggregatedStats) {
            super.setValues(view, iconDrawable, name, aggregatedStats);

            avgSpeed = view.findViewById(R.id.aggregated_stats_avg_rate);
            avgSpeedUnit = view.findViewById(R.id.aggregated_stats_avg_rate_unit);
            avgSpeedLabel = view.findViewById(R.id.aggregated_stats_avg_rate_label);
            maxSpeed = view.findViewById(R.id.aggregated_stats_max_rate);
            maxSpeedUnit = view.findViewById(R.id.aggregated_stats_max_rate_unit);
            maxSpeedLabel = view.findViewById(R.id.aggregated_stats_max_rate_label);

            {
                Pair<String, String> parts = StringUtils.getSpeedParts(context, aggregatedStats.getTrackStatistics().getAverageMovingSpeed(), metricsUnits, reportSpeed);
                avgSpeed.setText(parts.first);
                avgSpeedUnit.setText(parts.second);
                avgSpeedLabel.setText(context.getString(R.string.stats_average_moving_speed));
            }

            {
                Pair<String, String> parts = StringUtils.getSpeedParts(context, aggregatedStats.getTrackStatistics().getMaxSpeed(), metricsUnits, reportSpeed);
                maxSpeed.setText(parts.first);
                maxSpeedUnit.setText(parts.second);
                maxSpeedLabel.setText(context.getString(R.string.stats_max_speed));
            }
        }
    }

    private class ViewPaceHolder extends ViewHolder {
        private TextView avgPace;
        private TextView avgPaceUnit;
        private TextView avgPaceLabel;
        private TextView maxPace;
        private TextView maxPaceUnit;
        private TextView maxPaceLabel;

        @Override
        public void setValues(View view, int iconDrawable, String name, AggregatedStatistics.AggregatedStatistic aggregatedStats) {
            super.setValues(view, iconDrawable, name, aggregatedStats);

            avgPace = view.findViewById(R.id.aggregated_stats_avg_rate);
            avgPaceUnit = view.findViewById(R.id.aggregated_stats_avg_rate_unit);
            avgPaceLabel = view.findViewById(R.id.aggregated_stats_avg_rate_label);

            maxPace = view.findViewById(R.id.aggregated_stats_max_rate);
            maxPaceUnit = view.findViewById(R.id.aggregated_stats_max_rate_unit);
            maxPaceLabel = view.findViewById(R.id.aggregated_stats_max_rate_label);

            {
                Pair<String, String> parts = StringUtils.getSpeedParts(context, aggregatedStats.getTrackStatistics().getAverageMovingSpeed(), metricsUnits, reportSpeed);
                avgPace.setText(parts.first);
                avgPaceUnit.setText(parts.second);
                avgPaceLabel.setText(R.string.stats_average_moving_pace);
            }

            {
                Pair<String, String> parts = StringUtils.getSpeedParts(context, aggregatedStats.getTrackStatistics().getMaxSpeed(), metricsUnits, reportSpeed);
                maxPace.setText(parts.first);
                maxPaceUnit.setText(parts.second);
                maxPaceLabel.setText(R.string.stats_fastest_pace);
            }
        }
    }
}
