package de.dennisguse.opentracks.io.file.exporter;

import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;

import androidx.annotation.NonNull;

/**
 * Create a new ResultReceive to receive results.
 * Your {@link #onReceiveResult} method will be called from the thread running <var>handler</var> if given, or from an arbitrary thread if null.
 */
public class ExportServiceResultReceiver extends ResultReceiver {

    public static final int RESULT_CODE_SUCCESS = 1;
    public static final int RESULT_CODE_ERROR = 0;

    public static final String RESULT_EXTRA_EXPORT_TASK = "result_extra_export_task";

    private final Receiver receiver;

    public ExportServiceResultReceiver(Handler handler, @NonNull Receiver receiver) {
        super(handler);
        this.receiver = receiver;
    }

    @Override
    protected void onReceiveResult(int resultCode, Bundle resultData) {
        ExportTask exportTask = resultData.getParcelable(ExportServiceResultReceiver.RESULT_EXTRA_EXPORT_TASK);
        switch (resultCode) {
            case RESULT_CODE_SUCCESS -> receiver.onExportSuccess(exportTask);
            case RESULT_CODE_ERROR -> receiver.onExportError(exportTask);
            default -> throw new RuntimeException("Unknown resultCode.");
        }
    }

    public interface Receiver {
        default void onExportSuccess(ExportTask exportTask) {
        }

        default void onExportError(ExportTask exportTask) {
        }
    }
}
