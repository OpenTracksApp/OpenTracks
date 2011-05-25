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

import com.google.android.apps.mytracks.DialogManager;
import com.google.android.apps.mytracks.content.MyTracksProviderUtils;
import com.google.android.apps.mytracks.content.TracksColumns;
import com.google.android.maps.mytracks.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
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

  private ProgressDialog progressDialog;
  private MyTracksProviderUtils providerUtils;

  @Override
  public void onCreate(Bundle savedState) {
    super.onCreate(savedState);

    providerUtils = MyTracksProviderUtils.Factory.get(this);
  }

  @Override
  public void onStart() {
    super.onStart();

    Intent intent = getIntent();
    String action = intent.getAction();
    if (!Intent.ACTION_VIEW.equals(action) &&
        !Intent.ACTION_ATTACH_DATA.equals(action)) {
      Log.e(TAG, "Received an intent with unsupported action: " + intent);
      finish();
      return;
    }

    if (!"file".equals(intent.getScheme())) {
      Log.e(TAG, "Received a VIEW intent with unsupported scheme " + intent.getScheme());
      finish();
      return;
    }

    Log.i(TAG, "Received a VIEW intent with file scheme. Importing.");
    startTrackImport(intent.getData().getPath());
  }

  private void startTrackImport(final String fileName) {
    progressDialog = new ProgressDialog(this);
    progressDialog.setOwnerActivity(this);
    progressDialog.setIcon(android.R.drawable.ic_dialog_info);
    progressDialog.setTitle(R.string.progress_title);
    progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
    progressDialog.setMessage(getString(R.string.import_progress_message));
    progressDialog.show();

    Thread t = new Thread() {
      @Override
      public void run() {
        int message = R.string.success;

        long[] trackIdsImported = null;

        try {
          try {
            InputStream is = new FileInputStream(fileName);
            trackIdsImported = GpxImporter.importGPXFile(is, providerUtils);
          } catch (SAXException e) {
            Log.e(TAG, "Caught an unexpected exception.", e);
            message = R.string.error_generic;
          } catch (ParserConfigurationException e) {
            Log.e(TAG, "Caught an unexpected exception.", e);
            message = R.string.error_generic;
          } catch (IOException e) {
            Log.e(TAG, "Caught an unexpected exception.", e);
            message = R.string.error_unable_to_read_file;
          } catch (NullPointerException e) {
            Log.e(TAG, "Caught an unexpected exception.", e);
            message = R.string.error_invalid_gpx_format;
          } catch (OutOfMemoryError e) {
            Log.e(TAG, "Caught an unexpected exception.", e);
            message = R.string.error_out_of_memory;
          }

          boolean success = (trackIdsImported != null && trackIdsImported.length > 0);
          onImportDone(success, message, trackIdsImported);
        } finally {
          runOnUiThread(new Runnable() {
            public void run() {
              progressDialog.dismiss();
            }
          });
        }
      }
    };
    t.start();
  }

  private void onImportDone(boolean success, int message, final long[] trackIds) {
    OnClickListener finishOnClick = new OnClickListener() {
      @Override
      public void onClick(DialogInterface arg0, int arg1) {
        finish();
      }
    };

    if (!success) {
      DialogManager.showMessageDialog(this, message, false /* success */,
          finishOnClick);
      return;
    }

    // Show a dialog telling the user about the import, and asking if he wishes
    // to open the track right away.
    final Builder dialogBuilder = new AlertDialog.Builder(this);
    dialogBuilder.setCancelable(true);
    dialogBuilder.setMessage(getString(R.string.import_success, trackIds.length));
    dialogBuilder.setPositiveButton(android.R.string.ok, finishOnClick);
    dialogBuilder.setNeutralButton(R.string.import_show_track, new OnClickListener() {
      @Override
      public void onClick(DialogInterface arg0, int arg1) {
        long lastTrackId = trackIds[trackIds.length - 1];
        Uri trackUri = ContentUris.withAppendedId(TracksColumns.CONTENT_URI, lastTrackId);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(trackUri, TracksColumns.CONTENT_ITEMTYPE);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
      }
    });

    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        dialogBuilder.show();
      }
    });
  }
}
