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
package com.google.android.apps.mytracks.maps;

import com.google.android.apps.mytracks.Constants;
import com.google.android.maps.mytracks.R;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.test.AndroidTestCase;

/**
 * Tests for the {@link DynamicSpeedTrackPathDescriptor}.
 *
 * @author Youtao Liu
 */
public class FixedSpeedTrackPathDescriptorTest extends AndroidTestCase {
  private Context context;
  private SharedPreferences sharedPreferences;
  private Editor sharedPreferencesEditor;
  private int slowDefault;
  private int normalDefault;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    context = getContext();
    sharedPreferences = context.getSharedPreferences(Constants.SETTINGS_NAME, Context.MODE_PRIVATE);
    sharedPreferencesEditor = sharedPreferences.edit();
    // Get the default value
    slowDefault = 9;
    normalDefault = 15;
  }

  /**
   * Tests the initialization of slowSpeed and normalSpeed in {@link
   * DynamicSpeedTrackPathDescriptor#DynamicSpeedTrackPathDescriptor(Context)}.
   */
  public void testConstructor() {
    String[] slowSpeedsInShPre = { "0", "1", "99", "" };
    int[] slowSpeedExpectations = { 0, 1, 99, slowDefault };
    String[] normalSpeedsInShPre = { "0", "1", "99", "" };
    int[] normalSpeedExpectations = { 0, 1, 99, normalDefault };
    for (int i = 0; i < slowSpeedsInShPre.length; i++) {
      sharedPreferencesEditor.putString(
          context.getString(R.string.track_color_mode_fixed_speed_slow_key), slowSpeedsInShPre[i]);
      sharedPreferencesEditor.putString(
          context.getString(R.string.track_color_mode_fixed_speed_medium_key),
          normalSpeedsInShPre[i]);
      sharedPreferencesEditor.commit();
      FixedSpeedTrackPathDescriptor fixedSpeedTrackPathDescriptor = new FixedSpeedTrackPathDescriptor(
          context);
      assertEquals(slowSpeedExpectations[i], fixedSpeedTrackPathDescriptor.getSlowSpeed());
      assertEquals(normalSpeedExpectations[i], fixedSpeedTrackPathDescriptor.getNormalSpeed());
    }
  }

  /**
   * Tests {@link
   * DynamicSpeedTrackPathDescriptor#onSharedPreferenceChanged(SharedPreferences,
   * String)} when the key is null.
   */
  public void testOnSharedPreferenceChanged_null_key() {
    FixedSpeedTrackPathDescriptor fixedSpeedTrackPathDescriptor = new FixedSpeedTrackPathDescriptor(
        context);
    int slowSpeed = fixedSpeedTrackPathDescriptor.getSlowSpeed();
    int normalSpeed = fixedSpeedTrackPathDescriptor.getNormalSpeed();
    // Change value in shared preferences
    sharedPreferencesEditor.putString(
        context.getString(R.string.track_color_mode_fixed_speed_slow_key),
        Integer.toString(slowSpeed + 2));
    sharedPreferencesEditor.putString(
        context.getString(R.string.track_color_mode_fixed_speed_medium_key),
        Integer.toString(normalSpeed + 2));
    sharedPreferencesEditor.commit();

    fixedSpeedTrackPathDescriptor.onSharedPreferenceChanged(sharedPreferences, null);
    assertEquals(slowSpeed, fixedSpeedTrackPathDescriptor.getSlowSpeed());
    assertEquals(normalSpeed, fixedSpeedTrackPathDescriptor.getNormalSpeed());
  }

  /**
   * Tests {@link
   * DynamicSpeedTrackPathDescriptor#onSharedPreferenceChanged(SharedPreferences,
   * String)} when the key is not null, and not slowSpeed and not normalSpeed.
   */
  public void testOnSharedPreferenceChanged_other_key() {
    FixedSpeedTrackPathDescriptor fixedSpeedTrackPathDescriptor = new FixedSpeedTrackPathDescriptor(
        context);
    int slowSpeed = fixedSpeedTrackPathDescriptor.getSlowSpeed();
    int normalSpeed = fixedSpeedTrackPathDescriptor.getNormalSpeed();
    // Change value in shared preferences
    sharedPreferencesEditor.putString(
        context.getString(R.string.track_color_mode_fixed_speed_slow_key),
        Integer.toString(slowSpeed + 2));
    sharedPreferencesEditor.putString(
        context.getString(R.string.track_color_mode_fixed_speed_medium_key),
        Integer.toString(normalSpeed + 2));
    sharedPreferencesEditor.commit();
    fixedSpeedTrackPathDescriptor.onSharedPreferenceChanged(sharedPreferences, "anyKey");
    assertEquals(slowSpeed, fixedSpeedTrackPathDescriptor.getSlowSpeed());
    assertEquals(normalSpeed, fixedSpeedTrackPathDescriptor.getNormalSpeed());
  }

  /**
   * Tests {@link
   * DynamicSpeedTrackPathDescriptor#onSharedPreferenceChanged(SharedPreferences,
   * String)} when the key is slowSpeed.
   */
  public void testOnSharedPreferenceChanged_slowSpeedKey() {
    FixedSpeedTrackPathDescriptor fixedSpeedTrackPathDescriptor = new FixedSpeedTrackPathDescriptor(
        context);
    int slowSpeed = fixedSpeedTrackPathDescriptor.getSlowSpeed();
    int normalSpeed = fixedSpeedTrackPathDescriptor.getNormalSpeed();
    // Change value in shared preferences
    sharedPreferencesEditor.putString(
        context.getString(R.string.track_color_mode_fixed_speed_slow_key),
        Integer.toString(slowSpeed + 2));
    sharedPreferencesEditor.putString(
        context.getString(R.string.track_color_mode_fixed_speed_medium_key),
        Integer.toString(normalSpeed + 2));
    sharedPreferencesEditor.commit();
    fixedSpeedTrackPathDescriptor.onSharedPreferenceChanged(sharedPreferences,
        context.getString(R.string.track_color_mode_fixed_speed_slow_key));
    assertEquals(slowSpeed + 2, fixedSpeedTrackPathDescriptor.getSlowSpeed());
    assertEquals(normalSpeed + 2, fixedSpeedTrackPathDescriptor.getNormalSpeed());
  }

  /**
   * Tests {@link
   * DynamicSpeedTrackPathDescriptor#onSharedPreferenceChanged(SharedPreferences,
   * String)} when the key is normalSpeed.
   */
  public void testOnSharedPreferenceChanged_normalSpeedKey() {
    FixedSpeedTrackPathDescriptor fixedSpeedTrackPathDescriptor = new FixedSpeedTrackPathDescriptor(
        context);
    int slowSpeed = fixedSpeedTrackPathDescriptor.getSlowSpeed();
    int normalSpeed = fixedSpeedTrackPathDescriptor.getNormalSpeed();
    sharedPreferencesEditor.putString(
        context.getString(R.string.track_color_mode_fixed_speed_slow_key),
        Integer.toString(slowSpeed + 4));
    sharedPreferencesEditor.putString(
        context.getString(R.string.track_color_mode_fixed_speed_medium_key),
        Integer.toString(normalSpeed + 4));
    sharedPreferencesEditor.commit();

    fixedSpeedTrackPathDescriptor.onSharedPreferenceChanged(sharedPreferences,
        context.getString(R.string.track_color_mode_fixed_speed_medium_key));
    assertEquals(slowSpeed + 4, fixedSpeedTrackPathDescriptor.getSlowSpeed());
    assertEquals(normalSpeed + 4, fixedSpeedTrackPathDescriptor.getNormalSpeed());
  }

  /**
   * Tests {@link
   * DynamicSpeedTrackPathDescriptor#onSharedPreferenceChanged(SharedPreferences,
   * String)} when the values of slowSpeed and normalSpeed in SharedPreference
   * is "". In such situation, the default value should get returned.
   */
  public void testOnSharedPreferenceChanged_emptyValue() {
    FixedSpeedTrackPathDescriptor fixedSpeedTrackPathDescriptor = new FixedSpeedTrackPathDescriptor(
        context);
    sharedPreferencesEditor.putString(
        context.getString(R.string.track_color_mode_fixed_speed_slow_key), "");
    sharedPreferencesEditor.putString(
        context.getString(R.string.track_color_mode_fixed_speed_medium_key), "");
    sharedPreferencesEditor.commit();

    fixedSpeedTrackPathDescriptor.onSharedPreferenceChanged(sharedPreferences,
        context.getString(R.string.track_color_mode_fixed_speed_medium_key));
    assertEquals(slowDefault, fixedSpeedTrackPathDescriptor.getSlowSpeed());
    assertEquals(normalDefault, fixedSpeedTrackPathDescriptor.getNormalSpeed());
  }
}