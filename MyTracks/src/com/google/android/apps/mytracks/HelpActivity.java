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

import com.google.android.apps.mytracks.fragments.AboutDialogFragment;
import com.google.android.apps.mytracks.fragments.AboutDialogFragment.AboutCaller;
import com.google.android.apps.mytracks.fragments.EulaDialogFragment;
import com.google.android.apps.mytracks.fragments.EulaDialogFragment.EulaCaller;
import com.google.android.maps.mytracks.R;

import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.util.Locale;

/**
 * An activity that displays the help page.
 * 
 * @author Sandor Dornbush
 */
public class HelpActivity extends AbstractMyTracksActivity implements AboutCaller, EulaCaller {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    WebView webView = (WebView) findViewById(R.id.help_webview);
    String language = Locale.getDefault().getLanguage();
    if (language == null || language.equals("")) {
      language = "en";
    }
    webView.loadUrl(getString(R.string.my_tracks_help_url, language));
    webView.getSettings().setJavaScriptEnabled(true);
    webView.setWebViewClient(new WebViewClient());

    findViewById(R.id.help_ok).setOnClickListener(new OnClickListener() {
      public void onClick(View v) {
        finish();
      }
    });
    findViewById(R.id.help_about).setOnClickListener(new OnClickListener() {
      public void onClick(View v) {
        new AboutDialogFragment().show(
            getSupportFragmentManager(), AboutDialogFragment.ABOUT_DIALOG_TAG);
      }
    });
  }

  @Override
  protected int getLayoutResId() {
    return R.layout.help;
  }

  @Override
  public void onAboutLicense() {
    EulaDialogFragment.newInstance(true)
        .show(getSupportFragmentManager(), EulaDialogFragment.EULA_DIALOG_TAG);
  }

  @Override
  public void onEulaDone() {
    // Do nothing
  }
}
