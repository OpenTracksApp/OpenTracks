package de.dennisguse.opentracks.ui.customRecordingLayout;

import android.content.Context;
import android.content.res.Resources;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import junit.framework.TestCase;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.settings.PreferencesUtils;

@RunWith(AndroidJUnit4.class)
public class RecordingLayoutTest extends TestCase {

    private final Context context = ApplicationProvider.getApplicationContext();
    private final Resources resources = ApplicationProvider.getApplicationContext().getResources();

    @Test
    public void testFromCsv() {
        // given a csv line
        String csv = "running;2;" + context.getString(R.string.stats_custom_layout_speed_key) + ",1,1,0;" + context.getString(R.string.stats_custom_layout_distance_key) + ",1,0,0;" + context.getString(R.string.stats_custom_layout_altitude_key) + ",0,1,0;" + context.getString(R.string.stats_custom_layout_gain_key) + ",0,0,0;";

        // when create a layout from CSV line
        RecordingLayout recordingLayout = RecordingLayoutIO.fromCsv(csv, resources);
        List<DataField> dataFieldList = recordingLayout.getFields();

        // then layout and data fields are built correctly
        assertEquals(recordingLayout.getName(), "running");
        assertEquals(recordingLayout.getColumnsPerRow(), 2);
        assertEquals(dataFieldList.size(), 4);
        assertEquals(dataFieldList.get(0).getKey(), context.getString(R.string.stats_custom_layout_speed_key));
        assertEquals(dataFieldList.get(1).getKey(), context.getString(R.string.stats_custom_layout_distance_key));
        assertEquals(dataFieldList.get(2).getKey(), context.getString(R.string.stats_custom_layout_altitude_key));
        assertEquals(dataFieldList.get(3).getKey(), context.getString(R.string.stats_custom_layout_gain_key));
        assertTrue(dataFieldList.get(0).isVisible());
        assertTrue(dataFieldList.get(0).isPrimary());
        assertFalse(dataFieldList.get(0).isWide());
        assertTrue(dataFieldList.get(1).isVisible());
        assertFalse(dataFieldList.get(1).isPrimary());
        assertFalse(dataFieldList.get(1).isWide());
        assertFalse(dataFieldList.get(2).isVisible());
        assertTrue(dataFieldList.get(2).isPrimary());
        assertFalse(dataFieldList.get(2).isWide());
        assertFalse(dataFieldList.get(3).isVisible());
        assertFalse(dataFieldList.get(3).isPrimary());
        assertFalse(dataFieldList.get(3).isWide());

    }

    @Test
    public void testFromCsv_Wrong1() {
        // given a csv line without number of columns
        String csv1 = "Layout Name;speed,1,1;distance,0,0;";

        // when create a layout from CSV line
        RecordingLayout recordingLayout = RecordingLayoutIO.fromCsv(csv1, resources);
        List<DataField> dataFieldList = recordingLayout.getFields();

        // then layout and data fields are built correctly
        assertEquals(recordingLayout.getName(), PreferencesUtils.getDefaultLayoutName());
        assertEquals(recordingLayout.getColumnsPerRow(), PreferencesUtils.getLayoutColumnsByDefault());
        assertEquals(recordingLayout.getFields().size(), 0);
    }

    @Test
    public void testFromCsv_Wrong2() {
        // given a csv line without any field
        String csv1 = "Layout Name;2;";

        // when create a layout from CSV line
        RecordingLayout recordingLayout = RecordingLayoutIO.fromCsv(csv1, resources);
        List<DataField> dataFieldList = recordingLayout.getFields();

        // then layout and data fields are built correctly
        assertEquals(recordingLayout.getName(), PreferencesUtils.getDefaultLayoutName());
        assertEquals(recordingLayout.getColumnsPerRow(), PreferencesUtils.getLayoutColumnsByDefault());
        assertEquals(recordingLayout.getFields().size(), 0);
    }

    @Test
    public void testFromCsv_Wrong3() {
        // given a csv line with wrong fields description
        String csv1 = "Layout Name;2;speed,distance,total time;";

        // when create a layout from CSV line
        RecordingLayout recordingLayout = RecordingLayoutIO.fromCsv(csv1, resources);
        List<DataField> dataFieldList = recordingLayout.getFields();

        // then layout and data fields are built correctly
        assertEquals(recordingLayout.getName(), "Layout Name");
        assertEquals(recordingLayout.getColumnsPerRow(), 2);
        assertEquals(recordingLayout.getFields().size(), 0);
    }

    @Test
    public void testFromCsv_Wrong4() {
        // given a csv line with the first field ok but not the others
        String csv1 = "Layout Name;2;speed,1,0;distance;";

        // when create a layout from CSV line
        RecordingLayout recordingLayout = RecordingLayoutIO.fromCsv(csv1, resources);
        List<DataField> dataFieldList = recordingLayout.getFields();

        // then layout and data fields are built correctly
        assertEquals(recordingLayout.getName(), "Layout Name");
        assertEquals(recordingLayout.getColumnsPerRow(), 2);
        assertEquals(recordingLayout.getFields().size(), 1);
    }

    @Test
    public void testToCsv() {
        // given a layout's object
        RecordingLayout recordingLayout = new RecordingLayout("Test Layout", 2);
        recordingLayout.addField(new DataField("key1", false, false, false));
        recordingLayout.addField(new DataField("key2", false, true, false));
        recordingLayout.addField(new DataField("key3", true, false, false));
        recordingLayout.addField(new DataField("key4", true, true, false));
        recordingLayout.addField(new DataField("key5", true, true, true));

        // when converts it to CSV
        String csv = recordingLayout.toCsv();

        // then csv is well built
        assertEquals(csv, "Test Layout;2;key1,0,0,0;key2,0,1,0;key3,1,0,0;key4,1,1,0;key5,1,1,1;");
    }

    @Test
    public void testToCsv_columnsByDefault() {
        // given a layout's object
        RecordingLayout recordingLayout = new RecordingLayout("Test Layout");
        recordingLayout.addField(new DataField("key1", false, false, false));
        recordingLayout.addField(new DataField("key2", false, true, false));
        recordingLayout.addField(new DataField("key3", true, false, false));
        recordingLayout.addField(new DataField("key4", true, true, false));
        recordingLayout.addField(new DataField("key5", true, true, true));

        // when converts it to CSV
        String csv = recordingLayout.toCsv();

        // then csv is well built
        assertEquals(csv, "Test Layout;" + PreferencesUtils.getLayoutColumnsByDefault() + ";key1,0,0,0;key2,0,1,0;key3,1,0,0;key4,1,1,0;key5,1,1,1;");
    }
}