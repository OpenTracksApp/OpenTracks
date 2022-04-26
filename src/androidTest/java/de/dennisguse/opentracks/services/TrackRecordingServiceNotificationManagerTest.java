package de.dennisguse.opentracks.services;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.NotificationManager;
import android.content.Context;

import androidx.core.app.NotificationCompat;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import de.dennisguse.opentracks.data.models.Distance;
import de.dennisguse.opentracks.data.models.Speed;
import de.dennisguse.opentracks.data.models.TrackPoint;
import de.dennisguse.opentracks.settings.UnitSystem;
import de.dennisguse.opentracks.stats.TrackStatistics;

@RunWith(MockitoJUnitRunner.class)
public class TrackRecordingServiceNotificationManagerTest {

    private final Context context = ApplicationProvider.getApplicationContext();

    @Mock
    private TrackPoint trackPointMock;

    @Mock
    private TrackStatistics trackStatisticsMock;

    @Mock
    private NotificationCompat.Builder notificationCompatBuilder;

    @Mock
    private NotificationManager notificationManager;

    @Test
    public void updateLocation_triggersAlertOnlyOnFirstInaccurateLocation() {
        when(trackPointMock.hasHorizontalAccuracy()).thenReturn(true);
        when(trackPointMock.getHorizontalAccuracy()).thenReturn(Distance.of(999f));
        when(trackPointMock.getSpeed()).thenReturn(Speed.of(0));
        when(trackStatisticsMock.getTotalDistance()).thenReturn(Distance.of(0));
        when(notificationCompatBuilder.setOnlyAlertOnce(anyBoolean()))
                .thenReturn(notificationCompatBuilder);

        TrackRecordingServiceNotificationManager subject = new TrackRecordingServiceNotificationManager(notificationManager, notificationCompatBuilder);
        subject.setUnitSystem(UnitSystem.METRIC);

        // when
        subject.updateTrackPoint(context, trackStatisticsMock, trackPointMock, Distance.of(100));
        subject.updateTrackPoint(context, trackStatisticsMock, trackPointMock, Distance.of(100));
        subject.updateTrackPoint(context, trackStatisticsMock, trackPointMock, Distance.of(1000));
        subject.updateTrackPoint(context, trackStatisticsMock, trackPointMock, Distance.of(100));

        // then
        verify(notificationCompatBuilder, times(6)).setOnlyAlertOnce(true);
        verify(notificationCompatBuilder, times(2)).setOnlyAlertOnce(false);
    }
}