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

import android.content.Context;
import android.test.AndroidTestCase;
import android.view.LayoutInflater;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests {@link ResultListAdapter}.
 *
 * @author Matthew Simmons
 */
public class ResultListAdapterTest extends AndroidTestCase {
  private static class TestResultListAdapter extends ResultListAdapter {
    public View contentView;
    public int drawableId;
    public int nameId;
    public int urlId;

    public TestResultListAdapter(Context context, List<SendResult> results) {
      super(context, 0, results);
    }

    @Override
    protected void setImage(View content, int drawableId) {
      saveContentView(content);
      this.drawableId = drawableId;
    }

    @Override
    protected void setName(View content, int nameId) {
      saveContentView(content);
      this.nameId = nameId;
    }

    @Override
    protected void setUrl(View content, int urlId) {
      saveContentView(content);
      this.urlId = urlId;
    }

    @SuppressWarnings("hiding")
    private void saveContentView(View contentView) {
      if (this.contentView == null) {
        this.contentView = contentView;
      } else {
        AndroidTestCase.assertEquals(this.contentView, contentView);
      }
    }
  }

  private final SendType sendType = SendType.MYMAPS;

  private View inflateContentView(int viewId) {
    LayoutInflater layoutInflater
        = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    return layoutInflater.inflate(viewId, null);
  }

  private List<SendResult> makeResult(SendType type, boolean success) {
    List<SendResult> results = new ArrayList<SendResult>();
    results.add(new SendResult(type, success));
    return results;
  }

  public void testSuccess_createsNewView() {
    TestResultListAdapter adapter = new TestResultListAdapter(getContext(),
        makeResult(sendType, true));
    adapter.getView(0, null, null);

    assertEquals(R.drawable.success, adapter.drawableId);
    assertEquals(sendType.getServiceName(), adapter.nameId);
    assertEquals(sendType.getServiceUrl(), adapter.urlId);
  }

  public void testSuccess_reusesView() {
    View contentView = inflateContentView(R.layout.send_to_google_result_list_item);
    TestResultListAdapter adapter = new TestResultListAdapter(getContext(),
        makeResult(sendType, true));
    adapter.getView(0, contentView, null);

    assertEquals(contentView, adapter.contentView);
    assertEquals(R.drawable.success, adapter.drawableId);
    assertEquals(sendType.getServiceName(), adapter.nameId);
    assertEquals(sendType.getServiceUrl(), adapter.urlId);
  }

  public void testFailure() {
    TestResultListAdapter adapter = new TestResultListAdapter(getContext(),
        makeResult(sendType, false));
    adapter.getView(0, null, null);

    assertEquals(R.drawable.failure, adapter.drawableId);
    assertEquals(sendType.getServiceName(), adapter.nameId);
    assertEquals(sendType.getServiceUrl(), adapter.urlId);
  }
}
