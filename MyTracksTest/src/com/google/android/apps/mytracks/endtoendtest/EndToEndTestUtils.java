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

import com.google.android.apps.mytracks.ChartView;
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
import java.util.HashMap;

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

  static final String DEFAULTACTIVITY = "TestActivity";
  static final String TRACK_NAME_PREFIX = "testTrackName";
  static final String GPX = "gpx";
  static final String KML = "kml";
  static final String CSV = "csv";
  static final String TCX = "tcx";
  static final String BACKUPS = "backups";
  static final String MENU_MORE = "More";
  static String trackName;
  
  // Following is some check strings in English and Chinese
  private static final HashMap<String, String> RELATIVE_STARTTIME_POSTFIX_MULTILINGUAL = new HashMap<String, String>(); 
  private static final HashMap<String, String> VIEW_MODE_ENGLISH_MULTILINGUAL = new HashMap<String, String>(); 
  public static String RELATIVE_STARTTIME_POSTFIX = "";
  public static String VIEW_MODE = "";
  static {
    RELATIVE_STARTTIME_POSTFIX_MULTILINGUAL.put("es", "mins ago");
    RELATIVE_STARTTIME_POSTFIX_MULTILINGUAL.put("de", "Minuten");
    RELATIVE_STARTTIME_POSTFIX_MULTILINGUAL.put("fr", "minute");
    RELATIVE_STARTTIME_POSTFIX_MULTILINGUAL.put("ar", "دقيقة");
    RELATIVE_STARTTIME_POSTFIX_MULTILINGUAL.put("zh", "分钟前");
    
    VIEW_MODE_ENGLISH_MULTILINGUAL.put("es", "mode");
    VIEW_MODE_ENGLISH_MULTILINGUAL.put("de", "modus");
    VIEW_MODE_ENGLISH_MULTILINGUAL.put("fr", "Mode");
    VIEW_MODE_ENGLISH_MULTILINGUAL.put("ar", "وضع");
    VIEW_MODE_ENGLISH_MULTILINGUAL.put("zh", "模式");
  }

  static Solo SOLO;
  static Instrumentation instrumentation;
  static TrackListActivity activityMytracks;
  // Check whether the UI has an action bar which is related with the version of
  // Android OS.
  static boolean hasActionBar = false;
  static boolean isEmulator = true;
  static boolean isCheckedFirstLaunch = false;

  private EndToEndTestUtils() {}
  
  /**
   * Checks the language, then sets the fields with right string.
   */
  private static void checkLanguage() {
    String deviceLanguage = instrumentation.getContext().getResources().getConfiguration().locale.getLanguage();
    if (RELATIVE_STARTTIME_POSTFIX_MULTILINGUAL.get(deviceLanguage) != null) {
      RELATIVE_STARTTIME_POSTFIX = RELATIVE_STARTTIME_POSTFIX_MULTILINGUAL.get(deviceLanguage);
      VIEW_MODE = VIEW_MODE_ENGLISH_MULTILINGUAL.get(deviceLanguage);
    } else {
      RELATIVE_STARTTIME_POSTFIX = RELATIVE_STARTTIME_POSTFIX_MULTILINGUAL.get("es");
      VIEW_MODE = VIEW_MODE_ENGLISH_MULTILINGUAL.get("es");
    }
  }

  /**
   * Sends Gps data to emulator.
   * 
   * @param number send times
   */
  public static void sendGps(int number) {
    if (number < 1) { 
      return; 
    }
    // If it's a real device, does not send simulated GPS signal.
    if (!isEmulator) {
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
   * Sets the status whether the test is run on an emulator or not.
   */
  static void setIsEmulator() {
    isEmulator = android.os.Build.MODEL.equals("google_sdk");
  }

  /**
   * A setup for all end-to-end tests.
   * 
   * @param instrumentation the instrumentation is used for test
   * @param activityMyTracks the startup activity
   */
  static void setupForAllTest(Instrumentation instrumentation, TrackListActivity activityMyTracks) {
    setIsEmulator();
    EndToEndTestUtils.instrumentation = instrumentation;
    EndToEndTestUtils.activityMytracks = activityMyTracks;
    EndToEndTestUtils.SOLO = new Solo(EndToEndTestUtils.instrumentation,
        EndToEndTestUtils.activityMytracks);
    // Check if open MyTracks first time after install. If so, there would be a
    // welcome view with accept buttons. And makes sure only check once.
    if (!EndToEndTestUtils.isCheckedFirstLaunch) {
      if ((EndToEndTestUtils.getButtonOnScreen(EndToEndTestUtils.activityMytracks
          .getString(R.string.eula_accept), false, false) != null)) {
        EndToEndTestUtils.verifyFirstLaunch();
      } else if (EndToEndTestUtils.SOLO.waitForText(
      // After reset setting, welcome page will show again.
          activityMytracks.getString(R.string.welcome_title), 0, 500)) {
        resetPreferredUnits();
      }
      checkLanguage();
      EndToEndTestUtils.isCheckedFirstLaunch = true;
      EndToEndTestUtils.setHasActionBar();
    } else if (EndToEndTestUtils.SOLO.waitForText(
        // After reset setting, welcome page will show again.
        activityMytracks.getString(R.string.welcome_title), 0, 500)) {
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
  private static void resetPreferredUnits() {
    getButtonOnScreen(activityMytracks.getString(R.string.generic_ok), true, true);
    SOLO.waitForText(activityMytracks.getString(R.string.settings_stats_units_title));
    getButtonOnScreen(activityMytracks.getString(R.string.generic_ok), true, true);
    instrumentation.waitForIdleSync();
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
    getButtonOnScreen(activityMytracks.getString(R.string.eula_accept), true, true);
    if (SOLO.waitForText(activityMytracks.getString(R.string.generic_ok))) {
      // Click for welcome.
      getButtonOnScreen(activityMytracks.getString(R.string.generic_ok), true, true);
      // Click for choose units.
      getButtonOnScreen(activityMytracks.getString(R.string.generic_ok), true, true);
      instrumentation.waitForIdleSync();
    }
  }

  /**
   * Creates a simple track which can be used by subsequent test. This method
   * will save a customized track name.
   * 
   * @param numberOfGpsData number of simulated Gps data
   */
  static void createSimpleTrack(int numberOfGpsData) {
    startRecording();
    EndToEndTestUtils.sendGps(numberOfGpsData);
    instrumentation.waitForIdleSync();
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
    } 

    View oneTrack = SOLO.getCurrentListViews().get(0).getChildAt(0);
    trackName = (String) ((TextView) oneTrack.findViewById(R.id.list_item_name)).getText();
    if (isClick) {
      SOLO.scrollUp();
      SOLO.clickOnView(oneTrack);
    }
    return false;
  }
  
  /**
   * Create a new track if the track is empty.
   * 
   * @param gpsNumber the number of gps signals
   * @param showTrackList whether stay on track list activity or track detail
   *          activity
   */
  static void createTrackIfEmpty(int gpsNumber, boolean showTrackList) {
    if (EndToEndTestUtils.isTrackListEmpty(!showTrackList)) {
      // Create a simple track.
      EndToEndTestUtils.createSimpleTrack(gpsNumber);
      if (showTrackList) {
        EndToEndTestUtils.SOLO.goBack();
      }
    }
  }

  /**
   * Starts recoding track.
   */
  static void startRecording() {
    if (hasActionBar) {
      Button startButton = getButtonOnScreen(activityMytracks.getString(R.string.menu_record_track), false, false);
      // In case a track is recording.
      if (startButton == null) {
        stopRecording(true);
        startButton = getButtonOnScreen(activityMytracks.getString(R.string.menu_record_track), false, false);
      }
      SOLO.clickOnView(startButton);
    } else {
      showMoreMenuItem();
      if (!SOLO.searchText(activityMytracks.getString(R.string.menu_record_track))) {
        // Check if in TrackDetailActivity.
        if (SOLO.searchText(activityMytracks.getString(R.string.menu_play))) {
          SOLO.goBack();
        } else {
          // In case a track is recording.
          stopRecording(true);
          showMoreMenuItem();
        }
      }
      instrumentation.waitForIdleSync();
      SOLO.clickOnText(activityMytracks.getString(R.string.menu_record_track));
    }
  }
  
  /**
   * Checks if the MyTracks is under recording.
   * 
   * @return true if it is under recording.
   */
  static boolean isUnderRecording() {
    if (hasActionBar) { 
      return getButtonOnScreen(activityMytracks
        .getString(R.string.menu_record_track), false, false) == null; 
    }
    showMoreMenuItem();
    if (SOLO.searchText(activityMytracks.getString(R.string.menu_record_track))
        || SOLO.searchText(activityMytracks.getString(R.string.menu_play))) {
      SOLO.goBack();
      return false;
    }
    SOLO.goBack();
    return true;
  }

  /**
   * Stops recoding track.
   * 
   * @param isSave true means should save this track
   */
  static void stopRecording(boolean isSave) {
    if (hasActionBar) {
      getButtonOnScreen(activityMytracks.getString(R.string.menu_stop_recording), false, true);
    } else {
      showMoreMenuItem();
      instrumentation.waitForIdleSync();
      SOLO.clickOnText(activityMytracks.getString(R.string.menu_stop_recording));
    }
    if (isSave) {
      EndToEndTestUtils.SOLO.waitForText(activityMytracks.getString(R.string.generic_save), 1, 5000);
      // Make every track name is unique to make sure every check can be
      // trusted.
      EndToEndTestUtils.trackName = EndToEndTestUtils.TRACK_NAME_PREFIX
          + System.currentTimeMillis();
      SOLO.sendKey(KeyEvent.KEYCODE_DEL);
      SOLO.enterText(0, trackName);
      SOLO.clickOnEditText(1);
      SOLO.sendKey(KeyEvent.KEYCODE_DEL);
      SOLO.enterText(1, DEFAULTACTIVITY);
      if(!EndToEndTestUtils.isEmulator) {
        // Close soft keyboard.
        EndToEndTestUtils.SOLO.goBack();
      }
      SOLO.clickLongOnText(activityMytracks.getString(R.string.generic_save));
      instrumentation.waitForIdleSync();
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
    instrumentation.waitForIdleSync();
    findMenuItem(activityMytracks.getString(R.string.menu_save), true);
    instrumentation.waitForIdleSync();
    SOLO.clickOnText(trackKind.toUpperCase());
   // rotateAllActivities();
    SOLO.waitForText(activityMytracks.getString(R.string.generic_success_title));
  }

  /**
   * Checks if a button is existed in the screen.
   * 
   * @param buttonName the name string of the button
   * @param isWait whether wait the text
   * @param isClick whether click the button if find it
   * @return the button to search, and null means can not find the button
   */
  static Button getButtonOnScreen(String buttonName, boolean isWait,boolean isClick) {
    instrumentation.waitForIdleSync();
    if (isWait) {
      SOLO.waitForText(buttonName);
    }
    ArrayList<Button> currentButtons = SOLO.getCurrentButtons();
    for (Button button : currentButtons) {
      String title = (String) button.getText();
      if (title.equalsIgnoreCase(buttonName)) { 
        if(isClick) {
          SOLO.clickOnView(button);
        }
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
    instrumentation.waitForIdleSync();
    // If can find record button without pressing Menu, it should be an action
    // bar.
    Button startButton = getButtonOnScreen(activityMytracks.getString(R.string.menu_record_track), false, false);
    Button stopButton = getButtonOnScreen(activityMytracks.getString(R.string.menu_stop_recording), false, false);
    if (startButton != null || stopButton != null) {
      hasActionBar = true;
    } else {
      showMoreMenuItem();
      if (SOLO.searchText(activityMytracks.getString(R.string.menu_record_track))
          || SOLO.searchText(activityMytracks.getString(R.string.menu_stop_recording))) {
        hasActionBar = false;
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
    instrumentation.waitForIdleSync();
  }

  /**
   * Rotates all activities.
   */
  static void rotateAllActivities() {
    ArrayList<Activity> allActivities = SOLO.getAllOpenedActivities();
    for (Activity activity : allActivities) {
      EndToEndTestUtils.rotateActivity(activity);
    }

    instrumentation.waitForIdleSync();
  }

  /**
   * Finds an item in the menu with the option to click the item.
   * 
   * @param menuName the name of item
   * @param click true means need click this menu
   * @return true if find this menu
   */
  static boolean findMenuItem(String menuName, boolean click) {
    boolean findResult = false;

    // Firstly find in action bar.
    Button button = getButtonOnScreen(menuName, false, false);
    if (button != null) {
      findResult = true;
      if (click) {
        SOLO.clickOnView(button);
        instrumentation.waitForIdleSync();
      }
      return findResult;
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
      instrumentation.waitForIdleSync();
    } else {
      SOLO.goBack();
    }
    return findResult;
  }

  /**
   * Get more menu items operation is different for different Android OS.
   */
  public static void showMoreMenuItem() {
    if (hasActionBar) {
      View moreButton = getMoreOptionView();
      if (moreButton != null) {
        SOLO.clickOnView(moreButton);
        return;
      } 
    }
    SOLO.sendKey(KeyEvent.KEYCODE_MENU);
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
      if (view instanceof ImageButton && view.getClass().getName().equals(MOREOPTION_CLASSNAME)) { 
        return view; 
      }
    }
    return null;
  }
  
  /**
   * Finds a text view with specified test in a view.
   * 
   * @param findText text to find
   * @param parent which text view in in
   * @return the text view, null means can not find it
   */
  static TextView findTextView(String findText, View parent) {
    ArrayList<TextView> textViews = EndToEndTestUtils.SOLO.getCurrentTextViews(parent);
    for (TextView textView : textViews) {
      String text = (String) textView.getText();
      if (textView.isShown() && text.endsWith(findText)) { 
        return textView; 
      }
    }
    return null;
  }

  /**
   * Gets the ChartView.
   * 
   * @return the ChartView or null if not find
   */
  static ChartView getChartView() {
    ArrayList<View> views = EndToEndTestUtils.SOLO.getViews();
    for (View view : views) {
      if (view instanceof ChartView) { 
        return (ChartView) view; 
      }
    }
    return null;
  }
  
  /**
   * Resets all settings of MyTracks.
   * 
   * @param activityMyTracks current activity
   * @param keepInSettingList whether keep in setting list or not
   */
  public static void resetAllSettings(Activity activityMyTracks, boolean keepInSettingList) {
    EndToEndTestUtils.findMenuItem(activityMyTracks.getString(R.string.menu_settings), true);
    SOLO.waitForText(activityMyTracks.getString(R.string.settings_reset));
    SOLO.clickOnText(activityMyTracks.getString(R.string.settings_reset));
    getButtonOnScreen(activityMytracks.getString(R.string.generic_ok), true, true);
    instrumentation.waitForIdleSync();
    if (!keepInSettingList) {
      EndToEndTestUtils.SOLO.goBack();
    }
  }

}
