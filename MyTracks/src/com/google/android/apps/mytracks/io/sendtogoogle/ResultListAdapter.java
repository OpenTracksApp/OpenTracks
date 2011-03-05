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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;

import java.util.List;

/**
 * This class implements the {@link ListAdapter} used by the send-to-Google
 * result dialog created by {@link ResultDialogFactory}.  It generates views
 * for each entry in an array of {@link SendResult} instances.
 * @author Matthew Simmons
 */
class ResultListAdapter extends ArrayAdapter<SendResult> {
  public ResultListAdapter(Context context, int textViewResourceId, List<SendResult> results) {
    super(context, textViewResourceId, results);
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    View view = convertView;
    if (view == null) {
      LayoutInflater inflater
          = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
      view = inflater.inflate(R.layout.send_to_google_result_list_item, null);
    }

    SendResult result = getItem(position);
    setImage(view, result.isSuccess() ? R.drawable.success : R.drawable.failure);
    setName(view, result.getType().getServiceName());
    setUrl(view, result.getType().getServiceUrl());
    return view;
  }

  @Override
  public boolean areAllItemsEnabled() {
    // We don't want the displayed items to be clickable
    return false;
  }

  // The following protected methods exist to be overridden for testing
  // purposes.  Doing so insulates the test class from the details of the
  // layout.

  protected void setImage(View content, int drawableId) {
    ImageView imageView = (ImageView) content.findViewById(R.id.send_to_google_result_icon);
    imageView.setImageDrawable(getContext().getResources().getDrawable(drawableId));
  }

  protected void setName(View content, int nameId) {
    setTextViewText(content, R.id.send_to_google_result_name, nameId);
  }

  protected void setUrl(View content, int urlId) {
    setTextViewText(content, R.id.send_to_google_result_url, urlId);
  }

  private void setTextViewText(View content, int viewId, int textId) {
    TextView textView = (TextView) content.findViewById(viewId);
    textView.setText(getContext().getString(textId));
  }
};
