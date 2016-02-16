/*
 * Copyright 2013 Google Inc.
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

package com.google.android.apps.mytracks.util;

import com.google.android.apps.mytracks.widgets.TrackWidgetProvider;

import android.annotation.TargetApi;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.os.Bundle;

/**
 * API level 17 specific implementation of the {@link ApiAdapter}.
 * 
 * @author Jimmy Shih
 */
@TargetApi(17)
public class Api17Adapter extends Api16Adapter {

  @Override
  protected int getAppWidgetSizeDefault(Bundle bundle) {
    boolean isKeyguard = bundle.getInt(AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY, -1)
        == AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD;
    return isKeyguard ? TrackWidgetProvider.KEYGUARD_DEFAULT_SIZE
        : TrackWidgetProvider.HOME_SCREEN_DEFAULT_SIZE;
  }
}
