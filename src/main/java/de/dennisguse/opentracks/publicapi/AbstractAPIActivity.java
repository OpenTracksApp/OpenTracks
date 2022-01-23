package de.dennisguse.opentracks.publicapi;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.services.TrackRecordingService;
import de.dennisguse.opentracks.services.TrackRecordingServiceConnection;
import de.dennisguse.opentracks.settings.PreferencesUtils;

public abstract class AbstractAPIActivity extends AppCompatActivity {

    private final String TAG = AbstractAPIActivity.class.getSimpleName();

    private final TrackRecordingServiceConnection.Callback serviceConnectedCallback = service -> {
        if (!isFinishing() && !isDestroyed()) {
            execute(service);
        }
        if (isPostExecuteStopService()) {
            AbstractAPIActivity.this.trackRecordingServiceConnection.unbindAndStop(AbstractAPIActivity.this);
        } else {
            AbstractAPIActivity.this.trackRecordingServiceConnection.unbind(AbstractAPIActivity.this);
        }
        finish();
    };

    private TrackRecordingServiceConnection trackRecordingServiceConnection;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (PreferencesUtils.isPublicAPIenabled()) {
            Log.i(TAG, "Received and trying to execute requested action.");
            trackRecordingServiceConnection = new TrackRecordingServiceConnection(serviceConnectedCallback);
            trackRecordingServiceConnection.startAndBind(this);
        } else {
            Toast.makeText(this, getString(R.string.settings_public_api_disabled_toast), Toast.LENGTH_LONG).show();
            Log.w(TAG, "Public API is disabled; ignoring request.");
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        trackRecordingServiceConnection = null;
    }

    protected abstract void execute(TrackRecordingService service);

    protected abstract boolean isPostExecuteStopService();
}
