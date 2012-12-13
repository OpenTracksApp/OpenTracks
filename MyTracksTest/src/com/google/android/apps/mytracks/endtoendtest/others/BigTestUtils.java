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

  /**
   * Set to false as default. True to run the test. Default to false since this
   * test can take a long time.
   */
  public static boolean runStressTest = true;
  public static boolean runSensorTest = true;
  public static boolean runResourceUsageTest = true;
  protected static final String DISABLE_MESSAGE = "This test is disabled";
  protected final static String MYTRACKS_PROCESS_NAME = "com.google.android.maps.mytracks";
  protected final static String MYTRACKS_TEST_INFO_FILE = "MyTracksTestInfo.txt";

  /**
   * Gets the memory usage of MyTracks process.
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
    return "Phone use " + rawlevel + " of " + scale + " battery";
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
    long startTime = (new Date()).getTime();

    while (((new Date()).getTime() - startTime) < testTime) {
      String memoryUsageString = getMyTracksPssMemoryInfo(context);
      String batteryUsageString = getBatteryUsageInfo(context);
      String oneInfo = (new java.util.Date()).toString() + ":" + memoryUsageString + ", "
          + batteryUsageString + ".\r\n";
      writeToFile(oneInfo, true);
      EndToEndTestUtils.sleep(interval);
    }
  }

}