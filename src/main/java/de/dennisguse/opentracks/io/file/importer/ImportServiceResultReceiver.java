package de.dennisguse.opentracks.io.file.importer;

import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;

import androidx.annotation.NonNull;

/**
 * Create a new ResultReceive to receive results.
 * Your {@link #onReceiveResult} method will be called from the thread running <var>handler</var> if given, or from an arbitrary thread if null.
 */
public class ImportServiceResultReceiver extends ResultReceiver {

    public static final int RESULT_CODE_ERROR = 0;
    public static final int RESULT_CODE_IMPORTED = 1;
    public static final int RESULT_CODE_ALREADY_EXISTS = 2;

    public static final String RESULT_EXTRA_LIST_TRACK_ID = "result_track_id";
    public static final String RESULT_EXTRA_FILENAME = "result_extra_filename";
    public static final String RESULT_EXTRA_MESSAGE = "result_extra_message";

    private final Receiver receiver;

    public ImportServiceResultReceiver(Handler handler, @NonNull Receiver receiver) {
        super(handler);
        this.receiver = receiver;
    }

    @Override
    protected void onReceiveResult(int resultCode, Bundle resultData) {
        receiver.onReceiveResult(resultCode, resultData);
    }

    public interface Receiver {
        void onReceiveResult(int resultCode, Bundle resultData);
    }
}
