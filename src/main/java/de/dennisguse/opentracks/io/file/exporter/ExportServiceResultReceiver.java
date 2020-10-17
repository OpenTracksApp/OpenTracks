package de.dennisguse.opentracks.io.file.exporter;

import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;

public class ExportServiceResultReceiver extends ResultReceiver {

    public static final int RESULT_UNKNOWN = 100;
    public static final int RESULT_OVERWRITTEN = 101;
    public static final int RESULT_SKIPPED = 102;
    public static final int RESULT_EXPORTED = 103;

    public static final String RESULT_EXTRA_TRACK_ID = "result_extra_track_id";
    public static final String RESULT_EXTRA_SUCCESS = "result_extra_success";

    private Receiver receiver;

    /**
     * Create a new ResultReceive to receive results.  Your
     * {@link #onReceiveResult} method will be called from the thread running
     * <var>handler</var> if given, or from an arbitrary thread if null.
     *
     * @param handler
     */
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
