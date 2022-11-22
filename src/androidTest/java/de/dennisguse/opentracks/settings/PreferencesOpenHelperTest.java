package de.dennisguse.opentracks.settings;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;

import androidx.preference.PreferenceManager;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.ui.customRecordingLayout.RecordingLayout;
import de.dennisguse.opentracks.ui.customRecordingLayout.RecordingLayoutIO;

@RunWith(AndroidJUnit4.class)
public class PreferencesOpenHelperTest {

    private final Context context = ApplicationProvider.getApplicationContext();
    private final Resources resources = ApplicationProvider.getApplicationContext().getResources();

    @Test
    public void test_upgradeFrom0To1_withoutStatsCustomLayouts() {
        // given the version 0
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(context.getString(R.string.stats_custom_layouts_key));
        editor.putInt(context.getString(R.string.prefs_last_version_key), 0);
        editor.commit();

        // when update to version 1
        PreferencesOpenHelper.newInstance(1).check();

        // then there should be one layout with old custom layout that has the new CSV value.
        List<RecordingLayout> recordingLayouts = PreferencesUtils.getAllCustomLayouts();
        assertNotNull(recordingLayouts);
        assertEquals(recordingLayouts.size(), 1);
        assertEquals(recordingLayouts.get(0).toCsv(), PreferencesUtils.getCustomLayout().toCsv());
    }

    @Test
    public void test_upgradeFrom1To2_withOldVersion() {
        // given the version 1
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        String oldCustomLayoutCsv = context.getString(R.string.activity_type_unknown) + ";distance,1,1,0;speed,1,1,0;";
        editor.putString(context.getString(R.string.stats_custom_layouts_key), oldCustomLayoutCsv);
        editor.putInt(context.getString(R.string.prefs_last_version_key), 1);

        editor.commit();

        // when update to version 2
        PreferencesOpenHelper.newInstance(2).check();

        String updatedOldCustomLayoutCsv = context.getString(R.string.activity_type_unknown) + ";"
                + PreferencesUtils.getLayoutColumnsByDefault() + ";distance,1,1,0;speed,1,1,0;";

        // then there should be one layout with old custom layout that has the new CSV value.
        List<RecordingLayout> recordingLayouts = PreferencesUtils.getAllCustomLayouts();
        assertNotNull(recordingLayouts);
        assertEquals(recordingLayouts.size(), 1);
        assertEquals(recordingLayouts.get(0).toCsv(), updatedOldCustomLayoutCsv);
    }

    @Test
    public void test_upgradeFrom1To2_withNewVersion() {
        // given the version 1
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        String oldCustomLayoutCsv = "whatever;3;distance,1,1,0;speed,1,1,0;";
        editor.putString(context.getString(R.string.stats_custom_layouts_key), oldCustomLayoutCsv);
        editor.putInt(context.getString(R.string.prefs_last_version_key), 1);

        editor.commit();

        // when update to version 2
        PreferencesOpenHelper.newInstance(2).check();

        String updatedOldCustomLayoutCsv = "whatever;3;distance,1,1,0;speed,1,1,0;";

        // then there should be one layout with old custom layout that has the new CSV value.
        List<RecordingLayout> recordingLayouts = PreferencesUtils.getAllCustomLayouts();
        assertNotNull(recordingLayouts);
        assertEquals(recordingLayouts.size(), 1);
        assertEquals(recordingLayouts.get(0).toCsv(), updatedOldCustomLayoutCsv);
    }

    @Test
    public void test_downgrade() {
        // given version 2
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        String customLayoutCsv = "whatever;3;distance,1,1,0;speed,1,1,0;";
        editor.putString(context.getString(R.string.stats_custom_layouts_key), customLayoutCsv);
        editor.putInt(context.getString(R.string.prefs_last_version_key), 2);

        editor.commit();

        // when downgrade to version 1
        PreferencesOpenHelper.newInstance(1).check();

        // then custom layout should be equals to default layout.
        RecordingLayout defaultRecordingLayout = RecordingLayoutIO.fromCsv(PreferencesUtils.buildDefaultLayout(), resources);
        List<RecordingLayout> customRecordingLayout = PreferencesUtils.getAllCustomLayouts();

        assertEquals(customRecordingLayout.size(), 1);
        assertEquals(defaultRecordingLayout.toCsv(), customRecordingLayout.get(0).toCsv());
    }
}
