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
import com.google.android.maps.mytracks.R;

import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.test.ActivityInstrumentationTestCase2;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RadioButton;

/**
 * Tests the {@link UploadServiceChooserActivity}.
 * 
 * @author Youtao Liu
 */
public class UploadServiceChooserActivityTest extends
    ActivityInstrumentationTestCase2<UploadServiceChooserActivity> {

  private Instrumentation instrumentation;
  private UploadServiceChooserActivity uploadServiceChooserActivity;

  public UploadServiceChooserActivityTest() {
    super(UploadServiceChooserActivity.class);
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    instrumentation = getInstrumentation();
  }

  /**
   * Tests the logic to control display all send options. This test cover code
   * in method {@link UploadServiceChooserActivity#onCreateDialog()},
   * {@link UploadServiceChooserActivity#updateStateBySendRequest()} and
   * {@link UploadServiceChooserActivity#updateStateBySelection()}.
   */
  public void testOnCreateDialog_displayAll() {
    // Initials activity to display all send items.
    initialActivity(true, true, true);
    instrumentation.waitForIdleSync();
    assertTrue(getMapsCheckBox().isShown());
    assertTrue(getFusionTablesCheckBox().isShown());
    assertTrue(getDocsCheckBox().isShown());
    assertTrue(getCancelButton().isEnabled());

    // Clicks to disable all send items.
    uploadServiceChooserActivity.runOnUiThread(new Runnable() {
      public void run() {
        if (getMapsCheckBox().isChecked()) {
          getMapsCheckBox().performClick();
        }
        if (getFusionTablesCheckBox().isChecked()) {
          getFusionTablesCheckBox().performClick();
        }
        if (getDocsCheckBox().isChecked()) {
          getDocsCheckBox().performClick();
        }
      }
    });
    instrumentation.waitForIdleSync();
    assertTrue(getMapsCheckBox().isShown());
    assertTrue(getFusionTablesCheckBox().isShown());
    assertTrue(getDocsCheckBox().isShown());
    assertTrue(getCancelButton().isEnabled());

    assertFalse(getNewMapRadioButton().isShown());
    assertFalse(getExistingMapRadioButton().isShown());
    assertFalse(getSendButton().isEnabled());
  }

  /**
   * Tests the logic to check the send to Google Maps option. This test cover
   * code in method {@link UploadServiceChooserActivity#onCreateDialog()},
   * {@link UploadServiceChooserActivity#updateStateBySendRequest()} and
   * {@link UploadServiceChooserActivity#updateStateBySelection()}.
   */
  public void testOnCreateDialog_displayOne() {
    // Initials activity to display all send items.
    initialActivity(true, false, false);
    instrumentation.waitForIdleSync();
    assertTrue(getMapsCheckBox().isShown());
    assertTrue(getCancelButton().isEnabled());

    // Clicks to enable this items.
    uploadServiceChooserActivity.runOnUiThread(new Runnable() {
      public void run() {
        if (!getMapsCheckBox().isChecked()) {
          getMapsCheckBox().performClick();
        }
      }
    });
    instrumentation.waitForIdleSync();
    assertTrue(getMapsCheckBox().isShown());
    assertTrue(getNewMapRadioButton().isShown());
    assertTrue(getExistingMapRadioButton().isShown());
    assertTrue(getSendButton().isEnabled());
    assertTrue(getCancelButton().isEnabled());
  }

  /**
   * Tests the logic to control display none. This test cover code in method
   * {@link UploadServiceChooserActivity#onCreateDialog()},
   * {@link UploadServiceChooserActivity#updateStateBySendRequest()} and
   * {@link UploadServiceChooserActivity#updateStateBySelection()}.
   */
  public void testOnCreateDialog_displayNone() {
    initialActivity(false, false, false);
    assertFalse(getMapsCheckBox().isShown());
    assertFalse(getFusionTablesCheckBox().isShown());
    assertFalse(getDocsCheckBox().isShown());
    assertFalse(getSendButton().isEnabled());
    assertTrue(getCancelButton().isEnabled());
  }

  /**
   * Tests the logic to initial state of check box to unchecked. This test cover
   * code in method {@link UploadServiceChooserActivity#onCreateDialog()},
   * {@link UploadServiceChooserActivity#initState()}.
   */
  public void testOnCreateDialog_initStateUnchecked() {
    initialActivity(true, true, true);
    // Initial all values to false in SharedPreferences.
    SharedPreferences prefs = uploadServiceChooserActivity.getSharedPreferences(
        Constants.SETTINGS_NAME, Context.MODE_PRIVATE);
    Editor editor = prefs.edit();
    editor.putBoolean(uploadServiceChooserActivity.getString(R.string.send_to_maps_key), false);
    editor.putBoolean(uploadServiceChooserActivity.getString(R.string.send_to_fusion_tables_key),
        false);
    editor.putBoolean(uploadServiceChooserActivity.getString(R.string.send_to_docs_key), false);
    editor.commit();
    uploadServiceChooserActivity.runOnUiThread(new Runnable() {
      public void run() {
        uploadServiceChooserActivity.initState();
      }
    });
    instrumentation.waitForIdleSync();
    assertFalse(getMapsCheckBox().isChecked());
    assertFalse(getFusionTablesCheckBox().isChecked());
    assertFalse(getDocsCheckBox().isChecked());
  }

  /**
   * Tests the logic to initial state of check box to checked. This test cover
   * code in method {@link UploadServiceChooserActivity#onCreateDialog()},
   * {@link UploadServiceChooserActivity#initState()}.
   */
  public void testOnCreateDialog_initStateChecked() {
    initialActivity(true, true, true);
    // Initial all values to true in SharedPreferences.
    SharedPreferences prefs = uploadServiceChooserActivity.getSharedPreferences(
        Constants.SETTINGS_NAME, Context.MODE_PRIVATE);
    Editor editor = prefs.edit();
    editor.putBoolean(uploadServiceChooserActivity.getString(R.string.send_to_maps_key), true);
    editor.putBoolean(uploadServiceChooserActivity.getString(R.string.pick_existing_map_key), true);
    editor.putBoolean(uploadServiceChooserActivity.getString(R.string.send_to_fusion_tables_key),
        true);
    editor.putBoolean(uploadServiceChooserActivity.getString(R.string.send_to_docs_key), true);
    editor.commit();
    uploadServiceChooserActivity.runOnUiThread(new Runnable() {
      public void run() {
        uploadServiceChooserActivity.initState();
      }
    });
    instrumentation.waitForIdleSync();
    assertTrue(getMapsCheckBox().isChecked());
    assertTrue(getFusionTablesCheckBox().isChecked());
    assertTrue(getDocsCheckBox().isChecked());
    assertTrue(getExistingMapRadioButton().isChecked());

    assertFalse(getNewMapRadioButton().isChecked());
  }

  /**
   * Tests the logic of saveState when click send button. This test cover code
   * in method {@link UploadServiceChooserActivity#saveState()},
   * {@link UploadServiceChooserActivity#initState()} ,
   * {@link UploadServiceChooserActivity#sendMaps()},
   * {@link UploadServiceChooserActivity#sendFusionTables()}, and
   * {@link UploadServiceChooserActivity#sendDocs()}.
   */
  public void testOnCreateDialog_saveState() {
    initialActivity(true, true, true);
    // Initial all values to true in SharedPreferences.
    SharedPreferences prefs = uploadServiceChooserActivity.getSharedPreferences(
        Constants.SETTINGS_NAME, Context.MODE_PRIVATE);
    Editor editor = prefs.edit();
    editor.putBoolean(uploadServiceChooserActivity.getString(R.string.send_to_maps_key), true);
    editor.putBoolean(uploadServiceChooserActivity.getString(R.string.send_to_fusion_tables_key),
        true);
    editor.putBoolean(uploadServiceChooserActivity.getString(R.string.send_to_docs_key), true);
    editor.commit();
    uploadServiceChooserActivity.runOnUiThread(new Runnable() {
      public void run() {
        uploadServiceChooserActivity.initState();
      }
    });
    instrumentation.waitForIdleSync();
    uploadServiceChooserActivity.saveState();
    // All values in SharedPreferences must be changed.
    assertTrue(prefs.getBoolean(uploadServiceChooserActivity.getString(R.string.send_to_maps_key),
        false));
    assertTrue(prefs.getBoolean(uploadServiceChooserActivity.getString(R.string.send_to_maps_key),
        false));
    assertTrue(prefs.getBoolean(uploadServiceChooserActivity.getString(R.string.send_to_maps_key),
        false));
  }

  /**
   * Tests the logic of startNextActivity when click send button. This test
   * cover code in method
   * {@link UploadServiceChooserActivity#startNextActivity()} ,
   * {@link UploadServiceChooserActivity#initState()} ,
   * {@link UploadServiceChooserActivity#sendMaps()},
   * {@link UploadServiceChooserActivity#sendFusionTables()}, and
   * {@link UploadServiceChooserActivity#sendDocs()}.
   */
  public void testOnCreateDialog_startNextActivity() {
    initialActivity(true, true, true);
    // Initial all values to true or false in SharedPreferences.
    SharedPreferences prefs = uploadServiceChooserActivity.getSharedPreferences(
        Constants.SETTINGS_NAME, Context.MODE_PRIVATE);
    Editor editor = prefs.edit();
    editor.putBoolean(uploadServiceChooserActivity.getString(R.string.send_to_maps_key), true);
    editor.putBoolean(uploadServiceChooserActivity.getString(R.string.send_to_docs_key), true);
    editor.putBoolean(uploadServiceChooserActivity.getString(R.string.send_to_fusion_tables_key),
        false);
    editor.commit();
    uploadServiceChooserActivity.runOnUiThread(new Runnable() {
      public void run() {
        uploadServiceChooserActivity.initState();
      }
    });
    instrumentation.waitForIdleSync();
    uploadServiceChooserActivity.startNextActivity();
    // All values in SendRequest must be same as set above.
    assertTrue(uploadServiceChooserActivity.getSendRequest().isSendMaps());
    assertTrue(uploadServiceChooserActivity.getSendRequest().isSendDocs());

    assertFalse(uploadServiceChooserActivity.getSendRequest().isSendFusionTables());
  }

  /**
   * Initials a activity to be tested.
   * 
   * @param showMaps
   * @param showFusionTables
   * @param showDocs
   */
  private void initialActivity(boolean showMaps, boolean showFusionTables, boolean showDocs) {
    Intent intent = new Intent();
    intent.putExtra(SendRequest.SEND_REQUEST_KEY, new SendRequest(1L, showMaps, showFusionTables,
        showDocs));
    setActivityIntent(intent);
    uploadServiceChooserActivity = this.getActivity();
  }

  private Button getSendButton() {
    return (Button) uploadServiceChooserActivity.getDialog()
        .findViewById(R.id.send_google_send_now);
  }

  Button getCancelButton() {
    return (Button) uploadServiceChooserActivity.getDialog().findViewById(R.id.send_google_cancel);
  }

  private CheckBox getMapsCheckBox() {
    return (CheckBox) uploadServiceChooserActivity.getDialog().findViewById(R.id.send_google_maps);
  }

  private CheckBox getFusionTablesCheckBox() {
    return (CheckBox) uploadServiceChooserActivity.getDialog().findViewById(
        R.id.send_google_fusion_tables);
  }

  private CheckBox getDocsCheckBox() {
    return (CheckBox) uploadServiceChooserActivity.getDialog().findViewById(R.id.send_google_docs);
  }

  private RadioButton getNewMapRadioButton() {
    return (RadioButton) uploadServiceChooserActivity.getDialog().findViewById(
        R.id.send_google_new_map);
  }

  private RadioButton getExistingMapRadioButton() {
    return (RadioButton) uploadServiceChooserActivity.getDialog().findViewById(
        R.id.send_google_existing_map);
  }
}
