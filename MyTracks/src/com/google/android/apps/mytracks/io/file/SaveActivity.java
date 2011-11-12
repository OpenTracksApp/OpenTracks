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
package com.google.android.apps.mytracks.io.file;

import static com.google.android.apps.mytracks.Constants.TAG;

import com.google.android.apps.mytracks.Constants;
import com.google.android.apps.mytracks.content.MyTracksProviderUtils;
import com.google.android.apps.mytracks.content.TracksColumns;
import com.google.android.apps.mytracks.io.file.TrackWriterFactory.TrackFileFormat;
import com.google.android.apps.mytracks.util.FileUtils;
import com.google.android.apps.mytracks.util.UriUtils;
import com.google.android.maps.mytracks.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import java.io.File;

/**
 * Activity for saving a track to a file (and optionally sending that file).
 *
 * @author Rodrigo Damazio
 */
public class SaveActivity extends Activity {
  public static final String EXTRA_SHARE_FILE = "share_file";
  public static final String EXTRA_FILE_FORMAT = "file_format";
  private static final int RESULT_DIALOG = 1;
  /* VisibleForTesting */
  static final int PROGRESS_DIALOG = 2;

  private MyTracksProviderUtils providerUtils;
  private long trackId;
  private TrackWriter writer;
  private boolean shareFile;
  private TrackFileFormat format;
  private WriteProgressController controller;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    providerUtils = MyTracksProviderUtils.Factory.get(this);
  }

  @Override
  protected void onStart() {
    super.onStart();

    Intent intent = getIntent();
    String action = intent.getAction();
    String type = intent.getType();
    Uri data = intent.getData();
    if (!getString(R.string.track_action_save).equals(action)
        || !TracksColumns.CONTENT_ITEMTYPE.equals(type)
        || !UriUtils.matchesContentUri(data, TracksColumns.CONTENT_URI)) {
      Log.e(TAG, "Got bad save intent: " + intent);
      finish();
      return;
    }

    trackId = ContentUris.parseId(data);

    int formatIdx = intent.getIntExtra(EXTRA_FILE_FORMAT, -1);
    format = TrackFileFormat.values()[formatIdx];
    shareFile = intent.getBooleanExtra(EXTRA_SHARE_FILE, false);

    writer = TrackWriterFactory.newWriter(this, providerUtils, trackId, format);
    if (writer == null) {
      Log.e(TAG, "Unable to build writer");
      finish();
      return;
    }

    if (shareFile) {
      // If the file is for sending, save it to a temporary location instead.
      FileUtils fileUtils = new FileUtils();
      String extension = format.getExtension();
      String dirName = fileUtils.buildExternalDirectoryPath(extension, "tmp");

      File dir = new File(dirName);
      writer.setDirectory(dir);
    }

    controller = new WriteProgressController(this, writer, PROGRESS_DIALOG);
    controller.setOnCompletionListener(new WriteProgressController.OnCompletionListener() {
      @Override
      public void onComplete() {
        onWriteComplete();
      }
    });
    controller.startWrite();
  }

  private void onWriteComplete() {
    if (shareFile) {
      shareWrittenFile();
    } else {
      showResultDialog();
    }
  }

  private void shareWrittenFile() {
    if (!writer.wasSuccess()) {
      showResultDialog();
      return;
    }

    // Share the file.
    Intent shareIntent = new Intent(Intent.ACTION_SEND);
    shareIntent.putExtra(Intent.EXTRA_SUBJECT,
        getResources().getText(R.string.share_track_subject).toString());
    shareIntent.putExtra(Intent.EXTRA_TEXT,
        getResources().getText(R.string.share_track_file_body_format)
        .toString());
    shareIntent.setType(format.getMimeType());
    Uri u = Uri.fromFile(new File(writer.getAbsolutePath()));
    shareIntent.putExtra(Intent.EXTRA_STREAM, u);
    shareIntent.putExtra(getString(R.string.track_id_broadcast_extra), trackId);
    startActivity(Intent.createChooser(shareIntent,
        getResources().getText(R.string.share_track_picker_title).toString()));
  }

  private void showResultDialog() {
    removeDialog(RESULT_DIALOG);
    showDialog(RESULT_DIALOG);
  }

  @Override
  protected Dialog onCreateDialog(int id) {
    switch (id) {
      case RESULT_DIALOG:
        return createResultDialog();
      case PROGRESS_DIALOG:
        if (controller != null) {
          return controller.createProgressDialog();
        }
        //$FALL-THROUGH$
      default:
        return super.onCreateDialog(id);
    }
  }

  private Dialog createResultDialog() {
    boolean success = writer.wasSuccess();

    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setMessage(writer.getErrorMessage());
    builder.setPositiveButton(R.string.generic_ok, new OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int arg1) {
        dialog.dismiss();
        finish();
      }
    });
    builder.setOnCancelListener(new OnCancelListener() {
      @Override
      public void onCancel(DialogInterface dialog) {
        dialog.dismiss();
        finish();
      }
    });
    builder.setIcon(success ? android.R.drawable.ic_dialog_info :
        android.R.drawable.ic_dialog_alert);
    builder.setTitle(success ? R.string.generic_success : R.string.generic_error);
    return builder.create();
  }

  public static void handleExportTrackAction(Context ctx, long trackId, int actionCode) {
    if (trackId < 0) {
      return;
    }

    TrackFileFormat exportFormat = null;
    switch (actionCode) {
      case Constants.SAVE_GPX_FILE:
      case Constants.SHARE_GPX_FILE:
        exportFormat = TrackFileFormat.GPX;
        break;
      case Constants.SAVE_KML_FILE:
      case Constants.SHARE_KML_FILE:
        exportFormat = TrackFileFormat.KML;
        break;
      case Constants.SAVE_CSV_FILE:
      case Constants.SHARE_CSV_FILE:
        exportFormat = TrackFileFormat.CSV;
        break;
      case Constants.SAVE_TCX_FILE:
      case Constants.SHARE_TCX_FILE:
        exportFormat = TrackFileFormat.TCX;
        break;
      default:
        throw new IllegalArgumentException("Warning unhandled action code: " + actionCode);
    }

    boolean shareFile = false;
    switch (actionCode) {
      case Constants.SHARE_GPX_FILE:
      case Constants.SHARE_KML_FILE:
      case Constants.SHARE_CSV_FILE:
      case Constants.SHARE_TCX_FILE:
        shareFile = true;
    }

    Uri uri = ContentUris.withAppendedId(TracksColumns.CONTENT_URI, trackId);

    Intent intent = new Intent(ctx, SaveActivity.class);
    intent.setAction(ctx.getString(R.string.track_action_save));
    intent.setDataAndType(uri, TracksColumns.CONTENT_ITEMTYPE);
    intent.putExtra(EXTRA_FILE_FORMAT, exportFormat.ordinal());
    intent.putExtra(EXTRA_SHARE_FILE, shareFile);
    ctx.startActivity(intent);
  }
}
