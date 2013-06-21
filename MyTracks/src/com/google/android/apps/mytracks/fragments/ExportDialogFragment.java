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

import com.google.android.apps.mytracks.io.file.TrackFileFormat;
import com.google.android.apps.mytracks.util.FileUtils;
import com.google.android.apps.mytracks.util.PreferencesUtils;
import com.google.android.maps.mytracks.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;

/**
 * A DialogFragment to export a track.
 * 
 * @author Jimmy Shih
 */
public class ExportDialogFragment extends DialogFragment {

  /**
   * Export types.
   * 
   * @author Jimmy Shih
   */
  public enum ExportType {
    GOOGLE_MAPS, GOOGLE_FUSION_TABLES, GOOGLE_SPREADSHEET, EXTERNAL_STORAGE
  }

  /**
   * Interface for caller of this dialog fragment.
   * 
   * @author Jimmy Shih
   */
  public interface ExportCaller {

    /**
     * Called when export is done.
     */
    public void onExportDone(ExportType exportType, TrackFileFormat trackFileFormat);
  }

  public static final String EXPORT_DIALOG_TAG = "export";

  private ExportCaller caller;
  private RadioGroup exportTypeOptions;
  private RadioGroup exportGoogleMapsOptions;
  private RadioGroup exportGoogleFusionTablesOptions;
  private RadioGroup exportExternalStorageOptions;

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
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    FragmentActivity fragmentActivity = getActivity();

    View view = fragmentActivity.getLayoutInflater().inflate(R.layout.export, null);

    exportTypeOptions = (RadioGroup) view.findViewById(R.id.export_type_options);
    exportGoogleMapsOptions = (RadioGroup) view.findViewById(R.id.export_google_maps_options);
    exportGoogleFusionTablesOptions = (RadioGroup) view.findViewById(
        R.id.export_google_fusion_tables_options);
    exportExternalStorageOptions = (RadioGroup) view.findViewById(
        R.id.export_external_storage_options);

    exportTypeOptions.setOnCheckedChangeListener(new OnCheckedChangeListener() {
        @Override
      public void onCheckedChanged(RadioGroup group, int checkedId) {
        exportGoogleMapsOptions.setVisibility(
            checkedId == R.id.export_google_maps ? View.VISIBLE : View.GONE);
        exportGoogleFusionTablesOptions.setVisibility(
            checkedId == R.id.export_google_fusion_tables ? View.VISIBLE : View.GONE);
        exportExternalStorageOptions.setVisibility(
            checkedId == R.id.export_external_storage ? View.VISIBLE : View.GONE);
      }
    });
    ExportType exportType = ExportType.valueOf(PreferencesUtils.getString(
        fragmentActivity, R.string.export_type_key, PreferencesUtils.EXPORT_TYPE_DEFAULT));
    exportTypeOptions.check(getExportTypeId(exportType));

    boolean exportGoogleMapsPublic = PreferencesUtils.getBoolean(fragmentActivity,
        R.string.export_google_maps_public_key, PreferencesUtils.EXPORT_GOOGLE_MAPS_PUBLIC_DEFAULT);
    exportGoogleMapsOptions.check(
        exportGoogleMapsPublic ? R.id.export_google_maps_public : R.id.export_google_maps_unlisted);

    boolean exportGoogleFusionTablesPublic = PreferencesUtils.getBoolean(fragmentActivity,
        R.string.export_google_fusion_tables_public_key,
        PreferencesUtils.EXPORT_GOOGLE_FUSION_TABLES_PUBLIC_DEFAULT);
    exportGoogleFusionTablesOptions.check(
        exportGoogleFusionTablesPublic ? R.id.export_google_fusion_tables_public
            : R.id.export_google_fusion_tables_private);

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

    return new AlertDialog.Builder(fragmentActivity).setNegativeButton(
        R.string.generic_cancel, null)
        .setPositiveButton(R.string.menu_export, new DialogInterface.OnClickListener() {
            @Override
          public void onClick(DialogInterface dialog, int which) {
            FragmentActivity context = getActivity();
            int id = exportTypeOptions.getCheckedRadioButtonId();
            ExportType type = getExportType(id);
            TrackFileFormat format = null;

            PreferencesUtils.setString(context, R.string.export_type_key, type.name());
            if (id == R.id.export_google_maps) {
              PreferencesUtils.setBoolean(context, R.string.export_google_maps_public_key,
                  exportGoogleMapsOptions.getCheckedRadioButtonId()
                  == R.id.export_google_maps_public);
            } else if (id == R.id.export_google_fusion_tables) {
              PreferencesUtils.setBoolean(context, R.string.export_google_fusion_tables_public_key,
                  exportGoogleFusionTablesOptions.getCheckedRadioButtonId()
                  == R.id.export_google_fusion_tables_public);
            } else if (id == R.id.export_external_storage) {
              format = getTrackFileFormat(exportExternalStorageOptions.getCheckedRadioButtonId());
              PreferencesUtils.setString(
                  context, R.string.export_external_storage_format_key, format.name());
            }
            caller.onExportDone(type, format);
          }
        }).setTitle(R.string.export_title).setView(view).create();
  }

  /**
   * Sets an external storage option.
   * 
   * @param radioButton the radio button
   * @param trackFileFormat the track file format
   */
  private void setExternalStorageOption(RadioButton radioButton, TrackFileFormat trackFileFormat) {
    radioButton.setText(getString(R.string.export_external_storage_option, trackFileFormat.name(),
        FileUtils.getDirectoryDisplayName(trackFileFormat.getExtension())));
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

  /**
   * Gets the export type id from an export type.
   * 
   * @param exportType the export type
   */
  private int getExportTypeId(ExportType exportType) {
    switch (exportType) {
      case GOOGLE_MAPS:
        return R.id.export_google_maps;
      case GOOGLE_FUSION_TABLES:
        return R.id.export_google_fusion_tables;
      case GOOGLE_SPREADSHEET:
        return R.id.export_google_spreadsheets;
      default:
        return R.id.export_external_storage;
    }
  }

  /**
   * Gets the export type from an export type id.
   * 
   * @param exportTypeId the export type id
   */
  private ExportType getExportType(int exportTypeId) {
    switch (exportTypeId) {
      case R.id.export_google_maps:
        return ExportType.GOOGLE_MAPS;
      case R.id.export_google_fusion_tables:
        return ExportType.GOOGLE_FUSION_TABLES;
      case R.id.export_google_spreadsheets:
        return ExportType.GOOGLE_SPREADSHEET;
      default:
        return ExportType.EXTERNAL_STORAGE;
    }
  }
}
