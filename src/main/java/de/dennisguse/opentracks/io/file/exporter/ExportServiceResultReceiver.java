package de.dennisguse.opentracks.io.file.exporter;

import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;

/**
 * Create a new ResultReceive to receive results.
 * Your {@link #onReceiveResult} method will be called from the thread running <var>handler</var> if given, or from an arbitrary thread if null.
 */
public class ExportServiceResultReceiver extends ResultReceiver {

    public static final int RESULT_CODE_SUCCESS = 1;
    public static final int RESULT_CODE_ERROR = 0;

    public static final String RESULT_EXTRA_TRACK_ID = "result_extra_track_id";

    private Receiver receiver;

    public ExportServiceResultReceiver(Handler handler) {
        super(handler);
    }

    @Override
    protected void onReceiveResult(int resultCode, Bundle resultData) {
        if (receiver != null) {
            receiver.onReceiveResult(resultCode, resultData);
        }
    }

    public void setReceiver(Receiver receiver) {
        this.receiver = receiver;
    }

    public interface Receiver {
        void onReceiveResult(int resultCode, Bundle resultData);
    }
}
