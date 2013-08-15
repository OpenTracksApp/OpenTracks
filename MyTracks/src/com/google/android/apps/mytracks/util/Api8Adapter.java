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

import com.google.android.apps.mytracks.Constants;
import com.google.android.apps.mytracks.ContextualActionModeCallback;
import com.google.android.apps.mytracks.TrackController;
import com.google.android.apps.mytracks.services.sensors.BluetoothConnectionManager;
import com.google.android.apps.mytracks.widgets.TrackWidgetProvider;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.apache.ApacheHttpTransport;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.SharedPreferences.Editor;
import android.util.Log;
import android.view.MenuItem;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TabWidget;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

/**
 * API level 8 specific implementation of the {@link ApiAdapter}.
 *
 * @author Bartlomiej Niechwiej
 */
public class Api8Adapter implements ApiAdapter {

  @Override
  public void applyPreferenceChanges(Editor editor) {
    editor.commit();
  }

  @Override
  public void enableStrictMode() {
    // Not supported
  }

  @Override
  public byte[] copyByteArray(byte[] input, int start, int end) {
    int length = end - start;
    byte[] output = new byte[length];
    System.arraycopy(input, start, output, 0, length);
    return output;
  }

  @Override
  public HttpTransport getHttpTransport() {
    return new ApacheHttpTransport();
  }

  @Override
  public boolean isGeoCoderPresent() {
    return true;
  }

  @Override
  public BluetoothSocket getBluetoothSocket(BluetoothDevice bluetoothDevice) throws IOException {
    try {
      Class<? extends BluetoothDevice> c = bluetoothDevice.getClass();
      Method insecure = c.getMethod("createInsecureRfcommSocket", Integer.class);
      insecure.setAccessible(true);
      return (BluetoothSocket) insecure.invoke(bluetoothDevice, 1);
    } catch (SecurityException e) {
      Log.d(Constants.TAG, "Unable to create insecure connection", e);
    } catch (NoSuchMethodException e) {
      Log.d(Constants.TAG, "Unable to create insecure connection", e);
    } catch (IllegalArgumentException e) {
      Log.d(Constants.TAG, "Unable to create insecure connection", e);
    } catch (IllegalAccessException e) {
      Log.d(Constants.TAG, "Unable to create insecure connection", e);
    } catch (InvocationTargetException e) {
      Log.d(Constants.TAG, "Unable to create insecure connection", e);
    }
    return bluetoothDevice.createRfcommSocketToServiceRecord(BluetoothConnectionManager.MY_TRACKS_UUID);
  }

  @Override
  public void hideTitle(Activity activity) {
    activity.requestWindowFeature(Window.FEATURE_NO_TITLE);
  }

  @Override
  public void configureActionBarHomeAsUp(Activity activity) {
    // Do nothing
  }

  @Override
  public void configureListViewContextualMenu(Activity activity, ListView listView,
      ContextualActionModeCallback contextualActionModeCallback) {
    activity.registerForContextMenu(listView);
  }

  @Override
  public void configureSearchWidget(
      Activity activity, MenuItem menuItem, TrackController trackController) {
    // Do nothing
  }

  @Override
  public boolean handleSearchMenuSelection(Activity activity) {
    activity.onSearchRequested();
    return true;
  }

  @Override
  public <T> void addAllToArrayAdapter(ArrayAdapter<T> arrayAdapter, List<T> items) {
    for (T item : items) {
      arrayAdapter.add(item);
    }
  }

  @Override
  public void invalidMenu(Activity activity) {
    // Do nothing
  }

  @Override
  public boolean isSpinnerBackgroundLight() {
    return true;    
  }
  
  @Override
  public void setTabBackground(TabWidget tabWidget) {
    // Do nothing    
  }
  
  @Override
  public boolean handleSearchKey(MenuItem menuItem) {
    // Return false and allow the framework to handle the search key.
    return false;
  }
  
  @Override
  public boolean isGoogleFeedbackAvailable() {
    return false;
  }

  @Override
  public int getAppWidgetSize(AppWidgetManager appWidgetManager, int appWidgetId) {
    return TrackWidgetProvider.HOME_SCREEN_DEFAULT_SIZE;
  }

  @Override
  public void setAppWidgetSize(AppWidgetManager appWidgetManager, int appWidgetId, int size) {
    // Do nothing    
  }

  @SuppressWarnings("deprecation")
  @Override
  public void removeGlobalLayoutListener(
      ViewTreeObserver observer, OnGlobalLayoutListener listener) {
    observer.removeGlobalOnLayoutListener(listener);    
  }
}
