package de.dennisguse.opentracks.services.handlers;

import android.content.Context;
import android.content.SharedPreferences;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import de.dennisguse.opentracks.content.data.Distance;
import de.dennisguse.opentracks.content.data.TrackPoint;

import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class HandlerServerTest {

    @Mock
    private Context context;

    @Mock
    private HandlerServer.HandlerServerInterface server;

    @Mock
    private LocationHandler locationHandler;

    @Mock
    private SharedPreferences sharedPreferences;

    private HandlerServer subject;

    @Before
    public void setUp() {
        subject = new HandlerServer(locationHandler, server);
        subject.start(context);
    }

    @After
    public void tearDown() {
        subject.stop();
    }

    @Ignore("ServiceExecutor disabled for #822")
    @Test
    public void sendTrackPoint() throws InterruptedException {
        // given
        TrackPoint trackPoint = new TrackPoint(TrackPoint.Type.TRACKPOINT, null);
        Distance horizontalAccuracyThreshold = Distance.of(50);

        // when
        subject.onNewTrackPoint(trackPoint, horizontalAccuracyThreshold);

        // then
        Thread.sleep(10); // Wait for executor service
        verify(server).newTrackPoint(trackPoint, horizontalAccuracyThreshold);
    }
}