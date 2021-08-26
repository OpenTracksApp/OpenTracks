package de.dennisguse.opentracks.services.tasks;

import android.content.Context;
import android.content.Intent;
import android.os.Looper;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.GrantPermissionRule;
import androidx.test.rule.ServiceTestRule;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import de.dennisguse.opentracks.content.data.Distance;
import de.dennisguse.opentracks.services.TrackRecordingService;
import de.dennisguse.opentracks.stats.TrackStatistics;

import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class VoiceAnnouncementManagerTest {

    @Rule
    public final ServiceTestRule mServiceRule = ServiceTestRule.withTimeout(5, TimeUnit.SECONDS);

    @Rule
    public GrantPermissionRule mRuntimePermissionRule = GrantPermissionRule.grant(android.Manifest.permission.ACCESS_FINE_LOCATION);

    @BeforeClass
    public static void preSetUp() {
        // Prepare looper for Android's message queue
        if (Looper.myLooper() == null) Looper.prepare();
    }

    @AfterClass
    public static void finalTearDown() {
        if (Looper.myLooper() != null) Looper.myLooper().quit();
    }


    private final Context context = ApplicationProvider.getApplicationContext();

    @Test
    public void calculateNextTaskDistance() throws TimeoutException {
        // given
        TrackRecordingService service = ((TrackRecordingService.Binder) mServiceRule.bindService(new Intent(context, TrackRecordingService.class)))
                .getService();

        VoiceAnnouncementManager voiceAnnouncementManager = new VoiceAnnouncementManager(service);
        voiceAnnouncementManager.setMetricUnits(true);
        voiceAnnouncementManager.setTaskFrequency(-5);

        // when
        TrackStatistics statistics = new TrackStatistics();
        statistics.setTotalDistance(Distance.of(13000));
        assertEquals(Distance.of(15000), voiceAnnouncementManager.calculateNextTaskDistance(statistics));

        statistics.setTotalDistance(Distance.of(15100));
        assertEquals(Distance.of(20000), voiceAnnouncementManager.calculateNextTaskDistance(statistics));
    }

}