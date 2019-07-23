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
package com.google.android.apps.mytracks;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.android.apps.mytracks.services.TrackRecordingService;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

/**
 * Tests for the BootReceiver.
 *
 * @author Youtao Liu
 */
@RunWith(AndroidJUnit4.class)
public class BootReceiverTest {
    private static final String SERVICE_NAME = "com.google.android.apps.mytracks.services.TrackRecordingService";

    /**
     * Tests the behavior when receive notification which is the phone boot.
     */
    @Test
    public void testOnReceive_startService() {
        // Make sure no TrackRecordingService
        Intent stopIntent = new Intent(InstrumentationRegistry.getInstrumentation().getContext(), TrackRecordingService.class);
        InstrumentationRegistry.getInstrumentation().getContext().stopService(stopIntent);
        Assert.assertFalse(isServiceExisted(InstrumentationRegistry.getInstrumentation().getContext(), SERVICE_NAME));

        BootReceiver bootReceiver = new BootReceiver();
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_BOOT_COMPLETED);
        bootReceiver.onReceive(InstrumentationRegistry.getInstrumentation().getContext(), intent);
        // Check if the service is started
        Assert.assertFalse(isServiceExisted(InstrumentationRegistry.getInstrumentation().getContext(), SERVICE_NAME));
    }

    /**
     * Tests the behavior when receive notification which is not the phone boot.
     */
    @Test
    public void testOnReceive_noStartService() {
        // Make sure no TrackRecordingService
        Intent stopIntent = new Intent(InstrumentationRegistry.getInstrumentation().getContext(), TrackRecordingService.class);
        InstrumentationRegistry.getInstrumentation().getContext().stopService(stopIntent);
        Assert.assertFalse(isServiceExisted(InstrumentationRegistry.getInstrumentation().getContext(), SERVICE_NAME));

        BootReceiver bootReceiver = new BootReceiver();
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_BUG_REPORT);
        bootReceiver.onReceive(InstrumentationRegistry.getInstrumentation().getContext(), intent);

        // Check if the service is not started
        Assert.assertFalse(isServiceExisted(InstrumentationRegistry.getInstrumentation().getContext(), SERVICE_NAME));
    }

    /**
     * Checks if a service is started in a context.
     *
     * @param context     the context for checking a service
     * @param serviceName the service name to find if existed
     */
    private boolean isServiceExisted(Context context, String serviceName) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> serviceList = activityManager.getRunningServices(Integer.MAX_VALUE);

        for (int i = 0; i < serviceList.size(); i++) {
            RunningServiceInfo serviceInfo = serviceList.get(i);
            ComponentName componentName = serviceInfo.service;
            if (componentName.getClassName().equals(serviceName)) {
                return true;
            }
        }
        return false;
    }
}