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
import de.dennisguse.opentracks.util.StringUtils;

import static de.dennisguse.opentracks.io.file.TrackFileFormat.GPX;

public class InstantPostWorkoutExport {

    private static final String TAG = InstantPostWorkoutExport.class.getSimpleName();

    private Context context;
    private ContentProviderUtils contentProviderUtils;
    private SharedPreferences sharedPreferences;

    public InstantPostWorkoutExport(Context context, ContentProviderUtils contentProviderUtils, SharedPreferences sharedPreferences) {
        this.context = context;
        this.contentProviderUtils = contentProviderUtils;
        this.sharedPreferences = sharedPreferences;
    }

    public void exportLastTrackToExternalStorage(String uriAsString) {
        String directoryAsString = sharedPreferences.getString(uriAsString, "invalid uri");
        Uri directoryUri = Uri.parse(directoryAsString);
        DocumentFile directory = DocumentFile.fromTreeUri(context, directoryUri);
        String startdateString = StringUtils.formatDateTimeIso8601(contentProviderUtils.getLastTrack().getTrackStatistics().getStartTime_ms());
        String fileName = startdateString + "." + GPX;
        assert directory != null;
        DocumentFile file = directory.findFile(fileName);
        if (file == null) {
            file = directory.createFile(GPX.getMimeType(), fileName);
        }
        try (OutputStream outputStream = context.getContentResolver().openOutputStream(file.getUri())) {
            SingleTrackExportThread exportThread = new SingleTrackExportThread(outputStream, contentProviderUtils);
            exportThread.run();
            Toast.makeText(context, R.string.toast_message_instant_track_was_exported, Toast.LENGTH_SHORT).show();
        }   catch (FileNotFoundException e) {
            Log.e(TAG, "Unable to open file " + file.getName(), e);
        } catch (IOException e) {
            Log.e(TAG, "Unable to close file output stream", e);
        }
    }
}
