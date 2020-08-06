package de.dennisguse.opentracks.io.file.exporter;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import androidx.documentfile.provider.DocumentFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.content.provider.ContentProviderUtils;
import de.dennisguse.opentracks.io.file.ExportFile;
import de.dennisguse.opentracks.io.file.TrackFileFormat;

public class InstantPostWorkoutExport {

    private static final String TAG = InstantPostWorkoutExport.class.getSimpleName();

    public static void exportTrackToUri(Uri directoryUri, Context context, ContentProviderUtils contentProviderUtils, TrackFileFormat trackFileFormat, Track.Id trackId) {
        DocumentFile directory = DocumentFile.fromTreeUri(context, directoryUri);

        Track track = contentProviderUtils.getTrack(trackId);
        DocumentFile file = ExportFile.getExportDocumentFile(trackId, trackFileFormat.getExtension(), directory, trackFileFormat.getExtension());

        try (OutputStream outputStream = context.getContentResolver().openOutputStream(file.getUri())) {
            TrackExportThread trackExportThread = new TrackExportThread(outputStream, context, trackFileFormat, track);
            trackExportThread.run();
            Toast.makeText(context, R.string.toast_message_instant_track_was_exported, Toast.LENGTH_SHORT).show();
        }   catch (FileNotFoundException e) {
            Log.e(TAG, "Unable to open file " + file.getName(), e);
        } catch (IOException e) {
            Log.e(TAG, "Unable to close file output stream", e);
        }
    }
}
