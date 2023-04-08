package de.dennisguse.opentracks.util;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.provider.DocumentsContract;
import android.util.Log;
import android.widget.Toast;

import androidx.documentfile.provider.DocumentFile;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.channels.Channels;
import java.util.ArrayList;
import java.util.List;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.data.models.Track;
import de.dennisguse.opentracks.io.file.TrackFileFormat;
import de.dennisguse.opentracks.io.file.exporter.ExportService;
import de.dennisguse.opentracks.io.file.exporter.ExportServiceResultReceiver;
import de.dennisguse.opentracks.io.file.exporter.TrackExporter;
import de.dennisguse.opentracks.settings.PreferencesUtils;
import de.dennisguse.opentracks.settings.SettingsActivity;

public class ExportUtils {

    private static final String TAG = ExportUtils.class.getSimpleName();

    public static void postWorkoutExport(Context context, Track.Id trackId) {
        if (PreferencesUtils.shouldInstantExportAfterWorkout()) {
            TrackFileFormat trackFileFormat = PreferencesUtils.getExportTrackFileFormat();
            DocumentFile directory = IntentUtils.toDocumentFile(context, PreferencesUtils.getDefaultExportDirectoryUri());

            if (directory == null || !directory.canWrite()) {
                Toast.makeText(context, R.string.export_cannot_write_to_dir, Toast.LENGTH_LONG).show();
                return;
            }

            ExportServiceResultReceiver resultReceiver = new ExportServiceResultReceiver(new Handler(), new ExportServiceResultReceiver.Receiver() {
                @Override
                public void onExportError(Track.Id trackId) {
                    Intent intent = new Intent(context, SettingsActivity.class);
                    intent.putExtra(SettingsActivity.EXTRAS_CHECK_EXPORT_DIRECTORY, true);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                }
            });

            ExportService.enqueue(context, resultReceiver, trackId, trackFileFormat, directory.getUri(), "local");
        }
    }

    public static boolean exportTrack(Context context, TrackFileFormat trackFileFormat, DocumentFile directory, Track track, String typeOfClick) {

        TrackExporter trackExporter = trackFileFormat.createTrackExporter(context);

        Uri exportDocumentFileUri = null;
        if (directory != null) {
            exportDocumentFileUri = getExportDocumentFileUri(context, track, trackFileFormat, directory);
            if (exportDocumentFileUri == null) {
                Log.e(TAG, "Couldn't create document file for export");
                return false;
            }
        }
        try {
            OutputStream outputStream = null;

            if (exportDocumentFileUri != null) {
                outputStream = context.getContentResolver().openOutputStream(exportDocumentFileUri);
            }
            System.out.println("click :" + typeOfClick);
            if (typeOfClick != null && typeOfClick.equals("cloud")) {
                Storage storage = StorageOptions.newBuilder()
                        .setProjectId("SOEN")
                        .build()
                        .getService();
                BlobId blobId = BlobId.of("soen_data1", track.getName() + ".kmz");
                BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();
                Blob blob = storage.create(blobInfo);
                outputStream = Channels.newOutputStream(blob.writer());
                System.out.println("Track name: " + track.getName());

            }
            if (trackExporter.writeTrack(track, outputStream)) {
                System.out.println("Writing into cloud");
                return true;
            } else {
                if (!DocumentFile.fromSingleUri(context, exportDocumentFileUri).delete()) {
                    Log.e(TAG, "Unable to delete exportDocumentFile");
                }
                Log.e(TAG, "Unable to export track");
                return false;
            }
        } catch (FileNotFoundException e) {
            Log.e(TAG, "Unable to open exportDocumentFile " + exportDocumentFileUri, e);
            return false;
        } catch (IOException e) {
            Log.e(TAG, "Unable to close exportDocumentFile output stream", e);
            return false;
        }
    }

    public static List<String> getAllFiles(Context context, Uri directoryUri) {
        List<String> fileNames = new ArrayList<>();
        final ContentResolver resolver = context.getContentResolver();
        final Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(directoryUri, DocumentsContract.getDocumentId(directoryUri));

        try (Cursor c = resolver.query(childrenUri, new String[]{DocumentsContract.Document.COLUMN_DISPLAY_NAME}, null, null, null)) {
            while (c.moveToNext()) {
                fileNames.add(c.getString(0));
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed query: " + e);
        }

        return fileNames;
    }

    private static Uri getExportDocumentFileUri(Context context, Track track, TrackFileFormat trackFileFormat, DocumentFile directory) {
        String exportFileName = PreferencesUtils.getTrackFileformatGenerator().format(track, trackFileFormat);
        Uri exportDocumentFileUri = findFile(context, directory.getUri(), exportFileName);
        if (exportDocumentFileUri == null) {
            final DocumentFile file = directory.createFile(trackFileFormat.getMimeType(), exportFileName);
            if (file != null) {
                exportDocumentFileUri = file.getUri();
            }
        }
        return exportDocumentFileUri;
    }

    private static Uri findFile(Context context, Uri directoryUri, String exportFileName) {
        final ContentResolver resolver = context.getContentResolver();
        final Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(directoryUri, DocumentsContract.getDocumentId(directoryUri));

        try (Cursor c = resolver.query(childrenUri, new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME}, null, null, null)) {

            while (c.moveToNext()) {
                final String documentId = c.getString(0);
                final String documentName = c.getString(1);
                if (documentName.equals(exportFileName)) {
                    return DocumentsContract.buildDocumentUriUsingTree(directoryUri, documentId);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Find file error: failed query: " + e);
        }

        return null;
    }
}
