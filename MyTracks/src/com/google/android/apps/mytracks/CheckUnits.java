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

import com.google.android.apps.mytracks.util.ApiAdapterFactory;
import com.google.android.maps.mytracks.R;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;

/**
 * Checks with the user if he prefers the metric units or the imperial units.
 *
 * @author Sandor Dornbush
 */
class CheckUnits {
  private static final String CHECK_UNITS_PREFERENCE_FILE = "checkunits";
  private static final String CHECK_UNITS_PREFERENCE_KEY = "checkunits.checked";
  private static boolean metric = true;

  public static void check(final Context context) {
    final SharedPreferences checkUnitsSharedPreferences = context.getSharedPreferences(
        CHECK_UNITS_PREFERENCE_FILE, Context.MODE_PRIVATE);
    if (checkUnitsSharedPreferences.getBoolean(CHECK_UNITS_PREFERENCE_KEY, false)) {
      return;
    }

    AlertDialog.Builder builder = new AlertDialog.Builder(context);
    builder.setTitle(context.getString(R.string.preferred_units_title));
    CharSequence[] items = { context.getString(R.string.preferred_units_metric),
        context.getString(R.string.preferred_units_imperial) };
    builder.setSingleChoiceItems(items, 0, new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {
        if (which == 0) {
          metric = true;
        } else {
          metric = false;
        }
      }
    });
    builder.setCancelable(true);
    builder.setPositiveButton(R.string.generic_ok, new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int which) {
        recordCheckPerformed(checkUnitsSharedPreferences);
        SharedPreferences useMetricPreferences = context.getSharedPreferences(
            Constants.SETTINGS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = useMetricPreferences.edit();
        String key = context.getString(R.string.metric_units_key);
        ApiAdapterFactory.getApiAdapter().applyPreferenceChanges(editor.putBoolean(key, metric));
      }
    });
    builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
      public void onCancel(DialogInterface dialog) {
        recordCheckPerformed(checkUnitsSharedPreferences);
      }
    });
    builder.show();
  }

  private static void recordCheckPerformed(SharedPreferences preferences) {
    ApiAdapterFactory.getApiAdapter().applyPreferenceChanges(
        preferences.edit().putBoolean(CHECK_UNITS_PREFERENCE_KEY, true));
  }

  private CheckUnits() {}
}
