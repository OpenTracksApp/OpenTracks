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

import com.google.android.apps.mytracks.content.MyTracksProviderUtils;
import com.google.android.apps.mytracks.content.Track;
import com.google.android.apps.mytracks.util.TrackUtils;
import com.google.android.maps.mytracks.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.util.SparseBooleanArray;

import java.util.ArrayList;

/**
 * A DialogFragment to play multiple tracks.
 * 
 * @author Jimmy Shih
 */
public class PlayMultipleDialogFragment extends AbstractMyTracksDialogFragment {

  public static final String PLAY_MULTIPLE_DIALOG_TAG = "playMultipleDialog";

  /**
   * Interface for caller of this dialog fragment.
   * 
   * @author Jimmy Shih
   */
  public interface PlayMultipleCaller {

    /**
     * Called when play multiple is done.
     */
    public void onPlayMultipleDone(long[] trackIds);
  }

  private static final String KEY_TRACK_ID = "trackId";

  public static PlayMultipleDialogFragment newInstance(long trackId) {
    Bundle bundle = new Bundle();
    bundle.putLong(KEY_TRACK_ID, trackId);

    PlayMultipleDialogFragment fragment = new PlayMultipleDialogFragment();
    fragment.setArguments(bundle);
    return fragment;
  }

  private PlayMultipleCaller caller;
  private String[] items;
  private boolean[] checkedItems;
  private long[] trackIds;

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    try {
      caller = (PlayMultipleCaller) activity;
    } catch (ClassCastException e) {
      throw new ClassCastException(
          activity.toString() + " must implement " + PlayMultipleCaller.class.getSimpleName());
    }
  }

  @Override
  protected Dialog createDialog() {
    loadData();
    return new AlertDialog.Builder(getActivity()).setMultiChoiceItems(items, checkedItems, null)
        .setNegativeButton(R.string.generic_cancel, null)
        .setPositiveButton(R.string.menu_play, new DialogInterface.OnClickListener() {
            @Override
          public void onClick(DialogInterface dialog, int which) {
            SparseBooleanArray array = ((AlertDialog) dialog).getListView()
                .getCheckedItemPositions();
            caller.onPlayMultipleDone(getChecked(array));
          }
        }).setTitle(R.string.menu_play_multiple).create();
  }

  private long[] getChecked(SparseBooleanArray array) {
    ArrayList<Long> checked = new ArrayList<Long>();
    for (int i = 0; i < array.size(); i++) {
      int key = array.keyAt(i);
      if (array.get(key)) {
        checked.add(trackIds[key]);
      }
    }
    long[] result = new long[checked.size()];
    for (int i = 0; i < checked.size(); i++) {
      result[i] = checked.get(i).longValue();
    }
    return result;
  }

  private void loadData() {
    long trackId = getArguments().getLong(KEY_TRACK_ID);
    MyTracksProviderUtils myTracksProviderUtils = MyTracksProviderUtils.Factory.get(getActivity());
    Cursor cursor = myTracksProviderUtils.getTrackCursor(null, null, TrackUtils.TRACK_SORT_ORDER);

    if (cursor == null) {
      items = new String[] {};
      checkedItems = new boolean[] {};
      trackIds = new long[] {};
    } else {
      int count = cursor.getCount();
      items = new String[count];
      checkedItems = new boolean[count];
      trackIds = new long[count];
      for (int i = 0; i < count; i++) {
        cursor.moveToPosition(i);
        Track track = myTracksProviderUtils.createTrack(cursor);
        items[i] = track.getName();
        checkedItems[i] = trackId != -1L && track.getId() == trackId;
        trackIds[i] = track.getId();
      }
    }
  }
}