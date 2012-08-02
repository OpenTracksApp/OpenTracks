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

import android.annotation.TargetApi;
import android.content.Intent;
import android.test.ActivityInstrumentationTestCase2;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Tests the {@link UploadResultActivity}.
 * 
 * @author Youtao Liu
 */
public class UploadResultActivityTest extends
    ActivityInstrumentationTestCase2<UploadResultActivity> {

  private UploadResultActivity uploadResultActivity;
  private TextView errorFooter;
  private TextView successFooter;
  private LinearLayout mapsResult;
  private LinearLayout fusionTablesResult;
  private LinearLayout docsResult;
  private ImageView mapsResultIcon;
  private ImageView fusionTablesResultIcon;
  private ImageView docsResultIcon;

  private String successTitle;
  private String errorTitle;

  /**
   * This method is necessary for ActivityInstrumentationTestCase2.
   */
  @TargetApi(8)
  public UploadResultActivityTest() {
    super(UploadResultActivity.class);
  }

  /**
   * Checks the display of dialog when send to all and all sends are successful.
   */
  public void testAllSuccess() {
    initialActivity(true, true, true, true, true, true);
    createDialogAndGetViews();
    assertEquals(View.VISIBLE, successFooter.getVisibility());
    assertEquals(View.GONE, errorFooter.getVisibility());

    assertEquals(View.VISIBLE, mapsResult.getVisibility());
    assertEquals(View.VISIBLE, fusionTablesResult.getVisibility());
    assertEquals(View.VISIBLE, docsResult.getVisibility());

    assertEquals(successTitle, mapsResultIcon.getContentDescription());
    assertEquals(successTitle, fusionTablesResultIcon.getContentDescription());
    assertEquals(successTitle, docsResultIcon.getContentDescription());
  }

  /**
   * Checks the display of dialog when send to all and all sends are failed.
   */
  public void testAllFailed() {
    // Send all kinds but all failed.
    initialActivity(true, true, true, false, false, false);
    createDialogAndGetViews();
    assertEquals(View.GONE, successFooter.getVisibility());
    assertEquals(View.VISIBLE, errorFooter.getVisibility());

    assertEquals(View.VISIBLE, mapsResult.getVisibility());
    assertEquals(View.VISIBLE, fusionTablesResult.getVisibility());
    assertEquals(View.VISIBLE, docsResult.getVisibility());

    assertEquals(errorTitle, mapsResultIcon.getContentDescription());
    assertEquals(errorTitle, fusionTablesResultIcon.getContentDescription());
    assertEquals(errorTitle, docsResultIcon.getContentDescription());
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
    createDialogAndGetViews();
    assertEquals(View.VISIBLE, errorFooter.getVisibility());

    assertEquals(View.VISIBLE, mapsResult.getVisibility());
    assertEquals(View.GONE, fusionTablesResult.getVisibility());
    assertEquals(View.VISIBLE, docsResult.getVisibility());

    assertEquals(successTitle, mapsResultIcon.getContentDescription());
    assertEquals(errorTitle, docsResultIcon.getContentDescription());
  }

  /**
   * Initials a {@link SendRequest} and then initials a activity to be tested.
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
    SendRequest sendRequest = new SendRequest(1L);
    sendRequest.setSendMaps(isSendMaps);
    sendRequest.setSendFusionTables(isSendFusionTables);
    sendRequest.setSendDocs(isSendDocs);
    sendRequest.setMapsSuccess(isMapsSuccess);
    sendRequest.setFusionTablesSuccess(isFusionTablesSuccess);
    sendRequest.setDocsSuccess(isDocsSuccess);
    intent.putExtra(SendRequest.SEND_REQUEST_KEY, sendRequest);
    setActivityIntent(intent);
    uploadResultActivity = this.getActivity();
    successTitle = uploadResultActivity.getString(R.string.generic_success_title);
    errorTitle = uploadResultActivity.getString(R.string.generic_error_title);
  }

  /**
   * Creates result dialog and gets views in the dialog.
   */
  private void createDialogAndGetViews() {
    uploadResultActivity.onCreateDialog(UploadResultActivity.DIALOG_RESULT_ID);
    View view = uploadResultActivity.view;
    errorFooter = (TextView) view.findViewById(R.id.upload_result_error_footer);
    successFooter = (TextView) view.findViewById(R.id.upload_result_success_footer);

    mapsResult = (LinearLayout) view.findViewById(R.id.upload_result_maps_result);
    fusionTablesResult = (LinearLayout) view.findViewById(R.id.upload_result_fusion_tables_result);
    docsResult = (LinearLayout) view.findViewById(R.id.upload_result_docs_result);

    mapsResultIcon = (ImageView) view.findViewById(R.id.upload_result_maps_result_icon);
    fusionTablesResultIcon = (ImageView) view
        .findViewById(R.id.upload_result_fusion_tables_result_icon);
    docsResultIcon = (ImageView) view.findViewById(R.id.upload_result_docs_result_icon);
  }
}
