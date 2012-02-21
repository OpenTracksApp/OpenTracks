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

package com.google.android.apps.mytracks.util;

import com.google.android.apps.mytracks.content.TracksColumns;
import com.google.android.apps.mytracks.io.file.SaveActivity;
import com.google.android.apps.mytracks.io.file.TrackWriterFactory.TrackFileFormat;
import com.google.android.maps.mytracks.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;

import java.util.List;

/**
 * Utilities for playing a track on Google Earth.
 *
 * @author Jimmy Shih
 */
public class PlayTrackUtils {

  /**
   * KML mime type.
   */
  public static final String KML_MIME_TYPE = "application/vnd.google-earth.kml+xml";

  private static final String GOOGLE_EARTH_PACKAGE = "com.google.earth";
  private static final String EARTN_MARKET_URI = "market://details?id=" + GOOGLE_EARTH_PACKAGE;

  private PlayTrackUtils() {}

  /**
   * Returns true if Google Earth is installed.
   *
   * @param context the context
   */
  public static boolean isEarthInstalled(Context context) {
    List<ResolveInfo> infos = context.getPackageManager().queryIntentActivities(
        new Intent().setType(KML_MIME_TYPE), PackageManager.MATCH_DEFAULT_ONLY);
    for (ResolveInfo info : infos) {
      if (info.activityInfo != null && info.activityInfo.packageName != null
          && info.activityInfo.packageName.equals(GOOGLE_EARTH_PACKAGE)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Plays a track by sending an intent to {@link SaveActivity}.
   *
   * @param context the context
   * @param trackId the track id
   */
  public static void playTrack(Context context, long trackId) {
    Uri uri = ContentUris.withAppendedId(TracksColumns.CONTENT_URI, trackId);
    Intent intent = new Intent(context, SaveActivity.class)
        .putExtra(SaveActivity.EXTRA_FILE_FORMAT, TrackFileFormat.KML.ordinal())
        .putExtra(SaveActivity.EXTRA_PLAY_FILE, true)
        .setAction(context.getString(R.string.track_action_save))
        .setDataAndType(uri, TracksColumns.CONTENT_ITEMTYPE);
    context.startActivity(intent);
  }

  /**
   * Creates a dialog to install Google Earth from the Android Market.
   * 
   * @param context the context
   */
  public static Dialog createInstallEarthDialog(final Context context) {
    return new AlertDialog.Builder(context)
        .setCancelable(true)
        .setMessage(R.string.track_list_play_install_earth_message)
        .setNegativeButton(android.R.string.cancel, null)
        .setPositiveButton(android.R.string.ok, new OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            Intent intent = new Intent();
            intent.setData(Uri.parse(EARTN_MARKET_URI));
            context.startActivity(intent);
          }
        })
        .create();
  }
}
