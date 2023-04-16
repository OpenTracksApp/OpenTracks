package de.dennisguse.opentracks.io.file.importer;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import de.dennisguse.opentracks.io.file.exporter.ExportActivity;
import de.dennisguse.opentracks.settings.PreferencesUtils;
import de.dennisguse.opentracks.util.IntentUtils;

public class DriveExporter extends AppCompatActivity {
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = IntentUtils.newIntent(this, ExportActivity.class);
        intent.putExtra(ExportActivity.EXTRA_TRACKFILEFORMAT_KEY, PreferencesUtils.getExportTrackFileFormat());
        intent.putExtra(ExportActivity.TYPE_OF_CLICK, "cloud");
        intent.putExtra(ImportActivity.EXTRA_DIRECTORY_URI_KEY, PreferencesUtils.getDefaultExportDirectoryUri());
        startActivity(intent);
        finish();
    }

}
