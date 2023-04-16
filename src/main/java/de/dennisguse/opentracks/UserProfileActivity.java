package de.dennisguse.opentracks;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import de.dennisguse.opentracks.databinding.UserProfileSettingBinding;

public class UserProfileActivity extends AppCompatActivity {

    private UserProfileSettingBinding viewBinding;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_profile_setting);
    }
}
