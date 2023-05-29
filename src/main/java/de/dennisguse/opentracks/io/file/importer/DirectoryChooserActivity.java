package de.dennisguse.opentracks.io.file.importer;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.io.file.exporter.ExportActivity;
import de.dennisguse.opentracks.settings.PreferencesUtils;
import de.dennisguse.opentracks.util.IntentUtils;

public abstract class DirectoryChooserActivity extends AppCompatActivity {

    protected final ActivityResultLauncher<Intent> directoryIntentLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    onActivityResultCustom(result.getData());
                }
                finish();
            });

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        DocumentFile directoryUri = configureDirectoryChooserIntent(intent);
        if (!isDirectoryValid(directoryUri)) {
            try {
                directoryIntentLauncher.launch(intent);
            } catch (final ActivityNotFoundException exception) {
                Toast.makeText(this, R.string.no_compatible_file_manager_installed, Toast.LENGTH_LONG).show();
            }
        } else {
            startActivity(createNextActivityIntent(directoryUri.getUri()));
            finish();
        }
    }

    protected boolean isDirectoryValid(final DocumentFile directoryUri) {
        return directoryUri != null && directoryUri.isDirectory() && directoryUri.canRead();
    }

    protected void onActivityResultCustom(@NonNull Intent resultData) {
        Uri directoryUri = resultData.getData();
        IntentUtils.persistDirectoryAccessPermission(this, directoryUri, resultData.getFlags());
        startActivity(createNextActivityIntent(directoryUri));
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
            return IntentUtils.toDocumentFile(this, PreferencesUtils.getDefaultExportDirectoryUri());
        }

        @Override
        protected boolean isDirectoryValid(final DocumentFile directoryUri) {
            return super.isDirectoryValid(directoryUri) && directoryUri.canWrite();
        }

        @Override
        protected Intent createNextActivityIntent(Uri directoryUri) {
            Intent intent = IntentUtils.newIntent(this, ExportActivity.class);
            intent.putExtra(ExportActivity.EXTRA_DIRECTORY_URI_KEY, directoryUri);
            intent.putExtra(ExportActivity.EXTRA_TRACKFILEFORMAT_KEY, PreferencesUtils.getExportTrackFileFormat());
            return intent;
        }
    }

    public static class ExportDirectoryChooserOneFileActivity extends DirectoryChooserActivity {

        @Override
        protected DocumentFile configureDirectoryChooserIntent(Intent intent) {
            super.configureDirectoryChooserIntent(intent);
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            return IntentUtils.toDocumentFile(this, PreferencesUtils.getDefaultExportDirectoryUri());
        }

        @Override
        protected boolean isDirectoryValid(final DocumentFile directoryUri) {
            return super.isDirectoryValid(directoryUri) && directoryUri.canWrite();
        }

        @Override
        protected Intent createNextActivityIntent(Uri directoryUri) {
            Intent intent = IntentUtils.newIntent(this, ExportActivity.class);
            intent.putExtra(ExportActivity.EXTRA_DIRECTORY_URI_KEY, directoryUri);
            intent.putExtra(ExportActivity.EXTRA_ONE_FILE_KEY, true);
            intent.putExtra(ExportActivity.EXTRA_TRACKFILEFORMAT_KEY, PreferencesUtils.getExportTrackFileFormat());
            return intent;
        }
    }

    public static class DefaultTrackExportDirectoryChooserActivity extends DirectoryChooserActivity {

        @Override
        protected void onActivityResultCustom(@NonNull Intent resultData) {
            Uri oldDirectoryUri = PreferencesUtils.getDefaultExportDirectoryUri();
            Uri newDirectoryUri = resultData.getData();
            if (oldDirectoryUri != null && !newDirectoryUri.equals(oldDirectoryUri)) {
                IntentUtils.releaseDirectoryAccessPermission(this, oldDirectoryUri);
            }

            PreferencesUtils.setDefaultExportDirectoryUri(newDirectoryUri);
            IntentUtils.persistDirectoryAccessPermission(this, newDirectoryUri, resultData.getFlags());
        }

        @Override
        protected boolean isDirectoryValid(final DocumentFile directoryUri) {
            return super.isDirectoryValid(directoryUri) && directoryUri.canWrite();
        }

        @Override
        protected DocumentFile configureDirectoryChooserIntent(Intent intent) {
            super.configureDirectoryChooserIntent(intent);
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            if (PreferencesUtils.isDefaultExportDirectoryUri()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, PreferencesUtils.getDefaultExportDirectoryUri());
                }
            }
            return null;
        }

        @Override
        protected Intent createNextActivityIntent(Uri directoryUri) {
            return null;
        }
    }
}
