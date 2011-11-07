/*
 * Copyright 2010 Google Inc.
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
package com.google.android.apps.mytracks.services;

import com.google.android.apps.mytracks.Constants;
import com.google.android.maps.mytracks.R;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Creates a default track category (activity type) based on current default
 * category settings.
 * 
 * @author Rimas Trumpa
 */
class DefaultCategoryFactory {
  private final Context context;

  DefaultCategoryFactory(Context context) {
    this.context = context;
  }

  /**
   * Creates a new track category (activity type).
   * 
   * @return The new track category.
   */
  String newTrackCategory() {
    if (getDefaultCategory() != null) {
      return getDefaultCategory();
    } else {
      return "";
    }
  }

  protected String getDefaultCategory() {
    SharedPreferences prefs = context.getSharedPreferences(Constants.SETTINGS_NAME, 0);
    return prefs.getString(context.getString(R.string.default_category_key), null);
  }
}
