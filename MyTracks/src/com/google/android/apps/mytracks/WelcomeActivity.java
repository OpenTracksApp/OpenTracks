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

import com.google.android.apps.mytracks.util.SystemUtils;
import com.google.android.maps.mytracks.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

/**
 * An activity that displays a welcome screen.
 *
 * @author Sandor Dornbush
 */
public class WelcomeActivity extends Activity {

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
        showAbout();
      }
    });
  }

  /**
   * Shows the "About My Tracks" dialog.
   */   
  private void showAbout() {
    LayoutInflater layoutInflator = LayoutInflater.from(this);
    View view = layoutInflator.inflate(R.layout.about, null);
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setView(view);
    builder.setPositiveButton(R.string.generic_ok, null);
    builder.setNegativeButton(R.string.about_license, new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int which) {
        Eula.showEula(WelcomeActivity.this);
      }
    });
    AlertDialog dialog = builder.create();
    dialog.show();
    
    TextView aboutVersionTextView = (TextView) dialog.findViewById(R.id.about_version);
    aboutVersionTextView.setText(SystemUtils.getMyTracksVersion(this));
  }
}
