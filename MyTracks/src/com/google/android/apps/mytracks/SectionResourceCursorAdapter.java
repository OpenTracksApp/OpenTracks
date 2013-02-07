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

package com.google.android.apps.mytracks;

import com.google.android.apps.mytracks.content.TracksColumns;
import com.google.android.maps.mytracks.R;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.ResourceCursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * A section based resource cursor adapter. Currently only supports one
 * additional section header, "Shared with me".
 * 
 * @author Jimmy Shih
 */
public abstract class SectionResourceCursorAdapter extends ResourceCursorAdapter {

  private enum ItemType {
    TRACK, HEADER
  }

  private final LayoutInflater layoutInflater;
  private int sharedWithMeIndex = -1;

  public SectionResourceCursorAdapter(Context context, int layout, Cursor cursor, int flags) {
    super(context, layout, cursor, flags);
    this.layoutInflater = LayoutInflater.from(context);
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    if (getItemViewType(position) == ItemType.TRACK.ordinal()) {
      return super.getView(getCursorPosition(position), convertView, parent);
    } else {
      TextView textView;
      if (convertView == null) {
        textView = (TextView) layoutInflater.inflate(R.layout.track_list_header, parent, false);
      } else {
        textView = (TextView) convertView;
      }
      return textView;
    }
  }

  @Override
  public int getViewTypeCount() {
    return ItemType.values().length;
  }

  @Override
  public int getCount() {
    int count = super.getCount();
    return count != 0 && sharedWithMeIndex != -1 ? count + 1 : count;
  }

  @Override
  public boolean isEnabled(int position) {
    return getItemViewType(position) == ItemType.TRACK.ordinal();
  }

  @Override
  public Object getItem(int position) {
    if (getItemViewType(position) == ItemType.TRACK.ordinal()) {
      return super.getItem(getCursorPosition(position));
    }
    return null;
  }

  @Override
  public long getItemId(int position) {
    if (getItemViewType(position) == ItemType.TRACK.ordinal()) {
      return super.getItemId(getCursorPosition(position));
    }
    return -1L;
  }

  @Override
  public int getItemViewType(int position) {
    if (sharedWithMeIndex != -1 && sharedWithMeIndex == position) {
      return ItemType.HEADER.ordinal();
    }
    return ItemType.TRACK.ordinal();
  }

  @Override
  public Cursor swapCursor(Cursor newCursor) {
    Cursor oldCursor = super.swapCursor(newCursor);
    sharedWithMeIndex = getSharedWithMeIndex();
    return oldCursor;
  }

  /**
   * Gets the Shared with me header index.
   */
  private int getSharedWithMeIndex() {
    int i = 0;
    Cursor cursor = getCursor();
    if (cursor != null && cursor.moveToFirst()) {
      int index = cursor.getColumnIndex(TracksColumns.SHAREDWITHME);
      do {
        int sharedWithMe = cursor.getInt(index);
        if (sharedWithMe == 1) {
          // Returns the index where sharedWithMe == 1
          return i;
        }
        i++;
      } while (cursor.moveToNext());
    }
    return -1;
  }

  /**
   * Gets the underlying cursor position.
   * 
   * @param position the requested position
   */
  private int getCursorPosition(int position) {
    if (sharedWithMeIndex != -1 && position > sharedWithMeIndex) {
      return position - 1;
    }
    return position;
  }
}
