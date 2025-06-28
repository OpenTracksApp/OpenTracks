package de.dennisguse.opentracks.io.file.exporter;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.Arrays;
import java.util.List;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.data.models.Track;
import de.dennisguse.opentracks.io.file.TrackFileFormat;

public class ExportWorker extends Worker {

    private static final String TAG = ExportWorker.class.getSimpleName();

    static final String TRACKIDS_KEY = "TRACKIDS_KEY";
    static final String DIRECTORY_URI_KEY = "DIRECTORY_URI_KEY";
    static final String TRACKFILEFORMAT_KEY = "TRACKFILEFORMAT_KEY";
    static final String FILENAME_KEY = "FILENAME_KEY"; //optional; only used for multiple tracks into one file

    static final String RESULT_EXPORT_ERROR_MESSAGE_KEY = "EXPORT_ERROR_MESSAGE";

    private final List<Track.Id> trackIds;

    private final DocumentFile directoryFile;

    private final TrackFileFormat trackFileFormat;

    private final String filename;

    public ExportWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);

        trackIds = Arrays.stream(getInputData().getLongArray(TRACKIDS_KEY))
                .mapToObj(Track.Id::new)
                .toList();

        directoryFile = DocumentFile.fromTreeUri(context, Uri.parse(getInputData().getString(DIRECTORY_URI_KEY)));

        trackFileFormat = TrackFileFormat.valueOf(getInputData().getString(TRACKFILEFORMAT_KEY));

        filename = getInputData().getString(FILENAME_KEY);
    }

    @NonNull
    @Override
    public Result doWork() {
        if (directoryFile == null || !directoryFile.canWrite()) {
            return Result.failure(new Data.Builder().putString(RESULT_EXPORT_ERROR_MESSAGE_KEY, getApplicationContext().getString(R.string.export_cannot_write_to_dir) + ": " + directoryFile).build());
        }

        try {
            //TODO move method to ExportWorker?
            ExportUtils.exportTrack(getApplicationContext(), directoryFile, filename, trackFileFormat, trackIds);
            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "Export failed: " + e);

            return Result.failure(new Data.Builder().putString(RESULT_EXPORT_ERROR_MESSAGE_KEY, e.getMessage()).build());
        }
    }
}
