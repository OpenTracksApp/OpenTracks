package de.dennisguse.opentracks;

import android.os.Bundle;
import android.view.Menu;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;

import de.dennisguse.opentracks.util.SystemUtils;
import de.dennisguse.opentracks.util.ViewUtils;

public class AboutActivity extends AbstractActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle(getString(R.string.about_preference_title));

        TextView textDescription = findViewById(R.id.about_text_description);
        textDescription.setText(getString(R.string.about_description));

        TextView textVersionName = findViewById(R.id.about_text_version_name);
        textVersionName.setText(getString(R.string.about_version_name, SystemUtils.getAppVersionName(this)));

        TextView textVersionCode = findViewById(R.id.about_text_version_code);
        textVersionCode.setText(getString(R.string.about_version_code, SystemUtils.getAppVersionCode(this)));

        TextView textURL = findViewById(R.id.about_app_url);
        textURL.setText(getString(R.string.about_url, getString(R.string.app_web_url)));

        ViewUtils.makeClickableLinks((ViewGroup) findViewById(android.R.id.content));
    }

    protected int getLayoutResId() {
        return R.layout.about;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return false;
    }
}
