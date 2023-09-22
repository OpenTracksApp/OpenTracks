package de.dennisguse.opentracks.util;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import junit.framework.TestCase;

import org.junit.Test;
import org.junit.runner.RunWith;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.ui.customRecordingLayout.DataField;
import de.dennisguse.opentracks.ui.customRecordingLayout.RecordingLayout;

@RunWith(AndroidJUnit4.class)
public class StatisticsUtilsTest extends TestCase {

    private final Context context = ApplicationProvider.getApplicationContext();

    @Test
    public void testEmptyValue() {
        assertEquals(context.getString(R.string.stats_empty_value_float), StatisticsUtils.emptyValue(context, context.getString(R.string.stats_custom_layout_distance_key)));
        assertEquals(context.getString(R.string.stats_empty_value_time), StatisticsUtils.emptyValue(context, context.getString(R.string.stats_custom_layout_total_time_key)));
        assertEquals(context.getString(R.string.stats_empty_value_float), StatisticsUtils.emptyValue(context, context.getString(R.string.stats_custom_layout_speed_key)));
        assertEquals(context.getString(R.string.stats_empty_value_time), StatisticsUtils.emptyValue(context, context.getString(R.string.stats_custom_layout_moving_time_key)));
        assertEquals(context.getString(R.string.stats_empty_value_float), StatisticsUtils.emptyValue(context, context.getString(R.string.stats_custom_layout_average_speed_key)));
        assertEquals(context.getString(R.string.stats_empty_value_float), StatisticsUtils.emptyValue(context, context.getString(R.string.stats_custom_layout_max_speed_key)));
        assertEquals(context.getString(R.string.stats_empty_value_float), StatisticsUtils.emptyValue(context, context.getString(R.string.stats_custom_layout_average_moving_speed_key)));
        assertEquals(context.getString(R.string.stats_empty_value_integer), StatisticsUtils.emptyValue(context, context.getString(R.string.stats_custom_layout_altitude_key)));
        assertEquals(context.getString(R.string.stats_empty_value_integer), StatisticsUtils.emptyValue(context, context.getString(R.string.stats_custom_layout_gain_key)));
        assertEquals(context.getString(R.string.stats_empty_value_integer), StatisticsUtils.emptyValue(context, context.getString(R.string.stats_custom_layout_loss_key)));
        assertEquals(context.getString(R.string.stats_empty_value_time), StatisticsUtils.emptyValue(context, context.getString(R.string.stats_custom_layout_pace_key)));
        assertEquals(context.getString(R.string.stats_empty_value_time), StatisticsUtils.emptyValue(context, context.getString(R.string.stats_custom_layout_average_moving_pace_key)));
        assertEquals(context.getString(R.string.stats_empty_value_time), StatisticsUtils.emptyValue(context, context.getString(R.string.stats_custom_layout_average_pace_key)));
        assertEquals(context.getString(R.string.stats_empty_value_time), StatisticsUtils.emptyValue(context, context.getString(R.string.stats_custom_layout_fastest_pace_key)));
        assertEquals(context.getString(R.string.stats_empty_value_coordinates), StatisticsUtils.emptyValue(context, context.getString(R.string.stats_custom_layout_coordinates_key)));
        assertEquals(context.getString(R.string.stats_empty_value_integer), StatisticsUtils.emptyValue(context, context.getString(R.string.stats_custom_layout_heart_rate_key)));
        assertEquals(context.getString(R.string.stats_empty_value_integer), StatisticsUtils.emptyValue(context, context.getString(R.string.stats_custom_layout_cadence_key)));
        assertEquals(context.getString(R.string.stats_empty_value_integer), StatisticsUtils.emptyValue(context, context.getString(R.string.stats_custom_layout_power_key)));
    }

    @Test
    public void testFilterVisible() {
        // given
        RecordingLayout recordingLayout = new RecordingLayout("profile");
        recordingLayout.addField(new DataField("key1", true, true, false));
        recordingLayout.addField(new DataField("key2", false, true, false));
        recordingLayout.addField(new DataField("key3", true, true, false));
        recordingLayout.addField(new DataField("key4", false, true, false));
        recordingLayout.addField(new DataField("key5", true, true, false));

        // when
        RecordingLayout resultTrue = recordingLayout.toRecordingLayout(true);
        RecordingLayout resultFalse = recordingLayout.toRecordingLayout(false);

        // then
        assertEquals(resultTrue.getFields().size(), 3);
        assertTrue(resultTrue.getFields().stream().anyMatch(f -> "key1".equals(f.getKey())));
        assertTrue(resultTrue.getFields().stream().anyMatch(f -> "key3".equals(f.getKey())));
        assertTrue(resultTrue.getFields().stream().anyMatch(f -> "key5".equals(f.getKey())));

        assertEquals(resultFalse.getFields().size(), 2);
        assertTrue(resultFalse.getFields().stream().anyMatch(f -> "key2".equals(f.getKey())));
        assertTrue(resultFalse.getFields().stream().anyMatch(f -> "key4".equals(f.getKey())));
    }
}
