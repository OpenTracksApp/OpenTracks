package de.dennisguse.opentracks.util;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.util.Log;

import androidx.documentfile.provider.DocumentFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.io.file.TrackFileFormat;
import de.dennisguse.opentracks.io.file.exporter.ExportService;
import de.dennisguse.opentracks.io.file.exporter.ExportServiceResultReceiver;
import de.dennisguse.opentracks.io.file.exporter.TrackExporter;

public class ExportUtils {

    private static final String TAG = ExportUtils.class.getSimpleName();

    public static void postWorkoutExport(Context context, Track.Id trackId, ExportServiceResultReceiver resultReceiver) {
        SharedPreferences sharedPreferences = PreferencesUtils.getSharedPreferences(context);
        if (PreferencesUtils.shouldInstantExportAfterWorkout(sharedPreferences, context)) {
            TrackFileFormat trackFileFormat = PreferencesUtils.getExportTrackFileFormat(sharedPreferences, context);
            DocumentFile directory = PreferencesUtils.getDefaultExportDirectoryUri(sharedPreferences, context);

            ExportService.enqueue(context, resultReceiver, trackId, trackFileFormat, directory.getUri());
        }
    }

    public static boolean exportTrack(Context context, TrackFileFormat trackFileFormat, DocumentFile directory, Track track) {
        TrackExporter trackExporter = trackFileFormat.createTrackExporter(context);

        Uri exportDocumentFileUri = getExportDocumentFileUri(context, track, trackFileFormat, directory);
        if (exportDocumentFileUri == null) {
            Log.e(TAG, "Couldn't create document file for export");
            return false;
        }

        try (OutputStream outputStream = context.getContentResolver().openOutputStream(exportDocumentFileUri)) {
            if (trackExporter.writeTrack(track, outputStream)) {
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

    public static boolean isExportFileExists(UUID uuuid, String trackFileFormatExtension, List<String> filesName) {
        String word = uuuid.toString().substring(0, 8) + ".*\\." + trackFileFormatExtension;
        Pattern pattern = Pattern.compile(word);
        return filesName.stream().anyMatch(w -> pattern.matcher(w).matches());
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
        String exportFileName = getExportFileNameForTrack(track, trackFileFormat.getExtension());
        Uri exportDocumentFileUri = findFile(context, directory.getUri(), exportFileName);
        if (exportDocumentFileUri == null) {
            final DocumentFile file = directory.createFile(trackFileFormat.getMimeType(), exportFileName);
            if (file != null) {
                exportDocumentFileUri = file.getUri();
            }
        }
        return exportDocumentFileUri;
    }

    private static String getExportFileNameForTrack(Track track, String trackFileFormatExtension) {
        return track.getUuid().toString().substring(0, 8) + "_" + track.getName() + "." + trackFileFormatExtension;
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
