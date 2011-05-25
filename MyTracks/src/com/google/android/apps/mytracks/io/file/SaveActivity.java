package com.google.android.apps.mytracks.io.file;

import com.google.android.apps.mytracks.Constants;
import com.google.android.apps.mytracks.DialogManager;
import com.google.android.apps.mytracks.content.MyTracksProviderUtils;
import com.google.android.apps.mytracks.io.file.TrackWriterFactory.TrackFileFormat;
import com.google.android.apps.mytracks.util.FileUtils;
import com.google.android.maps.mytracks.R;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import java.io.File;

public class SaveActivity extends Activity {
  public static final String EXTRA_TRACK_ID = "track_id";
  public static final String EXTRA_SHARE_FILE = "share_file";
  public static final String EXTRA_FILE_FORMAT = "file_format";

  private MyTracksProviderUtils providerUtils;
  private long trackId;
  private TrackWriter writer;
  private boolean shareFile;
  private TrackFileFormat format;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    providerUtils = MyTracksProviderUtils.Factory.get(this);
  }

  @Override
  protected void onStart() {
    super.onStart();

    Intent intent = getIntent();
    trackId = intent.getLongExtra(EXTRA_TRACK_ID, -1);
    int formatIdx = intent.getIntExtra(EXTRA_FILE_FORMAT, -1);
    format = TrackFileFormat.values()[formatIdx];
    shareFile = intent.getBooleanExtra(EXTRA_SHARE_FILE, false);

    writer = TrackWriterFactory.newWriter(this, providerUtils, trackId, format);

    if (shareFile) {
      // If the file is for sending, save it to a temporary location instead.
      FileUtils fileUtils = new FileUtils();
      String extension = format.getExtension();
      String dirName = fileUtils.buildExternalDirectoryPath(extension, "tmp");

      File dir = new File(dirName);
      writer.setDirectory(dir);
    }

    WriteProgressController controller = new WriteProgressController(this, writer);
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
        getResources().getText(R.string.send_track_subject).toString());
    shareIntent.putExtra(Intent.EXTRA_TEXT,
        getResources().getText(R.string.send_track_body_format)
        .toString());
    shareIntent.setType(format.getMimeType());
    Uri u = Uri.fromFile(new File(writer.getAbsolutePath()));
    shareIntent.putExtra(Intent.EXTRA_STREAM, u);
    startActivity(Intent.createChooser(shareIntent,
        getResources().getText(R.string.share_track).toString()));
  }

  private void showResultDialog() {
    DialogManager.showMessageDialog(this, writer.getErrorMessage(), writer.wasSuccess(),
        new OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int arg1) {
            dialog.dismiss();
            finish();
          }
        });
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

    Intent intent = new Intent(ctx, SaveActivity.class);
    intent.putExtra(EXTRA_TRACK_ID, trackId);
    intent.putExtra(EXTRA_FILE_FORMAT, exportFormat.ordinal());
    intent.putExtra(EXTRA_SHARE_FILE, shareFile);
    intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
    ctx.startActivity(intent);
  }
}
