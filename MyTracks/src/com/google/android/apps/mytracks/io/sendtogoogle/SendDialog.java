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
package com.google.android.apps.mytracks.io.sendtogoogle;

import com.google.android.apps.mytracks.Constants;
import com.google.android.apps.mytracks.util.ApiFeatures;
import com.google.android.maps.mytracks.R;

import android.app.Dialog;
import android.content.Context;
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
 * A dialog where the user can choose where to send a track to, i.e., Google
 * Maps, Google Fusion Tables, and Google Docs.
 *
 * @author Leif Hendrik Wilden
 */
public class SendDialog extends Dialog {

  private boolean mapsEnabled;
  private boolean fusionTablesEnabled;
  private boolean docsEnabled;

  private TableRow mapsTableRow;
  private TableRow fusionTablesTableRow;
  private TableRow docsTableRow;

  private CheckBox mapsCheckBox;
  private CheckBox fusionTablesCheckBox;
  private CheckBox docsCheckBox;

  private TableRow mapsOptionTableRow;
  private RadioButton newMapRadioButton;
  private RadioButton existingMapRadioButton;

  private Button send;

  private OnClickListener clickListener;

  public SendDialog(Context context) {
    super(context);
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    setContentView(R.layout.mytracks_send_to_google);

    mapsTableRow = (TableRow) findViewById(R.id.send_google_maps_row);
    fusionTablesTableRow = (TableRow) findViewById(R.id.send_google_fusion_tables_row);
    docsTableRow = (TableRow) findViewById(R.id.send_google_docs_row);

    OnCheckedChangeListener checkBoxListener = new OnCheckedChangeListener() {
      public void onCheckedChanged(CompoundButton button, boolean checked) {
        updateDisplay();
      }
    };

    mapsCheckBox = (CheckBox) findViewById(R.id.send_google_maps);
    mapsCheckBox.setOnCheckedChangeListener(checkBoxListener);

    fusionTablesCheckBox = (CheckBox) findViewById(R.id.send_google_fusion_tables);
    fusionTablesCheckBox.setOnCheckedChangeListener(checkBoxListener);

    docsCheckBox = (CheckBox) findViewById(R.id.send_google_docs);
    docsCheckBox.setOnCheckedChangeListener(checkBoxListener);

    mapsOptionTableRow = (TableRow) findViewById(R.id.send_google_maps_option_row);
    newMapRadioButton = (RadioButton) findViewById(R.id.send_google_new_map);
    existingMapRadioButton = (RadioButton) findViewById(R.id.send_google_existing_map);

    Button cancel = (Button) findViewById(R.id.send_google_cancel);
    cancel.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        if (clickListener != null) {
          clickListener.onClick(SendDialog.this, BUTTON_NEGATIVE);
        }
        dismiss();
      }
    });

    send = (Button) findViewById(R.id.send_google_send_now);
    send.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        if (clickListener != null) {
          clickListener.onClick(SendDialog.this, BUTTON_POSITIVE);
        }
        dismiss();
      }
    });
  }

  @Override
  protected void onStop() {
    super.onStop();
    SharedPreferences prefs = getContext().getSharedPreferences(
        Constants.SETTINGS_NAME, Context.MODE_PRIVATE);
    if (prefs != null) {
      Editor editor = prefs.edit();
      if (editor != null) {
        editor.putBoolean(
            getContext().getString(R.string.pick_existing_map_key), !getCreateNewMap());
        if (mapsEnabled && fusionTablesEnabled && docsEnabled) {
          editor.putBoolean(
              getContext().getString(R.string.send_to_maps_key), getSendToMaps());
          editor.putBoolean(
              getContext().getString(R.string.send_to_fusion_tables_key), getSendToFusionTables());
          editor.putBoolean(getContext().getString(R.string.send_to_docs_key), getSendToDocs());
        }
        ApiFeatures.getInstance().getApiAdapter().applyPreferenceChanges(editor);
      }
    }
  }

  /**
   * Sets the sendType. If sendType is null, allows sending to all the Google
   * services.
   *
   * @param sendType sendType
   */
  public void setSendType(SendType sendType) {
    updateState();

    /*
     * Note that sendType can be null, so cannot use a switch statement based on
     * the sendType.
     */
    if (sendType == SendType.MAPS) {
      mapsEnabled = true;
      fusionTablesEnabled = false;
      docsEnabled = false;
      mapsCheckBox.setChecked(true);
    } else if (sendType == SendType.FUSION_TABLES) {
      mapsEnabled = false;
      fusionTablesEnabled = true;
      docsEnabled = false;
      fusionTablesCheckBox.setChecked(true);
    } else {
      mapsEnabled = true;
      fusionTablesEnabled = true;
      docsEnabled = true;
    }
    mapsTableRow.setVisibility(mapsEnabled ? View.VISIBLE : View.GONE);
    fusionTablesTableRow.setVisibility(fusionTablesEnabled ? View.VISIBLE : View.GONE);
    docsTableRow.setVisibility(docsEnabled ? View.VISIBLE : View.GONE);
    updateDisplay();
  }

  /**
   * Updates the UI state from the shared preferences.
   */
  private void updateState() {
    SharedPreferences prefs = getContext().getSharedPreferences(
        Constants.SETTINGS_NAME, Context.MODE_PRIVATE);
    if (prefs != null) {
      boolean pickExistingMap = prefs.getBoolean(
          getContext().getString(R.string.pick_existing_map_key), false);

      newMapRadioButton.setChecked(!pickExistingMap);
      existingMapRadioButton.setChecked(pickExistingMap);

      mapsCheckBox.setChecked(
          prefs.getBoolean(getContext().getString(R.string.send_to_maps_key), true));
      fusionTablesCheckBox.setChecked(
          prefs.getBoolean(getContext().getString(R.string.send_to_fusion_tables_key), true));
      docsCheckBox.setChecked(
          prefs.getBoolean(getContext().getString(R.string.send_to_docs_key), true));
    }
  }

  /**
   * Updates the UI display based on the current state.
   */
  private void updateDisplay() {
    send.setEnabled(getSendToMaps() || getSendToFusionTables() || getSendToDocs());
    mapsOptionTableRow.setVisibility(getSendToMaps() ? View.VISIBLE : View.GONE);
  }

  public void setOnClickListener(OnClickListener clickListener) {
    this.clickListener = clickListener;
  }

  public boolean getCreateNewMap() {
    return newMapRadioButton.isChecked();
  }

  public boolean getSendToMaps() {
    return mapsEnabled && mapsCheckBox.isChecked();
  }

  public boolean getSendToFusionTables() {
    return fusionTablesEnabled && fusionTablesCheckBox.isChecked();
  }

  public boolean getSendToDocs() {
    return docsEnabled && docsCheckBox.isChecked();
  }
}