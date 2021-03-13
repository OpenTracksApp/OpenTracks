package de.dennisguse.opentracks.io.file.exporter;

import android.app.job.JobService;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.ResultReceiver;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;
import androidx.documentfile.provider.DocumentFile;

import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.content.provider.ContentProviderUtils;
import de.dennisguse.opentracks.io.file.TrackFileFormat;
import de.dennisguse.opentracks.util.ExportUtils;

public class ExportService extends JobIntentService {

    private static final int JOB_ID = 1;

    private static final String EXTRA_RECEIVER = "extra_receiver";
    private static final String EXTRA_TRACK_ID = "extra_track_id";
    private static final String EXTRA_TRACK_FILE_FORMAT = "extra_track_file_format";
    private static final String EXTRA_DIRECTORY_URI = "extra_directory_uri";

    public static void enqueue(Context context, ExportServiceResultReceiver receiver, Track.Id trackId, TrackFileFormat trackFileFormat, Uri directoryUri) {
        Intent intent = new Intent(context, JobService.class);
        intent.putExtra(EXTRA_RECEIVER, receiver);
        intent.putExtra(EXTRA_TRACK_ID, trackId);
        intent.putExtra(EXTRA_TRACK_FILE_FORMAT, trackFileFormat);
        intent.putExtra(EXTRA_DIRECTORY_URI, directoryUri);
        enqueueWork(context, ExportService.class, JOB_ID, intent);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        // Get all data.
        ResultReceiver resultReceiver = intent.getParcelableExtra(EXTRA_RECEIVER);
        Track.Id trackId = intent.getParcelableExtra(EXTRA_TRACK_ID);
        TrackFileFormat trackFileFormat = (TrackFileFormat) intent.getSerializableExtra(EXTRA_TRACK_FILE_FORMAT);
        Uri directoryUri = intent.getParcelableExtra(EXTRA_DIRECTORY_URI);

        // Build directory file.
        DocumentFile directoryFile = DocumentFile.fromTreeUri(this, directoryUri);

        // Export.
        ContentProviderUtils contentProviderUtils = new ContentProviderUtils(this);
        Track track = contentProviderUtils.getTrack(trackId);
        boolean success = ExportUtils.exportTrack(this, trackFileFormat, directoryFile, track);

        // Prepare resultCode and bundle to send to the receiver.
        Bundle bundle = new Bundle();
        bundle.putParcelable(ExportServiceResultReceiver.RESULT_EXTRA_TRACK_ID, trackId);

        // Send result to the receiver.
        int resultCode = success ? ExportServiceResultReceiver.RESULT_CODE_SUCCESS : ExportServiceResultReceiver.RESULT_CODE_ERROR;
        resultReceiver.send(resultCode, bundle);
    }
}
