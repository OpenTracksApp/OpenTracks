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
     */
    public void onExportDone(
        ExportType exportType, TrackFileFormat trackFileFormat);
  }

  public static final String EXPORT_DIALOG_TAG = "export";

  private ExportCaller caller;

  // UI elements
  private Spinner exportTypeOptions;
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
  protected Dialog createDialog() {
    FragmentActivity fragmentActivity = getActivity();

    // Get views
    View view = fragmentActivity.getLayoutInflater().inflate(R.layout.export, null);
    exportExternalStorageOptions = (RadioGroup) view.findViewById(
            R.id.export_external_storage_options);

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

    return new AlertDialog.Builder(fragmentActivity)
            .setNegativeButton(R.string.generic_cancel, null)
            .setPositiveButton(R.string.menu_export, new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialog, int which) {
                FragmentActivity context = getActivity();
                TrackFileFormat format = null;

                PreferencesUtils.setString(context, R.string.export_type_key, ExportType.EXTERNAL_STORAGE.name());

                format = getTrackFileFormat(exportExternalStorageOptions.getCheckedRadioButtonId());
                PreferencesUtils.setString(context, R.string.export_external_storage_format_key, format.name());
                caller.onExportDone(ExportType.EXTERNAL_STORAGE, format);
              }
            })
            .setTitle(R.string.export_title).setView(view).create();
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
