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

import com.google.android.apps.mytracks.TrackController;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.view.MenuItem;
import android.widget.SearchView;

/**
 * API level 14 specific implementation of the {@link ApiAdapter}.
 * 
 * @author Jimmy Shih
 */
@TargetApi(14)
public class Api14Adapter extends Api11Adapter {

  @Override
  public void configureActionBarHomeAsUp(Activity activity) {
    ActionBar actionBar = activity.getActionBar();
    if (actionBar != null) {
      actionBar.setHomeButtonEnabled(true);
      actionBar.setDisplayHomeAsUpEnabled(true);
    }
  }

  @Override
  public void configureSearchWidget(
      Activity activity, final MenuItem menuItem, TrackController trackController) {
    super.configureSearchWidget(activity, menuItem, trackController);
    SearchView searchView = (SearchView) menuItem.getActionView();
    searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
        @Override
      public boolean onQueryTextSubmit(String query) {
        menuItem.collapseActionView();
        return false;
      }

        @Override
      public boolean onQueryTextChange(String newText) {
        return false;
      }
    });
    searchView.setOnSuggestionListener(new SearchView.OnSuggestionListener() {
        @Override
      public boolean onSuggestionSelect(int position) {
        return false;
      }

        @Override
      public boolean onSuggestionClick(int position) {
        menuItem.collapseActionView();
        return false;
      }
    });
  }

  @Override
  public boolean handleSearchKey(MenuItem menuItem) {
    menuItem.expandActionView();
    return true;
  }
  
  @Override
  public boolean isGoogleFeedbackAvailable() {
    return true;
  }
}
