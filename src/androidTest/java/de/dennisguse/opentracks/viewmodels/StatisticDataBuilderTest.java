package de.dennisguse.opentracks.viewmodels;

import static org.mockito.Mockito.when;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import junit.framework.TestCase;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.Duration;
import java.util.List;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.data.models.Cadence;
import de.dennisguse.opentracks.data.models.Distance;
import de.dennisguse.opentracks.data.models.HeartRate;
import de.dennisguse.opentracks.data.models.Power;
import de.dennisguse.opentracks.data.models.Track;
import de.dennisguse.opentracks.services.RecordingData;
import de.dennisguse.opentracks.stats.SensorStatistics;
import de.dennisguse.opentracks.stats.TrackStatistics;
import de.dennisguse.opentracks.ui.customRecordingLayout.Layout;

@RunWith(MockitoJUnitRunner.class)
public class StatisticDataBuilderTest extends TestCase {

    private final Context context = ApplicationProvider.getApplicationContext();

    @Mock
    private Track trackMock;

    @Mock
    private TrackStatistics trackStatisticsMock;

    @Mock
    private RecordingData recordingDataMock;

    @Mock
    private SensorStatistics sensorStatisticsMock;

    @Test
    public void testFromRecordingData() {
        when(trackMock.getTrackStatistics()).thenReturn(trackStatisticsMock);
        when(trackStatisticsMock.getTotalTime()).thenReturn(Duration.ofMillis(0));
        when(trackStatisticsMock.getMovingTime()).thenReturn(Duration.ofMillis(0));
        when(trackStatisticsMock.getTotalDistance()).thenReturn(Distance.of(0));
        when(recordingDataMock.getTrackStatistics()).thenReturn(trackStatisticsMock);

        // given
        Layout layout = new Layout(context.getString(R.string.default_activity_default));
        layout.addField(context.getString(R.string.stats_custom_layout_total_time_key), context.getString(R.string.stats_total_time), true, true, false);
        layout.addField(context.getString(R.string.stats_custom_layout_moving_time_key), context.getString(R.string.stats_moving_time), true, true, false);
        layout.addField(context.getString(R.string.stats_custom_layout_distance_key), context.getString(R.string.stats_distance), true, true, false);
        layout.addField(context.getString(R.string.stats_custom_layout_speed_key), context.getString(R.string.stats_speed), false, true, false);
        layout.addField(context.getString(R.string.stats_custom_layout_max_speed_key), context.getString(R.string.stats_max_speed), false, true, false);

        // when
        List<StatisticData> statisticDataList = StatisticDataBuilder.fromRecordingData(context, recordingDataMock, layout, true);

        // then
        assertEquals(statisticDataList.size(), 3);
        assertTrue(statisticDataList.stream().anyMatch(i -> i.getField().getTitle().equals(context.getString(R.string.stats_total_time))));
        assertTrue(statisticDataList.stream().anyMatch(i -> i.getField().getTitle().equals(context.getString(R.string.stats_moving_time))));
        assertTrue(statisticDataList.stream().anyMatch(i -> i.getField().getTitle().equals(context.getString(R.string.stats_distance))));
    }

    @Test
    public void testFromRecordingData_allFields() {
        when(trackMock.getTrackStatistics()).thenReturn(trackStatisticsMock);
        when(trackStatisticsMock.getTotalTime()).thenReturn(Duration.ofMillis(0));
        when(trackStatisticsMock.getMovingTime()).thenReturn(Duration.ofMillis(0));
        when(trackStatisticsMock.getTotalDistance()).thenReturn(Distance.of(0));
        when(recordingDataMock.getTrackStatistics()).thenReturn(trackStatisticsMock);

        // given
        Layout layout = new Layout(context.getString(R.string.default_activity_default));
        layout.addField(context.getString(R.string.stats_custom_layout_total_time_key), context.getString(R.string.stats_total_time), true, true, false);
        layout.addField(context.getString(R.string.stats_custom_layout_moving_time_key), context.getString(R.string.stats_moving_time), true, true, false);
        layout.addField(context.getString(R.string.stats_custom_layout_distance_key), context.getString(R.string.stats_distance), true, true, false);
        layout.addField(context.getString(R.string.stats_custom_layout_speed_key), context.getString(R.string.stats_speed), true, true, false);
        layout.addField(context.getString(R.string.stats_custom_layout_average_moving_speed_key), context.getString(R.string.stats_average_moving_speed), true, true, false);
        layout.addField(context.getString(R.string.stats_custom_layout_average_speed_key), context.getString(R.string.stats_average_speed), true, true, false);
        layout.addField(context.getString(R.string.stats_custom_layout_max_speed_key), context.getString(R.string.stats_max_speed), true, true, false);
        layout.addField(context.getString(R.string.stats_custom_layout_pace_key), context.getString(R.string.stats_pace), true, true, false);
        layout.addField(context.getString(R.string.stats_custom_layout_average_moving_pace_key), context.getString(R.string.stats_average_moving_pace), true, true, false);
        layout.addField(context.getString(R.string.stats_custom_layout_average_pace_key), context.getString(R.string.stats_average_pace), true, true, false);
        layout.addField(context.getString(R.string.stats_custom_layout_fastest_pace_key), context.getString(R.string.stats_fastest_pace), true, true, false);
        layout.addField(context.getString(R.string.stats_custom_layout_altitude_key), context.getString(R.string.stats_altitude), true, true, false);
        layout.addField(context.getString(R.string.stats_custom_layout_gain_key), context.getString(R.string.stats_gain), true, true, false);
        layout.addField(context.getString(R.string.stats_custom_layout_loss_key), context.getString(R.string.stats_loss), true, true, false);
        layout.addField(context.getString(R.string.stats_custom_layout_coordinates_key), context.getString(R.string.stats_coordinates), true, true, false);
        layout.addField(context.getString(R.string.stats_custom_layout_heart_rate_key), context.getString(R.string.stats_sensors_heart_rate), true, true, false);
        layout.addField(context.getString(R.string.stats_custom_layout_cadence_key), context.getString(R.string.stats_sensors_cadence), true, true, false);
        layout.addField(context.getString(R.string.stats_custom_layout_power_key), context.getString(R.string.stats_sensors_power), true, true, false);

        // when
        List<StatisticData> statisticDataList = StatisticDataBuilder.fromRecordingData(context, recordingDataMock, layout, true);

        // then
        assertEquals(statisticDataList.size(), 18);
        assertTrue(statisticDataList.stream().anyMatch(i -> i.getField().getTitle().equals(context.getString(R.string.stats_total_time))));
        assertTrue(statisticDataList.stream().anyMatch(i -> i.getField().getTitle().equals(context.getString(R.string.stats_moving_time))));
        assertTrue(statisticDataList.stream().anyMatch(i -> i.getField().getTitle().equals(context.getString(R.string.stats_distance))));
        assertTrue(statisticDataList.stream().anyMatch(i -> i.getField().getTitle().equals(context.getString(R.string.stats_speed))));
        assertTrue(statisticDataList.stream().anyMatch(i -> i.getField().getTitle().equals(context.getString(R.string.stats_average_moving_speed))));
        assertTrue(statisticDataList.stream().anyMatch(i -> i.getField().getTitle().equals(context.getString(R.string.stats_average_speed))));
        assertTrue(statisticDataList.stream().anyMatch(i -> i.getField().getTitle().equals(context.getString(R.string.stats_max_speed))));
        assertTrue(statisticDataList.stream().anyMatch(i -> i.getField().getTitle().equals(context.getString(R.string.stats_pace))));
        assertTrue(statisticDataList.stream().anyMatch(i -> i.getField().getTitle().equals(context.getString(R.string.stats_average_moving_pace))));
        assertTrue(statisticDataList.stream().anyMatch(i -> i.getField().getTitle().equals(context.getString(R.string.stats_average_pace))));
        assertTrue(statisticDataList.stream().anyMatch(i -> i.getField().getTitle().equals(context.getString(R.string.stats_fastest_pace))));
        assertTrue(statisticDataList.stream().anyMatch(i -> i.getField().getTitle().equals(context.getString(R.string.stats_altitude))));
        assertTrue(statisticDataList.stream().anyMatch(i -> i.getField().getTitle().equals(context.getString(R.string.stats_gain))));
        assertTrue(statisticDataList.stream().anyMatch(i -> i.getField().getTitle().equals(context.getString(R.string.stats_loss))));
        assertTrue(statisticDataList.stream().anyMatch(i -> i.getField().getTitle().equals(context.getString(R.string.stats_coordinates))));
        assertTrue(statisticDataList.stream().anyMatch(i -> i.getField().getTitle().equals(context.getString(R.string.stats_sensors_heart_rate))));
        assertTrue(statisticDataList.stream().anyMatch(i -> i.getField().getTitle().equals(context.getString(R.string.stats_sensors_cadence))));
        assertTrue(statisticDataList.stream().anyMatch(i -> i.getField().getTitle().equals(context.getString(R.string.stats_sensors_power))));
    }

    @Test
    public void testFromSensorStatistics_onlyHeartRate() {
        when(sensorStatisticsMock.hasHeartRate()).thenReturn(true);
        when(sensorStatisticsMock.getMaxHeartRate()).thenReturn(HeartRate.of(200f));
        when(sensorStatisticsMock.getAvgHeartRate()).thenReturn(HeartRate.of(150f));
        when(sensorStatisticsMock.hasCadence()).thenReturn(false);
        when(sensorStatisticsMock.hasPower()).thenReturn(false);

        // when
        List<StatisticData> statisticDataList = StatisticDataBuilder.fromSensorStatistics(context, sensorStatisticsMock);

        // then
        assertEquals(statisticDataList.size(), 2);
        assertTrue(statisticDataList.stream().anyMatch(i -> i.getField().getTitle().equals(context.getString(R.string.sensor_state_heart_rate_max))));
        assertTrue(statisticDataList.stream().anyMatch(i -> i.getField().getTitle().equals(context.getString(R.string.sensor_state_heart_rate_avg))));
    }

    @Test
    public void testFromSensorStatistics_onlyCadence() {
        when(sensorStatisticsMock.hasHeartRate()).thenReturn(false);
        when(sensorStatisticsMock.hasCadence()).thenReturn(true);
        when(sensorStatisticsMock.getAvgCadence()).thenReturn(Cadence.of(90f));
        when(sensorStatisticsMock.getMaxCadence()).thenReturn(Cadence.of(110f));
        when(sensorStatisticsMock.hasPower()).thenReturn(false);

        // when
        List<StatisticData> statisticDataList = StatisticDataBuilder.fromSensorStatistics(context, sensorStatisticsMock);

        // then
        assertEquals(statisticDataList.size(), 2);
        assertTrue(statisticDataList.stream().anyMatch(i -> i.getField().getTitle().equals(context.getString(R.string.sensor_state_cadence_max))));
        assertTrue(statisticDataList.stream().anyMatch(i -> i.getField().getTitle().equals(context.getString(R.string.sensor_state_cadence_avg))));
    }

    @Test
    public void testFromSensorStatistics_onlyPower() {
        when(sensorStatisticsMock.hasHeartRate()).thenReturn(false);
        when(sensorStatisticsMock.hasCadence()).thenReturn(false);
        when(sensorStatisticsMock.hasPower()).thenReturn(true);
        when(sensorStatisticsMock.getAvgPower()).thenReturn(Power.of(300f));

        // when
        List<StatisticData> statisticDataList = StatisticDataBuilder.fromSensorStatistics(context, sensorStatisticsMock);
        assertTrue(statisticDataList.stream().anyMatch(i -> i.getField().getTitle().equals(context.getString(R.string.sensor_state_power_avg))));

        // then
        assertEquals(statisticDataList.size(), 1);
    }

    @Test
    public void testFromSensorStatistics() {
        when(sensorStatisticsMock.hasHeartRate()).thenReturn(true);
        when(sensorStatisticsMock.getMaxHeartRate()).thenReturn(HeartRate.of(200f));
        when(sensorStatisticsMock.getAvgHeartRate()).thenReturn(HeartRate.of(150f));
        when(sensorStatisticsMock.hasCadence()).thenReturn(true);
        when(sensorStatisticsMock.getAvgCadence()).thenReturn(Cadence.of(90f));
        when(sensorStatisticsMock.getMaxCadence()).thenReturn(Cadence.of(110f));
        when(sensorStatisticsMock.hasPower()).thenReturn(true);
        when(sensorStatisticsMock.getAvgPower()).thenReturn(Power.of(300f));

        // when
        List<StatisticData> statisticDataList = StatisticDataBuilder.fromSensorStatistics(context, sensorStatisticsMock);

        // then
        assertEquals(statisticDataList.size(), 5);
        assertTrue(statisticDataList.stream().anyMatch(i -> i.getField().getTitle().equals(context.getString(R.string.sensor_state_heart_rate_max))));
        assertTrue(statisticDataList.stream().anyMatch(i -> i.getField().getTitle().equals(context.getString(R.string.sensor_state_heart_rate_avg))));
        assertTrue(statisticDataList.stream().anyMatch(i -> i.getField().getTitle().equals(context.getString(R.string.sensor_state_cadence_max))));
        assertTrue(statisticDataList.stream().anyMatch(i -> i.getField().getTitle().equals(context.getString(R.string.sensor_state_cadence_avg))));
        assertTrue(statisticDataList.stream().anyMatch(i -> i.getField().getTitle().equals(context.getString(R.string.sensor_state_power_avg))));
    }
}
