package de.dennisguse.opentracks.io.file.importer;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.util.PreferencesUtils;

public class SingleTrackExportDirectoryChooserActivity extends AppCompatActivity {

    private static final int DIRECTORY_PICKER_REQUEST_CODE = 6;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

        startActivityForResult(intent, DIRECTORY_PICKER_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);
        if (requestCode == DIRECTORY_PICKER_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                Uri directoryUri = resultData.getData();

                SharedPreferences sharedPreferences = PreferencesUtils.getSharedPreferences(getApplicationContext());
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString(getString(R.string.settings_single_export_directory_key), directoryUri.toString());
                editor.apply();

                persistDirectoryAccessPermission(resultData, directoryUri);

            }
            finish();
        }
    }

    private void persistDirectoryAccessPermission(Intent resultData, Uri directoryUri) {
        final int takeFlags = resultData.getFlags()
                & (Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        getContentResolver().takePersistableUriPermission(directoryUri, takeFlags);
    }

}
