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
import com.jayway.android.robotium.solo.Solo;

import android.app.Instrumentation;
import android.content.Intent;
import android.test.ActivityInstrumentationTestCase2;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * Tests the {@link UploadResultActivity}.
 * 
 * @author Youtao Liu
 */
public class UploadResultActivityTest extends
    ActivityInstrumentationTestCase2<UploadResultActivity> {

  private Instrumentation instrumentation;
  private UploadResultActivity uploadResultActivity;
  private Solo solo;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    instrumentation = getInstrumentation();
    solo = new Solo(instrumentation);
  }

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

    HashSet<String> stringHashSet = new HashSet<String>();
    ArrayList<View> view = solo.getViews();
    for (View oneView : view) {
      if (oneView instanceof TextView && oneView.isShown()) {
        stringHashSet.add((String) ((TextView) oneView).getText());
      }
    }

    assertTrue(stringHashSet.contains(uploadResultActivity
        .getString(R.string.generic_success_title)));
    assertTrue(stringHashSet.contains(uploadResultActivity.getString(R.string.send_google_maps)));
    assertTrue(stringHashSet.contains(uploadResultActivity
        .getString(R.string.send_google_fusion_tables)));
    assertTrue(stringHashSet.contains(uploadResultActivity.getString(R.string.send_google_docs)));
    assertTrue(stringHashSet.contains(uploadResultActivity
        .getString(R.string.send_google_success_footer)));

    assertFalse(stringHashSet
        .contains(uploadResultActivity.getString(R.string.generic_error_title)));
    assertFalse(stringHashSet.contains(uploadResultActivity.getString(R.string.send_google_error)));
  }

  /**
   * Checks the display of dialog when send to all and all sends are failed.
   */
  public void testAllFailed() {
    // Send all kinds but all failed.
    initialActivity(true, true, true, false, false, false);
    HashSet<String> stringHashSet = new HashSet<String>();
    ArrayList<View> view = solo.getViews();
    for (View oneView : view) {
      if (oneView instanceof TextView && oneView.isShown()) {
        stringHashSet.add((String) ((TextView) oneView).getText());
      }
    }

    assertTrue(stringHashSet.contains((Object) uploadResultActivity
        .getString(R.string.generic_error_title)));
    assertTrue(stringHashSet.contains(uploadResultActivity.getString(R.string.send_google_maps)));
    assertTrue(stringHashSet.contains(uploadResultActivity
        .getString(R.string.send_google_fusion_tables)));
    assertTrue(stringHashSet.contains(uploadResultActivity.getString(R.string.send_google_docs)));
    assertTrue(stringHashSet.contains(uploadResultActivity.getString(R.string.send_google_error)));

    assertFalse(stringHashSet.contains(uploadResultActivity
        .getString(R.string.generic_success_title)));
    assertFalse(stringHashSet.contains(uploadResultActivity
        .getString(R.string.send_google_success_footer)));
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

    HashSet<String> stringHashSet = new HashSet<String>();
    ArrayList<View> view = solo.getViews();
    for (View oneView : view) {
      if (oneView instanceof TextView && oneView.isShown()) {
        stringHashSet.add((String) ((TextView) oneView).getText());
      }
    }

    assertTrue(stringHashSet.contains(uploadResultActivity.getString(R.string.generic_error_title)));
    assertTrue(stringHashSet.contains(uploadResultActivity.getString(R.string.send_google_maps)));
    assertTrue(stringHashSet.contains(uploadResultActivity.getString(R.string.send_google_docs)));
    assertTrue(stringHashSet.contains(uploadResultActivity.getString(R.string.send_google_error)));

    assertFalse(stringHashSet.contains(uploadResultActivity
        .getString(R.string.send_google_fusion_tables)));
    assertFalse(stringHashSet.contains(uploadResultActivity
        .getString(R.string.generic_success_title)));
    assertFalse(stringHashSet.contains(uploadResultActivity
        .getString(R.string.send_google_success_footer)));
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
