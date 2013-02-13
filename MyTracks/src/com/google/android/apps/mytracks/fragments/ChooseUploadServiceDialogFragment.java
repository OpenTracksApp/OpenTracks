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

package com.google.android.apps.mytracks.fragments;

import com.google.android.apps.mytracks.io.sendtogoogle.AccountChooserActivity;
import com.google.android.apps.mytracks.io.sendtogoogle.SendRequest;
import com.google.android.apps.mytracks.util.AnalyticsUtils;
import com.google.android.apps.mytracks.util.IntentUtils;
import com.google.android.apps.mytracks.util.PreferencesUtils;
import com.google.android.maps.mytracks.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.RadioButton;
import android.widget.TableRow;
import android.widget.Toast;

/**
 * A DialogFragment to choose upload service.
 * 
 * @author Jimmy Shih
 */
public class ChooseUploadServiceDialogFragment extends DialogFragment {

  public static final String CHOOSE_UPLOAD_SERVICE_DIALOG_TAG = "chooseUploadService";

  private static final String KEY_SEND_REQUEST = "sendRequest";
  private static final String KEY_HIDE_DRIVE = "hideDrive";

  public static ChooseUploadServiceDialogFragment newInstance(
      SendRequest sendRequest, boolean hideDrive) {
    Bundle bundle = new Bundle();
    bundle.putParcelable(KEY_SEND_REQUEST, sendRequest);
    bundle.putBoolean(KEY_HIDE_DRIVE, hideDrive);

    ChooseUploadServiceDialogFragment chooseUploadServiceDialogFragment = new ChooseUploadServiceDialogFragment();
    chooseUploadServiceDialogFragment.setArguments(bundle);
    return chooseUploadServiceDialogFragment;
  }

  private FragmentActivity activity;
  private SendRequest sendRequest;
  private boolean hideDrive;

  private CheckBox driveCheckBox;
  private CheckBox mapsCheckBox;
  private CheckBox fusionTablesCheckBox;
  private CheckBox spreadsheetsCheckBox;

  private TableRow mapsOptionTableRow;
  private RadioButton existingMapRadioButton;

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    activity = getActivity();
    sendRequest = getArguments().getParcelable(KEY_SEND_REQUEST);
    hideDrive = getArguments().getBoolean(KEY_HIDE_DRIVE);

    View view = activity.getLayoutInflater().inflate(R.layout.choose_upload_service, null);

    view.findViewById(R.id.choose_upload_service_drive_options)
        .setVisibility(hideDrive ? View.GONE : View.VISIBLE);

    driveCheckBox = (CheckBox) view.findViewById(R.id.choose_upload_service_drive);
    mapsCheckBox = (CheckBox) view.findViewById(R.id.choose_upload_service_maps);
    fusionTablesCheckBox = (CheckBox) view.findViewById(R.id.choose_upload_service_fusion_tables);
    spreadsheetsCheckBox = (CheckBox) view.findViewById(R.id.choose_upload_service_spreadsheets);

    driveCheckBox.setChecked(PreferencesUtils.getBoolean(
        activity, R.string.send_to_drive_key, PreferencesUtils.SEND_TO_DRIVE_DEFAULT));
    mapsCheckBox.setChecked(PreferencesUtils.getBoolean(
        activity, R.string.send_to_maps_key, PreferencesUtils.SEND_TO_MAPS_DEFAULT));
    fusionTablesCheckBox.setChecked(PreferencesUtils.getBoolean(
        activity, R.string.send_to_fusion_tables_key,
        PreferencesUtils.SEND_TO_FUSION_TABLES_DEFAULT));

    spreadsheetsCheckBox.setChecked(PreferencesUtils.getBoolean(
        activity, R.string.send_to_spreadsheets_key,
        PreferencesUtils.SEND_TO_SPREADSHEETS_DEFAULT));

    mapsCheckBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
      public void onCheckedChanged(CompoundButton button, boolean checked) {
        updateMapsOption();
      }
    });

    mapsOptionTableRow = (TableRow) view.findViewById(R.id.choose_upload_service_maps_options);

    RadioButton newMapRadioButton = (RadioButton) view.findViewById(
        R.id.choose_upload_service_new_map);
    boolean defaultMapPublic = PreferencesUtils.getBoolean(
        activity, R.string.default_map_public_key, PreferencesUtils.DEFAULT_MAP_PUBLIC_DEFAULT);
    newMapRadioButton.setText(defaultMapPublic ? R.string.send_google_new_public_map
        : R.string.send_google_new_unlisted_map);

    existingMapRadioButton = (RadioButton) view.findViewById(
        R.id.choose_upload_service_existing_map);

    updateMapsOption();
    if (PreferencesUtils.getBoolean(
        activity, R.string.pick_existing_map_key, PreferencesUtils.PICK_EXISTING_MAP_DEFAULT)) {
      existingMapRadioButton.setChecked(true);
    } else {
      newMapRadioButton.setChecked(true);
    }

    // Update fusion table
    boolean defaultTablePublic = PreferencesUtils.getBoolean(
        activity, R.string.default_table_public_key, PreferencesUtils.DEFAULT_TABLE_PUBLIC_DEFAULT);
    String text = getString(R.string.send_google_fusion_tables) + "\n"
        + getString(defaultTablePublic ? R.string.send_google_fusion_tables_public
            : R.string.send_google_fusion_tables_private);
    fusionTablesCheckBox.setText(text);

    return new AlertDialog.Builder(activity).setNegativeButton(R.string.generic_cancel, null)
        .setPositiveButton(R.string.send_google_send_now, new DialogInterface.OnClickListener() {
            @Override
          public void onClick(DialogInterface dialog, int which) {
            PreferencesUtils.setBoolean(
                activity, R.string.send_to_drive_key, driveCheckBox.isChecked());
            PreferencesUtils.setBoolean(
                activity, R.string.pick_existing_map_key, existingMapRadioButton.isChecked());
            PreferencesUtils.setBoolean(
                activity, R.string.send_to_maps_key, mapsCheckBox.isChecked());
            PreferencesUtils.setBoolean(
                activity, R.string.send_to_fusion_tables_key, fusionTablesCheckBox.isChecked());
            PreferencesUtils.setBoolean(
                activity, R.string.send_to_spreadsheets_key, spreadsheetsCheckBox.isChecked());
            if (sendDrive() || mapsCheckBox.isChecked() || fusionTablesCheckBox.isChecked()
                || spreadsheetsCheckBox.isChecked()) {
              startNextActivity();
            } else {
              Toast.makeText(activity, R.string.send_google_no_service_selected, Toast.LENGTH_LONG)
                  .show();
            }
          }
        }).setTitle(R.string.send_google_title).setView(view).create();
  }

  /**
   * Updates map option.
   */
  private void updateMapsOption() {
    mapsOptionTableRow.setVisibility(mapsCheckBox.isChecked() ? View.VISIBLE : View.GONE);
  }

  /**
   * Starts the next activity, {@link AccountChooserActivity}.
   */
  private void startNextActivity() {
    sendRequest.setSendDrive(sendDrive());
    sendRequest.setSendMaps(mapsCheckBox.isChecked());
    sendRequest.setSendFusionTables(fusionTablesCheckBox.isChecked());
    sendRequest.setSendSpreadsheets(spreadsheetsCheckBox.isChecked());
    sendRequest.setNewMap(!existingMapRadioButton.isChecked());
    sendStats();
    Intent intent = IntentUtils.newIntent(activity, AccountChooserActivity.class)
        .putExtra(SendRequest.SEND_REQUEST_KEY, sendRequest);
    startActivity(intent);
  }

  /**
   * Sends stats to Google Analytics.
   */
  private void sendStats() {
    if (sendRequest.isSendDrive()) {
      AnalyticsUtils.sendPageViews(activity, "/send/drive");
    }
    if (sendRequest.isSendMaps()) {
      AnalyticsUtils.sendPageViews(activity, "/send/maps");
    }
    if (sendRequest.isSendFusionTables()) {
      AnalyticsUtils.sendPageViews(activity, "/send/fusion_tables");
    }
    if (sendRequest.isSendSpreadsheets()) {
      AnalyticsUtils.sendPageViews(activity, "/send/spreadsheets");
    }
  }

  /**
   * True to send to Google Drive.
   */
  private boolean sendDrive() {
    return !hideDrive && driveCheckBox.isChecked();
  }
}
