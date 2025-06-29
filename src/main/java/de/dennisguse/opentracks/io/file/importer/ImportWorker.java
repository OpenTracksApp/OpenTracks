package de.dennisguse.opentracks.io.file.importer;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.IOException;
import java.util.ArrayList;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.data.ContentProviderUtils;
import de.dennisguse.opentracks.data.models.Distance;
import de.dennisguse.opentracks.data.models.Track;
import de.dennisguse.opentracks.io.file.TrackFileFormat;
import de.dennisguse.opentracks.settings.PreferencesUtils;
import de.dennisguse.opentracks.util.FileUtils;

public class ImportWorker extends Worker {

    private static final String TAG = ImportWorker.class.getSimpleName();

    static final String URI_KEY = "DIRECTORY_URI_KEY";

    static final String RESULT_SUCCESS_LIST_TRACKIDS_KEY = "RESULT_TRACK_IDS";
    static final String RESULT_URI_KEY = "RESULT_URI";
    static final String RESULT_MESSAGE_KEY = "RESULT_MESSAGE";
    static final String RESULT_FAILURE_IS_DUPLICATE = "RESULT_FAILURE_IS_DUPLICATE";

    private final Uri uri;

    public ImportWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);

        uri = Uri.parse(getInputData().getString(URI_KEY));
    }

    @NonNull
    @Override
    public Result doWork() {
        ArrayList<Track.Id> trackIds = new ArrayList<>();
        Context context = getApplicationContext();

        Distance maxRecordingDistance = PreferencesUtils.getMaxRecordingDistance();
        boolean preventReimport = PreferencesUtils.getPreventReimportTracks();
        TrackImporter trackImporter = new TrackImporter(context, new ContentProviderUtils(context), maxRecordingDistance, preventReimport);

        Data.Builder data = new Data.Builder()
                .putString(RESULT_URI_KEY, uri.toString());


        String fileExtension = FileUtils.getExtension(uri);
        try {
            if (TrackFileFormat.GPX.getExtension().equals(fileExtension)) {
                trackIds.addAll(new XMLImporter(new GPXTrackImporter(getApplicationContext(), trackImporter)).importFile(context, uri));
            } else if (TrackFileFormat.KML_WITH_TRACKDETAIL_AND_SENSORDATA.getExtension().equals(fileExtension)) {
                trackIds.addAll(new XMLImporter(new KMLTrackImporter(getApplicationContext(), trackImporter)).importFile(context, uri));
            } else if (TrackFileFormat.KMZ_WITH_TRACKDETAIL_AND_SENSORDATA_AND_PICTURES.getExtension().equals(fileExtension)) {
                trackIds.addAll(new KMZTrackImporter(context, trackImporter).importFile(uri));
            } else {
                Log.d(TAG, "Unsupported file format.");
                return Result.failure(data
                        .putString(RESULT_MESSAGE_KEY, context.getString(R.string.import_unsupported_format))
                        .build());
            }

            if (!trackIds.isEmpty()) {
                return Result.success(data
                        .putString(RESULT_MESSAGE_KEY, context.getString(R.string.import_file_imported, uri.toString())) //TODO unused?
                        .putLongArray(RESULT_SUCCESS_LIST_TRACKIDS_KEY, trackIds.stream().mapToLong(Track.Id::id).toArray())
                        .build());
            }

            return Result.failure(data
                    .putString(RESULT_MESSAGE_KEY, context.getString(R.string.import_unable_to_import_file, uri.toString()))
                    .build());

        } catch (IOException e) {
            Log.d(TAG, "Unable to import file", e);
            return Result.failure(data
                    .putString(RESULT_MESSAGE_KEY, context.getString(R.string.import_unable_to_import_file, e.getMessage()))
                    .build());
        } catch (ImportParserException e) {
            Log.d(TAG, "Parser error: " + e.getMessage(), e);
            return Result.failure(data
                    .putString(RESULT_MESSAGE_KEY, context.getString(R.string.import_parser_error, e.getMessage()))
                    .build());
        } catch (ImportAlreadyExistsException e) {
            Log.d(TAG, "Track already exists: " + e.getMessage(), e);
            return Result.failure(data
                    .putString(RESULT_MESSAGE_KEY, e.getMessage()) //TODO This is not used
                    .putBoolean(RESULT_FAILURE_IS_DUPLICATE, true)
                    .build());
        }
    }
}
