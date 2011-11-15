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
 * Constants used by the MyTracks application.
 *
 * @author Leif Hendrik Wilden
 */
public abstract class Constants {

  /**
   * Should be used by all log statements
   */
  public static final String TAG = "MyTracks";

  /**
   * Name of the top-level directory inside the SD card where our files will
   * be read from/written to.
   */
  public static final String SDCARD_TOP_DIR = "MyTracks";

  /*
   * onActivityResult request codes:
   */

  public static final int GET_LOGIN = 0;
  public static final int GET_MAP = 1;
  public static final int SHOW_TRACK = 2;
  public static final int AUTHENTICATE_TO_MY_MAPS = 3;
  public static final int AUTHENTICATE_TO_FUSION_TABLES = 4;
  public static final int AUTHENTICATE_TO_DOCLIST = 5;
  public static final int AUTHENTICATE_TO_TRIX = 6;
  public static final int SHARE_GPX_FILE = 7;
  public static final int SHARE_KML_FILE = 8;
  public static final int SHARE_CSV_FILE = 9;
  public static final int SHARE_TCX_FILE = 10;
  public static final int SAVE_GPX_FILE = 11;
  public static final int SAVE_KML_FILE = 12;
  public static final int SAVE_CSV_FILE = 13;
  public static final int SAVE_TCX_FILE = 14;
  public static final int SHOW_WAYPOINT = 15;
  public static final int WELCOME = 16;

  /*
   * Menu ids:
   */

  public static final int MENU_MY_LOCATION = 1;
  public static final int MENU_TOGGLE_LAYERS = 2;
  public static final int MENU_CHART_SETTINGS = 3;

  /*
   * Context menu ids:
   */

  public static final int MENU_EDIT = 100;
  public static final int MENU_DELETE = 101;
  public static final int MENU_SEND_TO_GOOGLE = 102;
  public static final int MENU_SHARE = 103;
  public static final int MENU_SHOW = 104;
  public static final int MENU_SHARE_LINK = 200;
  public static final int MENU_SHARE_GPX_FILE = 201;
  public static final int MENU_SHARE_KML_FILE = 202;
  public static final int MENU_SHARE_CSV_FILE = 203;
  public static final int MENU_SHARE_TCX_FILE = 204;
  public static final int MENU_WRITE_TO_SD_CARD = 205;
  public static final int MENU_SAVE_GPX_FILE = 206;
  public static final int MENU_SAVE_KML_FILE = 207;
  public static final int MENU_SAVE_CSV_FILE = 208;
  public static final int MENU_SAVE_TCX_FILE = 209;
  public static final int MENU_CLEAR_MAP = 210;

  /**
   * The number of distance readings to smooth to get a stable signal.
   */
  public static final int DISTANCE_SMOOTHING_FACTOR = 25;

  /**
   * The number of elevation readings to smooth to get a somewhat accurate
   * signal.
   */
  public static final int ELEVATION_SMOOTHING_FACTOR = 25;

  /**
   * The number of grade readings to smooth to get a somewhat accurate signal.
   */
  public static final int GRADE_SMOOTHING_FACTOR = 5;

  /**
   * The number of speed reading to smooth to get a somewhat accurate signal.
   */
  public static final int SPEED_SMOOTHING_FACTOR = 25;

  /**
   * Maximum number of track points displayed by the map overlay.
   */
  public static final int MAX_DISPLAYED_TRACK_POINTS = 10000;

  /**
   * Target number of track points displayed by the map overlay.
   * We may display more than this number of points.
   */
  public static final int TARGET_DISPLAYED_TRACK_POINTS = 5000;

  /**
   * Maximum number of track points ever loaded at once from the provider into
   * memory.
   * With a recording frequency of 2 seconds, 15000 corresponds to 8.3 hours.
   */
  public static final int MAX_LOADED_TRACK_POINTS = 20000;

  /**
   * Maximum number of track points ever loaded at once from the provider into
   * memory in a single call to read points.
   */
  public static final int MAX_LOADED_TRACK_POINTS_PER_BATCH = 1000;

  /**
   * Maximum number of way points displayed by the map overlay.
   */
  public static final int MAX_DISPLAYED_WAYPOINTS_POINTS = 128;

  /**
   * Maximum number of way points that will be loaded at one time.
   */
  public static final int MAX_LOADED_WAYPOINTS_POINTS = 10000;

  /**
   * Any time segment where the distance traveled is less than this value will
   * not be considered moving.
   */
  public static final double MAX_NO_MOVEMENT_DISTANCE = 2;

  /**
   * Anything faster than that (in meters per second) will be considered moving.
   */
  public static final double MAX_NO_MOVEMENT_SPEED = 0.224;

  /**
   * Ignore any acceleration faster than this.
   * Will ignore any speeds that imply accelaration greater than 2g's
   * 2g = 19.6 m/s^2 = 0.0002 m/ms^2 = 0.02 m/(m*ms)
   */
  public static final double MAX_ACCELERATION = 0.02;

  /** Maximum age of a GPS location to be considered current. */
  public static final long MAX_LOCATION_AGE_MS = 60 * 1000;  // 1 minute

  /** Maximum age of a network location to be considered current. */
  public static final long MAX_NETWORK_AGE_MS = 1000 * 60 * 10;  // 10 minutes

  /**
   * The type of account that we can use for gdata uploads.
   */
  public static final String ACCOUNT_TYPE = "com.google";

  /**
   * The name of extra intent property to indicate whether we want to resume
   * a previously recorded track.
   */
  public static final String RESUME_TRACK_EXTRA_NAME =
      "com.google.android.apps.mytracks.RESUME_TRACK";

  public static int getActionFromMenuId(int menuId) {
    switch (menuId) {
      case Constants.MENU_SHARE_KML_FILE:
        return Constants.SHARE_KML_FILE;
      case Constants.MENU_SHARE_GPX_FILE:
        return Constants.SHARE_GPX_FILE;
      case Constants.MENU_SHARE_CSV_FILE:
        return Constants.SHARE_CSV_FILE;
      case Constants.MENU_SHARE_TCX_FILE:
        return Constants.SHARE_TCX_FILE;
      case Constants.MENU_SAVE_GPX_FILE:
        return Constants.SAVE_GPX_FILE;
      case Constants.MENU_SAVE_KML_FILE:
        return Constants.SAVE_KML_FILE;
      case Constants.MENU_SAVE_CSV_FILE:
        return Constants.SAVE_CSV_FILE;
      case Constants.MENU_SAVE_TCX_FILE:
        return Constants.SAVE_TCX_FILE;
      default:
        return -1;
    }
  }

  public static final String MAPSHOP_BASE_URL =
    "https://maps.google.com/maps/ms";

  /*
   * Default values - keep in sync with those in preferences.xml.
   */

  public static final int DEFAULT_ANNOUNCEMENT_FREQUENCY = -1;
  public static final int DEFAULT_AUTO_RESUME_TRACK_TIMEOUT = 10;  // In min.
  public static final int DEFAULT_MAX_RECORDING_DISTANCE = 200;
  public static final int DEFAULT_MIN_RECORDING_DISTANCE = 5;
  public static final int DEFAULT_MIN_RECORDING_INTERVAL = 0;
  public static final int DEFAULT_MIN_REQUIRED_ACCURACY = 200;
  public static final int DEFAULT_SPLIT_FREQUENCY = 0;

  public static final String SETTINGS_NAME = "SettingsActivity";

  /**
   * This is an abstract utility class.
   */
  protected Constants() { }
}
