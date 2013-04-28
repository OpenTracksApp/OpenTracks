/*
 * Copyright 2013 Google Inc.
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

package com.google.android.apps.mytracks.fragments;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import java.util.List;

/**
 * Image adapter for choosing an activity type.
 *
 * @author apoorvn
 */
public class ChooseActivityTypeImageAdapter extends BaseAdapter {

  private final Context context;
  private final List<Integer> imageIds;
  private final int width;
  private final int height;
  private final int padding;

  public ChooseActivityTypeImageAdapter(
      Context context, List<Integer> imageIds, int width, int height, int padding) {
    this.context = context;
    this.imageIds = imageIds;
    this.width = width;
    this.height = height;
    this.padding = padding;
  }

  @Override
  public int getCount() {
    return imageIds.size();
  }

  @Override
  public Object getItem(int position) {
    return null;
  }

  @Override
  public long getItemId(int position) {
    return 0;
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    ImageView imageView;
    if (convertView == null) {
      imageView = new ImageView(context);
    } else {
      imageView = (ImageView) convertView;
    }
    imageView.setImageResource(imageIds.get(position));
    imageView.setMinimumHeight(height);
    imageView.setMinimumWidth(width);
    imageView.setPadding(padding, padding, padding, padding);
    return imageView;
  }
}
