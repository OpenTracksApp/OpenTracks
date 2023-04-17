package de.dennisguse.opentracks;

import android.os.Bundle;
import android.view.View;

import de.dennisguse.opentracks.databinding.HelpBinding;

public class QuitActivity extends AbstractActivity {

    private HelpBinding helpBinding;

    @Override
    protected View getRootView() {
        this.finishAffinity();
        helpBinding = HelpBinding.inflate(getLayoutInflater());
        return helpBinding.getRoot();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
}
