package de.dennisguse.opentracks.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.io.file.TrackFileFormat;

import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class PreferencesUtilsTest {

    private final Context context = ApplicationProvider.getApplicationContext();

    @Test
    public void ExportTrackFileFormat_ok() {
        // given
        SharedPreferences sharedPreferences = PreferencesUtils.getSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putString(context.getString(R.string.export_trackfileformat_key), TrackFileFormat.KMZ_WITH_TRACKDETAIL.name());
        editor.commit();

        // when
        TrackFileFormat trackFileFormat = PreferencesUtils.getExportTrackFileFormat(sharedPreferences, context);

        // then
        assertEquals(TrackFileFormat.KMZ_WITH_TRACKDETAIL, trackFileFormat);
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
}