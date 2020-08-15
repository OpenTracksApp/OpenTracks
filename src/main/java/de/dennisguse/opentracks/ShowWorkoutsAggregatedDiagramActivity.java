package de.dennisguse.opentracks;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.github.mikephil.charting.charts.CombinedChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.CombinedData;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.content.provider.ContentProviderUtils;
import de.dennisguse.opentracks.stats.dto.AggregatedWorkoutValues;
import de.dennisguse.opentracks.stats.dto.DataPointAverageSpeed;
import de.dennisguse.opentracks.stats.dto.DataPointDistance;
import de.dennisguse.opentracks.util.PreferencesUtils;
import de.dennisguse.opentracks.util.StringUtils;
import de.dennisguse.opentracks.util.TrackIconUtils;

import static android.widget.AdapterView.*;
import static com.github.mikephil.charting.charts.CombinedChart.*;

public class ShowWorkoutsAggregatedDiagramActivity extends AbstractActivity {

    CombinedChart chart;
    String selectedWorkoutType;
    private ContentProviderUtils contentProviderUtils;
    private boolean isMetricsUnits;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_workouts_aggregated);
        setTitle(getString(R.string.workout_statistics));
        isMetricsUnits = PreferencesUtils.isMetricUnits(getApplicationContext());
        selectedWorkoutType = PreferencesUtils.getDefaultActivity(getApplicationContext());
        contentProviderUtils = new ContentProviderUtils(getApplicationContext());
        addWorkoutTypeSpinner();
        chart = createChart();
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.activity_show_workouts_aggregated;
    }

    private LineData createAverageSpeedLineData(ArrayList<DataPointAverageSpeed> averageSpeedValues) {
        final ArrayList<Entry> averageSpeedEntries = new ArrayList<>();
        for (DataPointAverageSpeed averageSpeedDataPoint: averageSpeedValues ) {
            Pair<String, String> parts = StringUtils.getSpeedParts(getApplicationContext(), averageSpeedDataPoint.getAverageSpeed(), isMetricsUnits, true);
            float averageSpeed = Float.valueOf(parts.first);
            averageSpeedEntries.add(new Entry( averageSpeedDataPoint.getTime(), averageSpeed));
        }

        LineDataSet lineDataSetAverageSpeed;
        lineDataSetAverageSpeed = new LineDataSet(averageSpeedEntries, getString(R.string.average_speed));
        lineDataSetAverageSpeed.setColor(Color.RED);
        lineDataSetAverageSpeed.setValueTextColor(Color.RED);
        lineDataSetAverageSpeed.setValueTextSize(10f);
        lineDataSetAverageSpeed.setDrawCircles(true);
        lineDataSetAverageSpeed.setLineWidth(4);
        lineDataSetAverageSpeed.setMode(LineDataSet.Mode.HORIZONTAL_BEZIER);

        ArrayList<ILineDataSet> dataSets = new ArrayList<>();
        dataSets.add(lineDataSetAverageSpeed);

        return new LineData(dataSets);
    }

    private BarData createDistanceBarData(ArrayList<DataPointDistance> distanceValues) {
        ArrayList<BarEntry> distanceEntries = new ArrayList<>();
        for(DataPointDistance dataPoint: distanceValues) {
            float distanceInKm = (float) dataPoint.getDistance() / 1000;
            distanceEntries.add(new BarEntry(dataPoint.getTime(), distanceInKm));
        }

        BarDataSet set1 = new BarDataSet(distanceEntries, "Distance");
        set1.setColors(Color.CYAN);
        set1.setValueTextColor(Color.LTGRAY);
        set1.setValueTextSize(15f);
        set1.setAxisDependency(YAxis.AxisDependency.LEFT);

        BarData barData = new BarData(set1);
        barData.setBarWidth(16f);
        return barData;

    }

    private void setMaxValues(AggregatedWorkoutValues aggregatedWorkoutValues) {
        TextView fastestAverageTextView = findViewById(R.id.fastestAverage);
        Pair<String, String> parts = StringUtils.getSpeedParts(getApplicationContext(), aggregatedWorkoutValues.getFastestAverage(), isMetricsUnits, true);
        String fastestAverageString = parts.first + ' ' + parts.second;
        fastestAverageTextView.setText(fastestAverageString);
        TextView fastestAverageDateTextView = findViewById(R.id.fastestAverageDate);
        fastestAverageDateTextView.setText(StringUtils.formatDateTime(getApplicationContext(),aggregatedWorkoutValues.getFastestAverageDate()));
        TextView greatestDistanceTextView = findViewById(R.id.greatestDistance);
        String formatedDistance = StringUtils.formatDistance(getApplicationContext(), aggregatedWorkoutValues.getGreatestDistance(), isMetricsUnits);
        greatestDistanceTextView.setText(formatedDistance);
        TextView greatestDistanceDateTextView = findViewById(R.id.greatestDistanceDate);
        greatestDistanceDateTextView.setText(StringUtils.formatDateTime(getApplicationContext(), aggregatedWorkoutValues.getGreatestDistanceDate()));
    }



    private void addWorkoutTypeSpinner() {
        Spinner spinner = findViewById(R.id.spinner);
        ArrayList<String> workoutTypes = createChoicesList();

        ArrayAdapter<String> SpinnerAdapter = new ArrayAdapter<String>
                (this, android.R.layout.simple_spinner_item, workoutTypes) {
            public View getView(int position, View convertView,
                                ViewGroup parent) {
                View v = super.getView(position, convertView, parent);
                ((TextView) v).setTextColor(Color.LTGRAY);
                return v;
            }

            public View getDropDownView(int position, View convertView,
                                        ViewGroup parent) {
                View v = super.getDropDownView(position, convertView,
                        parent);
                v.setBackgroundColor(Color.DKGRAY);
                ((TextView) v).setTextColor(Color.parseColor("#ffffff"));
                return v;
            }
        };
        spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                selectedWorkoutType = workoutTypes.get(position);
                List<Track> tracks = contentProviderUtils.getAllTracks();

                AggregatedWorkoutValues aggregatedWorkoutValues = new AggregatedWorkoutValues(tracks);
                setMaxValues(aggregatedWorkoutValues);
                LineData averageSpeedData = createAverageSpeedLineData(aggregatedWorkoutValues.getAverageSpeedData());
                BarData distanceData = createDistanceBarData(aggregatedWorkoutValues.getDistanceData());
                chart.clear();
                if (tracks.size() == 0) {
                    return;
                }

                CombinedData combinedData = new CombinedData();
                combinedData.setData(distanceData);
                combinedData.setData(averageSpeedData);
                chart.setData(combinedData);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }


        });
        SpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(SpinnerAdapter);
    }

    private ArrayList<String> createChoicesList() {
        ArrayList<String> workoutTypes = new ArrayList<>();
        for (String iconValue : TrackIconUtils.getAllIconValues()) {
            workoutTypes.add(iconValue);
        }

        return workoutTypes;
    }

    private CombinedChart createChart() {
        CombinedChart chart = findViewById(R.id.combinedChartAggregatedWorkouts);

        AxisBase xAxis = chart.getXAxis();
        xAxis.setValueFormatter(new ValueFormatter() {

            private final SimpleDateFormat mFormat = new SimpleDateFormat("dd MMM", Locale.ENGLISH);

            @Override
            public String getFormattedValue(float value) {

                long millis = TimeUnit.HOURS.toMillis((long) value);
                return mFormat.format(new Date(millis));
            }
        });

        chart.getAxisLeft().setTextColor(Color.DKGRAY);
        chart.getAxisLeft().setTextSize(15f);
        chart.getAxisRight().setTextColor(Color.DKGRAY);
        chart.getAxisRight().setTextSize(15f);

        chart.getXAxis().setTextColor(Color.DKGRAY);
        chart.getXAxis().setTextSize(15f);

        chart.setNoDataText(getString(R.string.no_workouts_recorded_for_this_activity));
        chart.setNoDataTextColor(Color.DKGRAY);

        Description description = new Description();
        description.setText(getString(R.string.maximum_average_speed));
        description.setTextColor(Color.DKGRAY);
        chart.setDescription(description);

        chart.setDrawOrder(new DrawOrder[]{
                DrawOrder.BAR, DrawOrder.BUBBLE, DrawOrder.CANDLE, DrawOrder.LINE, DrawOrder.SCATTER
        });

        return chart;
    }

}