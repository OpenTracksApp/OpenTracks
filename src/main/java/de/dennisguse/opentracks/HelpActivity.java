package de.dennisguse.opentracks;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import de.dennisguse.opentracks.databinding.HelpBinding;
import de.dennisguse.opentracks.ui.util.ViewUtils;

public class HelpActivity extends AbstractActivity<HelpBinding> {

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setSupportActionBar(viewBinding.bottomAppBarLayout.bottomAppBar);
        ViewUtils.makeClickableLinks(findViewById(android.R.id.content));
    }

    @NonNull
    @Override
    protected HelpBinding createRootView() {
        return HelpBinding.inflate(getLayoutInflater());
    }
}
