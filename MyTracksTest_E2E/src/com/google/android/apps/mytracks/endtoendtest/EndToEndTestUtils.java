package com.google.android.apps.mytracks.endtoendtest;

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

import com.google.android.apps.mytracks.ChartView;
import com.google.android.apps.mytracks.TrackListActivity;
import com.google.android.apps.mytracks.util.FileUtils;
import com.google.android.apps.mytracks.util.StringUtils;
import com.google.android.maps.mytracks.R;
import com.jayway.android.robotium.solo.Solo;

import android.annotation.SuppressLint;
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
import android.widget.ListView;
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

import junit.framework.Assert;

/**
 * Provides utilities to smoke test.
 * 
 * @author Youtao Liu
 */
public class EndToEndTestUtils {
  public static int emulatorPort = 5554;
  // usually 5554.
  private static final int ORIENTATION_PORTRAIT = 1;
  private static final int ORIENTATION_LANDSCAPE = 0;

  private static final String ANDROID_LOCAL_IP = "10.0.2.2";
  private static final String NO_GPS_MESSAGE_PREFIX = "GPS is not available";
  private static final String MOREOPTION_CLASSNAME = "com.android.internal.view.menu.ActionMenuPresenter$OverflowMenuButton";
  private static final String MENUITEM_CLASSNAME = "com.android.internal.view.menu.IconMenuItemView";

  // Pause 200ms between each send.
  public static final double START_LONGITUDE = 51;
  public static final double START_LATITUDE = -1.3f;
  public static final double DELTA_LONGITUDE = 0.0005f;
  public static final double DELTA_LADITUDE = 0.0005f;
  public static final String WAYPOINT_NAME = "testWaypoint";
  public static final String WAYPOINT_TYPE = "testWaypoinType";
  public static final String WAYPOINT_DESC = "testWaypointDesc";
  public static final String DEFAULTACTIVITYTYPE = "TestActivity";
  public static final String TRACK_NAME_PREFIX = "testTrackName";
  public static final String TRACK_DESC_PREFIX = "testTrackDesc";
  public static final String GPX = "gpx";
  public static final String KML = "kml";
  public static final String CSV = "csv";
  public static final String TCX = "tcx";
  public static final String BACKUPS = "backups";
  public static final String MENU_MORE = "More";
  public static final String LOG_TAG = "MyTracksTest";

  // Following is some check strings in English and Chinese
  private static final HashMap<String, String> RELATIVE_STARTTIME_POSTFIX_MULTILINGUAL = new HashMap<String, String>();
  private static final HashMap<String, String> KM_MULTILINGUAL = new HashMap<String, String>();
  private static final HashMap<String, String> MILE_MULTILINGUAL = new HashMap<String, String>();

  public static final int TINY_WAIT_TIME = 200;
  public static final int VERY_SHORT_WAIT_TIME = 500;
  public static final int SHORT_WAIT_TIME = 2000;
  public static final int NORMAL_WAIT_TIME = 8000;
  public static final int LONG_WAIT_TIME = 15000;
  public static final int SUPER_LONG_WAIT_TIME = 100000;

  public static String activityType = DEFAULTACTIVITYTYPE;
  public static String RELATIVE_STARTTIME_POSTFIX = "";
  public static String KM = "";
  public static String MILE = "";
  public static String deviceLanguage = "";
  public static String trackName;
  public static String trackDesc;

  static {
    RELATIVE_STARTTIME_POSTFIX_MULTILINGUAL.put("es", "mins ago");
    RELATIVE_STARTTIME_POSTFIX_MULTILINGUAL.put("de", "Minuten");
    RELATIVE_STARTTIME_POSTFIX_MULTILINGUAL.put("fr", "minute");
    RELATIVE_STARTTIME_POSTFIX_MULTILINGUAL.put("ar", "دقيقة");
    RELATIVE_STARTTIME_POSTFIX_MULTILINGUAL.put("zh", "分钟前");

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

  public static Solo SOLO;
  public static Instrumentation instrumentation;
  public static TrackListActivity activityMytracks;

  /*
   * Check whether the UI has an action bar which is related with the version of
   * Android OS.
   */
  public static boolean hasActionBar = false;
  public static boolean isEmulator = true;
  public static boolean hasGpsSingal = true;
  public static boolean isCheckedFirstLaunch = false;
  public static boolean isGooglePlayServicesLatest = true;

  private EndToEndTestUtils() {};

  /**
   * Checks the language, then sets the fields with right string.
   */
  private static void checkLanguage() {
    Locale locale = null;
    Configuration config = null;
    config = activityMytracks.getBaseContext().getResources().getConfiguration();
    locale = new Locale("en");
    Locale.setDefault(locale);
    config.locale = locale;

    deviceLanguage = instrumentation.getContext().getResources().getConfiguration().locale
        .getLanguage();
    if (RELATIVE_STARTTIME_POSTFIX_MULTILINGUAL.get(deviceLanguage) != null) {
      RELATIVE_STARTTIME_POSTFIX = RELATIVE_STARTTIME_POSTFIX_MULTILINGUAL.get(deviceLanguage);
      KM = KM_MULTILINGUAL.get(deviceLanguage);
      MILE = MILE_MULTILINGUAL.get(deviceLanguage);
    } else {
      RELATIVE_STARTTIME_POSTFIX = RELATIVE_STARTTIME_POSTFIX_MULTILINGUAL.get("es");
      KM = KM_MULTILINGUAL.get("es");
      MILE = MILE_MULTILINGUAL.get("es");
    }
  }

  /**
   * Sends Gps data to emulator, and the start value has an offset.
   * 
   * @param number send times
   * @param offset is used to compute the start latitude and longitude
   * @param pause pause interval between each sending
   */
  public static void sendGps(int number, int offset, int pause) {
    if (number < 1) {
      return;
    }

    int pauseInterval = TINY_WAIT_TIME;
    if (pause != -1) {
      pauseInterval = pause;
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
      double longitude = START_LONGITUDE + offset * DELTA_LONGITUDE;
      double latitude = START_LATITUDE + offset * DELTA_LADITUDE;
      for (int i = 0; i < number; i++) {
        out.println("geo fix " + longitude + " " + latitude);
        longitude += DELTA_LONGITUDE;
        latitude += DELTA_LADITUDE;
        Thread.sleep(pauseInterval);
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
   * Send Gps data to emulator.
   * 
   * @param number number of signals
   * @param offset is used to compute the start latitude and longitude
   */
  public static void sendGps(int number, int offset) {
    sendGps(number, offset, -1);
  }

  /**
   * Send Gps data to emulator.
   * 
   * @param number number of signals
   */
  public static void sendGps(int number) {
    sendGps(number, 0, -1);
  }

  /**
   * Sets the status whether the test is run on an emulator or not.
   */
  private static void setIsEmulator() {
    isEmulator = android.os.Build.MODEL.equals("google_sdk");
  }

  /**
   * Checks whether the Google Play Services need update.
   */
  private static boolean isGooglePlayServicesLatest() {
    return findTextView("Google Play services") == null;
  }

  /**
   * A setup for debugging end-to-end tests.
   * 
   * @param instrumentation the instrumentation is used for test
   * @param activityMyTracks the startup activity
   */
  public static void setupForDebug(Instrumentation instrumentation,
      TrackListActivity activityMyTracks) {
    EndToEndTestUtils.instrumentation = instrumentation;
    EndToEndTestUtils.activityMytracks = activityMyTracks;
    SOLO = new Solo(EndToEndTestUtils.instrumentation, EndToEndTestUtils.activityMytracks);
    setIsEmulator();
    hasActionBar = setHasActionBar();
    checkLanguage();
  }

  /**
   * A setup for all end-to-end tests.
   * 
   * @param instrumentation the instrumentation is used for test
   * @param activityMyTracks the startup activity
   */
  public static void setupForAllTest(Instrumentation instrumentation,
      TrackListActivity activityMyTracks) {
    EndToEndTestUtils.instrumentation = instrumentation;
    EndToEndTestUtils.activityMytracks = activityMyTracks;
    SOLO = new Solo(EndToEndTestUtils.instrumentation, EndToEndTestUtils.activityMytracks);

    if (!isGooglePlayServicesLatest) {
      SOLO.finishOpenedActivities();
      Assert.fail();
      Log.e(LOG_TAG, "Need update Google Play Services");
    }

    // Check if open MyTracks first time after install. If so, there would be a
    // welcome view with accept buttons. And makes sure only check once.
    if (!isCheckedFirstLaunch) {
      isGooglePlayServicesLatest = isGooglePlayServicesLatest();
      if (!isGooglePlayServicesLatest) {
        SOLO.finishOpenedActivities();
        Assert.fail();
        Log.e(LOG_TAG, "Need update Google Play Services");
      }
      setIsEmulator();

      verifyFirstLaunch();
      hasActionBar = setHasActionBar();
      checkLanguage();
      isCheckedFirstLaunch = true;

      instrumentation.waitForIdleSync();
      // Check the status of real phone. For emulator, we would fix GPS signal.
      if (!isEmulator) {
        findAndClickMyLocation(activityMyTracks);
        hasGpsSingal = !SOLO.waitForText(NO_GPS_MESSAGE_PREFIX, 1, SHORT_WAIT_TIME);
        SOLO.goBack();
      }
    }

    int trackNumber = SOLO.getCurrentViews(ListView.class).get(0).getCount();
    // Delete all tracks when there are two many tracks which may make some test
    // run slowly, such as sync test cases.
    if (trackNumber > 3) {
      deleteAllTracks();
    }

    // Check whether is under recording. If previous test failed, the recording
    // may not be recording.
    if (isUnderRecording()) {
      stopRecording(true);
    }

    resetAllSettings(activityMyTracks, false);
    instrumentation.waitForIdleSync();
  }

  /**
   * Rotates the given activity.
   * 
   * @param activity a given activity
   */
  private static void rotateActivity(Activity activity) {
    activity
        .setRequestedOrientation(activity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT ? ORIENTATION_LANDSCAPE
            : ORIENTATION_PORTRAIT);
  }

  /**
   * Accepts terms and configures units.
   */
  private static void verifyFirstLaunch() {
    if ((getButtonOnScreen(activityMytracks.getString(R.string.eula_accept), false, false) != null)) {
      getButtonOnScreen(activityMytracks.getString(R.string.eula_accept), true, true);
    }
    if (SOLO.waitForText(activityMytracks.getString(R.string.sync_drive_title))) {
      // Click for Sync with Google Drive(Don't enable sync as default).
      getButtonOnScreen(activityMytracks.getString(R.string.generic_no), true, true);
      instrumentation.waitForIdleSync();
    }
  }

  /**
   * Creates a simple track which can be used by subsequent test. This method
   * will save a customized track name.
   * 
   * @param numberOfGpsData number of simulated Gps data
   * @param showTrackList whether stay on track list activity or track detail
   *          activity
   */
  public static void createSimpleTrack(int numberOfGpsData, boolean showTrackList) {
    startRecording();
    sendGps(numberOfGpsData);
    instrumentation.waitForIdleSync();
    stopRecording(true);

    instrumentation.waitForIdleSync();
    if (showTrackList) {
      SOLO.goBack();
    }
    instrumentation.waitForIdleSync();
  }

  /**
   * Creates a track which contains pause during recording.
   * 
   * @param numberOfGpsData number of simulated Gps data before pause and after
   *          resume
   */
  public static void createTrackWithPause(int numberOfGpsData) {
    startRecording();
    sendGps(numberOfGpsData);
    pauseRecording();
    resumeRecording();
    sendGps(numberOfGpsData, numberOfGpsData);
    stopRecording(true);
  }

  /**
   * Checks if there is no track in track list. For some tests need at least one
   * track, the method can save time to create a new track.
   * 
   * @param isClick if not empty, true means click any track
   * @return return true if the track list is empty
   */
  public static boolean isTrackListEmpty(boolean isClick) {
    instrumentation.waitForIdleSync();
    int trackNumber = SOLO.getCurrentViews(ListView.class).get(0).getCount();
    if (trackNumber <= 0) {
      return true;
    }
    View oneTrack = SOLO.getCurrentViews(ListView.class).get(0).getChildAt(0);
    View aa = oneTrack.findViewById(R.id.list_item_name);
    if (aa != null) {
      trackName = (String) ((TextView) oneTrack.findViewById(R.id.list_item_name)).getText();
    }

    if (isClick) {
      SOLO.scrollUp();
      SOLO.clickOnView(oneTrack);
      SOLO.waitForText(activityMytracks.getString(R.string.track_detail_chart_tab));
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
  public static void createTrackIfEmpty(int gpsNumber, boolean showTrackList) {
    if (isTrackListEmpty(!showTrackList)) {
      // Create a simple track.
      createSimpleTrack(gpsNumber, showTrackList);
    }
  }

  /**
   * Starts recoding track.
   */
  public static void startRecording() {
    View startButton = SOLO.getCurrentActivity().findViewById(R.id.track_controller_record);
    if (startButton != null && startButton.isShown()) {
      SOLO.clickOnView(startButton);
    }
    instrumentation.waitForIdleSync();
  }

  /**
   * Pauses recoding track.
   */
  public static void pauseRecording() {
    View pauseButton = SOLO.getCurrentActivity().findViewById(R.id.track_controller_record);
    if (pauseButton != null && pauseButton.isShown()) {
      SOLO.clickOnView(pauseButton);
    }
    instrumentation.waitForIdleSync();
  }

  /**
   * Resume recoding track.
   */
  public static void resumeRecording() {
    View startButton = SOLO.getCurrentActivity().findViewById(R.id.track_controller_record);
    if (startButton != null && startButton.isShown()) {
      SOLO.clickOnView(startButton);
    }
    instrumentation.waitForIdleSync();
  }

  /**
   * Checks if the MyTracks is under recording.
   * 
   * @return true if it is under recording.
   */
  public static boolean isUnderRecording() {
    instrumentation.waitForIdleSync();
    return SOLO.getCurrentActivity().findViewById(R.id.track_controller_stop).isEnabled();
  }

  /**
   * Stops recoding track.
   * 
   * @param isSave true means should save this track
   */
  public static void stopRecording(boolean isSave) {
    View stopButton = SOLO.getCurrentActivity().findViewById(R.id.track_controller_stop);
    if (stopButton != null && stopButton.isShown()) {
      SOLO.clickOnView(stopButton);
      if (isSave) {
        SOLO.waitForText(activityMytracks.getString(R.string.generic_save), 1, 5000);
        // Make every track name is unique to make sure every check can be
        // trusted.
        long currentMillis = System.currentTimeMillis();
        trackName = TRACK_NAME_PREFIX + currentMillis;
        trackDesc = TRACK_DESC_PREFIX + currentMillis;
        SOLO.sendKey(KeyEvent.KEYCODE_DEL);
        enterTextAvoidSoftKeyBoard(0, trackName);
        enterTextAvoidSoftKeyBoard(1, activityType);
        enterTextAvoidSoftKeyBoard(2, trackDesc);
        SOLO.clickOnText(activityMytracks.getString(R.string.generic_save));
        instrumentation.waitForIdleSync();
      }
    }
  }

  /**
   * Deletes a kind of track in MyTracks folder.
   * 
   * @param trackKind the kind of track
   */
  public static void deleteExportedFiles(String trackKind) {
    File[] allFiles = (new File(FileUtils.getDirectoryPath(trackKind.toLowerCase()))).listFiles();
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
  public static void deleteAllTracks() {
    if (!isTrackListEmpty(false)) {
      findMenuItem(activityMytracks.getString(R.string.menu_delete_all), true);
      getButtonOnScreen(activityMytracks.getString(R.string.generic_yes), true, true);
      waitTextToDisappear(activityMytracks.getString(R.string.generic_progress_title));
    }
  }

  /**
   * Gets a kind of exported files.
   * 
   * @param trackKind the kind of track
   * @return files array of such kind of exported tracks
   */
  public static File[] getExportedFiles(final String trackKind) {
    String directoryPath = FileUtils.getDirectoryPath(trackKind);
    FileFilter filter = new FileFilter() {
      @Override
      public boolean accept(File pathname) {
        return pathname.getName().indexOf("." + trackKind) > 0;
      }
    };
    return (new File(directoryPath)).listFiles(filter);
  }

  /**
   * Saves a track to a kind of file in SD card.
   * 
   * @param trackKind the kind of track
   */
  public static void saveAllTrackToSdCard(String trackKind) {
    deleteExportedFiles(trackKind);
    instrumentation.waitForIdleSync();
    findMenuItem(activityMytracks.getString(R.string.menu_export_all), true);
    instrumentation.waitForIdleSync();
    SOLO.clickOnText(trackKind.toUpperCase());
    getButtonOnScreen(activityMytracks.getString(R.string.generic_ok), true, true);
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
  public static View getButtonOnScreen(String buttonName, boolean isWait, boolean isClick) {
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
        if (className.indexOf("ActionMenuItemView") > 0) {
          String menuItemNameString = view.getContentDescription().toString();
          if (menuItemNameString.equalsIgnoreCase(buttonName)) {
            button = view;
            break;
          }
        }
      }
    }

    // Get all buttons and find.
    if (button == null) {
      ArrayList<Button> currentButtons = SOLO.getCurrentViews(Button.class);
      for (Button oneButton : currentButtons) {
        String title = (String) oneButton.getText();
        if (title.equalsIgnoreCase(buttonName)) {
          button = oneButton;
        }
      }
    }

    if (button != null && isClick) {
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
  @SuppressLint("NewApi")
  private static boolean setHasActionBar() {
    try {
      return activityMytracks.getActionBar() == null ? false : true;
    } catch (Throwable e) {
      // For in Android which does not has action bar, here will meet a error.
      return false;
    }
  }

  /**
   * Rotates all activities.
   */
  public static void rotateCurrentActivity() {
    rotateActivity(SOLO.getCurrentActivity());
    instrumentation.waitForIdleSync();
  }

  /**
   * Finds an item in the menu with the option to click the item.
   * 
   * @param menuName the name of item
   * @param click true means need click this menu
   * @return true if find this menu
   */
  public static boolean findMenuItem(String menuName, boolean click) {
    instrumentation.waitForIdleSync();
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
      findResult = SOLO.waitForText(menuName, 1, LONG_WAIT_TIME, true);
    } else {
      // Non-ICS phone.
      SOLO.sendKey(KeyEvent.KEYCODE_MENU);
      if (SOLO.searchText(menuName, 1, true)) {
        findResult = true;
      } else if (getMoreOptionView() != null) {
        SOLO.clickOnView(getMoreOptionView());
        findResult = SOLO.searchText(menuName, 1, true);
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
   * <li>The view name equals {@link #MOREOPTION_CLASSNAME}.</li>
   * </ul>
   * 
   * @return the more option view. Null means can not find it.
   */
  private static View getMoreOptionView() {
    View viewResult = null;
    if (hasActionBar) {
      ArrayList<View> allViews = SOLO.getViews();
      for (View view : allViews) {
        if (view instanceof ImageButton && view.getClass().getName().equals(MOREOPTION_CLASSNAME)) {
          viewResult = view;
          break;
        }
      }
    } else {
      ArrayList<View> allViews = SOLO.getViews();
      for (View view : allViews) {
        if (view.getClass().getName().equals(MENUITEM_CLASSNAME)) {
          viewResult = view;
          // No break here to get the last menu item.
        }
      }
    }
    return viewResult;
  }

  /**
   * Finds a displayed text view with specified text in a view.
   * 
   * @param text text to find
   * @param parent which text view in in
   * @return the text view, null means can not find it
   */
  public static TextView findTextViewInView(String text, View parent) {
    ArrayList<TextView> textViews = SOLO.getCurrentViews(TextView.class, parent);
    for (TextView textView : textViews) {
      String textString = (String) textView.getText();
      if (textView.isShown() && textString.endsWith(text)) {
        return textView;
      }
    }
    return null;
  }

  /**
   * Finds a displayed text view with specified text.
   * 
   * @param text text to find
   * @return the text view, null means can not find it
   */
  public static TextView findTextView(String text) {
    ArrayList<View> allViews = SOLO.getViews();
    if (allViews != null) {
      for (View view : allViews) {
        if (view instanceof TextView) {
          TextView textView = (TextView) view;
          String textString = (String) textView.getText();
          if (textView.isShown() && textString.endsWith(text)) {
            return textView;
          }
        }
      }
    }
    return null;
  }

  /**
   * Finds a displayed text view with specified text and index number.
   * 
   * @param text text to find
   * @param index the index to search
   * @return the text view, null means can not find it
   */
  public static TextView findTextViewByIndex(String text, int index) {
    int number = 1;
    ArrayList<View> allViews = SOLO.getViews();
    for (View view : allViews) {
      if (view instanceof TextView) {
        TextView textView = (TextView) view;
        String textString = (String) textView.getText();
        if (textView.isShown() && textString.endsWith(text)) {
          if (number == index) {
            return textView;
          } else {
            number++;
          }
        }
      }
    }
    return null;
  }

  /**
   * Gets the ChartView.
   * 
   * @return the ChartView or null if not find
   */
  public static ChartView getChartView() {
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
    SOLO.clickOnText(activityMyTracks.getString(R.string.settings_backup_reset));
    Assert.assertTrue(SOLO.waitForText(activityMyTracks.getString(R.string.settings_reset)));
    SOLO.clickOnText(activityMyTracks.getString(R.string.settings_reset_summary));
    getButtonOnScreen(activityMytracks.getString(R.string.generic_yes), true, true);
    Assert.assertTrue(SOLO.waitForText(activityMyTracks.getString(R.string.settings_reset_done)));
    instrumentation.waitForIdleSync();
    SOLO.goBack();
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
    EditText editText = SOLO.getEditText(editTextIndex);
    SOLO.enterText(editText, text);
    SOLO.hideSoftKeyboard();

    // Above line does not work every time. And it is should be a Robotium
    // issue.
    InputMethodManager imm = (InputMethodManager) activityMytracks.getApplicationContext()
        .getSystemService(Context.INPUT_METHOD_SERVICE);
    imm.hideSoftInputFromWindow(editText.getWindowToken(), InputMethodManager.HIDE_IMPLICIT_ONLY);
  }

  /**
   * Waits n milliseconds.
   * 
   * @param milliseconds time to sleep
   */
  public static void sleep(long milliseconds) {
    try {
      Thread.sleep(milliseconds);
    } catch (InterruptedException e) {
      Log.e(LOG_TAG, "Unable to sleep " + milliseconds, e);
    }
  }

  /**
   * Checks whether the recording is not started.
   */
  public static void checkNotRecording() {
    instrumentation.waitForIdleSync();
    Assert.assertEquals(activityMytracks.getString(R.string.icon_record_track), (String) SOLO
        .getCurrentActivity().findViewById(R.id.track_controller_record).getContentDescription());
    Assert.assertFalse(SOLO.getCurrentActivity().findViewById(R.id.track_controller_stop)
        .isEnabled());
    Assert.assertNull(findTextView(activityMytracks.getString(R.string.generic_recording)));
    Assert.assertNull(findTextView(activityMytracks.getString(R.string.generic_paused)));
    TextView totalTime = (TextView) SOLO.getCurrentActivity().findViewById(
        R.id.track_controller_total_time);
    Assert.assertEquals(StringUtils.formatElapsedTimeWithHour(0), totalTime.getText().toString());
  }

  /**
   * Checks whether the MyTracks is under recording.
   */
  public static void checkUnderRecording() {
    instrumentation.waitForIdleSync();
    Assert.assertEquals(activityMytracks.getString(R.string.icon_pause_recording), (String) SOLO
        .getCurrentActivity().findViewById(R.id.track_controller_record).getContentDescription());
    Assert.assertTrue(SOLO.getCurrentActivity().findViewById(R.id.track_controller_stop)
        .isEnabled());
    Assert.assertNotNull(findTextView(activityMytracks.getString(R.string.generic_recording)));
    Assert.assertNull(findTextView(activityMytracks.getString(R.string.generic_paused)));

    String totalTimeOld = ((TextView) SOLO.getCurrentActivity().findViewById(
        R.id.track_controller_total_time)).getText().toString();
    sleep(2000);
    String totalTimeNew = ((TextView) SOLO.getCurrentActivity().findViewById(
        R.id.track_controller_total_time)).getText().toString();
    Assert.assertFalse(totalTimeOld.equalsIgnoreCase(totalTimeNew));
  }

  /**
   * Checks whether the recording is paused.
   */
  public static void checkUnderPaused() {
    instrumentation.waitForIdleSync();
    Assert.assertEquals(activityMytracks.getString(R.string.icon_record_track), (String) SOLO
        .getCurrentActivity().findViewById(R.id.track_controller_record).getContentDescription());
    Assert.assertTrue(SOLO.getCurrentActivity().findViewById(R.id.track_controller_stop)
        .isEnabled());
    Assert.assertNull(findTextView(activityMytracks.getString(R.string.generic_recording)));
    Assert.assertNotNull(findTextView(activityMytracks.getString(R.string.generic_paused)));

    String totalTimeOld = ((TextView) SOLO.getCurrentActivity().findViewById(
        R.id.track_controller_total_time)).getText().toString();
    sleep(2000);
    String totalTimeNew = ((TextView) SOLO.getCurrentActivity().findViewById(
        R.id.track_controller_total_time)).getText().toString();
    Assert.assertTrue(totalTimeOld.equalsIgnoreCase(totalTimeNew));
  }

  /**
   * Creates a way point during track recording.
   * 
   * @param markerNumber of number of previous markers
   */
  public static void createWaypoint(int markerNumber) {
    findMenuItem(activityMytracks.getString(R.string.menu_markers), true);
    if (markerNumber > 0 && hasGpsSingal) {
      SOLO.waitForText(WAYPOINT_NAME);
      int actualMarkerNumber = SOLO.getCurrentViews(ListView.class).get(0).getCount();
      Assert.assertEquals(markerNumber, actualMarkerNumber);
    } else {
      Assert.assertTrue(SOLO.waitForText(activityMytracks
          .getString(R.string.marker_list_empty_message)));
    }
    findMenuItem(activityMytracks.getString(R.string.menu_insert_marker), true);
    enterTextAvoidSoftKeyBoard(0, WAYPOINT_NAME + (markerNumber + 1));
    enterTextAvoidSoftKeyBoard(1, WAYPOINT_TYPE + (markerNumber + 1));
    enterTextAvoidSoftKeyBoard(2, WAYPOINT_DESC + (markerNumber + 1));
    SOLO.clickOnButton(activityMytracks.getString(R.string.generic_add));
    instrumentation.waitForIdleSync();
    if (hasGpsSingal) {
      Assert.assertTrue(SOLO.waitForText(WAYPOINT_NAME, 1, LONG_WAIT_TIME, true));
    } else {
      Assert.assertFalse(SOLO.searchText(WAYPOINT_NAME));
    }
    SOLO.goBack();
  }

  /**
   * Waits a text to disappear.
   */
  public static void waitTextToDisappear(String text) {
    instrumentation.waitForIdleSync();
    SOLO.waitForText(text, 1, VERY_SHORT_WAIT_TIME);
    while (SOLO.waitForText(text, 1, VERY_SHORT_WAIT_TIME)) {}
    return;
  }

  /**
   * Finds the My Location view and click it.
   * 
   * @param activity
   */
  public static void findAndClickMyLocation(Activity activity) {
    createTrackIfEmpty(1, false);
    sendGps(30);

    View myLocation = SOLO.getCurrentActivity().findViewById(R.id.map_my_location);
    instrumentation.waitForIdleSync();
    // Find the My Location button in another if null.
    if (myLocation == null) {
      ArrayList<ImageButton> aa = SOLO.getCurrentViews(ImageButton.class);
      for (ImageButton imageButton : aa) {
        if (imageButton.getContentDescription() != null
            && imageButton.getContentDescription().equals(
                activity.getString(R.string.icon_my_location))) {
          myLocation = imageButton;
          break;
        }
      }
    }
    SOLO.clickOnView(myLocation);
  }
}