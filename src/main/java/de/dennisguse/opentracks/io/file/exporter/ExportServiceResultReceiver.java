package de.dennisguse.opentracks.io.file.exporter;

import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;

import androidx.annotation.NonNull;

import de.dennisguse.opentracks.data.models.Track;

/**
 * Create a new ResultReceive to receive results.
 * Your {@link #onReceiveResult} method will be called from the thread running <var>handler</var> if given, or from an arbitrary thread if null.
 */
public class ExportServiceResultReceiver extends ResultReceiver {

    public static final int RESULT_CODE_SUCCESS = 1;
    public static final int RESULT_CODE_ERROR = 0;

    public static final String RESULT_EXTRA_TRACK_ID = "result_extra_track_id";

    private final Receiver receiver;

    public ExportServiceResultReceiver(Handler handler, @NonNull Receiver receiver) {
        super(handler);
        this.receiver = receiver;
    }

    @Override
    protected void onReceiveResult(int resultCode, Bundle resultData) {
        Track.Id trackId = resultData.getParcelable(ExportServiceResultReceiver.RESULT_EXTRA_TRACK_ID);
        switch (resultCode) {
            case RESULT_CODE_SUCCESS:
                receiver.onExportSuccess(trackId);
            case RESULT_CODE_ERROR:
                receiver.onExportError(trackId);
            default:
                throw new RuntimeException("Unknown resultCode.");
        }
    }

    public interface Receiver {
        default void onExportSuccess(Track.Id trackId) {
        }

        default void onExportError(Track.Id trackId) {
        }
    }
}
