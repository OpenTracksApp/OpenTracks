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

import com.google.android.apps.mytracks.util.ApiFeatures;
import com.google.android.apps.mytracks.util.ResourceUtils;
import com.google.android.maps.mytracks.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;

/**
 * This class handles display of EULAs ("End User License Agreements") to the
 * user.
 */
class Eula {
  private static final String PREFERENCE_EULA_ACCEPTED = "eula.accepted";
  private static final String PREFERENCES_EULA = "eula";

  private Eula() {}

  /**
   * Displays the EULA if necessary. This method should be called from the
   * onCreate() method of your main Activity.  If the user accepts, the EULA
   * will never be displayed again.  If the user refuses, the activity will
   * finish (exit).
   *
   * @param activity The Activity to finish if the user rejects the EULA
   */
  static void showEulaRequireAcceptance(final Activity activity) {
    final SharedPreferences preferences =
        activity.getSharedPreferences(PREFERENCES_EULA, Activity.MODE_PRIVATE);
    if (preferences.getBoolean(PREFERENCE_EULA_ACCEPTED, false)) {
      return;
    }

    final AlertDialog.Builder builder = initDialog(activity);
    builder.setPositiveButton(R.string.accept,
        new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            accept(activity, preferences);
          }
        });
    builder.setNegativeButton(R.string.decline,
        new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            refuse(activity);
          }
        });
    builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
      @Override
      public void onCancel(DialogInterface dialog) {
        refuse(activity);
      }
    });
    builder.show();
  }

  /**
   * Display the EULA to the user in an informational context.  They won't be
   * given the choice of accepting or declining the EULA -- we're simply
   * displaying it for them to read.
   */
  static void showEula(Context context) {
    AlertDialog.Builder builder = initDialog(context);
    builder.setPositiveButton(R.string.ok, null);
    builder.show();
  }
  
  private static AlertDialog.Builder initDialog(Context context) {
    AlertDialog.Builder builder = new AlertDialog.Builder(context);
    builder.setCancelable(true);
    builder.setTitle(R.string.eula_title);
    builder.setMessage(ResourceUtils.readFile(context, R.raw.eula));
    return builder;
  }

  private static void accept(Activity activity, SharedPreferences preferences) {
    ApiFeatures.getInstance().getApiPlatformAdapter().applyPreferenceChanges(
        preferences.edit().putBoolean(PREFERENCE_EULA_ACCEPTED, true));
    Intent startIntent = new Intent(activity, WelcomeActivity.class);
    activity.startActivityForResult(startIntent, Constants.WELCOME);
  }

  private static void refuse(Activity activity) {
    activity.finish();
  }
}
