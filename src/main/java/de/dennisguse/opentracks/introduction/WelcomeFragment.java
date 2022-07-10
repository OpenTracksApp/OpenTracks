package de.dennisguse.opentracks.introduction;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import de.dennisguse.opentracks.databinding.WelcomeBinding;

public class WelcomeFragment extends Fragment {

    public static WelcomeFragment newInstance() {
        return new WelcomeFragment();
    }

    private WelcomeBinding viewBinding;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        viewBinding = WelcomeBinding.inflate(inflater, container, false);
        return viewBinding.getRoot();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        viewBinding = null;
    }
}