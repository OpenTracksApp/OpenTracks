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

package de.dennisguse.opentracks.fragments;

import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import androidx.core.content.ContextCompat;

import java.util.List;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.ui.util.ResourceUtils;

/**
 * Image adapter for choosing an activity type.
 *
 * @author apoorvn
 */
class ChooseActivityTypeImageAdapter extends BaseAdapter {

    private final List<Integer> imageIds;
    private int selected = -1;

    ChooseActivityTypeImageAdapter(List<Integer> imageIds) {
        this.imageIds = imageIds;
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

    public void setSelected(int position) {
        selected = position;
        notifyDataSetChanged();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ImageView imageView;
        if (convertView == null) {
            imageView = new ImageView(parent.getContext());
            imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            imageView.setAdjustViewBounds(true);

            int padding = ResourceUtils.dpToPx(parent.getContext(), 8);
            imageView.setPaddingRelative(padding, padding, padding, padding);
        } else {
            imageView = (ImageView) convertView;
        }

        if (position == selected) {
            imageView.setBackgroundColor(ContextCompat.getColor(parent.getContext(), R.color.opentracks));
        } else {
            imageView.setBackgroundColor(Color.TRANSPARENT);
        }

        imageView.setImageResource(imageIds.get(position));
        return imageView;
    }
}
