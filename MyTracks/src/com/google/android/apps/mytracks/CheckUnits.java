/*
 * Copyright 2010 Google Inc.
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
import com.google.android.maps.mytracks.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;

import java.util.Locale;

/**
 * Check to see if the units are the default for the locale.
 * This comes down to warning Americans that the default is metric.
 *
 * @author Sandor Dornbush
 */
class CheckUnits {
  private static final String PREFERENCE_UNITS_CHECKED = "checkunits.checked";
  private static final String PREFERENCES_CHECK_UNITS = "checkunits";

  static void check(final Context context) {
    final SharedPreferences preferences =
        context.getSharedPreferences(PREFERENCES_CHECK_UNITS, Activity.MODE_PRIVATE);

    // Has the user already warned about the default units?
    if (preferences.getBoolean(PREFERENCE_UNITS_CHECKED, false)) {
      return;
    }

    // Is the user in the US?
    Locale current = Locale.getDefault();
    Locale enUs = new Locale(Locale.ENGLISH.getLanguage(),
                             Locale.US.getCountry());
    if (!current.equals(enUs)) {
      return;
    }

    final AlertDialog.Builder builder = new AlertDialog.Builder(context);
    builder.setTitle(R.string.check_units_title);
    builder.setCancelable(true);
    builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int which) {
        accept(context, preferences);
      }
    });
    builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int which) {
        recordCheckPerformed(preferences);
      }
    });
    builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
      public void onCancel(DialogInterface dialog) {
        recordCheckPerformed(preferences);
      }
    });
    builder.setMessage(R.string.check_units_message);
    builder.show();
  }

  private static void accept(Context context, SharedPreferences preferences) {
    recordCheckPerformed(preferences);
    Intent startIntent = new Intent(context, SettingsActivity.class);
    startIntent.putExtra(context.getString(R.string.open_settings_screen), 
                         context.getString(R.string.settings_display));
    context.startActivity(startIntent);
  }

  private static void recordCheckPerformed(SharedPreferences preferences) {
    ApiFeatures.getInstance().getApiPlatformAdapter().applyPreferenceChanges(
        preferences.edit().putBoolean(PREFERENCE_UNITS_CHECKED, true));
  }

  private CheckUnits() {
  }
}
