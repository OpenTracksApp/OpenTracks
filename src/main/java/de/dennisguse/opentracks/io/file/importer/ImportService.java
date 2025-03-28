package de.dennisguse.opentracks.io.file.importer;

import android.app.job.JobService;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;
import androidx.documentfile.provider.DocumentFile;

import java.io.IOException;
import java.util.ArrayList;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.data.ContentProviderUtils;
import de.dennisguse.opentracks.data.models.Distance;
import de.dennisguse.opentracks.data.models.Track;
import de.dennisguse.opentracks.io.file.TrackFileFormat;
import de.dennisguse.opentracks.settings.PreferencesUtils;
import de.dennisguse.opentracks.util.FileUtils;

public class ImportService extends JobIntentService {

    private static final String TAG = ImportService.class.getSimpleName();

    private static final int JOB_ID = 2;

    private static final String EXTRA_RECEIVER = "extra_receiver";
    private static final String EXTRA_URI = "extra_uri";

    private ResultReceiver resultReceiver;

    public static void enqueue(Context context, ImportServiceResultReceiver receiver, Uri uri) {
        Intent intent = new Intent(context, JobService.class);
        intent.putExtra(EXTRA_RECEIVER, receiver);
        intent.putExtra(EXTRA_URI, uri);
        enqueueWork(context, ImportService.class, JOB_ID, intent);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        resultReceiver = intent.getParcelableExtra(EXTRA_RECEIVER);
        Uri uri = intent.getParcelableExtra(EXTRA_URI);
        importFile(DocumentFile.fromSingleUri(this, uri));
    }

    private void importFile(DocumentFile file) {
        ArrayList<Track.Id> trackIds = new ArrayList<>();

        String fileExtension = FileUtils.getExtension(file);
        try {
            Distance maxRecordingDistance = PreferencesUtils.getMaxRecordingDistance();
            boolean preventReimport = PreferencesUtils.getPreventReimportTracks();

            TrackImporter trackImporter = new TrackImporter(this, new ContentProviderUtils(this), maxRecordingDistance, preventReimport);

            if (TrackFileFormat.GPX.getExtension().equals(fileExtension)) {
                trackIds.addAll(new XMLImporter(new GPXTrackImporter(this, trackImporter)).importFile(this, file.getUri()));
            } else if (TrackFileFormat.KML_WITH_TRACKDETAIL_AND_SENSORDATA.getExtension().equals(fileExtension)) {
                trackIds.addAll(new XMLImporter(new KMLTrackImporter(this, trackImporter)).importFile(this, file.getUri()));
            } else if (TrackFileFormat.KMZ_WITH_TRACKDETAIL_AND_SENSORDATA_AND_PICTURES.getExtension().equals(fileExtension)) {
                trackIds.addAll(new KMZTrackImporter(this, trackImporter).importFile(file.getUri()));
            } else {
                Log.d(TAG, "Unsupported file format.");
                sendResult(ImportServiceResultReceiver.RESULT_CODE_ERROR, null, file, getString(R.string.import_unsupported_format));
                return;
            }

            if (!trackIds.isEmpty()) {
                sendResult(ImportServiceResultReceiver.RESULT_CODE_IMPORTED, trackIds, file, getString(R.string.import_file_imported, file.getName()));
            } else {
                sendResult(ImportServiceResultReceiver.RESULT_CODE_ERROR, trackIds, file, getString(R.string.import_unable_to_import_file, file.getName()));
            }
        } catch (IOException e) {
            Log.d(TAG, "Unable to import file", e);
            sendResult(ImportServiceResultReceiver.RESULT_CODE_ERROR, null, file, getString(R.string.import_unable_to_import_file, e.getMessage()));
        } catch (ImportParserException e) {
            Log.d(TAG, "Parser error: " + e.getMessage(), e);
            sendResult(ImportServiceResultReceiver.RESULT_CODE_ERROR, null, file, getString(R.string.import_parser_error, e.getMessage()));
        } catch (ImportAlreadyExistsException e) {
            Log.d(TAG, "Track already exists: " + e.getMessage(), e);
            sendResult(ImportServiceResultReceiver.RESULT_CODE_ALREADY_EXISTS, null, file, e.getMessage());
        }
    }

    private void sendResult(int resultCode, ArrayList<Track.Id> trackId, DocumentFile file, String message) {
        Bundle bundle = new Bundle();
        bundle.putParcelableArrayList(ImportServiceResultReceiver.RESULT_EXTRA_LIST_TRACK_ID, trackId);
        bundle.putString(ImportServiceResultReceiver.RESULT_EXTRA_FILENAME, file.getName());
        bundle.putString(ImportServiceResultReceiver.RESULT_EXTRA_MESSAGE, message);
        resultReceiver.send(resultCode, bundle);
    }
}
