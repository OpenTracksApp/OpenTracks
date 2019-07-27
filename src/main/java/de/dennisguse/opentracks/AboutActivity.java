package de.dennisguse.opentracks;

import android.os.Bundle;
import android.view.Menu;
import android.widget.TextView;

import androidx.annotation.Nullable;

import de.dennisguse.opentracks.util.SystemUtils;

public class AboutActivity extends AbstractActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle(getString(R.string.about_preference_title));

        TextView textDescription = findViewById(R.id.about_text_description);
        textDescription.setText(getString(R.string.about_description));

        TextView textVersion = findViewById(R.id.about_text_version);
        textVersion.setText(getString(R.string.about_version, SystemUtils.getAppVersion(this)));

        TextView textURL = findViewById(R.id.about_app_url);
        textURL.setText(getString(R.string.about_url, getString(R.string.app_web_url)));
    }

    protected int getLayoutResId() {
        return R.layout.about;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return false;
    }
}
