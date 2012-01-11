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
import com.google.android.apps.mytracks.util.ApiFeatures;
import com.google.android.maps.mytracks.R;

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
 * @author jshih@google.com (Jimmy Shih)
 */
public class UploadServiceChooserActivity extends Activity {

  /**
   * The track id.
   */
  public static final String TRACK_ID = "trackId";
  
  /**
   * The send type. Null to send to all Google services.
   */
  public static final String SEND_TYPE = "sendType";

  private static final int SERVICE_PICKER_DIALOG = 1;

  private long trackId;
  private SendType sendType;

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

    Intent intent = getIntent();
    trackId = intent.getLongExtra(TRACK_ID, -1L);
    sendType = intent.getParcelableExtra(SEND_TYPE);
    showDialog(SERVICE_PICKER_DIALOG);
  }

  @Override
  protected Dialog onCreateDialog(int id) {
    switch (id) {
      case SERVICE_PICKER_DIALOG:
        final Dialog dialog = new Dialog(this);
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
            Intent intent = new Intent(UploadServiceChooserActivity.this, SendActivity.class);
            intent.putExtra(SendActivity.TRACK_ID, trackId);
            intent.putExtra(SendActivity.SHARE_URL, sendType != null);
            intent.putExtra(SendActivity.SEND_MAPS, sendMaps());
            intent.putExtra(SendActivity.SEND_FUSION_TABLES, sendFusionTables());
            intent.putExtra(SendActivity.SEND_DOCS, sendDocs());
            intent.putExtra(SendActivity.CREATE_MAP, !existingMapRadioButton.isChecked());
            startActivity(intent);
            finish();
          }
        });

        // Setup initial state
        initState();

        // Update state based on sendType
        updateStateBySendType();

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
  private void initState() {
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
   * Updates the UI state based on the sendType.
   */
  private void updateStateBySendType() {
    if (sendType == SendType.MAPS) {
      mapsCheckBox.setChecked(true);
    } else if (sendType == SendType.FUSION_TABLES) {
      fusionTablesCheckBox.setChecked(true);
    } else if (sendType == SendType.DOCS) {
      docsCheckBox.setChecked(true);
    } else {
      // sendType == null
    }
    mapsTableRow.setVisibility(showMaps() ? View.VISIBLE : View.GONE);
    fusionTablesTableRow.setVisibility(showFusionTables() ? View.VISIBLE : View.GONE);
    docsTableRow.setVisibility(showDocs() ? View.VISIBLE : View.GONE);
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
  private void saveState() {
    SharedPreferences prefs = getSharedPreferences(Constants.SETTINGS_NAME, Context.MODE_PRIVATE);
    Editor editor = prefs.edit();
    editor.putBoolean(
        getString(R.string.pick_existing_map_key), existingMapRadioButton.isChecked());
    if (sendType == null) {
      editor.putBoolean(getString(R.string.send_to_maps_key), sendMaps());
      editor.putBoolean(getString(R.string.send_to_fusion_tables_key), sendFusionTables());
      editor.putBoolean(getString(R.string.send_to_docs_key), sendDocs());
    }
    ApiFeatures.getInstance().getApiAdapter().applyPreferenceChanges(editor);
  }

  /**
   * Returns true to show the Google Maps option.
   */
  private boolean showMaps() {
    return sendType == null || sendType == SendType.MAPS;
  }

  /**
   * Returns true to show the Google Fusion Tables option.
   */
  private boolean showFusionTables() {
    return sendType == null || sendType == SendType.FUSION_TABLES;
  }

  /**
   * Returns true to show the Google Docs option.
   */
  private boolean showDocs() {
    return sendType == null || sendType == SendType.DOCS;
  }

  /**
   * Returns true to send to Google Maps.
   */
  private boolean sendMaps() {
    return showMaps() && mapsCheckBox.isChecked();
  }

  /**
   * Returns true to send to Google Fusion Tables.
   */
  private boolean sendFusionTables() {
    return showFusionTables() && fusionTablesCheckBox.isChecked();
  }

  /**
   * Returns true to send to Google Docs.
   */
  private boolean sendDocs() {
    return showDocs() && docsCheckBox.isChecked();
  }
}
