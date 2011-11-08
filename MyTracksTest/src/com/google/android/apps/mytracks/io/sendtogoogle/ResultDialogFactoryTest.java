/*
 * Copyright 2011 Google Inc.
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

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.test.ActivityInstrumentationTestCase2;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests {@link ResultDialogFactory}.
 *
 * @author Matthew Simmons
 */
public class ResultDialogFactoryTest extends ActivityInstrumentationTestCase2<SendActivity> {
  public ResultDialogFactoryTest() {
    super(SendActivity.class);
  }

  private List<SendResult> makeResults(SendResult... results) {
    List<SendResult> resultsList = new ArrayList<SendResult>();
    for (SendResult result : results) {
      resultsList.add(result);
    }
    return resultsList;
  }

  private DialogInterface.OnClickListener clickListener = new DialogInterface.OnClickListener() {
    @Override
    public void onClick(DialogInterface dialog, int which) {}
  };

  public void testSuccess_noShare() {
    List<SendResult> results = makeResults(new SendResult(SendType.MYMAPS, true),
        new SendResult(SendType.DOCS, true));

    AlertDialog dialog = ResultDialogFactory.makeDialog(getActivity(), results, clickListener, null, null);
    dialog.show();

    ListView listView = (ListView) dialog.findViewById(R.id.send_to_google_result_list);
    ListAdapter listAdapter = listView.getAdapter();
    assertEquals(2, listAdapter.getCount());

    assertEquals(SendType.MYMAPS, ((SendResult) listAdapter.getItem(0)).getType());
    assertEquals(SendType.DOCS, ((SendResult) listAdapter.getItem(1)).getType());

    // Checking the return code of Dialog#getButton doesn't appear to be
    // sufficient, as a Button will be returned even if it doesn't appear in
    // the rendered dialog.
    assertEquals(View.VISIBLE, dialog.getButton(AlertDialog.BUTTON_POSITIVE).getVisibility());
    assertEquals(View.GONE, dialog.getButton(AlertDialog.BUTTON_NEUTRAL).getVisibility());

    assertEquals(View.VISIBLE,
        dialog.findViewById(R.id.send_to_google_result_comment).getVisibility());
    assertEquals(View.GONE,
        dialog.findViewById(R.id.send_to_google_result_error).getVisibility());

    // TODO: Test correct listener invocation.  Neither TouchUtils#clickView
    // nor Button#performClick appear to cause listener invocation.  This
    // might be Android bug
    // http://code.google.com/p/android/issues/detail?id=6564
  }

  public void testSuccess_share() {
    List<SendResult> results = makeResults(new SendResult(SendType.MYMAPS, true),
        new SendResult(SendType.DOCS, true));

    AlertDialog dialog = ResultDialogFactory.makeDialog(getActivity(), results,
        clickListener, clickListener, null);
    dialog.show();

    assertEquals(View.VISIBLE, dialog.getButton(AlertDialog.BUTTON_POSITIVE).getVisibility());
    assertEquals(View.VISIBLE, dialog.getButton(AlertDialog.BUTTON_NEUTRAL).getVisibility());
  }

  public void testFailure_noShare() {
    List<SendResult> results = makeResults(new SendResult(SendType.MYMAPS, true),
        new SendResult(SendType.DOCS, false));

    AlertDialog dialog = ResultDialogFactory.makeDialog(getActivity(), results, clickListener, null, null);
    dialog.show();

    assertEquals(View.GONE,
        dialog.findViewById(R.id.send_to_google_result_comment).getVisibility());
    assertEquals(View.VISIBLE,
        dialog.findViewById(R.id.send_to_google_result_error).getVisibility());
  }

  public void testFailure_share() {
    List<SendResult> results = makeResults(new SendResult(SendType.MYMAPS, true),
        new SendResult(SendType.DOCS, false));

    AlertDialog dialog = ResultDialogFactory.makeDialog(getActivity(), results,
        clickListener, clickListener, null);
    dialog.show();

    assertEquals(View.VISIBLE, dialog.getButton(AlertDialog.BUTTON_POSITIVE).getVisibility());
    assertEquals(View.VISIBLE, dialog.getButton(AlertDialog.BUTTON_NEUTRAL).getVisibility());
  }
}
