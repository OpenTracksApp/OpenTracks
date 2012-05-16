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
package com.google.android.apps.mytracks.endtoendtest;

import com.google.android.apps.mytracks.TrackListActivity;
import com.google.android.apps.mytracks.util.FileUtils;
import com.google.android.maps.mytracks.R;
import com.jayway.android.robotium.solo.Solo;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.res.Configuration;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;

/**
 * Provides utilities to smoke test.
 * 
 * @author Youtao Liu
 */
public class EndToEndTestUtils {
  private static final String ANDROID_LOCAL_IP = "10.0.2.2";
  // usually 5554.
  private static final int ANDROID_LOCAL_PORT = 5554;

  private static final int ORIENTATION_PORTRAIT = 1;
  private static final int ORIENTATION_LANDSCAPE = 0;
  // Pause 200ms between each send.
  static int PAUSE = 200;
  private static final float START_LONGITUDE = 51;
  private static final float START_LATITUDE = -1.3f;
  private static final float DELTA_LONGITUDE = 0.0005f;
  private static final float DELTA_LADITUDE = 0.0005f;
  
  private static final String MOREOPTION_CLASSNAME = "com.android.internal.view.menu.ActionMenuPresenter$OverflowMenuButton";

  static final String TRACK_NAME_PREFIX = "testTrackName";
  static final String GPX = "gpx";
  static final String KML = "kml";
  static final String CSV = "csv";
  static final String TCX = "tcx";
  static final String BACKUPS = "backups";
  static final String MENU_MORE = "More";
  static String TRACK_NAME;

  static Solo SOLO;
  static Instrumentation INSTRUMENTATION;
  static TrackListActivity ACTIVITYMYTRACKS;
  // Check whether the UI has an action bar which is related with the version of
  // Android OS.
  static boolean HAS_ACTIONBAR = false;
  static boolean CHECKED_FIRSTLAUNCH = false;

  private EndToEndTestUtils() {}

  /**
   * Sends Gps data to emulator.
   * 
   * @param number send times
   */
  public static void sendGps(int number) {
    if (number < 1) { 
      return; 
    }
    PrintStream out = null;
    Socket socket = null;
    try {
      socket = new Socket(ANDROID_LOCAL_IP, ANDROID_LOCAL_PORT);
      out = new PrintStream(socket.getOutputStream());
      float longitude = START_LONGITUDE;
      float latitude = START_LATITUDE;
      for (int i = 0; i < number; i++) {
        Thread.sleep(PAUSE);
        out.println("geo fix " + latitude + " " + longitude);
        longitude += DELTA_LONGITUDE;
        latitude += DELTA_LADITUDE;
      }
    } catch (UnknownHostException e) {
      System.exit(-1);
    } catch (IOException e) {
      System.exit(-1);
    } catch (InterruptedException e) {
      System.exit(-1);
    } finally {
      if (out != null) {
        out.close();
      }
    }
  }

  /**
   * A setup for all end-to-end tests.
   * 
   * @param instrumentation the instrumentation is used for test
   * @param activityMyTracks the startup activity
   */
  static void setupForAllTest(Instrumentation instrumentation, TrackListActivity activityMyTracks) {
    EndToEndTestUtils.INSTRUMENTATION = instrumentation;
    EndToEndTestUtils.ACTIVITYMYTRACKS = activityMyTracks;
    EndToEndTestUtils.SOLO = new Solo(EndToEndTestUtils.INSTRUMENTATION,
        EndToEndTestUtils.ACTIVITYMYTRACKS);
    // Check if open MyTracks first time after install. If so, there would be a
    // welcome view with accept buttons. And makes sure only check once.
    if (!EndToEndTestUtils.CHECKED_FIRSTLAUNCH) {
      if ((EndToEndTestUtils.getButtonOnScreen(EndToEndTestUtils.ACTIVITYMYTRACKS
          .getString(R.string.eula_accept)) != null)) {
        EndToEndTestUtils.verifyFirstLaunch();
      } else if (EndToEndTestUtils.SOLO.waitForText(
      // After reset setting, welcome page will show again.
          ACTIVITYMYTRACKS.getString(R.string.welcome_title), 0, 500)) {
        resetPreferredUnits();
      }

      EndToEndTestUtils.CHECKED_FIRSTLAUNCH = true;
      EndToEndTestUtils.setHasActionBar();
    } else if (EndToEndTestUtils.SOLO.waitForText(
        // After reset setting, welcome page will show again.
        ACTIVITYMYTRACKS.getString(R.string.welcome_title), 0, 500)) {
      resetPreferredUnits();
    }
    
    // Check whether is under recording. If previous test failed, the recording
    // may not be recording.
    if (isUnderRecording()) {
      stopRecording(true);
    }
  }
  
  /**
   * Checks if need reset preferred units.
   */
  public static void resetPreferredUnits() {
    EndToEndTestUtils.SOLO.clickOnText(ACTIVITYMYTRACKS.getString(R.string.generic_ok));
    SOLO.waitForText(ACTIVITYMYTRACKS.getString(R.string.settings_stats_units_title));
    EndToEndTestUtils.SOLO.clickOnText(ACTIVITYMYTRACKS.getString(R.string.generic_ok));
    INSTRUMENTATION.waitForIdleSync();
  }


  /**
   * Rotates the given activity.
   * 
   * @param activity a given activity
   */
  static void rotateActivity(Activity activity) {
    activity
        .setRequestedOrientation(activity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT ? ORIENTATION_LANDSCAPE
            : ORIENTATION_PORTRAIT);
  }

  /**
   * Accepts terms and configures units.
   */
  static void verifyFirstLaunch() {
    SOLO.clickOnText(ACTIVITYMYTRACKS.getString(R.string.eula_accept));
    // Click for welcome.
    SOLO.clickOnText(ACTIVITYMYTRACKS.getString(R.string.generic_ok));
    // Click for choose units.
    SOLO.clickOnText(ACTIVITYMYTRACKS.getString(R.string.generic_ok));
    INSTRUMENTATION.waitForIdleSync();
  }

  /**
   * Creates a simple track which can be used by subsequent test.
   * 
   * @param numberOfGpsData number of simulated Gps data
   */
  static void createSimpleTrack(int numberOfGpsData) {
    startRecording();
    EndToEndTestUtils.sendGps(numberOfGpsData);
    INSTRUMENTATION.waitForIdleSync();
    stopRecording(true);
  }
  
  /**
   * Checks if there is no track in track list. For some tests need at least one
   * track, the method can save time to create a new track.
   * 
   * @param isClick if not empty, true means click any track
   * @return return true if the track list is empty
   */
  static boolean isTrackListEmpty(boolean isClick) {
    int trackNumber = SOLO.getCurrentListViews().get(0).getCount();
    if(trackNumber <= 0) {
      return true;
    } else {
      View oneTrack = SOLO.getCurrentListViews().get(0).getChildAt(0);
      TRACK_NAME = (String) ((TextView) oneTrack.findViewById(R.id.list_item_name)).getText();
      if (isClick) {
        SOLO.clickOnView(oneTrack);
      }
      return false;
    }
  }

  /**
   * Starts recoding track.
   */
  static void startRecording() {
    if (HAS_ACTIONBAR) {
      Button startButton = getButtonOnScreen(ACTIVITYMYTRACKS.getString(R.string.menu_record_track));
      // In case a track is recording.
      if (startButton == null) {
        stopRecording(true);
        startButton = getButtonOnScreen(ACTIVITYMYTRACKS.getString(R.string.menu_record_track));
      }
      SOLO.clickOnView(startButton);
    } else {
      showMoreMenuItem();
      if (!SOLO.searchText(ACTIVITYMYTRACKS.getString(R.string.menu_record_track))) {
        // Check if in TrackDetailActivity.
        if (SOLO.searchText(ACTIVITYMYTRACKS.getString(R.string.menu_play))) {
          SOLO.goBack();
        } else {
          // In case a track is recording.
          stopRecording(true);
          showMoreMenuItem();
        }
      }
      INSTRUMENTATION.waitForIdleSync();
      SOLO.clickOnText(ACTIVITYMYTRACKS.getString(R.string.menu_record_track));
    }
  }
  
  /**
   * Checks if the MyTracks is under recording.
   * 
   * @return true if it is under recording.
   */
  static boolean isUnderRecording() {
    if (HAS_ACTIONBAR) {
      Button startButton = getButtonOnScreen(ACTIVITYMYTRACKS.getString(R.string.menu_record_track));
      // In case a track is recording.
      if (startButton == null) {
        return true;
      }
    } else {
      showMoreMenuItem();
      if (!SOLO.searchText(ACTIVITYMYTRACKS.getString(R.string.menu_record_track))) {
        // Check if in TrackDetailActivity.
        if (SOLO.searchText(ACTIVITYMYTRACKS.getString(R.string.menu_play))) {
          SOLO.goBack();
        } else {
          return true;
        }
      } else {
        SOLO.goBack();
      }
    }
    return false;
  }

  /**
   * Stops recoding track.
   * 
   * @param isSave ture means should save this track
   */
  static void stopRecording(boolean isSave) {
    if (HAS_ACTIONBAR) {
      SOLO.clickOnView(getButtonOnScreen(ACTIVITYMYTRACKS.getString(R.string.menu_stop_recording)));
    } else {
      showMoreMenuItem();
      INSTRUMENTATION.waitForIdleSync();
      SOLO.clickOnText(ACTIVITYMYTRACKS.getString(R.string.menu_stop_recording));
    }
    if (isSave) {
      INSTRUMENTATION.waitForIdleSync();
      // Make every track name is unique to make sure every check can be
      // trusted.
      EndToEndTestUtils.TRACK_NAME = EndToEndTestUtils.TRACK_NAME_PREFIX
          + System.currentTimeMillis();
      SOLO.enterText(0, TRACK_NAME);
      SOLO.clickLongOnText(ACTIVITYMYTRACKS.getString(R.string.generic_save));
    }
  }

  /**
   * Deletes a kind of track in MyTracks folder.
   * 
   * @param trackKind the kind of track
   */
  static void deleteExportedFiles(String trackKind) {
    File[] allFiles = (new File(FileUtils.buildExternalDirectoryPath(trackKind.toLowerCase())))
        .listFiles();
    if (allFiles != null) {
      for (File oneFile : allFiles) {
        oneFile.delete();
      }
    }
  }

  /**
   * Gets a kind of exported files.
   * 
   * @param trackKind the kind of track
   * @return files array of such kind of exported tracks
   */
  static File[] getExportedFiles(final String trackKind) {
    String filePath = FileUtils.buildExternalDirectoryPath(trackKind);
    FileFilter filter = new FileFilter() {
      @Override
      public boolean accept(File pathname) {
        return pathname.getName().indexOf("." + trackKind) > 0;
      }
    };
    return (new File(filePath)).listFiles(filter);
  }

  /**
   * Saves a track to a kind of file in SD card.
   * 
   * @param trackKind the kind of track
   */
  static void saveTrackToSdCard(String trackKind) {
    deleteExportedFiles(trackKind);
    INSTRUMENTATION.waitForIdleSync();
    findMenuItem(ACTIVITYMYTRACKS.getString(R.string.menu_save), true, true);
    INSTRUMENTATION.waitForIdleSync();
    SOLO.clickOnText(trackKind.toUpperCase());
    rotateAllActivities();
    SOLO.waitForText(ACTIVITYMYTRACKS.getString(R.string.generic_success_title));
  }

  /**
   * Checks if a button is existed in the screen.
   * 
   * @param buttonName the name string of the button
   * @return the button to search, and null means can not find the button
   */
  static Button getButtonOnScreen(String buttonName) {
    ArrayList<Button> currentButtons = SOLO.getCurrentButtons();
    for (Button button : currentButtons) {
      String title = (String) button.getText();
      if (title.equalsIgnoreCase(buttonName)) { 
        return button; 
      }
    }
    return null;
  }

  /**
   * Checks whether an action bar is shown.
   * 
   * @return false means can not check failed.
   */
  static boolean setHasActionBar() {
    INSTRUMENTATION.waitForIdleSync();
    // If can find record button without pressing Menu, it should be an action
    // bar.
    Button startButton = getButtonOnScreen(ACTIVITYMYTRACKS.getString(R.string.menu_record_track));
    Button stopButton = getButtonOnScreen(ACTIVITYMYTRACKS.getString(R.string.menu_stop_recording));
    if (startButton != null || stopButton != null) {
      HAS_ACTIONBAR = true;
    } else {
      showMoreMenuItem();
      if (SOLO.searchText(ACTIVITYMYTRACKS.getString(R.string.menu_record_track))
          || SOLO.searchText(ACTIVITYMYTRACKS.getString(R.string.menu_stop_recording))) {
        HAS_ACTIONBAR = false;
      } else {
        return false;
      }
      SOLO.goBack();
    }
    return true;
  }

  /**
   * Rotates the current activity.
   */
  static void rotateCurrentActivity() {
    EndToEndTestUtils.rotateActivity(SOLO.getCurrentActivity());
    INSTRUMENTATION.waitForIdleSync();
  }

  /**
   * Rotates all activities.
   */
  static void rotateAllActivities() {
    ArrayList<Activity> allActivities = SOLO.getAllOpenedActivities();
    for (Activity activity : allActivities) {
      EndToEndTestUtils.rotateActivity(activity);
    }

    INSTRUMENTATION.waitForIdleSync();
  }

  /**
   * Finds an item in the menu with the option to click the item.
   * 
   * @param menuName the name of item
   * @param click true means need click this menu
   * @param checkActionBar whether this item may on the action bar
   * @return true if find this menu
   */
  static boolean findMenuItem(String menuName, boolean click, boolean checkActionBar) {
    boolean findResult = false;

    // Firstly find in action bar.
    if (checkActionBar) {
      Button button = getButtonOnScreen(menuName);
      if (button != null) {
        findResult = true;
        if (click) {
          SOLO.clickOnView(button);
        }
        return findResult;
      }
    }

    showMoreMenuItem();
    if (SOLO.searchText(menuName)) {
      findResult = true;
    } else if (SOLO.searchText(MENU_MORE)) {
      SOLO.clickOnText(MENU_MORE);
      findResult = SOLO.searchText(menuName);
    }

    if (findResult && click) {
      SOLO.clickOnText(menuName);
    } else {
      SOLO.goBack();
    }
    return findResult;
  }

  /**
   * Get more menu items operation is different for different Android OS.
   */
  public static void showMoreMenuItem() {
    if (HAS_ACTIONBAR) {
      View moreButton = getMoreOptionView();
      if (moreButton == null) {
        SOLO.sendKey(KeyEvent.KEYCODE_MENU);
      } else {
        SOLO.clickOnView(moreButton);
      }

    } else {
      SOLO.sendKey(KeyEvent.KEYCODE_MENU);
    }
  }
  
  /**
   * Finds the more option menu. It should match following conditions:
   * <ul>
   * <li>The view extends ImageButton.</li>
   * <li>The view name equals {@link EndToEndTestUtils#MOREOPTION_CLASSNAME}.</li>
   * </ul>
   * @return the more option view. Null means can not find it.
   */
  private static View getMoreOptionView() {
    ArrayList<View> allViews = SOLO.getViews();
    for (View view : allViews) {
      if (view instanceof ImageButton && view.getClass().getName().equals(MOREOPTION_CLASSNAME)) { return view; }
    }
    return null;
  }
}
