package de.dennisguse.opentracks.viewmodels;

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
import de.dennisguse.opentracks.content.data.Distance;
import de.dennisguse.opentracks.content.data.Layout;
import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.services.TrackRecordingService;
import de.dennisguse.opentracks.stats.SensorStatistics;
import de.dennisguse.opentracks.stats.TrackStatistics;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class StatsDataBuilderTest extends TestCase {

    private final Context context = ApplicationProvider.getApplicationContext();

    @Mock
    private Track trackMock;

    @Mock
    private TrackStatistics trackStatisticsMock;

    @Mock
    private TrackRecordingService.RecordingData recordingDataMock;

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
        layout.addField(context.getString(R.string.stats_total_time), true, true);
        layout.addField(context.getString(R.string.stats_moving_time), true, true);
        layout.addField(context.getString(R.string.stats_distance), true, true);
        layout.addField(context.getString(R.string.stats_speed), false, true);
        layout.addField(context.getString(R.string.stats_max_speed), false, true);

        // when
        List<StatsData> statsDataList = StatsDataBuilder.fromRecordingData(context, recordingDataMock, layout, true);

        // then
        assertEquals(statsDataList.size(), 3);
        assertTrue(statsDataList.stream().anyMatch(i -> i.getDescMain().equals(context.getString(R.string.stats_total_time))));
        assertTrue(statsDataList.stream().anyMatch(i -> i.getDescMain().equals(context.getString(R.string.stats_moving_time))));
        assertTrue(statsDataList.stream().anyMatch(i -> i.getDescMain().equals(context.getString(R.string.stats_distance))));
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
        layout.addField(context.getString(R.string.stats_total_time), true, true);
        layout.addField(context.getString(R.string.stats_moving_time), true, true);
        layout.addField(context.getString(R.string.stats_distance), true, true);
        layout.addField(context.getString(R.string.stats_speed), true, true);
        layout.addField(context.getString(R.string.stats_average_moving_speed), true, true);
        layout.addField(context.getString(R.string.stats_average_speed), true, true);
        layout.addField(context.getString(R.string.stats_max_speed), true, true);
        layout.addField(context.getString(R.string.stats_pace), true, true);
        layout.addField(context.getString(R.string.stats_average_moving_pace), true, true);
        layout.addField(context.getString(R.string.stats_average_pace), true, true);
        layout.addField(context.getString(R.string.stats_fastest_pace), true, true);
        layout.addField(context.getString(R.string.stats_altitude), true, true);
        layout.addField(context.getString(R.string.stats_gain), true, true);
        layout.addField(context.getString(R.string.stats_loss), true, true);
        layout.addField(context.getString(R.string.stats_latitude), true, true);
        layout.addField(context.getString(R.string.stats_longitude), true, true);
        layout.addField(context.getString(R.string.stats_sensors_heart_rate), true, true);
        layout.addField(context.getString(R.string.stats_sensors_cadence), true, true);
        layout.addField(context.getString(R.string.stats_sensors_power), true, true);

        // when
        List<StatsData> statsDataList = StatsDataBuilder.fromRecordingData(context, recordingDataMock, layout, true);

        // then
        assertEquals(statsDataList.size(), 19);
        assertTrue(statsDataList.stream().anyMatch(i -> i.getDescMain().equals(context.getString(R.string.stats_total_time))));
        assertTrue(statsDataList.stream().anyMatch(i -> i.getDescMain().equals(context.getString(R.string.stats_moving_time))));
        assertTrue(statsDataList.stream().anyMatch(i -> i.getDescMain().equals(context.getString(R.string.stats_distance))));
        assertTrue(statsDataList.stream().anyMatch(i -> i.getDescMain().equals(context.getString(R.string.stats_speed))));
        assertTrue(statsDataList.stream().anyMatch(i -> i.getDescMain().equals(context.getString(R.string.stats_average_moving_speed))));
        assertTrue(statsDataList.stream().anyMatch(i -> i.getDescMain().equals(context.getString(R.string.stats_average_speed))));
        assertTrue(statsDataList.stream().anyMatch(i -> i.getDescMain().equals(context.getString(R.string.stats_max_speed))));
        assertTrue(statsDataList.stream().anyMatch(i -> i.getDescMain().equals(context.getString(R.string.stats_pace))));
        assertTrue(statsDataList.stream().anyMatch(i -> i.getDescMain().equals(context.getString(R.string.stats_average_moving_pace))));
        assertTrue(statsDataList.stream().anyMatch(i -> i.getDescMain().equals(context.getString(R.string.stats_average_pace))));
        assertTrue(statsDataList.stream().anyMatch(i -> i.getDescMain().equals(context.getString(R.string.stats_fastest_pace))));
        assertTrue(statsDataList.stream().anyMatch(i -> i.getDescMain().equals(context.getString(R.string.stats_altitude))));
        assertTrue(statsDataList.stream().anyMatch(i -> i.getDescMain().equals(context.getString(R.string.stats_gain))));
        assertTrue(statsDataList.stream().anyMatch(i -> i.getDescMain().equals(context.getString(R.string.stats_loss))));
        assertTrue(statsDataList.stream().anyMatch(i -> i.getDescMain().equals(context.getString(R.string.stats_latitude))));
        assertTrue(statsDataList.stream().anyMatch(i -> i.getDescMain().equals(context.getString(R.string.stats_longitude))));
        assertTrue(statsDataList.stream().anyMatch(i -> i.getDescMain().equals(context.getString(R.string.stats_sensors_heart_rate))));
        assertTrue(statsDataList.stream().anyMatch(i -> i.getDescMain().equals(context.getString(R.string.stats_sensors_cadence))));
        assertTrue(statsDataList.stream().anyMatch(i -> i.getDescMain().equals(context.getString(R.string.stats_sensors_power))));
    }

    @Test
    public void testFromSensorStatistics_onlyHeartRate() {
        when(sensorStatisticsMock.hasHeartRate()).thenReturn(true);
        when(sensorStatisticsMock.getMaxHeartRate()).thenReturn(200f);
        when(sensorStatisticsMock.getAvgHeartRate()).thenReturn(150f);
        when(sensorStatisticsMock.hasCadence()).thenReturn(false);
        when(sensorStatisticsMock.hasPower()).thenReturn(false);

        // when
        List<StatsData> statsDataList = StatsDataBuilder.fromSensorStatistics(context, sensorStatisticsMock);

        // then
        assertEquals(statsDataList.size(), 2);
        assertTrue(statsDataList.stream().anyMatch(i -> i.getDescMain().equals(context.getString(R.string.sensor_state_heart_rate_max))));
        assertTrue(statsDataList.stream().anyMatch(i -> i.getDescMain().equals(context.getString(R.string.sensor_state_heart_rate_avg))));
    }

    @Test
    public void testFromSensorStatistics_onlyCadence() {
        when(sensorStatisticsMock.hasHeartRate()).thenReturn(false);
        when(sensorStatisticsMock.hasCadence()).thenReturn(true);
        when(sensorStatisticsMock.getAvgCadence()).thenReturn(90f);
        when(sensorStatisticsMock.getMaxCadence()).thenReturn(110f);
        when(sensorStatisticsMock.hasPower()).thenReturn(false);

        // when
        List<StatsData> statsDataList = StatsDataBuilder.fromSensorStatistics(context, sensorStatisticsMock);

        // then
        assertEquals(statsDataList.size(), 2);
        assertTrue(statsDataList.stream().anyMatch(i -> i.getDescMain().equals(context.getString(R.string.sensor_state_cadence_max))));
        assertTrue(statsDataList.stream().anyMatch(i -> i.getDescMain().equals(context.getString(R.string.sensor_state_cadence_avg))));
    }

    @Test
    public void testFromSensorStatistics_onlyPower() {
        when(sensorStatisticsMock.hasHeartRate()).thenReturn(false);
        when(sensorStatisticsMock.hasCadence()).thenReturn(false);
        when(sensorStatisticsMock.hasPower()).thenReturn(true);
        when(sensorStatisticsMock.getAvgPower()).thenReturn(300f);

        // when
        List<StatsData> statsDataList = StatsDataBuilder.fromSensorStatistics(context, sensorStatisticsMock);
        assertTrue(statsDataList.stream().anyMatch(i -> i.getDescMain().equals(context.getString(R.string.sensor_state_power_avg))));

        // then
        assertEquals(statsDataList.size(), 1);
    }

    @Test
    public void testFromSensorStatistics() {
        when(sensorStatisticsMock.hasHeartRate()).thenReturn(true);
        when(sensorStatisticsMock.getMaxHeartRate()).thenReturn(200f);
        when(sensorStatisticsMock.getAvgHeartRate()).thenReturn(150f);
        when(sensorStatisticsMock.hasCadence()).thenReturn(true);
        when(sensorStatisticsMock.getAvgCadence()).thenReturn(90f);
        when(sensorStatisticsMock.getMaxCadence()).thenReturn(110f);
        when(sensorStatisticsMock.hasPower()).thenReturn(true);
        when(sensorStatisticsMock.getAvgPower()).thenReturn(300f);

        // when
        List<StatsData> statsDataList = StatsDataBuilder.fromSensorStatistics(context, sensorStatisticsMock);

        // then
        assertEquals(statsDataList.size(), 5);
        assertTrue(statsDataList.stream().anyMatch(i -> i.getDescMain().equals(context.getString(R.string.sensor_state_heart_rate_max))));
        assertTrue(statsDataList.stream().anyMatch(i -> i.getDescMain().equals(context.getString(R.string.sensor_state_heart_rate_avg))));
        assertTrue(statsDataList.stream().anyMatch(i -> i.getDescMain().equals(context.getString(R.string.sensor_state_cadence_max))));
        assertTrue(statsDataList.stream().anyMatch(i -> i.getDescMain().equals(context.getString(R.string.sensor_state_cadence_avg))));
        assertTrue(statsDataList.stream().anyMatch(i -> i.getDescMain().equals(context.getString(R.string.sensor_state_power_avg))));
    }
}
