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
import android.widget.RadioGroup;

/**
 * A dialog where the user can choose where to send the tracks to, i.e.
 * to Google My Maps, Google Docs, etc.
 *
 * @author Leif Hendrik Wilden
 */
public class SendDialog extends Dialog {

  private RadioButton newMapRadioButton;
  private RadioButton existingMapRadioButton;
  private CheckBox myMapsCheckBox;
  private CheckBox fusionTablesCheckBox;
  private CheckBox docsCheckBox;
  private OnClickListener clickListener;

  public SendDialog(Context context) {
    super(context);
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    setContentView(R.layout.mytracks_send_to_google);

    final Button sendButton = (Button) findViewById(R.id.send_google_send_now);

    final RadioGroup myMapsGroup = (RadioGroup) findViewById(R.id.send_google_my_maps_group);
    newMapRadioButton = (RadioButton) findViewById(R.id.send_google_new_map);
    existingMapRadioButton = (RadioButton) findViewById(R.id.send_google_existing_map);
    
    myMapsCheckBox = (CheckBox) findViewById(R.id.send_google_my_maps);
    fusionTablesCheckBox = (CheckBox) findViewById(R.id.send_google_fusion_tables);
    docsCheckBox = (CheckBox) findViewById(R.id.send_google_docs);

    Button cancel = (Button) findViewById(R.id.send_google_cancel);
    cancel.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        if (clickListener != null) {
          clickListener.onClick(SendDialog.this, BUTTON_NEGATIVE);
        }
        dismiss();
      }
    });

    Button send = (Button) findViewById(R.id.send_google_send_now);
    send.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        if (clickListener != null) {
          clickListener.onClick(SendDialog.this, BUTTON_POSITIVE);
        }
        dismiss();
      }
    });

    OnCheckedChangeListener checkBoxListener = new OnCheckedChangeListener() {
      public void onCheckedChanged(CompoundButton button, boolean checked) {
        sendButton.setEnabled(myMapsCheckBox.isChecked() || fusionTablesCheckBox.isChecked()
            || docsCheckBox.isChecked());
        myMapsGroup.setVisibility(myMapsCheckBox.isChecked() ? View.VISIBLE : View.GONE);
      }
    };
    
    myMapsCheckBox.setOnCheckedChangeListener(checkBoxListener);
    fusionTablesCheckBox.setOnCheckedChangeListener(checkBoxListener);
    docsCheckBox.setOnCheckedChangeListener(checkBoxListener);

    SharedPreferences prefs = getContext().getSharedPreferences(
        Constants.SETTINGS_NAME, Context.MODE_PRIVATE);
    if (prefs != null) {
      boolean pickExistingMap = prefs.getBoolean(
          getContext().getString(R.string.pick_existing_map_key), false);

      newMapRadioButton.setChecked(!pickExistingMap);
      existingMapRadioButton.setChecked(pickExistingMap);
      
      myMapsCheckBox.setChecked(
          prefs.getBoolean(getContext().getString(R.string.send_to_my_maps_key), true));
      fusionTablesCheckBox.setChecked(
          prefs.getBoolean(getContext().getString(R.string.send_to_fusion_tables_key), true));
      docsCheckBox.setChecked(
          prefs.getBoolean(getContext().getString(R.string.send_to_docs_key), true));
    }
  }

  @Override
  protected void onStop() {
    super.onStop();
    SharedPreferences prefs = getContext().getSharedPreferences(
        Constants.SETTINGS_NAME, Context.MODE_PRIVATE);
    if (prefs != null) {
      Editor editor = prefs.edit();
      if (editor != null) {
        editor.putBoolean(getContext().getString(R.string.pick_existing_map_key),
            existingMapRadioButton.isChecked());        
        editor.putBoolean(getContext().getString(R.string.send_to_my_maps_key),
            myMapsCheckBox.isChecked());
        editor.putBoolean(getContext().getString(R.string.send_to_fusion_tables_key),
            fusionTablesCheckBox.isChecked());
        editor.putBoolean(getContext().getString(R.string.send_to_docs_key),
            docsCheckBox.isChecked());
        ApiFeatures.getInstance().getApiAdapter().applyPreferenceChanges(editor);
      }
    }
  }

  public void setOnClickListener(OnClickListener clickListener) {
    this.clickListener = clickListener;
  }

  public boolean getCreateNewMap() {
    return newMapRadioButton.isChecked();
  }

  public boolean getSendToMyMaps() {
    return myMapsCheckBox.isChecked();
  }
  
  public boolean getSendToFusionTables() {
    return fusionTablesCheckBox.isChecked();
  }

  public boolean getSendToDocs() {
    return docsCheckBox.isChecked();
  }
}