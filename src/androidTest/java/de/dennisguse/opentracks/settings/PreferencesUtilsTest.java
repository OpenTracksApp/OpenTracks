package de.dennisguse.opentracks.settings;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;

import androidx.preference.PreferenceManager;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.content.data.DataField;
import de.dennisguse.opentracks.content.data.Layout;
import de.dennisguse.opentracks.io.file.TrackFileFormat;

@RunWith(AndroidJUnit4.class)
public class PreferencesUtilsTest {

    private final Context context = ApplicationProvider.getApplicationContext();
    private final Resources resources = ApplicationProvider.getApplicationContext().getResources();

    @Test
    public void ExportTrackFileFormat_ok() {
        // given
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putString(context.getString(R.string.export_trackfileformat_key), TrackFileFormat.KML_WITH_TRACKDETAIL_AND_SENSORDATA.name());
        editor.commit();

        // when
        TrackFileFormat trackFileFormat = PreferencesUtils.getExportTrackFileFormat();

        // then
        assertEquals(TrackFileFormat.KML_WITH_TRACKDETAIL_AND_SENSORDATA, trackFileFormat);
    }

    @Test
    public void ExportTrackFileFormat_invalid() {
        // given
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(context.getString(R.string.export_trackfileformat_key), "invalid");
        editor.commit();

        // when
        TrackFileFormat trackFileFormat = PreferencesUtils.getExportTrackFileFormat();

        // then
        assertEquals(TrackFileFormat.KMZ_WITH_TRACKDETAIL_AND_SENSORDATA, trackFileFormat);
    }

    @Test
    public void ExportTrackFileFormat_noValue() {
        // given
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.commit();

        // when
        TrackFileFormat trackFileFormat = PreferencesUtils.getExportTrackFileFormat();

        // then
        assertEquals(TrackFileFormat.KMZ_WITH_TRACKDETAIL_AND_SENSORDATA, trackFileFormat);
    }

    @Test
    public void testGetAllCustomLayouts_default() {
        // given
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.commit();

        // when
        List<Layout> layouts = PreferencesUtils.getAllCustomLayouts();

        // then
        assertEquals(layouts.size(), 1);
        assertTrue(layouts.get(0).getFields().size() > 0);
        assertEquals(layouts.get(0).getName(), context.getString(R.string.stats_custom_layout_default_layout));
        assertTrue(layouts.get(0).getFields().stream().anyMatch(DataField::isVisible));
    }

    @Test
    public void testGetCustomLayout_default() {
        // given
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.commit();

        // when
        Layout layout = PreferencesUtils.getCustomLayout();

        // then
        assertTrue(layout.getFields().size() > 0);
        assertEquals(layout.getName(), context.getString(R.string.stats_custom_layout_default_layout));
        assertTrue(layout.getFields().stream().anyMatch(DataField::isVisible));
    }

    @Test
    public void testGetCustomLayout_1() {
        // given
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(
                context.getString(R.string.stats_custom_layouts_key),
                "run;2;"
                        + context.getString(R.string.stats_custom_layout_moving_time_key) + ",1,1;"
                        + context.getString(R.string.stats_custom_layout_distance_key) + ",1,0;"
                        + context.getString(R.string.stats_custom_layout_average_moving_speed_key) + ",0,1;"
                        + context.getString(R.string.stats_custom_layout_speed_key) + ",0,0;");
        editor.apply();

        // when
        Layout layout = PreferencesUtils.getCustomLayout();

        // then
        assertEquals(layout.getFields().size(), 4);
        assertEquals(layout.getName(), "run");
        assertEquals(layout.getColumnsPerRow(), 2);

        assertEquals(layout.getFields().get(0).getKey(), context.getString(R.string.stats_custom_layout_moving_time_key));
        assertTrue(layout.getFields().get(0).isVisible());
        assertTrue(layout.getFields().get(0).isPrimary());

        assertEquals(layout.getFields().get(1).getKey(), context.getString(R.string.stats_custom_layout_distance_key));
        assertTrue(layout.getFields().get(1).isVisible());
        assertFalse(layout.getFields().get(1).isPrimary());

        assertEquals(layout.getFields().get(2).getKey(), context.getString(R.string.stats_custom_layout_average_moving_speed_key));
        assertFalse(layout.getFields().get(2).isVisible());
        assertTrue(layout.getFields().get(2).isPrimary());

        assertEquals(layout.getFields().get(3).getKey(), context.getString(R.string.stats_custom_layout_speed_key));
        assertFalse(layout.getFields().get(3).isVisible());
        assertFalse(layout.getFields().get(3).isPrimary());
    }

    @Test
    public void testGetCustomLayout_coordinatesIsWide() {
        // given
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(
                context.getString(R.string.stats_custom_layouts_key),
                "walking;2;"
                        + context.getString(R.string.stats_custom_layout_moving_time_key) + ",1,1;"
                        + context.getString(R.string.stats_custom_layout_distance_key) + ",1,0;"
                        + context.getString(R.string.stats_custom_layout_coordinates_key) + ",0,1;"
                        + context.getString(R.string.stats_custom_layout_speed_key) + ",0,0;");
        editor.apply();

        // when
        Layout layout = PreferencesUtils.getCustomLayout();

        // then
        assertEquals(layout.getFields().size(), 4);
        assertEquals(layout.getName(), "walking");
        assertEquals(layout.getColumnsPerRow(), 2);

        assertEquals(layout.getFields().get(0).getKey(), context.getString(R.string.stats_custom_layout_moving_time_key));
        assertTrue(layout.getFields().get(0).isVisible());
        assertTrue(layout.getFields().get(0).isPrimary());

        assertEquals(layout.getFields().get(1).getKey(), context.getString(R.string.stats_custom_layout_distance_key));
        assertTrue(layout.getFields().get(1).isVisible());
        assertFalse(layout.getFields().get(1).isPrimary());

        assertEquals(layout.getFields().get(2).getKey(), context.getString(R.string.stats_custom_layout_coordinates_key));
        assertFalse(layout.getFields().get(2).isVisible());
        assertTrue(layout.getFields().get(2).isPrimary());
        assertTrue(layout.getFields().get(2).isWide());

        assertEquals(layout.getFields().get(3).getKey(), context.getString(R.string.stats_custom_layout_speed_key));
        assertFalse(layout.getFields().get(3).isVisible());
        assertFalse(layout.getFields().get(3).isPrimary());
    }

    @Test
    public void testSetCustomLayout() {
        // given
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        Layout layoutSrc = new Layout("road cycling");
        layoutSrc.addField(new DataField(context.getString(R.string.stats_custom_layout_moving_time_key), context.getString(R.string.stats_moving_time), true, true, false));
        layoutSrc.addField(new DataField(context.getString(R.string.stats_custom_layout_distance_key), context.getString(R.string.stats_distance), true, false, false));
        layoutSrc.addField(new DataField(context.getString(R.string.stats_custom_layout_average_moving_speed_key), context.getString(R.string.stats_average_moving_speed), false, true, false));
        layoutSrc.addField(new DataField(context.getString(R.string.stats_custom_layout_speed_key), context.getString(R.string.stats_speed), false, false, false));

        // when
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(context.getString(R.string.stats_custom_layouts_key), layoutSrc.toCsv());
        editor.commit();

        // then
        String csv = sharedPreferences.getString(context.getString(R.string.stats_custom_layouts_key), null);
        assertNotNull(csv);
        assertEquals(csv,
                "road cycling;" + PreferencesUtils.getLayoutColumnsByDefault() + ";"
                + context.getString(R.string.stats_custom_layout_moving_time_key) + ",1,1,0;"
                + context.getString(R.string.stats_custom_layout_distance_key) + ",1,0,0;"
                + context.getString(R.string.stats_custom_layout_average_moving_speed_key) + ",0,1,0;"
                + context.getString(R.string.stats_custom_layout_speed_key) + ",0,0,0;");

        Layout layoutDst = PreferencesUtils.getCustomLayout();
        assertEquals(layoutSrc.getName(), layoutDst.getName());
        assertEquals(layoutSrc.getFields().size(), layoutDst.getFields().size());
        for (int i = 0; i < layoutSrc.getFields().size(); i++) {
            assertEquals(layoutSrc.getFields().get(i).getKey(), layoutDst.getFields().get(i).getKey());
            assertEquals(layoutSrc.getFields().get(i).isVisible(), layoutDst.getFields().get(i).isVisible());
            assertEquals(layoutSrc.getFields().get(i).isPrimary(), layoutDst.getFields().get(i).isPrimary());
        }
    }

    @Test
    public void testEditCustomLayouts() {
        // update all custom layouts

        // given a custom layout with two profiles
        String cyclingProfile = "cycling;2;"
                + context.getString(R.string.stats_custom_layout_moving_time_key) + ",1,1;"
                + context.getString(R.string.stats_custom_layout_distance_key) + ",1,1;"
                + context.getString(R.string.stats_custom_layout_average_moving_speed_key) + ",1,1;"
                + context.getString(R.string.stats_custom_layout_speed_key) + ",1,1;";

        String runningProfile = "running;2;"
                + context.getString(R.string.stats_custom_layout_moving_time_key) + ",1,1;"
                + context.getString(R.string.stats_custom_layout_distance_key) + ",1,1;"
                + context.getString(R.string.stats_custom_layout_average_pace_key) + ",0,0;"
                + context.getString(R.string.stats_custom_layout_pace_key) + ",0,0;";

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(context.getString(R.string.stats_custom_layouts_key), cyclingProfile + "\n" + runningProfile);
        editor.apply();

        List<Layout> layoutsBefore = PreferencesUtils.getAllCustomLayouts();

        // when cyling profile is updated
        String cyclingProfileUpdated = "cycling;2;"
                + context.getString(R.string.stats_custom_layout_moving_time_key) + ",1,1;"
                + context.getString(R.string.stats_custom_layout_distance_key) + ",0,0;"
                + context.getString(R.string.stats_custom_layout_average_moving_speed_key) + ",0,0;"
                + context.getString(R.string.stats_custom_layout_speed_key) + ",0,0;";

        List<Layout> layoutsToBeUpdated = new ArrayList<>();
        layoutsToBeUpdated.add(Layout.fromCsv(cyclingProfileUpdated, resources));
        layoutsToBeUpdated.add(Layout.fromCsv(runningProfile, resources));

        PreferencesUtils.updateCustomLayouts(layoutsToBeUpdated);

        // then only updated profile is modified in the custom layouts
        List<Layout> layoutsAfter = PreferencesUtils.getAllCustomLayouts();

        assertEquals(layoutsBefore.size(), 2);
        assertEquals(layoutsAfter.size(), 2);

        assertEquals(layoutsBefore.get(0).getFields().stream().filter(DataField::isVisible).count(), 4);
        assertEquals(layoutsAfter.get(0).getFields().stream().filter(DataField::isVisible).count(), 1);
    }

    @Test
    public void testEditCustomLayout() {
        // Update only one custom layout

        // given a custom layout with two profiles
        String cyclingProfile = "cycling;2;"
                + context.getString(R.string.stats_custom_layout_moving_time_key) + ",1,1;"
                + context.getString(R.string.stats_custom_layout_distance_key) + ",1,1;"
                + context.getString(R.string.stats_custom_layout_average_moving_speed_key) + ",1,1;"
                + context.getString(R.string.stats_custom_layout_speed_key) + ",1,1;";

        String runningProfile = "running;2;"
                + context.getString(R.string.stats_custom_layout_moving_time_key) + ",1,1;"
                + context.getString(R.string.stats_custom_layout_distance_key) + ",1,1;"
                + context.getString(R.string.stats_custom_layout_average_pace_key) + ",0,0;"
                + context.getString(R.string.stats_custom_layout_pace_key) + ",0,0;";

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(context.getString(R.string.stats_custom_layouts_key), cyclingProfile + "\n" + runningProfile);
        editor.apply();

        List<Layout> layoutsBefore = PreferencesUtils.getAllCustomLayouts();

        // when cyling profile is updated
        String cyclingProfileUpdated = "cycling;2;"
                + context.getString(R.string.stats_custom_layout_moving_time_key) + ",1,1;"
                + context.getString(R.string.stats_custom_layout_distance_key) + ",0,0;"
                + context.getString(R.string.stats_custom_layout_average_moving_speed_key) + ",0,0;"
                + context.getString(R.string.stats_custom_layout_speed_key) + ",0,0;";
        Layout layoutToBeUpdated = Layout.fromCsv(cyclingProfileUpdated, resources);
        PreferencesUtils.updateCustomLayout(layoutToBeUpdated);

        // then only updated profile is modified in the custom layouts
        List<Layout> layoutsAfter = PreferencesUtils.getAllCustomLayouts();

        assertEquals(layoutsBefore.size(), 2);
        assertEquals(layoutsAfter.size(), 2);

        assertEquals(layoutsBefore.get(0).getFields().stream().filter(DataField::isVisible).count(), 4);
        assertEquals(layoutsAfter.get(0).getFields().stream().filter(DataField::isVisible).count(), 1);
    }

    @Test
    public void testGetCustomLayout_whenSelectedOneNotExists() {
        // given a custom layout with two profiles and not existing custom layout selected
        String cyclingProfile = "cycling;2;"
                + context.getString(R.string.stats_custom_layout_moving_time_key) + ",1,1;"
                + context.getString(R.string.stats_custom_layout_distance_key) + ",1,1;"
                + context.getString(R.string.stats_custom_layout_average_moving_speed_key) + ",1,1;"
                + context.getString(R.string.stats_custom_layout_speed_key) + ",1,1;";

        String runningProfile = "running;2;"
                + context.getString(R.string.stats_custom_layout_moving_time_key) + ",1,1;"
                + context.getString(R.string.stats_custom_layout_distance_key) + ",1,1;"
                + context.getString(R.string.stats_custom_layout_average_pace_key) + ",0,0;"
                + context.getString(R.string.stats_custom_layout_pace_key) + ",0,0;";

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(context.getString(R.string.stats_custom_layouts_key), cyclingProfile + "\n" + runningProfile);
        editor.putString(context.getString(R.string.stats_custom_layout_selected_layout_key), "Not Exists");
        editor.apply();

        // when it gets the custom layout
        Layout layout = PreferencesUtils.getCustomLayout();

        // then the first one was returned
        assertEquals(layout.getName(), "cycling");
    }
}
