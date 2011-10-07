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

import com.google.android.apps.mytracks.content.MyTracksProviderUtils;
import com.google.android.apps.mytracks.content.MyTracksProviderUtilsFactory;
import com.google.android.apps.mytracks.content.TracksColumns;
import com.google.android.apps.mytracks.util.UriUtils;
import com.google.android.maps.mytracks.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

/**
 * Helper activity which imports tracks from a file.
 *
 * @author Rodrigo Damazio
 */
public class ImportActivity extends Activity {

  private static final int PROGRESS_DIALOG = 1;
  private static final int SUCCESS_DIALOG = 2;
  private static final int FAILURE_DIALOG = 3;

  private MyTracksProviderUtils providerUtils;

  private ProgressDialog progressDialog;
  private int resultMessage;
  private long importedTrackIds[];

  @Override
  public void onCreate(Bundle savedState) {
    super.onCreate(savedState);

    providerUtils = MyTracksProviderUtilsFactory.get(this);
  }

  @Override
  public void onStart() {
    super.onStart();

    Intent intent = getIntent();
    String action = intent.getAction();
    Uri data = intent.getData();
    if (!(Intent.ACTION_VIEW.equals(action) || Intent.ACTION_ATTACH_DATA.equals(action))
        || !UriUtils.isFileUri(data)) {
      Log.e(TAG, "Received an intent with unsupported action or data: " + intent);
      finish();
      return;
    }

    Log.i(TAG, "Importing GPX file at " + data);
    startTrackImport(data.getPath());
  }

  @Override
  protected Dialog onCreateDialog(int id) {
    switch (id) {
      case PROGRESS_DIALOG:
        progressDialog = new ProgressDialog(this);
        progressDialog.setIcon(android.R.drawable.ic_dialog_info);
        progressDialog.setTitle(R.string.progress_title);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setMessage(getString(R.string.import_progress_message));
        return progressDialog;
      case SUCCESS_DIALOG:
        final Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setCancelable(true);
        dialogBuilder.setPositiveButton(android.R.string.ok, new OnClickListener() {
          @Override
          public void onClick(DialogInterface arg0, int arg1) {
            finish();
          }
        });
        dialogBuilder.setNeutralButton(R.string.import_show_track, new OnClickListener() {
          @Override
          public void onClick(DialogInterface arg0, int arg1) {
            showImportedTrack();
            finish();
          }
        });
        return dialogBuilder.create();
      case FAILURE_DIALOG:
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setNeutralButton(R.string.ok, new OnClickListener() {
          @Override
          public void onClick(DialogInterface arg0, int arg1) {
            finish();
          }
        });
        builder.setIcon(android.R.drawable.ic_dialog_alert);
        builder.setTitle(R.string.error);
        return builder.create();

    }

    return null;
  }

  @Override
  protected void onPrepareDialog(int id, Dialog dialog) {
    super.onPrepareDialog(id, dialog);

    switch (id) {
      case SUCCESS_DIALOG:
        AlertDialog successDialog = (AlertDialog) dialog;
        successDialog.setMessage(getString(R.string.import_success, importedTrackIds.length));
        break;
      case FAILURE_DIALOG:
        AlertDialog failureDialog = (AlertDialog) dialog;
        failureDialog.setMessage(getString(resultMessage));
        break;
    }
  }

  protected void showImportedTrack() {
    long lastTrackId = importedTrackIds[importedTrackIds.length - 1];
    Uri trackUri = ContentUris.withAppendedId(TracksColumns.DATABASE_CONTENT_URI, lastTrackId);

    Intent intent = new Intent(Intent.ACTION_VIEW);
    intent.setDataAndType(trackUri, TracksColumns.CONTENT_ITEMTYPE);
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    startActivity(intent);
    finish();
  }

  private void startTrackImport(final String fileName) {
    showDialog(PROGRESS_DIALOG);

    Thread t = new Thread() {
      @Override
      public void run() {
        resultMessage = R.string.success;

        importedTrackIds = null;

        try {
          try {
            InputStream is = new FileInputStream(fileName);
            importedTrackIds = GpxImporter.importGPXFile(is, providerUtils);
          } catch (SAXException e) {
            Log.e(TAG, "Caught an unexpected exception.", e);
            resultMessage = R.string.error_generic;
          } catch (ParserConfigurationException e) {
            Log.e(TAG, "Caught an unexpected exception.", e);
            resultMessage = R.string.error_generic;
          } catch (IOException e) {
            Log.e(TAG, "Caught an unexpected exception.", e);
            resultMessage = R.string.error_unable_to_read_file;
          } catch (NullPointerException e) {
            Log.e(TAG, "Caught an unexpected exception.", e);
            resultMessage = R.string.error_invalid_gpx_format;
          } catch (OutOfMemoryError e) {
            Log.e(TAG, "Caught an unexpected exception.", e);
            resultMessage = R.string.error_out_of_memory;
          }

          boolean success = (importedTrackIds != null && importedTrackIds.length > 0);
          showImportResult(success);
        } finally {
          runOnUiThread(new Runnable() {
            public void run() {
              dismissDialog(PROGRESS_DIALOG);
            }
          });
        }
      }
    };
    t.start();
  }

  private void showImportResult(boolean success) {
    final int dialogToShow = success ? SUCCESS_DIALOG : FAILURE_DIALOG;

    // Show a dialog telling the user about the import, and asking if he wishes
    // to open the track right away.
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        showDialog(dialogToShow);
      }
    });
  }
}
