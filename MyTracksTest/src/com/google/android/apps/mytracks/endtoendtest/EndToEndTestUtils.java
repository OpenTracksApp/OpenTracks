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
import android.content.Context;
import android.content.res.Configuration;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
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
import java.util.Locale;

/**
 * Provides utilities to smoke test.
 * 
 * @author Youtao Liu
 */
public class EndToEndTestUtils {
  
  private static final String ANDROID_LOCAL_IP = "10.0.2.2";
  // usually 5554.
  public static int emulatorPort = 5554;

  private static final int ORIENTATION_PORTRAIT = 1;
  private static final int ORIENTATION_LANDSCAPE = 0;
  // Pause 200ms between each send.
  static int PAUSE = 200;
  private static final float START_LONGITUDE = 51;
  private static final float START_LATITUDE = -1.3f;
  private static final float DELTA_LONGITUDE = 0.0005f;
  private static final float DELTA_LADITUDE = 0.0005f;
  private static final String NO_GPS_MESSAGE_PREFIX = "GPS is not available";
  
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
  private static final HashMap<String, String> VIEW_MODE_MULTILINGUAL = new HashMap<String, String>(); 
  private static final HashMap<String, String> KM_MULTILINGUAL = new HashMap<String, String>(); 
  private static final HashMap<String, String> MILE_MULTILINGUAL = new HashMap<String, String>(); 
  public static String RELATIVE_STARTTIME_POSTFIX = "";
  public static String VIEW_MODE = "";
  public static String KM = "";
  public static String MILE = "";
  
  public static int SHORT_WAIT_TIME = 2000;
  public static int NORMAL_WAIT_TIME = 8000;
  public static int LONG_WAIT_TIME = 15000;
  public static int SUPER_LONG_WAIT_TIME = 100000;
  
  public static String deviceLanguage = "";
  
  static {
    RELATIVE_STARTTIME_POSTFIX_MULTILINGUAL.put("es", "mins ago");
    RELATIVE_STARTTIME_POSTFIX_MULTILINGUAL.put("de", "Minuten");
    RELATIVE_STARTTIME_POSTFIX_MULTILINGUAL.put("fr", "minute");
    RELATIVE_STARTTIME_POSTFIX_MULTILINGUAL.put("ar", "دقيقة");
    RELATIVE_STARTTIME_POSTFIX_MULTILINGUAL.put("zh", "分钟前");
    
    VIEW_MODE_MULTILINGUAL.put("es", "mode");
    VIEW_MODE_MULTILINGUAL.put("de", "modus");
    VIEW_MODE_MULTILINGUAL.put("fr", "Mode");
    VIEW_MODE_MULTILINGUAL.put("ar", "وضع");
    VIEW_MODE_MULTILINGUAL.put("zh", "模式");
    
    KM_MULTILINGUAL.put("es", "km");
    KM_MULTILINGUAL.put("de", "km");
    KM_MULTILINGUAL.put("fr", "km");
    KM_MULTILINGUAL.put("ar", "كم");
    KM_MULTILINGUAL.put("zh", "公里");
    
    MILE_MULTILINGUAL.put("es", "mi");
    MILE_MULTILINGUAL.put("de", "mi");
    MILE_MULTILINGUAL.put("fr", "mile");
    MILE_MULTILINGUAL.put("ar", "ميل");
    MILE_MULTILINGUAL.put("zh", "英里");
  }

  static Solo SOLO;
  static Instrumentation instrumentation;
  static TrackListActivity activityMytracks;
  // Check whether the UI has an action bar which is related with the version of
  // Android OS.
  static boolean hasActionBar = false;
  static boolean isEmulator = true;
  static boolean hasGpsSingal = true;
  static boolean isCheckedFirstLaunch = false;
  public static final String LOG_TAG = "MyTracksTest";

  private EndToEndTestUtils() {}
  
  /**
   * Checks the language, then sets the fields with right string.
   */
  private static void checkLanguage() {
    Locale locale = null;
    Configuration config=null;
     config = activityMytracks.getBaseContext().getResources().getConfiguration();
    locale = new Locale("en");
    Locale.setDefault(locale);
    config.locale = locale;
    
    
    deviceLanguage = instrumentation.getContext().getResources().getConfiguration().locale.getLanguage();
    if (RELATIVE_STARTTIME_POSTFIX_MULTILINGUAL.get(deviceLanguage) != null) {
      RELATIVE_STARTTIME_POSTFIX = RELATIVE_STARTTIME_POSTFIX_MULTILINGUAL.get(deviceLanguage);
      VIEW_MODE = VIEW_MODE_MULTILINGUAL.get(deviceLanguage);
      KM = KM_MULTILINGUAL.get(deviceLanguage);
      MILE = MILE_MULTILINGUAL.get(deviceLanguage);
    } else {
      RELATIVE_STARTTIME_POSTFIX = RELATIVE_STARTTIME_POSTFIX_MULTILINGUAL.get("es");
      VIEW_MODE = VIEW_MODE_MULTILINGUAL.get("es");
      KM = KM_MULTILINGUAL.get("es");
      MILE = MILE_MULTILINGUAL.get("es");
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
      socket = new Socket(ANDROID_LOCAL_IP, emulatorPort);
      out = new PrintStream(socket.getOutputStream());
      float longitude = START_LONGITUDE;
      float latitude = START_LATITUDE;
      for (int i = 0; i < number; i++) {
        Thread.sleep(PAUSE);
        out.println("geo fix " + latitude + " " + longitude);
        longitude += DELTA_LONGITUDE;
        latitude += DELTA_LADITUDE;
      }
      // Wait the GPS signal can be obtained by MyTracks.  
      Thread.sleep(SHORT_WAIT_TIME);
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
    EndToEndTestUtils.instrumentation = instrumentation;
    EndToEndTestUtils.activityMytracks = activityMyTracks;
    SOLO = new Solo(EndToEndTestUtils.instrumentation,
        EndToEndTestUtils.activityMytracks);
    
    // Check if open MyTracks first time after install. If so, there would be a
    // welcome view with accept buttons. And makes sure only check once.
    if (!isCheckedFirstLaunch) {
      setIsEmulator();
      if ((getButtonOnScreen(EndToEndTestUtils.activityMytracks
          .getString(R.string.eula_accept), false, false) != null)) {
        verifyFirstLaunch();
      } else if (SOLO.waitForText(
      // After reset setting, welcome page will show again.
          activityMytracks.getString(R.string.welcome_title), 0, SHORT_WAIT_TIME)) {
        resetPreferredUnits();
      }
      hasActionBar = setHasActionBar();
      checkLanguage();
      isCheckedFirstLaunch = true;
      deleteAllTracks();
      resetAllSettings(activityMyTracks, false);
      
      // Check the status of real phone. For emulator, we would fix GPS signal.
      if(!isEmulator) {
        GoToMyLocationTest.findAndClickMyLocation(activityMyTracks);
        hasGpsSingal = !SOLO.waitForText(NO_GPS_MESSAGE_PREFIX, 1,
            EndToEndTestUtils.SHORT_WAIT_TIME);
        SOLO.goBack();
      }
    } else if (SOLO.waitForText(
        // After reset setting, welcome page will show again.
        activityMytracks.getString(R.string.welcome_title), 0, SHORT_WAIT_TIME)) {
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
    sendGps(numberOfGpsData);
    instrumentation.waitForIdleSync();
    stopRecording(true);
  }
  
  /**
   * Creates a track which contains pause during recording.
   * 
   * @param numberOfGpsData number of simulated Gps data
   */
  public static void createTrackWithPause(int numberOfGpsData) {
    EndToEndTestUtils.startRecording();
    EndToEndTestUtils.sendGps(numberOfGpsData);
    EndToEndTestUtils.findMenuItem(activityMytracks.getString(R.string.menu_pause_track), true);
    EndToEndTestUtils.findMenuItem(activityMytracks.getString(R.string.menu_record_track), false);
    EndToEndTestUtils.sendGps(numberOfGpsData);
    EndToEndTestUtils.stopRecording(true);
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
    if (isTrackListEmpty(!showTrackList)) {
      // Create a simple track.
      createSimpleTrack(gpsNumber);
      instrumentation.waitForIdleSync();
      if (showTrackList) {
        SOLO.goBack();
      }
      instrumentation.waitForIdleSync();
    }
  }

  /**
   * Starts recoding track.
   */
  static void startRecording() {
    if (hasActionBar) {
      View startButton = getButtonOnScreen(activityMytracks.getString(R.string.menu_record_track), false, false);
      // In case a track is recording.
      if (startButton == null) {
        stopRecording(true);
        startButton = getButtonOnScreen(activityMytracks.getString(R.string.menu_record_track), false, false);
      }
      SOLO.clickOnView(startButton);
    } else {
      showMenuItem();
      if (!SOLO.searchText(activityMytracks.getString(R.string.menu_record_track))) {
        // Check if in TrackDetailActivity.
        if (SOLO.searchText(activityMytracks.getString(R.string.menu_play))) {
          SOLO.goBack();
        } else {
          // In case a track is recording.
          stopRecording(true);
          showMenuItem();
        }
      }
      instrumentation.waitForIdleSync();
      SOLO.clickOnText(activityMytracks.getString(R.string.menu_record_track));
    }
    instrumentation.waitForIdleSync();
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
    showMenuItem();
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
      showMenuItem();
      instrumentation.waitForIdleSync();
      SOLO.clickOnText(activityMytracks.getString(R.string.menu_stop_recording));
    }
    if (isSave) {
      SOLO.waitForText(activityMytracks.getString(R.string.generic_save), 1, 5000);
      // Make every track name is unique to make sure every check can be
      // trusted.
      trackName = TRACK_NAME_PREFIX
          + System.currentTimeMillis();
      SOLO.sendKey(KeyEvent.KEYCODE_DEL);
      enterTextAvoidSoftKeyBoard(0, trackName);
      enterTextAvoidSoftKeyBoard(1, DEFAULTACTIVITY);
      SOLO.clickOnText(activityMytracks.getString(R.string.generic_save));
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
   * Deletes all tracks. This method should be call when the TracksListActivity
   * is shown.
   */
  static void deleteAllTracks() {
    findMenuItem(activityMytracks.getString(R.string.menu_delete_all), true);
    getButtonOnScreen(activityMytracks.getString(R.string.generic_ok), true, true);
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
  static View getButtonOnScreen(String buttonName, boolean isWait,boolean isClick) {
    View button = null;

    instrumentation.waitForIdleSync();
    if (isWait) {
      SOLO.waitForText(buttonName);
    }
    
    // Find on action bar.
    if (hasActionBar) {
      ArrayList<View> allViews = SOLO.getViews();
      for (View view : allViews) {
        String className = view.getClass().getName();
        if(className.indexOf("ActionMenuItemView") > 0) {
          String menuItemNameString = view.getContentDescription().toString();
          if(menuItemNameString.equalsIgnoreCase(buttonName)) {
            button = view;
            break;
          }
        }
      }
    }

    // Get all buttons and find.
    if(button == null) {
      ArrayList<Button> currentButtons = SOLO.getCurrentButtons();
      for (Button oneButton : currentButtons) {
        String title = (String) oneButton.getText();
        if (title.equalsIgnoreCase(buttonName)) { 
          button = oneButton;
        }
      }
    }
    
    if(button != null && isClick) {
      SOLO.clickOnView(button);
    }
    
    if (button == null && isClick) {
      Log.d(LOG_TAG, "Don't find the button " + buttonName);
    }
    
    return button;
  }

  /**
   * Checks whether an action bar is shown.
   * 
   * @return false means can not check failed.
   */
  static boolean setHasActionBar() {
    try {
      return activityMytracks.getActionBar() == null ? false : true;
    }catch (Throwable e) {
      // For in Android which does not has action bar, here will meet a error.
      return false;
    }
  }

  /**
   * Rotates the current activity.
   */
  static void rotateCurrentActivity() {
    rotateActivity(SOLO.getCurrentActivity());
    instrumentation.waitForIdleSync();
  }

  /**
   * Rotates all activities.
   */
  static void rotateAllActivities() {
    if (hasActionBar) {
      ArrayList<Activity> allActivities = SOLO.getAllOpenedActivities();
      for (Activity activity : allActivities) {
        rotateActivity(activity);
      }
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
    boolean isMoreMenuOpened = false;

    // ICS phone.
    if (hasActionBar) {
      // Firstly find in action bar.
      View button = getButtonOnScreen(menuName, false, false);
      if (button != null) {
        findResult = true;
        if (click) {
          SOLO.clickOnView(button);
          instrumentation.waitForIdleSync();
        }
        return findResult;
      }
      showMenuItem();
      findResult = SOLO.getText(menuName) != null;
    } else {
      // Non-ICS phone.
      SOLO.sendKey(KeyEvent.KEYCODE_MENU);
      if (SOLO.getText(menuName) != null) {
        findResult = true;
      } else if (SOLO.searchText(MENU_MORE)) {
        SOLO.clickOnText(MENU_MORE);
        findResult = SOLO.getText(menuName) != null;
        isMoreMenuOpened = true;
      }
    }

    if (findResult && click) {
      SOLO.clickOnView(SOLO.getText(menuName));
      instrumentation.waitForIdleSync();
    } else {
      // Quit more menu list if opened.
      if (isMoreMenuOpened) {
        SOLO.goBack();
      }
      // Quit menu list.
      SOLO.goBack();
    }
    return findResult;
  }

  /**
   * Show menu item list.
   */
  public static void showMenuItem() {
    showMenuItem(0);
  }
  
  /**
   * Gets more menu items operation is different for different Android OS. When
   * get overflow button view on action, it usually be able to click. But in
   * some situation, will meet an error when click it. So catch it and try
   * again. In most situation, the second click will be pass.
   * 
   * @param depth control the depth of recursion to prevent dead circulation
   */
  private static void showMenuItem(int depth) {
    // ICS phone.
    if (hasActionBar) {
      instrumentation.waitForIdleSync();
      View moreButton = getMoreOptionView();
      // ICS phone without menu key.
      if (moreButton != null) {
        try {
          SOLO.clickOnView(moreButton);
        } catch (Throwable e) {
          if (depth < 5 && e.getMessage().indexOf("Click can not be completed") > -1) {
            showMenuItem(depth++);
          }
        }
        return;
      } else {
        // ICS phone with menu key.
        SOLO.sendKey(KeyEvent.KEYCODE_MENU);
      }
    } else {
      // Non-ICS phone with menu key.
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
    ArrayList<TextView> textViews = SOLO.getCurrentTextViews(parent);
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
    ArrayList<View> views = SOLO.getViews();
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
    findMenuItem(activityMyTracks.getString(R.string.menu_settings), true);
    SOLO.waitForText(activityMyTracks.getString(R.string.settings_reset));
    SOLO.clickOnText(activityMyTracks.getString(R.string.settings_reset));
    getButtonOnScreen(activityMytracks.getString(R.string.generic_ok), true, true);
    instrumentation.waitForIdleSync();
    if (!keepInSettingList) {
      SOLO.goBack();
    }
  }
  
  /**
   * Finds a edit text and enter text in it. This method can hides soft key
   * board when input text.
   * 
   * @param editTextIndex the index of edit text
   * @param text to enter
   */
  public static void enterTextAvoidSoftKeyBoard(int editTextIndex, String text) {
    InputMethodManager imm = (InputMethodManager) activityMytracks.getApplicationContext().getSystemService(Context.INPUT_METHOD_SERVICE);
    EditText editText = SOLO.getEditText(editTextIndex);
    imm.hideSoftInputFromWindow(editText.getWindowToken(), InputMethodManager.HIDE_IMPLICIT_ONLY);
    SOLO.enterText(editText, text);
  }

}