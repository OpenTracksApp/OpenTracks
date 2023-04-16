package de.dennisguse.opentracks.io.file.importer;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.PersistableBundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import de.dennisguse.opentracks.util.IntentUtils;

public class DriveImporter extends AppCompatActivity {

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = IntentUtils.newIntent(this, ImportActivity.class);
        intent.putExtra(ImportActivity.VALIDATE_CLOUD_OR_NOT, "cloud");
        startActivity(intent);
        finish();
    }


}
