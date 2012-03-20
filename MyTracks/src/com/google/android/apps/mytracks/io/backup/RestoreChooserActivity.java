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

package com.google.android.apps.mytracks.io.backup;

import com.google.android.apps.mytracks.util.FileUtils;
import com.google.android.apps.mytracks.util.StringUtils;
import com.google.android.maps.mytracks.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;

/**
 * An activity to choose a date to restore from.
 *
 * @author Jimmy Shih
 */
public class RestoreChooserActivity extends Activity {

  private static final Comparator<Date> REVERSE_DATE_ORDER = new Comparator<Date>() {
    @Override
    public int compare(Date s1, Date s2) {
      return s2.compareTo(s1);
    }
  };
  private static final int DIALOG_CHOOSER_ID = 0;

  private Date[] backupDates;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    FileUtils fileUtils = new FileUtils();
    ExternalFileBackup externalFileBackup = new ExternalFileBackup(this, fileUtils);

    // Get the list of existing backups
    if (!fileUtils.isSdCardAvailable()) {
      Toast.makeText(this, R.string.sd_card_error_no_storage, Toast.LENGTH_LONG).show();
      finish();
      return;
    }

    if (!externalFileBackup.isBackupsDirectoryAvailable(false)) {
      Toast.makeText(this, R.string.settings_backup_restore_no_backup, Toast.LENGTH_LONG).show();
      finish();
      return;
    }

    backupDates = externalFileBackup.getAvailableBackups();
    if (backupDates == null || backupDates.length == 0) {
      Toast.makeText(this, R.string.settings_backup_restore_no_backup, Toast.LENGTH_LONG).show();
      finish();
      return;
    }

    if (backupDates.length == 1) {
      startActivity(new Intent(this, RestoreActivity.class)
          .putExtra(RestoreActivity.EXTRA_DATE, backupDates[0].getTime()));
      finish();
      return;
    }

    Arrays.sort(backupDates, REVERSE_DATE_ORDER);
    showDialog(DIALOG_CHOOSER_ID);
  }

  @Override
  protected Dialog onCreateDialog(int id) {
    switch (id) {
      case DIALOG_CHOOSER_ID:
        String items[] = new String[backupDates.length];
        for (int i = 0; i < backupDates.length; i++) {
          items[i] = StringUtils.formatDateTime(this, backupDates[i].getTime());
        }
        return new AlertDialog.Builder(this)
            .setCancelable(true)
            .setItems(items, new OnClickListener() {
              @Override
              public void onClick(DialogInterface dialog, int which) {
                startActivity(new Intent(RestoreChooserActivity.this, RestoreActivity.class)
                    .putExtra(RestoreActivity.EXTRA_DATE, backupDates[which].getTime()));
                finish();
              }
            })
            .setOnCancelListener(new OnCancelListener() {
              @Override
              public void onCancel(DialogInterface dialog) {
                finish();
              }
            })
            .setTitle(R.string.settings_backup_restore_select_title)
            .create();
      default:
        return null;
    }
  }
}
