package com.google.android.apps.mytracks.util;

import com.google.android.apps.mytracks.Constants;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;

import android.content.SharedPreferences.Editor;
import android.os.StrictMode;
import android.util.Log;

import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import java.util.Locale;

/**
 * API level 9 specific implementation of the {@link ApiLevelAdapter}.
 *
 * @author Rodrigo Damazio
 */
public class ApiLevel9Adapter extends ApiLevel8Adapter {
  
  @Override
  public void applyPreferenceChanges(Editor editor) {
    // Apply asynchronously
    editor.apply();
  }

  @Override
  public void enableStrictMode() {
    Log.d(Constants.TAG, "Enabling strict mode");
    StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
        .detectDiskWrites()
        .detectNetwork()
        .penaltyLog()
        .build());
    StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
        .detectAll()
        .penaltyLog()
        .build());
  }
  
  @Override
  public byte[] copyByteArray(byte[] input, int start, int end) {
    return Arrays.copyOfRange(input, start, end);
  }
  
  @Override
  public DecimalFormatSymbols getDecimalFormatSymbols(Locale locale) {
    return DecimalFormatSymbols.getInstance(locale);
  }
  
  @Override
  public HttpTransport getHttpTransport() {
    return new NetHttpTransport();
  }
}
