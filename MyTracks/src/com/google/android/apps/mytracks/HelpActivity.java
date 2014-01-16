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

import com.google.android.apps.mytracks.fragments.EulaDialogFragment;
import com.google.android.apps.mytracks.fragments.EulaDialogFragment.EulaCaller;
import com.google.android.apps.mytracks.util.ApiAdapterFactory;
import com.google.android.apps.mytracks.util.GoogleFeedbackUtils;
import com.google.android.apps.mytracks.util.SystemUtils;
import com.google.android.maps.mytracks.R;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;

import java.util.Locale;

/**
 * An activity that displays the help page.
 * 
 * @author Sandor Dornbush
 */
public class HelpActivity extends AbstractMyTracksActivity implements EulaCaller {

  WebView webView;

  @SuppressLint("SetJavaScriptEnabled")
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    String subtitle = getString(R.string.my_tracks_app_name) + " v"
        + SystemUtils.getMyTracksVersion(this);
    ApiAdapterFactory.getApiAdapter()
        .setTitleAndSubtitle(this, getString(R.string.menu_help), subtitle);

    webView = (WebView) findViewById(R.id.help_webview);
    webView.getSettings().setJavaScriptEnabled(true);
    webView.setWebViewClient(new WebViewClient());

    if (savedInstanceState == null) {
      String language = Locale.getDefault().getLanguage();
      if (language == null || language.equals("")) {
        language = "en";
      }
      webView.loadUrl(getString(R.string.my_tracks_help_url, language));
    } else {
      webView.restoreState(savedInstanceState);
    }

    Button feedback = (Button) findViewById(R.id.help_feedback);
    boolean showFeedback = ApiAdapterFactory.getApiAdapter().isGoogleFeedbackAvailable();
    feedback.setVisibility(showFeedback ? View.VISIBLE : View.GONE);
    if (showFeedback) {
      feedback.setOnClickListener(new View.OnClickListener() {
          @Override
        public void onClick(View v) {
          GoogleFeedbackUtils.bindFeedback(HelpActivity.this);
        }
      });
    }
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    if (webView != null) {
      webView.saveState(outState);
    }
  }

  @Override
  protected int getLayoutResId() {
    return R.layout.help;
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.help, menu);
    return super.onCreateOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    Intent intent;
    switch (item.getItemId()) {
      case R.id.help_play_store:
        intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("market://details?id=com.google.android.maps.mytracks"));
        startActivity(intent);
        return true;
      case R.id.help_eula:
        EulaDialogFragment.newInstance(true)
            .show(getSupportFragmentManager(), EulaDialogFragment.EULA_DIALOG_TAG);
        return true;
      case R.id.help_forum:
        intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(
            "http://groups.google.com/a/googleproductforums.com/forum/#!categories/maps/mytracks/"));
        startActivity(intent);
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  @Override
  public void onEulaDone() {
    // Do nothing
  }
}
