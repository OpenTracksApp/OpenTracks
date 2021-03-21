package de.dennisguse.opentracks.services;

import android.app.NotificationManager;
import android.content.Context;

import androidx.core.app.NotificationCompat;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import de.dennisguse.opentracks.content.data.TrackPoint;
import de.dennisguse.opentracks.stats.TrackStatistics;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
        when(trackPointMock.hasAccuracy()).thenReturn(true);
        when(trackPointMock.getAccuracy()).thenReturn(999f);
        when(trackStatisticsMock.getTotalDistance()).thenReturn(0d);
        when(notificationCompatBuilder.setContentText(anyString())).thenReturn(notificationCompatBuilder);
        when(notificationCompatBuilder.setOnlyAlertOnce(anyBoolean())).thenReturn(notificationCompatBuilder);

        TrackRecordingServiceNotificationManager subject = new TrackRecordingServiceNotificationManager(notificationManager, notificationCompatBuilder);
        subject.setMetricUnits(true);

        // when
        subject.updateTrackPoint(context, trackStatisticsMock, trackPointMock, 100);
        subject.updateTrackPoint(context, trackStatisticsMock, trackPointMock, 100);
        subject.updateTrackPoint(context, trackStatisticsMock, trackPointMock, 1000);
        subject.updateTrackPoint(context, trackStatisticsMock, trackPointMock, 100);

        // then
        verify(notificationCompatBuilder, times(6)).setOnlyAlertOnce(true);
        verify(notificationCompatBuilder, times(2)).setOnlyAlertOnce(false);
    }
}