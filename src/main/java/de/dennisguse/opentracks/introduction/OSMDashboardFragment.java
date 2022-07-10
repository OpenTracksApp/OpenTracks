package de.dennisguse.opentracks.introduction;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import de.dennisguse.opentracks.databinding.OsmDashboardBinding;

public class OSMDashboardFragment extends Fragment {

    public static OSMDashboardFragment newInstance() {
        return new OSMDashboardFragment();
    }

    private OsmDashboardBinding viewBinding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        viewBinding = OsmDashboardBinding.inflate(inflater, container, false);
        return viewBinding.getRoot();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        viewBinding = null;
    }
}