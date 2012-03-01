/*
 * Copyright 2012 Google Inc.
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
package com.google.android.apps.mytracks.io.sendtogoogle;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import com.google.android.apps.mytracks.Constants;
import com.google.android.apps.mytracks.util.ApiAdapterFactory;
import com.google.android.apps.mytracks.util.SystemUtils;
import com.google.android.maps.mytracks.R;
import com.google.common.annotations.VisibleForTesting;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.RadioButton;
import android.widget.TableRow;

/**
 * A chooser to select the Google services to upload a track to.
 *
 * @author Jimmy Shih
 */
public class UploadServiceChooserActivity extends Activity {

  private static final int SERVICE_PICKER_DIALOG = 1;

  private SendRequest sendRequest;
  private Dialog dialog;

  private TableRow mapsTableRow;
  private TableRow fusionTablesTableRow;
  private TableRow docsTableRow;

  private CheckBox mapsCheckBox;
  private CheckBox fusionTablesCheckBox;
  private CheckBox docsCheckBox;

  private TableRow mapsOptionTableRow;
  private RadioButton newMapRadioButton;
  private RadioButton existingMapRadioButton;

  private Button cancel;
  private Button send;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    sendRequest = getIntent().getParcelableExtra(SendRequest.SEND_REQUEST_KEY);
  }

  @Override
  protected void onResume() {
    super.onResume();
    showDialog(SERVICE_PICKER_DIALOG);
  }

  @Override
  protected Dialog onCreateDialog(int id) {
    switch (id) {
      case SERVICE_PICKER_DIALOG:
        dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.upload_service_chooser);
        dialog.setCancelable(true);
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
          @Override
          public void onCancel(DialogInterface d) {
            finish();
          }
        });

        mapsTableRow = (TableRow) dialog.findViewById(R.id.send_google_maps_row);
        fusionTablesTableRow = (TableRow) dialog.findViewById(R.id.send_google_fusion_tables_row);
        docsTableRow = (TableRow) dialog.findViewById(R.id.send_google_docs_row);

        mapsCheckBox = (CheckBox) dialog.findViewById(R.id.send_google_maps);
        fusionTablesCheckBox = (CheckBox) dialog.findViewById(R.id.send_google_fusion_tables);
        docsCheckBox = (CheckBox) dialog.findViewById(R.id.send_google_docs);

        mapsOptionTableRow = (TableRow) dialog.findViewById(R.id.send_google_maps_option_row);
        newMapRadioButton = (RadioButton) dialog.findViewById(R.id.send_google_new_map);
        existingMapRadioButton = (RadioButton) dialog.findViewById(R.id.send_google_existing_map);

        // Setup checkboxes
        OnCheckedChangeListener checkBoxListener = new OnCheckedChangeListener() {
          public void onCheckedChanged(CompoundButton button, boolean checked) {
            updateStateBySelection();
          }
        };
        mapsCheckBox.setOnCheckedChangeListener(checkBoxListener);
        fusionTablesCheckBox.setOnCheckedChangeListener(checkBoxListener);
        docsCheckBox.setOnCheckedChangeListener(checkBoxListener);

        // Setup buttons
        cancel = (Button) dialog.findViewById(R.id.send_google_cancel);
        cancel.setOnClickListener(new View.OnClickListener() {
          public void onClick(View v) {
            finish();
          }
        });
        send = (Button) dialog.findViewById(R.id.send_google_send_now);
        send.setOnClickListener(new View.OnClickListener() {
          public void onClick(View v) {
            saveState();
            startNextActivity();
          }
        });

        // Setup initial state
        initState();

        // Update state based on sendRequest
        updateStateBySendRequest();

        // Update state based on current user selection
        updateStateBySelection();

        return dialog;
      default:
        return null;
    }
  }

  /**
   * Initializes the UI state based on the shared preferences.
   */
  @VisibleForTesting
  void initState() {
    SharedPreferences prefs = getSharedPreferences(Constants.SETTINGS_NAME, Context.MODE_PRIVATE);
    boolean pickExistingMap = prefs.getBoolean(getString(R.string.pick_existing_map_key), false);

    newMapRadioButton.setChecked(!pickExistingMap);
    existingMapRadioButton.setChecked(pickExistingMap);

    mapsCheckBox.setChecked(prefs.getBoolean(getString(R.string.send_to_maps_key), true));
    fusionTablesCheckBox.setChecked(
        prefs.getBoolean(getString(R.string.send_to_fusion_tables_key), true));
    docsCheckBox.setChecked(prefs.getBoolean(getString(R.string.send_to_docs_key), true));
  }

  /**
   * Updates the UI state based on sendRequest.
   */
  private void updateStateBySendRequest() {
    if (!sendRequest.isShowAll()) {
      if (sendRequest.isShowMaps()) {
        mapsCheckBox.setChecked(true);
      } else if (sendRequest.isShowFusionTables()) {
        fusionTablesCheckBox.setChecked(true);
      } else if (sendRequest.isShowDocs()) {
        docsCheckBox.setChecked(true);
      }
    }
    mapsTableRow.setVisibility(sendRequest.isShowMaps() ? View.VISIBLE : View.GONE);
    fusionTablesTableRow.setVisibility(sendRequest.isShowFusionTables() ? View.VISIBLE : View.GONE);
    docsTableRow.setVisibility(sendRequest.isShowDocs() ? View.VISIBLE : View.GONE);
  }

  /**
   * Updates the UI state based on the current selection.
   */
  private void updateStateBySelection() {
    mapsOptionTableRow.setVisibility(sendMaps() ? View.VISIBLE : View.GONE);
    send.setEnabled(sendMaps() || sendFusionTables() || sendDocs());
  }

  /**
   * Saves the UI state to the shared preferences.
   */
  @VisibleForTesting
  void saveState() {
    SharedPreferences prefs = getSharedPreferences(Constants.SETTINGS_NAME, Context.MODE_PRIVATE);
    Editor editor = prefs.edit();
    editor.putBoolean(
        getString(R.string.pick_existing_map_key), existingMapRadioButton.isChecked());
    if (sendRequest.isShowAll()) {
      editor.putBoolean(getString(R.string.send_to_maps_key), sendMaps());
      editor.putBoolean(getString(R.string.send_to_fusion_tables_key), sendFusionTables());
      editor.putBoolean(getString(R.string.send_to_docs_key), sendDocs());
    }
    ApiAdapterFactory.getApiAdapter().applyPreferenceChanges(editor);
  }

  /**
   * Returns true to send to Google Maps.
   */
  private boolean sendMaps() {
    return sendRequest.isShowMaps() && mapsCheckBox.isChecked();
  }

  /**
   * Returns true to send to Google Fusion Tables.
   */
  private boolean sendFusionTables() {
    return sendRequest.isShowFusionTables() && fusionTablesCheckBox.isChecked();
  }

  /**
   * Returns true to send to Google Docs.
   */
  private boolean sendDocs() {
    return sendRequest.isShowDocs() && docsCheckBox.isChecked();
  }

  /**
   * Starts the next activity, {@link AccountChooserActivity}.
   */
  @VisibleForTesting
  protected void startNextActivity() {
    sendStats();
    sendRequest.setSendMaps(sendMaps());
    sendRequest.setSendFusionTables(sendFusionTables());
    sendRequest.setSendDocs(sendDocs());
    sendRequest.setNewMap(!existingMapRadioButton.isChecked());
    Intent intent = new Intent(this, AccountChooserActivity.class)
        .putExtra(SendRequest.SEND_REQUEST_KEY, sendRequest);
    startActivity(intent);
    finish();
  }
  
  /**
   * Sends stats to Google Analytics.
   */
  private void sendStats() {
    GoogleAnalyticsTracker tracker = GoogleAnalyticsTracker.getInstance();
    tracker.start(getString(R.string.my_tracks_analytics_id), getApplicationContext());
    tracker.setProductVersion("android-mytracks", SystemUtils.getMyTracksVersion(this));
    if (sendRequest.isSendMaps()) {
      tracker.trackPageView("/send/maps");
    }
    if (sendRequest.isSendFusionTables()) {
      tracker.trackPageView("/send/fusion_tables");
    }
    if (sendRequest.isSendDocs()) {
      tracker.trackPageView("/send/docs");
    }
    tracker.dispatch();
    tracker.stop();
  }
  
  @VisibleForTesting
  Dialog getDialog() {
    return dialog;
  }
  
  @VisibleForTesting
  SendRequest getSendRequest() {
    return sendRequest;
  }
}
