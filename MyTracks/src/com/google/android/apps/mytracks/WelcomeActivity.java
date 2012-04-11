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

import com.google.android.apps.mytracks.util.EulaUtils;
import com.google.android.apps.mytracks.util.SystemUtils;
import com.google.android.maps.mytracks.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

/**
 * An activity that displays a welcome screen.
 *
 * @author Sandor Dornbush
 */
public class WelcomeActivity extends Activity {

  private static final int DIALOG_ABOUT_ID = 0;
  private static final int DIALOG_EULA_ID = 1;
  
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.welcome);
    findViewById(R.id.welcome_ok).setOnClickListener(new OnClickListener() {
      public void onClick(View v) {
        finish();
      }
    });
    findViewById(R.id.welcome_about).setOnClickListener(new OnClickListener() {
      public void onClick(View v) {
        showDialog(DIALOG_ABOUT_ID);
      }
    });
  }

  @Override
  protected Dialog onCreateDialog(int id) {
    switch (id) {
      case DIALOG_ABOUT_ID:
        View view = getLayoutInflater().inflate(R.layout.about, null);
        TextView aboutVersion = (TextView) view.findViewById(R.id.about_version);
        aboutVersion.setText(SystemUtils.getMyTracksVersion(this));
        return new AlertDialog.Builder(this)
            .setCancelable(true)
            .setNegativeButton(R.string.about_license, new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialog, int which) {
                showDialog(DIALOG_EULA_ID);
              }
            })
            .setPositiveButton(R.string.generic_ok, null)
            .setTitle(R.string.my_tracks_app_name)
            .setView(view)
            .create();
      case DIALOG_EULA_ID:
        return new AlertDialog.Builder(this)
            .setCancelable(true)
            .setMessage(EulaUtils.getEulaMessage(this))
            .setPositiveButton(R.string.generic_ok, null)
            .setTitle(R.string.eula_title)
            .create();
      default:
        return null;
    }
  }
}
