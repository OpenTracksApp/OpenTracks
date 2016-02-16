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

import com.google.android.apps.mytracks.TrackListActivity;
import com.google.android.maps.mytracks.R;
import com.robotium.solo.Solo;

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

  public static final int TINY_WAIT_TIME = 200;
  public static final int VERY_SHORT_WAIT_TIME = 500;
  public static final int SHORT_WAIT_TIME = 2000;
  public static final int NORMAL_WAIT_TIME = 8000;
  public static final int LONG_WAIT_TIME = 15000;
  public static final int SUPER_LONG_WAIT_TIME = 100000;
  
  // Pause 200ms between each send.
  public static final double START_LONGITUDE = 51;
  public static final double START_LATITUDE = -1.3f;
  public static final double DELTA_LONGITUDE = 0.0005f;
  public static final double DELTA_LADITUDE = 0.0005f;
  public static final String WAYPOINT_NAME = "testWaypoint";
  public static final String WAYPOINT_TYPE = "testWaypoinType";
  public static final String WAYPOINT_DESCRIPTION = "testWaypointDesc";
  public static final String DEFAULT_ACTIVITY_TYPE = "TestActivity";
  public static final String TRACK_NAME_PREFIX = "testTrackName"; 
  
  private static final String TAG = EndToEndTestUtils.class.getSimpleName();
  private static final String TRACK_DESCRIPTION_PREFIX = "testTrackDesc";
  
  private static final int ORIENTATION_PORTRAIT = 1;
  private static final int ORIENTATION_LANDSCAPE = 0;

  private static final String ANDROID_LOCAL_IP = "10.0.2.2";
  private static final String NO_GPS_MESSAGE_PREFIX = "GPS is not available";
  private static final String MORE_OPTION_CLASSNAME = "com.android.internal.view.menu.ActionMenuPresenter$OverflowMenuButton";
  private static final String MENUITEM_CLASSNAME = "com.android.internal.view.menu.IconMenuItemView";

  // Following is some check strings in English and Chinese
  private static final HashMap<String, String>
      RELATIVE_START_TIME_SUFFIX_MULTILINGUAL = new HashMap<String, String>();

  static {
    RELATIVE_START_TIME_SUFFIX_MULTILINGUAL.put("en", "mins ago");
    RELATIVE_START_TIME_SUFFIX_MULTILINGUAL.put("de", "Minuten");
    RELATIVE_START_TIME_SUFFIX_MULTILINGUAL.put("fr", "minute");
    RELATIVE_START_TIME_SUFFIX_MULTILINGUAL.put("ar", "دقيقة");
    RELATIVE_START_TIME_SUFFIX_MULTILINGUAL.put("zh", "分钟前");
  }
  
  public static int emulatorPort = 5554; // usually 5554.
  public static String activityType = DEFAULT_ACTIVITY_TYPE;
  public static String trackName;
  public static String trackDescription;
  public static com.robotium.solo.Solo SOLO;
  public static Instrumentation instrumentation;
  public static TrackListActivity trackListActivity;

  /*
   * Check whether the UI has an action bar which is related with the version of
   * Android OS.
   */
  public static boolean hasActionBar = false;
  public static boolean isEmulator = true;
  public static boolean hasGpsSignal = true;

  private static boolean isCheckedFirstLaunch = false;
  private static boolean isGooglePlayServicesLatest = true;
  private static String language;
  
  private EndToEndTestUtils() {};

  /**
   * Checks the language, then sets the fields with right string.
   */
  private static void checkLanguage() {
    Locale locale = new Locale("en");
    Locale.setDefault(locale);

    Configuration configuration = trackListActivity.getBaseContext()
        .getResources().getConfiguration();
    configuration.locale = locale;

    language = instrumentation.getContext().getResources().getConfiguration().locale.getLanguage();
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
   * @param inst the instrumentation
   * @param activity the track list activity
   */
  public static void setupForDebug(Instrumentation inst, TrackListActivity activity) {
    instrumentation = inst;
    trackListActivity = activity;
    SOLO = new Solo(instrumentation, trackListActivity);
    setIsEmulator();
    hasActionBar = setHasActionBar();
    checkLanguage();
  }

  /**
   * A setup for all end-to-end tests.
   * 
   * @param inst the instrumentation
   * @param activity the track list activity
   */
  public static void setupForAllTest(Instrumentation inst, TrackListActivity activity) {
    instrumentation = inst;
    trackListActivity = activity;
    SOLO = new Solo(instrumentation, trackListActivity);

    if (!isGooglePlayServicesLatest) {
      SOLO.finishOpenedActivities();
      Assert.fail();
      Log.e(TAG, "Need update Google Play Services");
    }

    // Check if open MyTracks first time after install. If so, there would be a
    // welcome view with accept buttons. And makes sure only check once.
    if (!isCheckedFirstLaunch) {
      isGooglePlayServicesLatest = isGooglePlayServicesLatest();
      if (!isGooglePlayServicesLatest) {
        SOLO.finishOpenedActivities();
        Assert.fail();
        Log.e(TAG, "Need update Google Play Services");
      }
      setIsEmulator();

      verifyFirstLaunch();
      hasActionBar = setHasActionBar();
      checkLanguage();
      isCheckedFirstLaunch = true;

      inst.waitForIdleSync();
      // Check the status of real phone. For emulator, we would fix GPS signal.
      if (!isEmulator) {
        findAndClickMyLocation(activity);
        hasGpsSignal = !SOLO.waitForText(NO_GPS_MESSAGE_PREFIX, 1, SHORT_WAIT_TIME);
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

    resetAllSettings(activity, false);
    inst.waitForIdleSync();
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
    if ((getButtonOnScreen(trackListActivity.getString(R.string.eula_accept), false, false) != null)) {
      getButtonOnScreen(trackListActivity.getString(R.string.eula_accept), true, true);
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

    if (showTrackList) {
      SOLO.goBack();
      instrumentation.waitForIdleSync();
    }
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
  private static boolean isTrackListEmpty(boolean isClick) {
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
      SOLO.waitForText(trackListActivity.getString(R.string.track_detail_chart_tab));
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
  private static boolean isUnderRecording() {
    instrumentation.waitForIdleSync();
    View view = SOLO.getCurrentActivity().findViewById(R.id.track_controller_stop);
    // TODO: understand why view can be null here
    return view != null ? view.isEnabled() : false;
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
        SOLO.waitForText(trackListActivity.getString(R.string.generic_save), 1, 5000);
        // Make every track name is unique to make sure every check can be
        // trusted.
        long currentMillis = System.currentTimeMillis();
        trackName = TRACK_NAME_PREFIX + currentMillis;
        trackDescription = TRACK_DESCRIPTION_PREFIX + currentMillis;
        SOLO.sendKey(KeyEvent.KEYCODE_DEL);
        enterTextAvoidSoftKeyBoard(0, trackName);
        enterTextAvoidSoftKeyBoard(1, activityType);
        enterTextAvoidSoftKeyBoard(2, trackDescription);
        SOLO.clickOnText(trackListActivity.getString(R.string.generic_save));
        instrumentation.waitForIdleSync();
      } else {
        instrumentation.waitForIdleSync();
      }
    }
  }

  /**
   * Deletes all tracks. This method should be call when the TracksListActivity
   * is shown.
   */
  public static void deleteAllTracks() {
    if (!isTrackListEmpty(false)) {
      findMenuItem(trackListActivity.getString(R.string.menu_delete_all), true);
      getButtonOnScreen(trackListActivity.getString(R.string.generic_yes), true, true);
      waitTextToDisappear(trackListActivity.getString(R.string.generic_progress_title));
    }
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
      Log.d(TAG, "Don't find the button " + buttonName);
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
      return trackListActivity.getActionBar() == null ? false : true;
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
  private static void showMenuItem() {
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
   * <li>The view name equals {@link #MORE_OPTION_CLASSNAME}.</li>
   * </ul>
   * 
   * @return the more option view. Null means can not find it.
   */
  private static View getMoreOptionView() {
    View viewResult = null;
    if (hasActionBar) {
      ArrayList<View> allViews = SOLO.getViews();
      for (View view : allViews) {
        if (view instanceof ImageButton && view.getClass().getName().equals(MORE_OPTION_CLASSNAME)) {
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
   * Resets all settings of MyTracks.
   * 
   * @param activityMyTracks current activity
   * @param keepInSettingList whether keep in setting list or not
   */
  public static void resetAllSettings(Activity activityMyTracks, boolean keepInSettingList) {
    findMenuItem(activityMyTracks.getString(R.string.menu_settings), true);
    SOLO.clickOnText(activityMyTracks.getString(R.string.settings_advanced));
    Assert.assertTrue(SOLO.waitForText(activityMyTracks.getString(R.string.settings_reset)));
    SOLO.clickOnText(activityMyTracks.getString(R.string.settings_reset));
    getButtonOnScreen(trackListActivity.getString(R.string.generic_yes), true, true);
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
    InputMethodManager imm = (InputMethodManager) trackListActivity.getApplicationContext()
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
      Log.e(TAG, "Unable to sleep " + milliseconds, e);
    }
  }

  /**
   * Checks whether the recording is not started.
   */
  public static void checkNotRecording() {
    instrumentation.waitForIdleSync();
    Assert.assertEquals(trackListActivity.getString(R.string.image_record),
        (String) SOLO.getCurrentActivity()
            .findViewById(R.id.track_controller_record).getContentDescription());
    Assert.assertFalse(
        SOLO.getCurrentActivity().findViewById(R.id.track_controller_stop).isEnabled());
    Assert.assertNull(findTextView(trackListActivity.getString(R.string.generic_recording)));
    Assert.assertNull(findTextView(trackListActivity.getString(R.string.generic_paused)));
    Assert.assertFalse(
        SOLO.getCurrentActivity().findViewById(R.id.track_controller_total_time).isShown());
  }

  /**
   * Checks whether the MyTracks is under recording.
   */
  public static void checkUnderRecording() {
    instrumentation.waitForIdleSync();
    Assert.assertEquals(trackListActivity.getString(R.string.image_pause), (String) SOLO
        .getCurrentActivity().findViewById(R.id.track_controller_record).getContentDescription());
    Assert.assertTrue(SOLO.getCurrentActivity().findViewById(R.id.track_controller_stop)
        .isEnabled());
    Assert.assertNotNull(findTextView(trackListActivity.getString(R.string.generic_recording)));
    Assert.assertNull(findTextView(trackListActivity.getString(R.string.generic_paused)));

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
    Assert.assertEquals(trackListActivity.getString(R.string.image_record), (String) SOLO
        .getCurrentActivity().findViewById(R.id.track_controller_record).getContentDescription());
    Assert.assertTrue(SOLO.getCurrentActivity().findViewById(R.id.track_controller_stop)
        .isEnabled());
    Assert.assertNull(findTextView(trackListActivity.getString(R.string.generic_recording)));
    Assert.assertNotNull(findTextView(trackListActivity.getString(R.string.generic_paused)));

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
    findMenuItem(trackListActivity.getString(R.string.menu_markers), true);
    instrumentation.waitForIdleSync();
    if (markerNumber > 0 && hasGpsSignal) {
      SOLO.waitForText(WAYPOINT_NAME);
      int actualMarkerNumber = SOLO.getCurrentViews(ListView.class).get(0).getCount();
      Assert.assertEquals(markerNumber, actualMarkerNumber);
    } else {
      Log.d(TAG, "marker number, hasGpsSignal: " + markerNumber + ", " + hasGpsSignal);
      Assert.assertTrue(SOLO.waitForText(trackListActivity
          .getString(R.string.marker_list_empty_message)));
    }
    findMenuItem(trackListActivity.getString(R.string.menu_insert_marker), true);
    enterTextAvoidSoftKeyBoard(0, WAYPOINT_NAME + (markerNumber + 1));
    enterTextAvoidSoftKeyBoard(1, WAYPOINT_TYPE + (markerNumber + 1));
    enterTextAvoidSoftKeyBoard(2, WAYPOINT_DESCRIPTION + (markerNumber + 1));
    SOLO.clickOnButton(trackListActivity.getString(R.string.generic_add));
    instrumentation.waitForIdleSync();
    if (hasGpsSignal) {
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
  private static void findAndClickMyLocation(Activity activity) {
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
                activity.getString(R.string.image_my_location))) {
          myLocation = imageButton;
          break;
        }
      }
    }
    SOLO.clickOnView(myLocation);
  }
  
  /**
   * Gets the relative start time suffix.
   */
  public static String getRelativeStartTimeSuffix() {
    String value = RELATIVE_START_TIME_SUFFIX_MULTILINGUAL.get(language);
    return value != null ? value : RELATIVE_START_TIME_SUFFIX_MULTILINGUAL.get("en");
  }
  
  /**
   * Changes to metric units.
   */
  public static void changeToMetricUnits() {
    EndToEndTestUtils.findMenuItem(trackListActivity.getString(R.string.menu_settings), true);
    EndToEndTestUtils.SOLO.clickOnText(
        trackListActivity.getString(R.string.track_detail_stats_tab));
    Assert.assertTrue(EndToEndTestUtils.SOLO.waitForText(
        trackListActivity.getString(R.string.settings_stats_units_title)));

    // Change the preferred units
    EndToEndTestUtils.SOLO.clickOnText(
        trackListActivity.getString(R.string.settings_stats_units_title));
    EndToEndTestUtils.SOLO.clickOnText(trackListActivity.getString(R.string.unit_kilometer));
    EndToEndTestUtils.SOLO.goBack();
    EndToEndTestUtils.SOLO.goBack();
  }
}