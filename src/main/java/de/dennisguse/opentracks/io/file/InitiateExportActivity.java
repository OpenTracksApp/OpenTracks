package de.dennisguse.opentracks.io.file;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import de.dennisguse.opentracks.io.file.exporter.ExportActivity;
import de.dennisguse.opentracks.io.file.importer.DirectoryChooserActivity;
import de.dennisguse.opentracks.util.IntentUtils;
import de.dennisguse.opentracks.util.PreferencesUtils;

public class InitiateExportActivity extends AppCompatActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Uri directoryUri = PreferencesUtils.getDefaultExportDirectoryUri(this);
        if (directoryUri != null) {
            startActivity(createExportAllIntent(directoryUri));
            finish();
            return;
        }

        Intent intent = IntentUtils.newIntent(this, DirectoryChooserActivity.ExportDirectoryChooserActivity.class);
        startActivity(intent);
        finish();
    }


    protected Intent createExportAllIntent(Uri directoryUri) {
        Intent intent = IntentUtils.newIntent(this, ExportActivity.class);
        intent.putExtra(ExportActivity.EXTRA_DIRECTORY_URI_KEY, directoryUri);
        intent.putExtra(ExportActivity.EXTRA_TRACKFILEFORMAT_KEY, PreferencesUtils.getExportTrackFileFormat(this));
        return intent;
    }
}
