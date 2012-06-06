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

package com.google.android.apps.mytracks.util;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;

import android.content.Context;

/**
 * Utitlites for sending pageviews to Google Analytics.
 *
 * @author Jimmy Shih
 */
public class AnalyticsUtils {

  private static final String UA = "UA-7222692-2";
  private static final String PRODUCT_NAME = "android-mytracks";
  
  private AnalyticsUtils() {}
  
  /**
   * Sends a page view.
   * 
   * @param context the context
   * @param page the page
   */
  public static void sendPageViews(Context context, String page) {
    GoogleAnalyticsTracker tracker = GoogleAnalyticsTracker.getInstance();
    tracker.startNewSession(UA, context);
    tracker.setProductVersion(PRODUCT_NAME, SystemUtils.getMyTracksVersion(context));
    tracker.trackPageView(page);
    tracker.dispatch();
    tracker.stopSession();
  }
}
