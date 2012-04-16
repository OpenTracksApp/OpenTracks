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

import com.google.android.maps.mytracks.R;

import android.app.Dialog;
import android.content.Intent;
import android.test.ActivityInstrumentationTestCase2;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Tests the {@link UploadResultActivity}.
 * 
 * @author Youtao Liu
 */
public class UploadResultActivityTest
    extends ActivityInstrumentationTestCase2<UploadResultActivity> {

  private UploadResultActivity uploadResultActivity;

  /**
   * This method is necessary for ActivityInstrumentationTestCase2.
   */
  public UploadResultActivityTest() {
    super(UploadResultActivity.class);
  }

  /**
   * Checks the display of dialog when send to all and all sends are successful.
   */
  public void testAllSuccess() {
    initialActivity(true, true, true, true, true, true);
    Dialog dialog = uploadResultActivity.getDialog();
    TextView textView = (TextView) dialog.findViewById(R.id.upload_result_success_footer);
    assertTrue(textView.isShown());
  }

  /**
   * Checks the display of dialog when send to all and all sends are failed.
   */
  public void testAllFailed() {
    // Send all kinds but all failed.
    initialActivity(true, true, true, false, false, false);
    Dialog dialog = uploadResultActivity.getDialog();
    TextView textView = (TextView) dialog.findViewById(R.id.upload_result_error_footer);
    assertTrue(textView.isShown());
  }

  /**
   * Checks the display of dialog when match following items:
   * <ul>
   * <li>Only send to Maps and Docs.</li>
   * <li>Send to Maps successful.</li>
   * <li>Send to Docs failed.</li>
   * </ul>
   */
  public void testPartialSuccess() {
    initialActivity(true, false, true, true, false, false);
    Dialog dialog = uploadResultActivity.getDialog();
    TextView textView = (TextView) dialog.findViewById(R.id.upload_result_error_footer);
    assertTrue(textView.isShown());
    LinearLayout mapsResult = (LinearLayout) dialog.findViewById(R.id.upload_result_maps_result);
    assertTrue(mapsResult.isShown());
    LinearLayout fusionTablesResult = (LinearLayout) dialog.findViewById(
        R.id.upload_result_fusion_tables_result);
    assertFalse(fusionTablesResult.isShown());
    LinearLayout docsResult = (LinearLayout) dialog.findViewById(R.id.upload_result_docs_result);
    assertTrue(docsResult.isShown());
  }

  /**
   * Initial a {@link SendRequest} and then initials a activity to be tested.
   * 
   * @param isSendMaps
   * @param isSendFusionTables
   * @param isSendDocs
   * @param isMapsSuccess
   * @param isFusionTablesSuccess
   * @param isDocsSuccess
   */
  private void initialActivity(boolean isSendMaps, boolean isSendFusionTables, boolean isSendDocs,
      boolean isMapsSuccess, boolean isFusionTablesSuccess, boolean isDocsSuccess) {
    Intent intent = new Intent();
    SendRequest sendRequest = new SendRequest(1L, true, true, true);
    sendRequest.setSendMaps(isSendMaps);
    sendRequest.setSendFusionTables(isSendFusionTables);
    sendRequest.setSendDocs(isSendDocs);
    sendRequest.setMapsSuccess(isMapsSuccess);
    sendRequest.setFusionTablesSuccess(isFusionTablesSuccess);
    sendRequest.setDocsSuccess(isDocsSuccess);
    intent.putExtra(SendRequest.SEND_REQUEST_KEY, sendRequest);
    setActivityIntent(intent);
    uploadResultActivity = this.getActivity();
  }
}
