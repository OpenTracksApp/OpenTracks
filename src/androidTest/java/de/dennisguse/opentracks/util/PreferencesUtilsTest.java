package de.dennisguse.opentracks.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.content.data.Layout;
import de.dennisguse.opentracks.io.file.TrackFileFormat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class PreferencesUtilsTest {

    private final Context context = ApplicationProvider.getApplicationContext();

    @Test
    public void ExportTrackFileFormat_ok() {
        // given
        SharedPreferences sharedPreferences = PreferencesUtils.getSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putString(context.getString(R.string.export_trackfileformat_key), TrackFileFormat.KML_WITH_TRACKDETAIL_AND_SENSORDATA.name());
        editor.commit();

        // when
        TrackFileFormat trackFileFormat = PreferencesUtils.getExportTrackFileFormat(sharedPreferences, context);

        // then
        assertEquals(TrackFileFormat.KML_WITH_TRACKDETAIL_AND_SENSORDATA, trackFileFormat);
    }

    @Test
    public void ExportTrackFileFormat_invalid() {
        // given
        SharedPreferences sharedPreferences = PreferencesUtils.getSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(context.getString(R.string.export_trackfileformat_key), "invalid");
        editor.commit();

        // when
        TrackFileFormat trackFileFormat = PreferencesUtils.getExportTrackFileFormat(sharedPreferences, context);

        // then
        assertEquals(TrackFileFormat.KMZ_WITH_TRACKDETAIL_AND_SENSORDATA, trackFileFormat);
    }

    @Test
    public void ExportTrackFileFormat_noValue() {
        // given
        SharedPreferences sharedPreferences = PreferencesUtils.getSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.commit();

        // when
        TrackFileFormat trackFileFormat = PreferencesUtils.getExportTrackFileFormat(sharedPreferences, context);

        // then
        assertEquals(TrackFileFormat.KMZ_WITH_TRACKDETAIL_AND_SENSORDATA, trackFileFormat);
    }

    @Test
    public void testGetCustomLayout_default() {
        // given
        SharedPreferences sharedPreferences = PreferencesUtils.getSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.commit();

        // when
        Layout layout = PreferencesUtils.getCustomLayout(sharedPreferences, context);

        // then
        assertTrue(layout.getFields().size() > 0);
        assertEquals(layout.getProfile(), context.getString(R.string.default_activity_default));
        assertTrue(layout.getFields().stream().anyMatch(Layout.Field::isVisible));
    }

    @Test
    public void testGetCustomLayout_1() {
        // given
        SharedPreferences sharedPreferences = PreferencesUtils.getSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(context.getString(R.string.stats_custom_layout_fields_key), "run;Moving Time,1,1;Distance,1,0;Moving Avg. Speed,0,1;Speed,0,0;");
        editor.apply();

        // when
        Layout layout = PreferencesUtils.getCustomLayout(sharedPreferences, context);

        // then
        assertEquals(layout.getFields().size(), 4);
        assertEquals(layout.getProfile(), "run");

        assertEquals(layout.getFields().get(0).getTitle(), "Moving Time");
        assertTrue(layout.getFields().get(0).isVisible());
        assertTrue(layout.getFields().get(0).isPrimary());

        assertEquals(layout.getFields().get(1).getTitle(), "Distance");
        assertTrue(layout.getFields().get(1).isVisible());
        assertFalse(layout.getFields().get(1).isPrimary());

        assertEquals(layout.getFields().get(2).getTitle(), "Moving Avg. Speed");
        assertFalse(layout.getFields().get(2).isVisible());
        assertTrue(layout.getFields().get(2).isPrimary());

        assertEquals(layout.getFields().get(3).getTitle(), "Speed");
        assertFalse(layout.getFields().get(3).isVisible());
        assertFalse(layout.getFields().get(3).isPrimary());
    }

    @Test
    public void testSetCustomLayout() {
        // given
        SharedPreferences sharedPreferences = PreferencesUtils.getSharedPreferences(context);
        Layout layoutSrc = new Layout("road cycling");
        layoutSrc.addField(new Layout.Field("Moving Time", true, true, false));
        layoutSrc.addField(new Layout.Field("Distance", true, false, false));
        layoutSrc.addField(new Layout.Field("Moving Avg. Speed", false, true, false));
        layoutSrc.addField(new Layout.Field("Speed", false, false, false));

        // when
        PreferencesUtils.setCustomLayout(sharedPreferences, context, layoutSrc);

        // then
        String csv = sharedPreferences.getString(context.getString(R.string.stats_custom_layout_fields_key), null);
        assertNotNull(csv);
        assertEquals(csv, "road cycling;Moving Time,1,1;Distance,1,0;Moving Avg. Speed,0,1;Speed,0,0;");

        Layout layoutDst = PreferencesUtils.getCustomLayout(sharedPreferences, context);
        assertEquals(layoutSrc.getProfile(), layoutDst.getProfile());
        assertEquals(layoutSrc.getFields().size(), layoutDst.getFields().size());
        for (int i = 0; i < layoutSrc.getFields().size(); i++) {
            assertEquals(layoutSrc.getFields().get(i).getTitle(), layoutDst.getFields().get(i).getTitle());
            assertEquals(layoutSrc.getFields().get(i).isVisible(), layoutDst.getFields().get(i).isVisible());
            assertEquals(layoutSrc.getFields().get(i).isPrimary(), layoutDst.getFields().get(i).isPrimary());
        }
    }
}
