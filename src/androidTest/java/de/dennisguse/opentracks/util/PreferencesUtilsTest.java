package de.dennisguse.opentracks.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.io.file.TrackFileFormat;

@RunWith(AndroidJUnit4.class)
public class PreferencesUtilsTest {

    private final Context context = ApplicationProvider.getApplicationContext();

    @Test
    public void ExportTrackFileFormat_ok() {
        // given
        SharedPreferences.Editor editor = PreferencesUtils.getSharedPreferences(context).edit();
        editor.putString(context.getString(R.string.export_trackfileformat_key), TrackFileFormat.KMZ_WITH_TRACKDETAIL.name());
        editor.commit();

        // when
        TrackFileFormat trackFileFormat = PreferencesUtils.getExportTrackFileFormat(context);

        // then
        Assert.assertEquals(TrackFileFormat.KMZ_WITH_TRACKDETAIL, trackFileFormat);
    }

    @Test
    public void ExportTrackFileFormat_invalid() {
        // given
        SharedPreferences.Editor editor = PreferencesUtils.getSharedPreferences(context).edit();
        editor.putString(context.getString(R.string.export_trackfileformat_key), "invalid");
        editor.commit();

        // when
        TrackFileFormat trackFileFormat = PreferencesUtils.getExportTrackFileFormat(context);

        // then
        Assert.assertEquals(TrackFileFormat.KML_ONLY_TRACK, trackFileFormat);
    }

    @Test
    public void ExportTrackFileFormat_noValue() {
        // given
        SharedPreferences.Editor editor = PreferencesUtils.getSharedPreferences(context).edit();
        editor.clear();
        editor.commit();

        // when
        TrackFileFormat trackFileFormat = PreferencesUtils.getExportTrackFileFormat(context);

        // then
        Assert.assertEquals(TrackFileFormat.KML_ONLY_TRACK, trackFileFormat);
    }
}