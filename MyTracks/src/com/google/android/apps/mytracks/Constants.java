/*
 * Copyright 2008 Google Inc.
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

/**
 * Common constants.
 * 
 * @author Leif Hendrik Wilden
 */
public class Constants {

  private Constants() {}

  /**
   * The google account type.
   */
  public static final String ACCOUNT_TYPE = "com.google";

  /**
   * Maximum number of track points displayed by the map overlay. Set to 2X of
   * {@link Constants#TARGET_DISPLAYED_TRACK_POINTS}
   */
  public static final int MAX_DISPLAYED_TRACK_POINTS = 10000;

  /**
   * Maximum number of waypoints displayed by the map overlay.
   */
  public static final int MAX_DISPLAYED_WAYPOINTS_POINTS = 128;

  /**
   * Maximum number of track points that will be loaded at one time. With
   * recording frequency of 2 seconds, 20000 corresponds to 11.1 hours.
   */
  public static final int MAX_LOADED_TRACK_POINTS = 20000;

  /**
   * Maximum number of waypoints that will be loaded at one time.
   */
  public static final int MAX_LOADED_WAYPOINTS_POINTS = 10000;

  /**
   * The settings file name.
   */
  public static final String SETTINGS_NAME = "SettingsActivity";

  /**
   * The log tag.
   */
  public static final String TAG = "MyTracks";

  /**
   * Target number of track points displayed by the map overlay. We may display
   * more than this number of points.
   */
  public static final int TARGET_DISPLAYED_TRACK_POINTS = 5000;
}
