package de.dennisguse.opentracks.publicapi;

import android.content.Context;
import android.content.Intent;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.rule.GrantPermissionRule;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.TestUtil;
import de.dennisguse.opentracks.data.ContentProviderUtils;
import de.dennisguse.opentracks.data.models.Track;
import de.dennisguse.opentracks.settings.PreferencesUtils;
import de.dennisguse.opentracks.util.IntentUtils;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class PublicApiTest {

    @Rule
    public GrantPermissionRule mGrantPermissionRule = TestUtil.createGrantPermissionRule();

    private final Context context = ApplicationProvider.getApplicationContext();

    //NOTE: this doesn't check if the TrackRecordingService was started in foreground.
    @Test
    public void StartStopTest() throws InterruptedException {
        PreferencesUtils.setBoolean(R.string.publicapi_enabled_key, true);
        Intent startIntent = IntentUtils.newIntent(context, StartRecording.class);
        startIntent.putExtra("TRACK_NAME", "trackName");
        startIntent.putExtra("TRACK_CATEGORY", "activityTypeLocalized");
        startIntent.putExtra("TRACK_ICON", "airplane");
        startIntent.putExtra("TRACK_DESCRIPTION", "description");
        context.startActivity(startIntent);

        Thread.sleep(5000);

        context.startActivity(IntentUtils.newIntent(context, StopRecording.class));

        List<Track> tracks = new ContentProviderUtils(context).getTracks();
        Assert.assertEquals(1, tracks.size());
        Track track = tracks.get(0);
        Assert.assertEquals("trackName", track.getName());
        Assert.assertEquals("activityTypeLocalized", track.getActivityTypeLocalized());
        Assert.assertEquals("airplane", track.getActivityType().getId());
    }

    @Test
    public void StopAndWait() throws InterruptedException {
        PreferencesUtils.setBoolean(R.string.publicapi_enabled_key, true);

        context.startActivity(IntentUtils.newIntent(context, StopRecording.class));

        Thread.sleep(10000);

        //No ForegroundServiceDidNotStartInTimeException should be happening.
    }
}