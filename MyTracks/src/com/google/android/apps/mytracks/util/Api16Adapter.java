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
import android.os.Bundle;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;

/**
 * API level 16 specific implementation of the {@link ApiAdapter}.
 * 
 * @author Jimmy Shih
 */
@TargetApi(16)
public class Api16Adapter extends Api14Adapter {

  private static final String APP_WIDGET_SIZE_KEY = "app_widget_size_key";

  @Override
  public int getAppWidgetSize(AppWidgetManager appWidgetManager, int appWidgetId) {
    Bundle bundle = appWidgetManager.getAppWidgetOptions(appWidgetId);
    return bundle.getInt(APP_WIDGET_SIZE_KEY, getAppWidgetSizeDefault(bundle));
  }
  
  @SuppressWarnings("unused")
  protected int getAppWidgetSizeDefault(Bundle bundle) {
    return TrackWidgetProvider.HOME_SCREEN_DEFAULT_SIZE;
  }
  
  @Override
  public void setAppWidgetSize(AppWidgetManager appWidgetManager, int appWidgetId, int size) {
    Bundle bundle = new Bundle();
    bundle.putInt(APP_WIDGET_SIZE_KEY, size);
    appWidgetManager.updateAppWidgetOptions(appWidgetId, bundle);
  }
  
  @Override
  public void removeGlobalLayoutListener(
      ViewTreeObserver observer, OnGlobalLayoutListener listener) {
    observer.removeOnGlobalLayoutListener(listener);
  }
}
