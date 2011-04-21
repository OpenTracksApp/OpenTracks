package com.google.android.apps.mytracks.util;

import com.google.android.apps.mytracks.Constants;

import android.content.SharedPreferences.Editor;
import android.os.StrictMode;
import android.util.Log;

/**
 * The Gingerbread (API level 9) specific implememntation of the
 * {@link ApiPlatformAdapter}.
 *
 * @author Rodrigo Damazio
 */
public class GingerbreadPlatformAdapter extends EclairPlatformAdapter {
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
}
