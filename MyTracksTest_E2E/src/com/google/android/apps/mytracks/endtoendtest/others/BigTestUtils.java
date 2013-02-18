package com.google.android.apps.mytracks.endtoendtest.others;

import com.google.android.apps.mytracks.endtoendtest.EndToEndTestUtils;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Environment;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager.LayoutParams;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.Date;
import java.util.List;

import junit.framework.Assert;

/**
 * A utility to support the big test cases, such as sensor test, stress test and
 * so forth which under the package
 * {@link com.google.android.apps.mytracks.endtoendtest.others}.
 * 
 * @author Youtao Liu
 */
public class BigTestUtils {

  public static final String DISABLE_MESSAGE = "This test is disabled";
  public static final String MYTRACKS_PROCESS_NAME = "com.google.android.maps.mytracks";
  public static final String MYTRACKS_TEST_INFO_FILE = "MyTracksTestInfo.txt";

  /**
   * Gets the memory usage of MyTracks process. This method would get the Pss
   * memory. Pss is the amount of memory shared with other processes, accounted
   * in a way that the amount is divided evenly between the processes that share
   * it. This is memory that would not be released if the process was
   * terminated, but is indicative of the amount that this process is
   * "contributing" to the overall memory load.
   * 
   * @param context
   */
  public static String getMyTracksPssMemoryInfo(Context context) {
    int MyTracksProcessId = -1;
    ActivityManager activityManager = (ActivityManager) context
        .getSystemService(Activity.ACTIVITY_SERVICE);
    List<RunningAppProcessInfo> runningAppProcesses = activityManager.getRunningAppProcesses();

    for (RunningAppProcessInfo runningAppProcessInfo : runningAppProcesses) {
      if (runningAppProcessInfo.processName.equalsIgnoreCase(MYTRACKS_PROCESS_NAME)) {
        MyTracksProcessId = runningAppProcessInfo.pid;
        break;
      }
    }
    if (MyTracksProcessId == -1) {
      Assert.fail();
    }
    int pids[] = { MyTracksProcessId };
    android.os.Debug.MemoryInfo[] memoryInfoArray = activityManager.getProcessMemoryInfo(pids);
    int memoryUsage = memoryInfoArray[0].getTotalPss();
    return String.format(" MyTracks TotalPss Memory: %d (kB) ", memoryUsage);
  }

  /**
   * Gets the battery info of device, and then writes it to a file.
   * 
   * @param context the context of application
   */
  public static String getBatteryUsageInfo(Context context) {
    Intent batteryIntent = context.getApplicationContext().registerReceiver(null,
        new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    int rawlevel = batteryIntent.getIntExtra("level", -1);
    double scale = batteryIntent.getIntExtra("scale", -1);
    return String.format("Device has %s of %s battery left", rawlevel, scale);
  }

  /**
   * Writes a string to battery info file. This method would add a time string
   * before the given value.
   * 
   * @param string string to write
   * @param isAppend true means append to file
   */
  public static void writeToFile(String string, boolean isAppend) {
    String path = Environment.getExternalStorageDirectory() + File.separator
        + MYTRACKS_TEST_INFO_FILE;
    try {
      FileOutputStream fileOutputStream = new FileOutputStream(new File(path), isAppend);
      OutputStreamWriter osw = new OutputStreamWriter(fileOutputStream);

      try {
        // Write the string to the file
        osw.write(string);
      } finally {
        osw.flush();
        osw.close();
      }
    } catch (Exception e) {
      Log.i(EndToEndTestUtils.LOG_TAG, "Meet error when write test info to file.");
    }
    return;
  }

  /**
   * Monitors the battery and memory usage during test, and writes the usage
   * info to a local file.
   * 
   * @param context application context
   * @param interval the milliseconds to get usage info
   * @param testTime the total test milliseconds
   */
  public static void moniterTest(Context context, int interval, int testTime) {
    long startTime = System.currentTimeMillis();

    while ((System.currentTimeMillis() - startTime) < testTime) {
      String memoryUsageString = getMyTracksPssMemoryInfo(context);
      String batteryUsageString = getBatteryUsageInfo(context);
      String oneInfo = String.format("{%s: memory use %s, battery left %s. \r\n",
          String.format("{%1$tm/%1$td/%1$tY %1$tH:%1$tM:%1$tS", new Date()), memoryUsageString,
          batteryUsageString);
      writeToFile(oneInfo, true);
      Log.i(EndToEndTestUtils.LOG_TAG, oneInfo);
      EndToEndTestUtils.sleep(interval);
    }
  }

  /**
   * Unlocks and wakes up the device.
   */
  public static void unlockAndWakeupDevice() {
    // The menu item of setting should be found if the screen is not locked.
    try {
      EndToEndTestUtils.SOLO.getCurrentActivity().runOnUiThread(new Runnable() {
        @Override
        public void run() {
          Window wind = EndToEndTestUtils.SOLO.getCurrentActivity().getWindow();
          wind.addFlags(LayoutParams.FLAG_DISMISS_KEYGUARD);
          wind.addFlags(LayoutParams.FLAG_SHOW_WHEN_LOCKED);
          wind.addFlags(LayoutParams.FLAG_TURN_SCREEN_ON);
        }
      });

    } catch (Exception e) {
      Log.i(EndToEndTestUtils.LOG_TAG,
          "Meet error when unlock device screen, may the device is not locked.");
    }
  }

}