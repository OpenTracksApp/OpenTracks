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

import com.google.android.apps.mytracks.Constants;
import com.google.android.apps.mytracks.io.file.TrackFileFormat;
import com.google.android.apps.mytracks.util.AccountUtils;
import com.google.android.apps.mytracks.util.FileUtils;
import com.google.android.apps.mytracks.util.PreferencesUtils;
import com.google.android.maps.mytracks.R;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;

import java.util.ArrayList;

/**
 * A DialogFragment to export a track.
 * 
 * @author Jimmy Shih
 */
public class ExportDialogFragment extends AbstractMyTracksDialogFragment {

  /**
   * Export types.
   * 
   * @author Jimmy Shih
   */
  public enum ExportType {
    GOOGLE_DRIVE(R.string.export_google_drive),
    GOOGLE_MAPS(R.string.export_google_maps),
    GOOGLE_FUSION_TABLES(R.string.export_google_fusion_tables),
    GOOGLE_SPREADSHEET(R.string.export_google_spreadsheets),
    EXTERNAL_STORAGE(R.string.export_external_storage);
    final int resId;

    ExportType(int resId) {
      this.resId = resId;
    }
  }

  /**
   * Interface for caller of this dialog fragment.
   * 
   * @author Jimmy Shih
   */
  public interface ExportCaller {

    /**
     * Called when export is done.
     * 
     * @param exportType the export type
     * @param trackFileFormat the track file format
     * @param account the account
     */
    public void onExportDone(
        ExportType exportType, TrackFileFormat trackFileFormat, Account account);
  }

  public static final String EXPORT_DIALOG_TAG = "export";

  private static final String KEY_HIDE_DRIVE = "hideDrive";

  public static ExportDialogFragment newInstance(boolean hideDrive) {
    Bundle bundle = new Bundle();
    bundle.putBoolean(KEY_HIDE_DRIVE, hideDrive);

    ExportDialogFragment fragment = new ExportDialogFragment();
    fragment.setArguments(bundle);
    return fragment;
  }

  private ExportCaller caller;
  private Account[] accounts;
  private ArrayList<ExportType> exportTypeOptionsList;
  
  // UI elements
  private Spinner exportTypeOptions;
  private RadioGroup exportGoogleMapsOptions;
  private RadioGroup exportGoogleFusionTablesOptions;
  private RadioGroup exportExternalStorageOptions;
  private Spinner accountSpinner;

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    try {
      caller = (ExportCaller) activity;
    } catch (ClassCastException e) {
      throw new ClassCastException(
          activity.toString() + " must implement " + ExportCaller.class.getSimpleName());
    }
  }

  @Override
  protected Dialog createDialog() {
    FragmentActivity fragmentActivity = getActivity();
    accounts = AccountManager.get(fragmentActivity).getAccountsByType(Constants.ACCOUNT_TYPE);

    // Get views
    View view = fragmentActivity.getLayoutInflater().inflate(R.layout.export, null);
    exportTypeOptions = (Spinner) view.findViewById(R.id.export_type_options);
    exportGoogleMapsOptions = (RadioGroup) view.findViewById(R.id.export_google_maps_options);
    exportGoogleFusionTablesOptions = (RadioGroup) view.findViewById(
        R.id.export_google_fusion_tables_options);
    exportExternalStorageOptions = (RadioGroup) view.findViewById(
        R.id.export_external_storage_options);
    accountSpinner = (Spinner) view.findViewById(R.id.export_account);
    
    // Setup exportTypeOptions
    setupExportTypeOptions(fragmentActivity);

    // Setup exportGoogleMapsOptions
    boolean exportGoogleMapsPublic = PreferencesUtils.getBoolean(fragmentActivity,
        R.string.export_google_maps_public_key, PreferencesUtils.EXPORT_GOOGLE_MAPS_PUBLIC_DEFAULT);
    exportGoogleMapsOptions.check(
        exportGoogleMapsPublic ? R.id.export_google_maps_public : R.id.export_google_maps_unlisted);

    // Setup exportGoogleFusionTablesOptions
    boolean exportGoogleFusionTablesPublic = PreferencesUtils.getBoolean(fragmentActivity,
        R.string.export_google_fusion_tables_public_key,
        PreferencesUtils.EXPORT_GOOGLE_FUSION_TABLES_PUBLIC_DEFAULT);
    exportGoogleFusionTablesOptions.check(
        exportGoogleFusionTablesPublic ? R.id.export_google_fusion_tables_public
            : R.id.export_google_fusion_tables_private);

    // Setup exportExternalStorageOptions
    setExternalStorageOption(
        (RadioButton) view.findViewById(R.id.export_external_storage_kml), TrackFileFormat.KML);
    setExternalStorageOption(
        (RadioButton) view.findViewById(R.id.export_external_storage_gpx), TrackFileFormat.GPX);
    setExternalStorageOption(
        (RadioButton) view.findViewById(R.id.export_external_storage_csv), TrackFileFormat.CSV);
    setExternalStorageOption(
        (RadioButton) view.findViewById(R.id.export_external_storage_tcx), TrackFileFormat.TCX);
    TrackFileFormat trackFileFormat = TrackFileFormat.valueOf(PreferencesUtils.getString(
        fragmentActivity, R.string.export_external_storage_format_key,
        PreferencesUtils.EXPORT_EXTERNAL_STORAGE_FORMAT_DEFAULT));
    exportExternalStorageOptions.check(getExternalStorageFormatId(trackFileFormat));

    // Setup accountSpinner
    AccountUtils.setupAccountSpinner(fragmentActivity, accountSpinner, accounts);
    
    return new AlertDialog.Builder(fragmentActivity).setNegativeButton(
        R.string.generic_cancel, null)
        .setPositiveButton(R.string.menu_export, new DialogInterface.OnClickListener() {
            @Override
          public void onClick(DialogInterface dialog, int which) {
            FragmentActivity context = getActivity();
            ExportType type = exportTypeOptionsList.get(
                exportTypeOptions.getSelectedItemPosition());
            TrackFileFormat format = null;

            PreferencesUtils.setString(context, R.string.export_type_key, type.name());
            if (type == ExportType.GOOGLE_MAPS) {
              PreferencesUtils.setBoolean(context, R.string.export_google_maps_public_key,
                  exportGoogleMapsOptions.getCheckedRadioButtonId()
                  == R.id.export_google_maps_public);
            } else if (type == ExportType.GOOGLE_FUSION_TABLES) {
              PreferencesUtils.setBoolean(context, R.string.export_google_fusion_tables_public_key,
                  exportGoogleFusionTablesOptions.getCheckedRadioButtonId()
                  == R.id.export_google_fusion_tables_public);
            } else if (type == ExportType.EXTERNAL_STORAGE) {
              format = getTrackFileFormat(exportExternalStorageOptions.getCheckedRadioButtonId());
              PreferencesUtils.setString(
                  context, R.string.export_external_storage_format_key, format.name());
            }
            Account account;
            if (accounts.length == 0) {
              account = null;
            } else if (accounts.length == 1) {
              account = accounts[0];
            } else {
              account = accounts[accountSpinner.getSelectedItemPosition()];
            }
            AccountUtils.updateShareTrackAccountPreference(context, account);
            caller.onExportDone(type, format, account);
          }
        }).setTitle(R.string.export_title).setView(view).create();
  }

  private void setupExportTypeOptions(FragmentActivity fragmentActivity) {
    boolean hideDrive = getArguments().getBoolean(KEY_HIDE_DRIVE);
    ExportType exportType = ExportType.valueOf(PreferencesUtils.getString(
        fragmentActivity, R.string.export_type_key, PreferencesUtils.EXPORT_TYPE_DEFAULT));

    if (hideDrive && exportType == ExportType.GOOGLE_DRIVE) {
      exportType = ExportType.GOOGLE_MAPS;
    }

    exportTypeOptionsList = new ArrayList<ExportDialogFragment.ExportType>();
    ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(
        getActivity(), android.R.layout.simple_spinner_item);
    if (accounts.length > 0) {
      if (!hideDrive) {
        exportTypeOptionsList.add(ExportType.GOOGLE_DRIVE);
      }
      exportTypeOptionsList.add(ExportType.GOOGLE_MAPS);
      exportTypeOptionsList.add(ExportType.GOOGLE_FUSION_TABLES);
      exportTypeOptionsList.add(ExportType.GOOGLE_SPREADSHEET);
    }
    exportTypeOptionsList.add(ExportType.EXTERNAL_STORAGE);

    int selection = 0;
    for (int i = 0; i < exportTypeOptionsList.size(); i++) {
      ExportType type = exportTypeOptionsList.get(i);
      adapter.add(getString(type.resId));
      if (type == exportType) {
        selection = i;
      }
    }
    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    exportTypeOptions.setAdapter(adapter);
    exportTypeOptions.setSelection(selection);
    exportTypeOptions.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

        @Override
      public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        ExportType type = exportTypeOptionsList.get(position);
        exportGoogleMapsOptions.setVisibility(
            type == ExportType.GOOGLE_MAPS ? View.VISIBLE : View.GONE);
        exportGoogleFusionTablesOptions.setVisibility(
            type == ExportType.GOOGLE_FUSION_TABLES ? View.VISIBLE : View.GONE);
        exportExternalStorageOptions.setVisibility(
            type == ExportType.EXTERNAL_STORAGE ? View.VISIBLE : View.GONE);
        accountSpinner.setVisibility(
            accounts.length > 1 && type != ExportType.EXTERNAL_STORAGE ? View.VISIBLE : View.GONE);
      }

        @Override
      public void onNothingSelected(AdapterView<?> parent) {
        // Safely ignore
      }
    });
  }

  /**
   * Sets an external storage option.
   * 
   * @param radioButton the radio button
   * @param trackFileFormat the track file format
   */
  private void setExternalStorageOption(RadioButton radioButton, TrackFileFormat trackFileFormat) {
    radioButton.setText(getString(R.string.export_external_storage_option, trackFileFormat.name(),
        FileUtils.getPathDisplayName(trackFileFormat.getExtension())));
  }

  /**
   * Gets the external storage format id from a track file format.
   * 
   * @param trackFileFormat the track file format
   */
  private int getExternalStorageFormatId(TrackFileFormat trackFileFormat) {
    switch (trackFileFormat) {
      case KML:
        return R.id.export_external_storage_kml;
      case GPX:
        return R.id.export_external_storage_gpx;
      case CSV:
        return R.id.export_external_storage_csv;
      default:
        return R.id.export_external_storage_tcx;
    }
  }

  /**
   * Gets the track file format from an external storage format id.
   * 
   * @param externalStorageFormatId the external storage format id
   */
  private TrackFileFormat getTrackFileFormat(int externalStorageFormatId) {
    switch (externalStorageFormatId) {
      case R.id.export_external_storage_kml:
        return TrackFileFormat.KML;
      case R.id.export_external_storage_gpx:
        return TrackFileFormat.GPX;
      case R.id.export_external_storage_csv:
        return TrackFileFormat.CSV;
      default:
        return TrackFileFormat.TCX;
    }
  }
}
