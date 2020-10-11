package de.dennisguse.opentracks.util;

import android.content.Context;
import android.util.Log;

import androidx.documentfile.provider.DocumentFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;

import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.io.file.TrackFileFormat;
import de.dennisguse.opentracks.io.file.exporter.TrackExporter;

public class ExportUtils {

    private static final String TAG = ExportUtils.class.getSimpleName();

    public static void postWorkoutExport(Context context, Track track) {
        if (PreferencesUtils.shouldInstantExportAfterWorkout(context)) {
            TrackFileFormat trackFileFormat = PreferencesUtils.getExportTrackFileFormat(context);
            DocumentFile directory = PreferencesUtils.getDefaultExportDirectoryUri(context);

            exportTrack(context, trackFileFormat, directory, track);
        }
    }

    public static boolean exportTrack(Context context, TrackFileFormat trackFileFormat, DocumentFile directory, Track track) {
        TrackExporter trackExporter = trackFileFormat.newTrackExporter(context);

        DocumentFile exportDocumentFile = ExportUtils.getExportDocumentFile(track.getId(), trackFileFormat.getExtension(), directory, trackFileFormat.getMimeType());

        try (OutputStream outputStream = context.getContentResolver().openOutputStream(exportDocumentFile.getUri())) {
            if (trackExporter.writeTrack(track, outputStream)) {
                return true;
            } else {
                if (!exportDocumentFile.delete()) {
                    Log.e(TAG, "Unable to delete exportDocumentFile");
                }
                Log.e(TAG, "Unable to export track");
                return false;
            }
        } catch (FileNotFoundException e) {
            Log.e(TAG, "Unable to open exportDocumentFile " + exportDocumentFile.getName(), e);
            return false;
        } catch (IOException e) {
            Log.e(TAG, "Unable to close exportDocumentFile output stream", e);
            return false;
        }
    }

    public static DocumentFile getExportDocumentFile(Track.Id trackId, String trackFileFormatExtension, DocumentFile directory, String mimeType) {
        String fileName = getExportFileNameByTrackId(trackId, trackFileFormatExtension);
        DocumentFile file = directory.findFile(fileName);
        if (file == null) {
            file = directory.createFile(mimeType, fileName);
        }
        return file;
    }

    private static String getExportFileNameByTrackId(Track.Id trackId, String trackFileFormatExtension) {
        return trackId.getId() + "." + trackFileFormatExtension;
    }
}
