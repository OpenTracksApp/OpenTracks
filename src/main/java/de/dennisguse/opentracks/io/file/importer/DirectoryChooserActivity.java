package de.dennisguse.opentracks.io.file.importer;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;

import de.dennisguse.opentracks.io.file.exporter.ExportActivity;
import de.dennisguse.opentracks.util.IntentUtils;
import de.dennisguse.opentracks.util.PreferencesUtils;

public abstract class DirectoryChooserActivity extends AppCompatActivity {

    private static final int DIRECTORY_PICKER_REQUEST_CODE = 6;

    protected SharedPreferences sharedPreferences;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPreferences = PreferencesUtils.getSharedPreferences(this);

        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        DocumentFile directoryUri = configureDirectoryChooserIntent(intent);
        if (directoryUri == null) {
            startActivityForResult(intent, DIRECTORY_PICKER_REQUEST_CODE);
        } else {
            startActivity(createNextActivityIntent(directoryUri.getUri())); //TODO Refactor to DocumentFile
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);
        onActivityResultCustom(requestCode, resultCode, resultData);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sharedPreferences = null;
    }

    protected void onActivityResultCustom(int requestCode, int resultCode, @Nullable Intent resultData) {
        if (requestCode == DIRECTORY_PICKER_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                Uri directoryUri = resultData.getData();

                int takeFlags = resultData.getFlags();
                takeFlags &= (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                getContentResolver().takePersistableUriPermission(directoryUri, takeFlags);

                startActivity(createNextActivityIntent(directoryUri));
            }
        }
        finish();
    }

    /**
     * @return null if directory needs to be selected.
     */
    protected DocumentFile configureDirectoryChooserIntent(Intent intent) {
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        return null;
    }

    protected abstract Intent createNextActivityIntent(Uri directoryUri);

    public static class ImportDirectoryChooserActivity extends DirectoryChooserActivity {

        @Override
        protected Intent createNextActivityIntent(Uri directoryUri) {
            Intent intent = IntentUtils.newIntent(this, ImportActivity.class);
            intent.putExtra(ImportActivity.EXTRA_DIRECTORY_URI_KEY, directoryUri);
            return intent;
        }
    }

    public static class ExportDirectoryChooserActivity extends DirectoryChooserActivity {

        @Override
        protected DocumentFile configureDirectoryChooserIntent(Intent intent) {
            super.configureDirectoryChooserIntent(intent);
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);

            return PreferencesUtils.getDefaultExportDirectoryUri(sharedPreferences, this);
        }

        @Override
        protected Intent createNextActivityIntent(Uri directoryUri) {
            Intent intent = IntentUtils.newIntent(this, ExportActivity.class);
            intent.putExtra(ExportActivity.EXTRA_DIRECTORY_URI_KEY, directoryUri);
            intent.putExtra(ExportActivity.EXTRA_TRACKFILEFORMAT_KEY, PreferencesUtils.getExportTrackFileFormat(sharedPreferences, this));
            return intent;
        }
    }

    public static class DefaultTrackExportDirectoryChooserActivity extends DirectoryChooserActivity {

        @Override
        protected void onActivityResultCustom(int requestCode, int resultCode, @Nullable Intent resultData) {
            if (requestCode == DIRECTORY_PICKER_REQUEST_CODE) {
                switch (resultCode) {
                    case RESULT_OK:
                        Uri directoryUri = resultData.getData();

                        PreferencesUtils.setDefaultExportDirectoryUri(sharedPreferences, this, directoryUri);
                        IntentUtils.persistDirectoryAccessPermission(getApplicationContext(), directoryUri);
                        break;
                    case RESULT_CANCELED:
                        PreferencesUtils.setDefaultExportDirectoryUri(sharedPreferences, this, null);
                        //TODO Remove stored permission contentResolver.releasePersistableUriPermission
                        break;
                }

            }
            finish();
        }

        @Override
        protected DocumentFile configureDirectoryChooserIntent(Intent intent) {
            super.configureDirectoryChooserIntent(intent);
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            if (PreferencesUtils.isDefaultExportDirectoryUri(sharedPreferences, this)) {
                intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, PreferencesUtils.getDefaultExportDirectoryUri(sharedPreferences, this).getUri());
            }
            return null;
        }

        @Override
        protected Intent createNextActivityIntent(Uri directoryUri) {
            finish();
            return null;
        }
    }
}
