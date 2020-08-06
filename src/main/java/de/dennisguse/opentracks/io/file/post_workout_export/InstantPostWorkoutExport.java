package de.dennisguse.opentracks.io.file.post_workout_export;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import androidx.documentfile.provider.DocumentFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.content.provider.ContentProviderUtils;
import de.dennisguse.opentracks.io.file.TrackFileFormat;
import de.dennisguse.opentracks.util.StringUtils;

public class InstantPostWorkoutExport {

    private static final String TAG = InstantPostWorkoutExport.class.getSimpleName();

    private Context context;
    private ContentProviderUtils contentProviderUtils;
    private SharedPreferences sharedPreferences;
    private TrackFileFormat trackFileFormat;

    public InstantPostWorkoutExport(Context context, ContentProviderUtils contentProviderUtils, SharedPreferences sharedPreferences, TrackFileFormat trackFileFormat) {
        this.context = context;
        this.contentProviderUtils = contentProviderUtils;
        this.sharedPreferences = sharedPreferences;
        this.trackFileFormat = trackFileFormat;
    }

    public void exportLastTrackToExternalStorage(String uriAsString) {
        String directoryAsString = sharedPreferences.getString(uriAsString, "invalid uri");
        Uri directoryUri = Uri.parse(directoryAsString);
        DocumentFile directory = DocumentFile.fromTreeUri(context, directoryUri);
        assert directory != null;

        String startdateString = StringUtils.formatDateTimeIso8601(contentProviderUtils.getLastTrack().getTrackStatistics().getStartTime_ms());
        String fileName = startdateString + "." + trackFileFormat.getExtension();

        DocumentFile file = directory.findFile(fileName);
        if (file == null) {
            file = directory.createFile(trackFileFormat.getMimeType(), fileName);
        }

        try (OutputStream outputStream = context.getContentResolver().openOutputStream(file.getUri())) {
            LastTrackExportThread exportThread = new LastTrackExportThread(outputStream, context, trackFileFormat);
            exportThread.run();
            Toast.makeText(context, R.string.toast_message_instant_track_was_exported, Toast.LENGTH_SHORT).show();
        }   catch (FileNotFoundException e) {
            Log.e(TAG, "Unable to open file " + file.getName(), e);
        } catch (IOException e) {
            Log.e(TAG, "Unable to close file output stream", e);
        }
    }
}
