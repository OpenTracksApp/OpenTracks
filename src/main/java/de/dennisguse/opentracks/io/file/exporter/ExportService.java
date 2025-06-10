package de.dennisguse.opentracks.io.file.exporter;

import android.app.job.JobService;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;
import androidx.documentfile.provider.DocumentFile;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.util.ExportUtils;

public class ExportService extends JobIntentService {

    private static final String TAG = ExportService.class.getSimpleName();

    private static final int JOB_ID = 1;

    private static final String EXTRA_RECEIVER = "extra_receiver";
    private static final String EXTRA_EXPORT_TASK = "export_task";
    private static final String EXTRA_DIRECTORY_URI = "extra_directory_uri";

    public static void enqueue(Context context, ExportServiceResultReceiver receiver, ExportTask exportTask, Uri directoryUri) {
        Intent intent = new Intent(context, JobService.class);
        intent.putExtra(EXTRA_RECEIVER, receiver);
        intent.putExtra(EXTRA_EXPORT_TASK, exportTask);
        intent.putExtra(EXTRA_DIRECTORY_URI, directoryUri);
        enqueueWork(context, ExportService.class, JOB_ID, intent);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        // Get all data.
        ResultReceiver resultReceiver = intent.getParcelableExtra(EXTRA_RECEIVER);
        ExportTask exportTask = intent.getParcelableExtra(EXTRA_EXPORT_TASK);
        Uri directoryUri = intent.getParcelableExtra(EXTRA_DIRECTORY_URI);

        // Prepare resultCode and bundle to send to the receiver.
        Bundle bundle = new Bundle();
        bundle.putParcelable(ExportServiceResultReceiver.RESULT_EXTRA_EXPORT_TASK, exportTask);

        // Build directory file.
        DocumentFile directoryFile = DocumentFile.fromTreeUri(this, directoryUri);
        if (directoryFile == null || !directoryFile.canWrite()) {
            bundle.putString(ExportServiceResultReceiver.EXTRA_EXPORT_ERROR_MESSAGE, getString(R.string.export_cannot_write_to_dir) + ": " + directoryFile);
            resultReceiver.send(ExportServiceResultReceiver.RESULT_CODE_ERROR, bundle);
            return;
        }

        // Export and send result
        try {
            ExportUtils.exportTrack(this, directoryFile, exportTask);
            resultReceiver.send(ExportServiceResultReceiver.RESULT_CODE_SUCCESS, bundle);
        } catch (Exception e) {
            Log.e(TAG, "Export failed: " + e);
            e.printStackTrace(); //TODO Remove

            bundle.putString(ExportServiceResultReceiver.EXTRA_EXPORT_ERROR_MESSAGE, e.getMessage());
            resultReceiver.send(ExportServiceResultReceiver.RESULT_CODE_ERROR, bundle);
        }
    }

    public static class ExportServiceResultReceiver extends ResultReceiver {

        public static final int RESULT_CODE_SUCCESS = 1;
        public static final int RESULT_CODE_ERROR = 0;

        public static final String RESULT_EXTRA_EXPORT_TASK = "result_extra_export_task";

        public static final String EXTRA_EXPORT_ERROR_MESSAGE = "extra_export_error_message";

        private final Receiver receiver;

        public ExportServiceResultReceiver(Handler handler, @NonNull Receiver receiver) {
            super(handler);
            this.receiver = receiver;
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            ExportTask exportTask = resultData.getParcelable(RESULT_EXTRA_EXPORT_TASK);
            switch (resultCode) {
                case RESULT_CODE_SUCCESS -> receiver.onExportSuccess(exportTask);
                case RESULT_CODE_ERROR -> receiver.onExportError(exportTask, resultData.getString(EXTRA_EXPORT_ERROR_MESSAGE));
                default -> throw new RuntimeException("Unknown resultCode.");
            }
        }

        public interface Receiver {
            default void onExportSuccess(ExportTask exportTask) {
            }

            default void onExportError(ExportTask exportTask, String errorMessage) {
            }
        }
    }
}
