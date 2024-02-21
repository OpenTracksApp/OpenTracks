package de.dennisguse.opentracks.publicapi;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.rule.GrantPermissionRule;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.TestUtil;
import de.dennisguse.opentracks.settings.PreferencesUtils;
import de.dennisguse.opentracks.util.IntentUtils;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class PublicApiTest {

    @Rule
    public GrantPermissionRule mGrantPermissionRule = TestUtil.createGrantPermissionRule();

    private final Context context = ApplicationProvider.getApplicationContext();

    @Test
    public void StartTest() {
        PreferencesUtils.setBoolean(R.string.publicapi_enabled_key, true);

        context.startActivity(IntentUtils.newIntent(context, StartRecording.class));
    }

    @Test
    public void StartStopTest() throws InterruptedException {
        PreferencesUtils.setBoolean(R.string.publicapi_enabled_key, true);

        context.startActivity(IntentUtils.newIntent(context, StartRecording.class));

        Thread.sleep(5000);

        context.startActivity(IntentUtils.newIntent(context, StopRecording.class));
    }

    @Test
    public void StopAndWait() throws InterruptedException {
        PreferencesUtils.setBoolean(R.string.publicapi_enabled_key, true);

        context.startActivity(IntentUtils.newIntent(context, StopRecording.class));

        Thread.sleep(10000);

        //No ForegroundServiceDidNotStartInTimeException should be happening.
    }
}