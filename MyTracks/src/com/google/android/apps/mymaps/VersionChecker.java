/*
 * Copyright 2009 Google Inc.
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
package com.google.android.apps.mymaps;

import com.google.android.apps.mymaps.MyMapsGDataWrapper.QueryFunction;
import com.google.android.apps.mytracks.MyTracksConstants;
import com.google.android.maps.mytracks.R;
import com.google.wireless.gdata.GDataException;
import com.google.wireless.gdata.client.HttpException;
import com.google.wireless.gdata.data.Entry;
import com.google.wireless.gdata.maps.MapsClient;
import com.google.wireless.gdata.parser.GDataParser;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.TabActivity;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import java.io.IOException;

public class VersionChecker {
  /** The User id for the account storing MyTracks versions. */
  private static final String VERSION_USER_ID = "116927647071651051525";
  private static final String VERSION_CLIENT = "AndroidMyTracks";

  private static final String VERSION_STATUS_LATEST = "latest";
  private static final String VERSION_STATUS_OKAY = "okay";
  private static final String VERSION_STATUS_OLD = "old";
  private static final String VERSION_STATUS_BAD = "bad";

  private final TabActivity parentActivity;
  private Dialog dialog;
  private boolean canceledAvailable;
  private boolean canceledRecommended;

  public VersionChecker(TabActivity parent) {
    parentActivity = parent;
    canceledAvailable = false;
    canceledRecommended = false;

    startMapsQueryThread();
  }

  private void startMapsQueryThread() {
    new Thread() {
      @Override
      public void run() {
        MyMapsGDataWrapper mapsGDataWrapper =
            new MyMapsGDataWrapper(parentActivity);
        mapsGDataWrapper.runQuery(new QueryFunction() {
          @Override
          public void query(MapsClient client) throws IOException, Exception {
            doMapsQuery(client);
          }
        });
        mapsGDataWrapper.cleanUp();
      }
    }.start();
  }

  private void doMapsQuery(MapsClient client)
      throws GDataException, IOException, HttpException {
    final boolean newVersion;  // Returned new version available
    final boolean newVersionRecommended;  // Returned new version recommended
    final boolean newVersionRequired;  // Returned new version required
    String currentVersion = parentActivity.getString(R.string.version);

    GDataParser parser = client.getParserForFeed(
            Entry.class,
            MapsClient.getVersionFeed(
                VERSION_USER_ID,
                VERSION_CLIENT,
                currentVersion),
            "");
    Entry en = parser.parseStandaloneEntry();
    if (currentVersion.equals(en.getTitle())) {
      if (VERSION_STATUS_LATEST.equals(en.getContent())) {
        newVersion = false;
        newVersionRecommended = false;
        newVersionRequired = false;
      } else if (VERSION_STATUS_OKAY.equals(en.getContent())) {
        newVersion = true;
        newVersionRecommended = false;
        newVersionRequired = false;
      } else if (VERSION_STATUS_OLD.equals(en.getContent())) {
        newVersion = true;
        newVersionRecommended = true;
        newVersionRequired = false;
      } else if (VERSION_STATUS_BAD.equals(en.getContent())) {
        newVersion = true;
        newVersionRecommended = true;
        newVersionRequired = true;
      } else {
        Log.e(MyTracksConstants.TAG,
            "Got unknown version status: " + en.getContent());
        return;
      }
    } else {
      Log.e(MyTracksConstants.TAG,
          "Current version '" + currentVersion + "' doesn't match entry: "
          + en.getTitle());
      return;
    }

    if (!newVersion) {
      Log.d(MyTracksConstants.TAG, "No new version available.");
      return;
    }

    final AlertDialog.Builder dialogBuilder =
        new AlertDialog.Builder(parentActivity);
    dialogBuilder.setTitle(R.string.version_check_title);
    dialogBuilder.setPositiveButton(R.string.version_check_upgrade_button,
        new OnClickListener() {
      public void onClick(DialogInterface di, int number) {
        Intent upgradeIntent = new Intent();
        upgradeIntent.setAction(Intent.ACTION_VIEW);
        upgradeIntent.setData(Uri.parse(String.format("market://search?q=%s",
            parentActivity.getResources().getString(
                R.string.app_name).toString())));
        parentActivity.startActivity(upgradeIntent);
        parentActivity.finish();
      }
    });

    if (newVersionRequired) {
      dialogBuilder.setCancelable(false);
    } else {
      final boolean recommended = newVersionRecommended;
      dialogBuilder.setNegativeButton(R.string.version_check_continue_button,
          new OnClickListener() {
        public void onClick(DialogInterface di, int number) {
          dialog.dismiss();
          canceledAvailable = true;
          if (recommended) {
            canceledRecommended = true;
          }
        }
      });
    }

    int messageResource;
    if (newVersionRequired) {
      messageResource = R.string.upgrade_required;
    } else if (newVersionRecommended) {
      messageResource = R.string.upgrade_recommended;
      if (canceledRecommended) {
        return;
      }
    } else {
      messageResource = R.string.upgrade_available;
      if (canceledAvailable) {
        return;
      }
    }

    String message = parentActivity.getResources().getString(messageResource);
    dialogBuilder.setMessage(String.format(message,
        parentActivity.getResources().getString(R.string.app_name)));
    parentActivity.getTabHost().post(new Runnable() {
      public void run() {
        dialog = dialogBuilder.create();
        dialog.show();
      }
    });
  }
}
