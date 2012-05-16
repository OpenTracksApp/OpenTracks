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

import com.google.android.apps.mytracks.util.PreferencesUtils;
import com.google.android.maps.mytracks.R;

import android.app.Instrumentation;
import android.content.Intent;
import android.test.ActivityInstrumentationTestCase2;
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
   * Tests the logic to display all options.
   */
  public void testOnCreateDialog_displayAll() {
    // Initials activity to display all send items.
    initialActivity();
    instrumentation.waitForIdleSync();
    assertTrue(getMapsCheckBox().isShown());
    assertTrue(getFusionTablesCheckBox().isShown());
    assertTrue(getDocsCheckBox().isShown());

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

    assertFalse(getNewMapRadioButton().isShown());
    assertFalse(getExistingMapRadioButton().isShown());
  }

  /**
   * Tests the logic to initial state of check box to unchecked. This test cover
   * code in method {@link UploadServiceChooserActivity#onCreateDialog(int)},
   * {@link UploadServiceChooserActivity#initState()}.
   */
  public void testOnCreateDialog_initStateUnchecked() {
    initialActivity();
    // Initial all values to false in SharedPreferences.
    PreferencesUtils.setBoolean(uploadServiceChooserActivity, R.string.send_to_maps_key, false);
    PreferencesUtils.setBoolean(
        uploadServiceChooserActivity, R.string.send_to_fusion_tables_key, false);
    PreferencesUtils.setBoolean(uploadServiceChooserActivity, R.string.send_to_docs_key, false);
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
   * code in method {@link UploadServiceChooserActivity#onCreateDialog(int)},
   * {@link UploadServiceChooserActivity#initState()}.
   */
  public void testOnCreateDialog_initStateChecked() {
    initialActivity();
    // Initial all values to true in SharedPreferences.
    PreferencesUtils.setBoolean(uploadServiceChooserActivity, R.string.pick_existing_map_key, true);
    PreferencesUtils.setBoolean(uploadServiceChooserActivity, R.string.send_to_maps_key, true);
    PreferencesUtils.setBoolean(
        uploadServiceChooserActivity, R.string.send_to_fusion_tables_key, true);
    PreferencesUtils.setBoolean(uploadServiceChooserActivity, R.string.send_to_docs_key, true);
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
   * in method {@link UploadServiceChooserActivity#initState()} and
   * {@link UploadServiceChooserActivity#saveState()},
   */
  public void testOnCreateDialog_saveState() {
    initialActivity();
    // Initial all values to true in SharedPreferences.
    PreferencesUtils.setBoolean(uploadServiceChooserActivity, R.string.send_to_maps_key, true);
    PreferencesUtils.setBoolean(
        uploadServiceChooserActivity, R.string.send_to_fusion_tables_key, true);
    PreferencesUtils.setBoolean(uploadServiceChooserActivity, R.string.send_to_docs_key, true);
    uploadServiceChooserActivity.runOnUiThread(new Runnable() {
      public void run() {
        uploadServiceChooserActivity.initState();
      }
    });
    instrumentation.waitForIdleSync();
    uploadServiceChooserActivity.saveState();
    // All values in SharedPreferences must be changed.
    assertTrue(PreferencesUtils.getBoolean(uploadServiceChooserActivity, R.string.send_to_maps_key,
        PreferencesUtils.SEND_TO_MAPS_DEFAULT));
    assertTrue(PreferencesUtils.getBoolean(
        uploadServiceChooserActivity, R.string.send_to_fusion_tables_key,
        PreferencesUtils.SEND_TO_FUSION_TABLES_DEFAULT));
    assertTrue(PreferencesUtils.getBoolean(uploadServiceChooserActivity, R.string.send_to_docs_key,
        PreferencesUtils.SEND_TO_DOCS_DEFAULT));
  }

  /**
   * Tests the logic of startNextActivity when click send button. This test
   * cover code in method {@link UploadServiceChooserActivity#initState()} and
   * {@link UploadServiceChooserActivity#startNextActivity()}.
   */
  public void testOnCreateDialog_startNextActivity() {
    initialActivity();
    // Initial all values to true or false in SharedPreferences.
    PreferencesUtils.setBoolean(uploadServiceChooserActivity, R.string.send_to_maps_key, true);
    PreferencesUtils.setBoolean(uploadServiceChooserActivity, R.string.send_to_docs_key, true);
    PreferencesUtils.setBoolean(
        uploadServiceChooserActivity, R.string.send_to_fusion_tables_key, false);
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
   */
  private void initialActivity() {
    Intent intent = new Intent();
    intent.putExtra(SendRequest.SEND_REQUEST_KEY, new SendRequest(1L));
    setActivityIntent(intent);
    uploadServiceChooserActivity = this.getActivity();
  }

  private CheckBox getMapsCheckBox() {
    return (CheckBox) uploadServiceChooserActivity.getAlertDialog().findViewById(R.id.send_google_maps);
  }

  private CheckBox getFusionTablesCheckBox() {
    return (CheckBox) uploadServiceChooserActivity.getAlertDialog().findViewById(
        R.id.send_google_fusion_tables);
  }

  private CheckBox getDocsCheckBox() {
    return (CheckBox) uploadServiceChooserActivity.getAlertDialog().findViewById(R.id.send_google_docs);
  }

  private RadioButton getNewMapRadioButton() {
    return (RadioButton) uploadServiceChooserActivity.getAlertDialog().findViewById(
        R.id.send_google_new_map);
  }

  private RadioButton getExistingMapRadioButton() {
    return (RadioButton) uploadServiceChooserActivity.getAlertDialog().findViewById(
        R.id.send_google_existing_map);
  }
}
