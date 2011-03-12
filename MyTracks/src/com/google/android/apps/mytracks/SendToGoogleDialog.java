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
public class SendToGoogleDialog extends Dialog {

  private RadioGroup groupMyMaps;
  private RadioButton createNewMapRadioButton;
  private RadioButton pickMapRadioButton;
  private CheckBox sendToMyMapsCheckBox;
  private CheckBox sendToFusionTablesCheckBox;
  private CheckBox sendToDocsCheckBox;
  private RadioButton sendStatsRadioButton;
  private RadioButton sendStatsAndPointsRadioButton;
  private Button sendButton;

  public SendToGoogleDialog(Context context) {
    super(context);
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    setContentView(R.layout.mytracks_send_to_google);

    Button cancel = (Button) findViewById(R.id.sendtogoogle_cancel);
    cancel.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        MyTracks.getInstance().dismissDialog(DialogManager.DIALOG_SEND_TO_GOOGLE);
      }
    });

    Button send = (Button) findViewById(R.id.sendtogoogle_send_now);
    send.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        dismiss();
        MyTracks.getInstance().sendToGoogle();
      }
    });

    OnCheckedChangeListener checkBoxListener = new OnCheckedChangeListener() {
      public void onCheckedChanged(CompoundButton button, boolean checked) {
        sendButton.setEnabled(
            sendToMyMapsCheckBox.isChecked() ||
            sendToFusionTablesCheckBox.isChecked() ||
            sendToDocsCheckBox.isChecked());
        groupMyMaps.setVisibility(sendToMyMapsCheckBox.isChecked() ? View.VISIBLE : View.INVISIBLE);
        //groupDocs.setVisibility(
        //    sendToDocsCheckBox.isChecked() ? View.VISIBLE : View.INVISIBLE);
      }
    };

    sendButton = (Button) findViewById(R.id.sendtogoogle_send_now);
    groupMyMaps = (RadioGroup) findViewById(R.id.sendtogoogle_group_mymaps);
    sendToMyMapsCheckBox =
        (CheckBox) findViewById(R.id.sendtogoogle_google_mymaps);
    sendToMyMapsCheckBox.setOnCheckedChangeListener(checkBoxListener);
    sendToFusionTablesCheckBox =
        (CheckBox) findViewById(R.id.sendtogoogle_google_fusiontables);
    sendToFusionTablesCheckBox.setOnCheckedChangeListener(checkBoxListener);
    sendToDocsCheckBox = (CheckBox) findViewById(R.id.sendtogoogle_google_docs);
    sendToDocsCheckBox.setOnCheckedChangeListener(checkBoxListener);
    createNewMapRadioButton =
        (RadioButton) findViewById(R.id.sendtogoogle_create_new_map);
    pickMapRadioButton =
        (RadioButton) findViewById(R.id.sendtogoogle_pick_existing_map);
    sendStatsRadioButton =
        (RadioButton) findViewById(R.id.sendtogoogle_send_stats);
    sendStatsAndPointsRadioButton = (RadioButton) findViewById(
        R.id.sendtogoogle_send_stats_and_points);

    SharedPreferences prefs =
        getContext().getSharedPreferences(MyTracksSettings.SETTINGS_NAME, 0);
    if (prefs != null) {
      sendToMyMapsCheckBox.setChecked(
          prefs.getBoolean(
              getContext().getString(R.string.send_to_my_maps_key), true));
      sendToFusionTablesCheckBox.setChecked(
          prefs.getBoolean(
              getContext().getString(R.string.send_to_fusion_tables_key), true));
      sendToDocsCheckBox.setChecked(
          prefs.getBoolean(
              getContext().getString(R.string.send_to_docs_key), true));
      pickMapRadioButton.setChecked(
          prefs.getBoolean(
              getContext().getString(R.string.pick_existing_map_key), false));
      createNewMapRadioButton.setChecked(
          !prefs.getBoolean(
              getContext().getString(R.string.pick_existing_map_key), false));
      sendStatsAndPointsRadioButton.setChecked(
          prefs.getBoolean(
              getContext().getString(R.string.send_stats_and_points_key),
              false));
      sendStatsRadioButton.setChecked(
          !prefs.getBoolean(
              getContext().getString(R.string.send_stats_and_points_key),
              false));
    }
  }

  @Override
  protected void onStop() {
    SharedPreferences prefs =
        getContext().getSharedPreferences(MyTracksSettings.SETTINGS_NAME, 0);
    if (prefs != null) {
      Editor editor = prefs.edit();
      if (editor != null) {
        editor.putBoolean(
            getContext().getString(R.string.send_to_my_maps_key),
            sendToMyMapsCheckBox.isChecked());
        editor.putBoolean(
            getContext().getString(R.string.send_to_fusion_tables_key),
            sendToFusionTablesCheckBox.isChecked());
        editor.putBoolean(
            getContext().getString(R.string.send_to_docs_key),
            sendToDocsCheckBox.isChecked());
        editor.putBoolean(
            getContext().getString(R.string.pick_existing_map_key),
            pickMapRadioButton.isChecked());
        editor.putBoolean(
            getContext().getString(R.string.send_stats_and_points_key),
            sendStatsAndPointsRadioButton.isChecked());
        editor.commit();
      }
    }
    super.onStop();
  }

  public boolean getSendToMyMaps() {
    return sendToMyMapsCheckBox.isChecked();
  }

  public boolean getSendToFusionTables() {
    return sendToFusionTablesCheckBox.isChecked();
  }

  public boolean getSendToDocs() {
    return sendToDocsCheckBox.isChecked();
  }

  public boolean getCreateNewMap() {
    return createNewMapRadioButton.isChecked();
  }

  public boolean getSendStatsAndPoints() {
    return sendStatsAndPointsRadioButton.isChecked();
  }
}
