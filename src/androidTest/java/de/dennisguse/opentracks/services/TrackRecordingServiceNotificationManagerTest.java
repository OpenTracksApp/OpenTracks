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

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TrackRecordingServiceNotificationManagerTest {

    private final Context context = ApplicationProvider.getApplicationContext();

    @Mock
    private TrackPoint locationMock;

    @Mock
    private NotificationCompat.Builder notificationCompatBuilder;

    @Mock
    private NotificationManager notificationManager;

    @Test
    public void updateLocation_triggersAlertOnlyOnFirstInaccurateLocation() {
        when(locationMock.hasAccuracy()).thenReturn(true);
        when(locationMock.getAccuracy()).thenReturn(999f);
        when(notificationCompatBuilder.setContentText(anyString())).thenReturn(notificationCompatBuilder);
        when(notificationCompatBuilder.setOnlyAlertOnce(anyBoolean())).thenReturn(notificationCompatBuilder);

        TrackRecordingServiceNotificationManager subject = new TrackRecordingServiceNotificationManager(notificationManager, notificationCompatBuilder);

        // when
        subject.updateTrackPoint(context, locationMock, 100);
        subject.updateTrackPoint(context, locationMock, 100);
        subject.updateTrackPoint(context, locationMock, 1000);
        subject.updateTrackPoint(context, locationMock, 100);

        // then
        verify(notificationCompatBuilder, times(6)).setOnlyAlertOnce(true);
        verify(notificationCompatBuilder, times(2)).setOnlyAlertOnce(false);
    }
}