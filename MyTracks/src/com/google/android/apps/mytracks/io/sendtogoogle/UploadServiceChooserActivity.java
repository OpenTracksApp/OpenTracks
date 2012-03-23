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

import com.google.android.apps.mytracks.Constants;
import com.google.android.apps.mytracks.util.AnalyticsUtils;
import com.google.android.apps.mytracks.util.ApiAdapterFactory;
import com.google.android.maps.mytracks.R;
import com.google.common.annotations.VisibleForTesting;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.RadioButton;
import android.widget.TableRow;
import android.widget.Toast;

import java.util.ArrayList;

/**
 * A chooser to select the Google services to upload a track to.
 * 
 * @author Jimmy Shih
 */
public class UploadServiceChooserActivity extends Activity {

  private static final int DIALOG_CHOOSER_ID = 0;

  private SendRequest sendRequest;
  private AlertDialog alertDialog;

  private TableRow mapsTableRow;
  private TableRow fusionTablesTableRow;
  private TableRow docsTableRow;

  private CheckBox mapsCheckBox;
  private CheckBox fusionTablesCheckBox;
  private CheckBox docsCheckBox;

  private TableRow mapsOptionTableRow;
  private RadioButton newMapRadioButton;
  private RadioButton existingMapRadioButton;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    sendRequest = getIntent().getParcelableExtra(SendRequest.SEND_REQUEST_KEY);
  }

  @Override
  protected void onResume() {
    super.onResume();
    showDialog(DIALOG_CHOOSER_ID);
  }

  @Override
  protected Dialog onCreateDialog(int id) {
    if (id != DIALOG_CHOOSER_ID) {
      return null;
    }
    View view = getLayoutInflater().inflate(R.layout.upload_service_chooser, null);

    mapsTableRow = (TableRow) view.findViewById(R.id.send_google_maps_row);
    fusionTablesTableRow = (TableRow) view.findViewById(R.id.send_google_fusion_tables_row);
    docsTableRow = (TableRow) view.findViewById(R.id.send_google_docs_row);

    mapsCheckBox = (CheckBox) view.findViewById(R.id.send_google_maps);
    fusionTablesCheckBox = (CheckBox) view.findViewById(R.id.send_google_fusion_tables);
    docsCheckBox = (CheckBox) view.findViewById(R.id.send_google_docs);

    mapsOptionTableRow = (TableRow) view.findViewById(R.id.send_google_maps_option_row);
    newMapRadioButton = (RadioButton) view.findViewById(R.id.send_google_new_map);
    existingMapRadioButton = (RadioButton) view.findViewById(R.id.send_google_existing_map);

    // Setup checkboxes
    OnCheckedChangeListener checkBoxListener = new OnCheckedChangeListener() {
      public void onCheckedChanged(CompoundButton button, boolean checked) {
        updateStateBySelection();
      }
    };
    mapsCheckBox.setOnCheckedChangeListener(checkBoxListener);
    fusionTablesCheckBox.setOnCheckedChangeListener(checkBoxListener);
    docsCheckBox.setOnCheckedChangeListener(checkBoxListener);

    // Setup initial state
    initState();

    // Update state based on sendRequest
    updateStateBySendRequest();

    // Update state based on current selection
    updateStateBySelection();

    alertDialog = new AlertDialog.Builder(this)
        .setCancelable(true)
        .setNegativeButton(R.string.generic_cancel, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            finish();
          }
        })
        .setOnCancelListener(new DialogInterface.OnCancelListener() {
          @Override
          public void onCancel(DialogInterface d) {
            finish();
          }
        })
        .setPositiveButton(R.string.send_google_send_now, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            saveState();
            if (sendMaps() || sendFusionTables() || sendDocs()) {
              startNextActivity();
            } else {
              Toast.makeText(UploadServiceChooserActivity.this,
                  R.string.send_google_no_service_selected, Toast.LENGTH_LONG).show();
              finish();
            }
          }
        })
        .setTitle(R.string.send_google_title)
        .setView(view)
        .create();
    return alertDialog;
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
    ArrayList<String> pages = new ArrayList<String>();
    if (sendRequest.isSendMaps()) {
      pages.add("/send/maps");
    }
    if (sendRequest.isSendFusionTables()) {
      pages.add("/send/fusion_tables");
    }
    if (sendRequest.isSendDocs()) {
      pages.add("/send/docs");
    }
    AnalyticsUtils.sendPageViews(this, pages.toArray(new String[pages.size()]));
  }

  @VisibleForTesting
  AlertDialog getAlertDialog() {
    return alertDialog;
  }

  @VisibleForTesting
  SendRequest getSendRequest() {
    return sendRequest;
  }
}
