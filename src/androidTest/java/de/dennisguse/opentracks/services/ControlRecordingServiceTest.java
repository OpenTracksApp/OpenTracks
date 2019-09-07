/*
 * Copyright 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package de.dennisguse.opentracks.services;

import android.content.Context;
import android.content.Intent;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.rule.GrantPermissionRule;
import androidx.test.rule.ServiceTestRule;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.concurrent.TimeoutException;

import de.dennisguse.opentracks.R;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


/**
 * Tests {@link ControlRecordingService}.
 *
 * @author Youtao Liu
 */
@RunWith(MockitoJUnitRunner.class)
public class ControlRecordingServiceTest {

    private Context context = ApplicationProvider.getApplicationContext();

    @Rule
    public final ServiceTestRule mServiceRule = new ServiceTestRule();

    @Rule
    public GrantPermissionRule mRuntimePermissionRule = GrantPermissionRule.grant(android.Manifest.permission.ACCESS_FINE_LOCATION);

    @Mock
    private ITrackRecordingService iTrackRecordingServiceMock;

    private ControlRecordingService subject;

    /**
     * Tests the start of {@link ControlRecordingService} and tests the method {@link ControlRecordingService#onHandleIntent(Intent, ITrackRecordingService)} to start a track recording.
     */
    @Test
    public void testStartRecording() throws TimeoutException {
        Assert.assertNull(subject);
        Intent intent = startControlRecordingService(context.getString(R.string.track_action_start));
        Assert.assertNotNull(subject);

        when(iTrackRecordingServiceMock.startNewTrack()).thenReturn(1L);
        subject.onHandleIntent(intent, iTrackRecordingServiceMock);
        verify(iTrackRecordingServiceMock).startNewTrack();
    }

    /**
     * Tests the method {@link ControlRecordingService#onHandleIntent(Intent, ITrackRecordingService)} to stop a track recording.
     */
    @Test
    public void testStopRecording() throws TimeoutException {
        Intent intent = startControlRecordingService(context.getString(R.string.track_action_end));

        subject.onHandleIntent(intent, iTrackRecordingServiceMock);
        verify(iTrackRecordingServiceMock).endCurrentTrack();
    }

    /**
     * Starts a ControlRecordingService with a specified action.
     *
     * @param action the action string in the start intent
     */
    private Intent startControlRecordingService(String action) throws TimeoutException {
        Intent intent = new Intent(context, ControlRecordingService.class);
        intent.setAction(action);
        mServiceRule.startService(intent);
        subject = ((ControlRecordingService.LocalBinder) mServiceRule.bindService(intent)).getService();
        return intent;
    }
}
