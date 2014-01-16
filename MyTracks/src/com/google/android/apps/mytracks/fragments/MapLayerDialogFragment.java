/*
 * Copyright 2014 Google Inc.
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

import com.google.android.apps.mytracks.util.PreferencesUtils;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.maps.mytracks.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;

/**
 * A DialogFragment to select a map layer.
 * 
 * @author Jimmy Shih
 */
public class MapLayerDialogFragment extends AbstractMyTracksDialogFragment {

  public static final String MAP_LAYER_DIALOG_TAG = "mapLayer";

  private static final int[] LAYERS = { R.string.menu_map, R.string.menu_satellite,
      R.string.menu_satellite_with_streets, R.string.menu_terrain };
  private static final int[] MAP_TYPES = { GoogleMap.MAP_TYPE_NORMAL, GoogleMap.MAP_TYPE_SATELLITE,
      GoogleMap.MAP_TYPE_HYBRID, GoogleMap.MAP_TYPE_TERRAIN };

  @Override
  protected Dialog createDialog() {
    String[] choices = new String[LAYERS.length];
    for (int i = 0; i < LAYERS.length; i++) {
      choices[i] = getString(LAYERS[i]);
    }

    int mapType = PreferencesUtils.getInt(
        getActivity(), R.string.map_type_key, PreferencesUtils.MAP_TYPE_DEFAUlT);

    return new AlertDialog.Builder(getActivity()).setNegativeButton(R.string.generic_cancel, null)
        .setPositiveButton(R.string.generic_ok, new OnClickListener() {
            @Override
          public void onClick(DialogInterface dialog, int which) {
            int position = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
            PreferencesUtils.setInt(getActivity(), R.string.map_type_key, MAP_TYPES[position]);
          }
        }).setSingleChoiceItems(choices, getPositionFromMapType(mapType), null)
        .setTitle(R.string.menu_map_layer).create();
  }

  private int getPositionFromMapType(int mapType) {
    for (int i = 0; i < MAP_TYPES.length; i++) {
      if (MAP_TYPES[i] == mapType) {
        return i;
      }
    }
    return 0;
  }
}