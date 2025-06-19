package de.dennisguse.opentracks;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import de.dennisguse.opentracks.databinding.AboutBinding;
import de.dennisguse.opentracks.ui.util.ViewUtils;
import de.dennisguse.opentracks.util.SystemUtils;

public class AboutActivity extends AbstractActivity {

    private AboutBinding viewBinding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle(getString(R.string.about_preference_title));

        viewBinding.aboutTextDescription.setText(getString(R.string.about_description));
        viewBinding.aboutTextVersionName.setText(getString(R.string.about_version_name, SystemUtils.getAppVersionName(this)));
        viewBinding.aboutTextVersionCode.setText(getString(R.string.about_version_code, SystemUtils.getAppVersionCode(this)));
        viewBinding.aboutAppUrl.setText(getString(R.string.about_url, getString(R.string.app_web_url)));

        setSupportActionBar(viewBinding.bottomAppBarLayout.bottomAppBar);

        ViewUtils.makeClickableLinks(findViewById(android.R.id.content));
    }

    @NonNull
    protected View createRootView() {
        viewBinding = AboutBinding.inflate(getLayoutInflater());
        return viewBinding.getRoot();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        viewBinding = null;
    }
}
