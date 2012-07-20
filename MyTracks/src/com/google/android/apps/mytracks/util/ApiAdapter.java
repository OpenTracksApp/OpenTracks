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
package com.google.android.apps.mytracks.util;

import com.google.android.apps.mytracks.ContextualActionModeCallback;
import com.google.android.apps.mytracks.io.backup.BackupPreferencesListener;
import com.google.android.apps.mytracks.services.tasks.PeriodicTask;
import com.google.api.client.http.HttpTransport;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.io.IOException;
import java.util.List;

/**
 * A set of methods that may be implemented differently depending on the Android
 * API level.
 *
 * @author Bartlomiej Niechwiej
 */
public interface ApiAdapter {

  /**
   * Gets a status announcer task.
   * <p>
   * Due to changes in API level 8.
   * 
   * @param context the context
   */
  public PeriodicTask getStatusAnnouncerTask(Context context);

  /**
   * Gets a {@link BackupPreferencesListener}.
   * <p>
   * Due to changes in API level 8.
   * 
   * @param context the context
   */
  public BackupPreferencesListener getBackupPreferencesListener(Context context);

  /**
   * Applies all the changes done to a given preferences editor. Changes may or
   * may not be applied immediately.
   * <p>
   * Due to changes in API level 9.
   * 
   * @param editor the editor
   */
  public void applyPreferenceChanges(SharedPreferences.Editor editor);

  /**
   * Enables strict mode where supported, only if this is a development build.
   * <p>
   * Due to changes in API level 9.
   */
  public void enableStrictMode();

  /**
   * Copies elements from an input byte array into a new byte array, from
   * indexes start (inclusive) to end (exclusive). The end index must be less
   * than or equal to the input length.
   * <p>
   * Due to changes in API level 9.
   *
   * @param input the input byte array
   * @param start the start index
   * @param end the end index
   * @return a new array containing elements from the input byte array.
   */
  public byte[] copyByteArray(byte[] input, int start, int end);

  /**
   * Gets a {@link HttpTransport}.
   * <p>
   * Due to changes in API level 9.
   */
  public HttpTransport getHttpTransport();

  /**
   * Returns true if GeoCoder is present.
   * <p>
   * Due to changes in API level 9.
   */
  public boolean isGeoCoderPresent();

  /**
   * Gets a {@link BluetoothSocket}.
   * <p>
   * Due to changes in API level 10.
   *
   * @param bluetoothDevice
   */
  public BluetoothSocket getBluetoothSocket(BluetoothDevice bluetoothDevice) throws IOException;

  /**
   * Hides the title. If the platform supports the action bar, do nothing.
   * Ideally, with the action bar, we would like to collapse the navigation tabs
   * into the action bar. However, collapsing is not supported by the
   * compatibility library.
   * <p>
   * Due to changes in API level 11.
   * 
   * @param activity the activity
   */
  public void hideTitle(Activity activity);

  /**
   * Configures the action bar with the Home button as an Up button. If the
   * platform doesn't support the action bar, do nothing.
   * <p>
   * Due to changes in API level 11.
   *
   * @param activity the activity
   */
  public void configureActionBarHomeAsUp(Activity activity);

  /**
   * Configures the list view context menu.
   * <p>
   * Due to changes in API level 11.
   *
   * @param activity the activity
   * @param listView the list view
   * @param contextualActionModeCallback the callback when an item is selected
   *          in the contextual action mode
   */
  public void configureListViewContextualMenu(Activity activity, ListView listView,
      ContextualActionModeCallback contextualActionModeCallback);

  /**
   * Configures the search widget.
   * <p>
   * Due to changes in API level 11.
   * 
   * @param activity the activity
   * @param menuItem the search menu item
   */
  public void configureSearchWidget(Activity activity, MenuItem menuItem);
 
  /**
   * Handles the search menu selection. Returns true if handled.
   * <p>
   * Due to changes in API level 11.
   * 
   * @param activity the activity
   */
  public boolean handleSearchMenuSelection(Activity activity);
  
  /**
   * Adds all items to an array adapter.
   * <p>
   * Due to changes in API level 11.
   *
   * @param arrayAdapter the array adapter
   * @param items list of items
   */
  public <T> void addAllToArrayAdapter(ArrayAdapter<T> arrayAdapter, List<T> items);

  /**
   * Invalidates the menu.
   * <p>
   * Due to changes in API level 11.
   */
  public void invalidMenu(Activity activity);

  /**
   * Diables hardware-accelerated rendering for a view.
   * <p>
   * Due to chagnes in API level 11.
   * 
   * @param view the view
   */
  public void disableHardwareAccelerated(View view);

  /**
   * Handles the search key press. Returns true if handled.
   * <p>
   * Due to changes in API level 14.
   * 
   * @param menu the search menu
   */
  public boolean handleSearchKey(MenuItem menu);  
}
