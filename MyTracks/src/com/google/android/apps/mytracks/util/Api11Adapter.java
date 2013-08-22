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

import com.google.android.apps.mytracks.ContextualActionModeCallback;
import com.google.android.apps.mytracks.TrackController;
import com.google.android.maps.mytracks.R;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TabWidget;

import java.util.ArrayList;
import java.util.List;

/**
 * API level 11 specific implementation of the {@link ApiAdapter}.
 * 
 * @author Jimmy Shih
 */
@TargetApi(11)
public class Api11Adapter extends Api10Adapter {

  @Override
  public void hideTitle(Activity activity) {
    // Do nothing
  }

  @Override
  public void configureActionBarHomeAsUp(Activity activity) {
    ActionBar actionBar = activity.getActionBar();
    if (actionBar != null) {
      actionBar.setDisplayHomeAsUpEnabled(true);
    }
  }

  @Override
  public void configureListViewContextualMenu(final Activity activity, final ListView listView,
      final ContextualActionModeCallback contextualActionModeCallback) {
    listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
    listView.setMultiChoiceModeListener(new MultiChoiceModeListener() {

        @Override
      public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        mode.getMenuInflater().inflate(R.menu.list_context_menu, menu);
        setActionModeTitle(mode);
        return true;
      }

        @Override
      public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        contextualActionModeCallback.onPrepare(
            menu, getCheckedPositions(listView), listView.getCheckedItemIds(), true);
        return true;
      }

        @Override
      public void onDestroyActionMode(ActionMode mode) {
        // Do nothing
      }

        @Override
      public void onItemCheckedStateChanged(
          ActionMode mode, int position, long id, boolean checked) {
        setActionModeTitle(mode);
        mode.invalidate();
      }

        @Override
      public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        if (contextualActionModeCallback.onClick(
            item.getItemId(), getCheckedPositions(listView), listView.getCheckedItemIds())) {
          mode.finish();
        }
        return true;
      }
        
      /**
       * Sets the action mode title
       * 
       * @param mode action mode
       */
      private void setActionModeTitle(ActionMode mode) {
        int count = listView.getCheckedItemCount();
        mode.setTitle(activity.getString(R.string.list_item_selected, count));
      }
      
      /**
       * Gets the checked positions in a list view.
       * 
       * @param list the list view
       */
      private int[] getCheckedPositions(ListView list) {
        SparseBooleanArray positions  = list.getCheckedItemPositions();
        ArrayList<Integer> arrayList = new ArrayList<Integer>();
        for (int i = 0; i < positions.size(); i++) {
          int key = positions.keyAt(i);
          if (positions.valueAt(i)) {
            arrayList.add(key);
          }
        }
        int[] result = new int[arrayList.size()];
        for (int i = 0; i < arrayList.size(); i++) {
          result[i] = arrayList.get(i);
        }
        return result;
      }
    });
  }

  @Override
  public void configureSearchWidget(
      Activity activity, MenuItem menuItem, final TrackController trackController) {
    SearchManager searchManager = (SearchManager) activity.getSystemService(Context.SEARCH_SERVICE);
    SearchView searchView = (SearchView) menuItem.getActionView();
    searchView.setSearchableInfo(searchManager.getSearchableInfo(activity.getComponentName()));
    searchView.setQueryRefinementEnabled(true);
    searchView.setOnQueryTextFocusChangeListener(new View.OnFocusChangeListener() {
      
        @Override
      public void onFocusChange(View v, boolean hasFocus) {
          // Hide and show trackController when search widget has focus/no focus
          if (trackController != null) {
            if (hasFocus) {
              trackController.hide();
            } else {
              trackController.show();
            }
          }        
      }
    });
  }

  @Override
  public boolean handleSearchMenuSelection(Activity activity) {
    // Returns false to allow the platform to expand the search widget.
    return false;
  }

  @Override
  public <T> void addAllToArrayAdapter(ArrayAdapter<T> arrayAdapter, List<T> items) {
    arrayAdapter.addAll(items);
  }

  @Override
  public void invalidMenu(Activity activity) {
    activity.invalidateOptionsMenu();
  }
  
  @Override
  public boolean isSpinnerBackgroundLight() {
    return false;
  }
  
  @Override
  public void setTabBackground(TabWidget tabWidget) {
    for (int i = 0; i < tabWidget.getChildCount(); i++) {
      tabWidget.getChildAt(i).setBackgroundResource(R.drawable.tab_indicator_mytracks);
    }
  }
}
