/*
 * Copyright 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package de.dennisguse.opentracks.ui.markers;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Lifecycle;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.data.ContentProviderUtils;
import de.dennisguse.opentracks.data.models.Marker;
import de.dennisguse.opentracks.databinding.MarkerDetailFragmentBinding;
import de.dennisguse.opentracks.share.ShareUtils;
import de.dennisguse.opentracks.util.IntentUtils;
import de.dennisguse.opentracks.util.StringUtils;

/**
 * A fragment to show marker details.
 *
 * @author Jimmy Shih
 */
public class MarkerDetailFragment extends Fragment {

    private static final String TAG = MarkerDetailFragment.class.getSimpleName();
    private static final String KEY_MARKER_ID = "markerId";

    private MenuItem shareMarkerImageMenuItem;

    private ContentProviderUtils contentProviderUtils;

    private Marker.Id markerId;
    private Marker marker;

    private MarkerDetailFragmentBinding viewBinding;

    public static MarkerDetailFragment newInstance(Marker.Id markerId) {
        Bundle bundle = new Bundle();
        bundle.putParcelable(KEY_MARKER_ID, markerId);

        MarkerDetailFragment fragment = new MarkerDetailFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        markerId = getArguments().getParcelable(KEY_MARKER_ID);
        if (markerId == null) {
            Log.d(TAG, "invalid marker id");
            getParentFragmentManager().popBackStack();
            return;
        }
        contentProviderUtils = new ContentProviderUtils(getContext());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        viewBinding = MarkerDetailFragmentBinding.inflate(inflater, container, false);

        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menuInflater.inflate(R.menu.marker_detail, menu);
                shareMarkerImageMenuItem = menu.findItem(R.id.marker_detail_share);
                updateMarker();
                updateMenuItems();
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem item) {
                FragmentActivity fragmentActivity = getActivity();

                if (item.getItemId() == R.id.marker_detail_show_on_map) {
                    IntentUtils.showCoordinateOnMap(getContext(), marker);
                    return true;
                }

                if (item.getItemId() == R.id.marker_detail_edit) {
                    Intent intent = IntentUtils.newIntent(fragmentActivity, MarkerEditActivity.class)
                            .putExtra(MarkerEditActivity.EXTRA_MARKER_ID, markerId);
                    startActivity(intent);
                    return true;
                }

                if (item.getItemId() == R.id.marker_detail_share) {
                    if (marker.hasPhoto()) {
                        Intent intent = ShareUtils.newShareFileIntent(getContext(), marker.getId());
                        intent = Intent.createChooser(intent, null);
                        startActivity(intent);
                    }
                    return true;
                }

                if (item.getItemId() == R.id.marker_detail_delete) {
                    DeleteMarkerDialogFragment.showDialog(getChildFragmentManager(), markerId);
                    return true;
                }

                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);

        return viewBinding.getRoot();
    }


    @Override
    public void onResume() {
        super.onResume();

        // Need to update the marker in case returning after an edit
        updateMarker();
        updateUi();
        updateMenuItems();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        viewBinding = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        markerId = null;
        marker = null;
    }

    @Override
    public void setMenuVisibility(boolean menuVisible) {
        super.setMenuVisibility(menuVisible);
        // View pager caches the neighboring fragments in the resumed state.
        // If becoming visible from the resumed state, update the UI to display the text above the image.
        if (isResumed() && menuVisible) {
            updateUi();
        }
    }

    private void updateMenuItems() {
        if (shareMarkerImageMenuItem != null) { // MenuProvider might not yet been initialized.
//            shareMarkerImageMenuItem.setEnabled(marker.hasPhoto());
        }
    }

    private void updateMarker() {
        marker = contentProviderUtils.getMarker(markerId);
        if (marker == null) {
            Log.d(TAG, "marker is null");
            getParentFragmentManager().popBackStack();
        }
    }

    private void updateUi() {
        if (marker.hasPhoto()) {
            viewBinding.markerDetailMarkerPhoto.setImageURI(marker.getPhotoUrl());
        } else {
            viewBinding.markerDetailMarkerPhoto.setImageDrawable(MarkerUtils.getDefaultPhoto(getContext()));
        }

        viewBinding.markerDetailMarkerCategory.setText(StringUtils.getCategory(marker.getCategory()));
        viewBinding.markerDetailMarkerDescription.setText(marker.getDescription());

        viewBinding.markerDetailMarkerLocation.setText(StringUtils.formatCoordinate(getContext(), marker.getPosition()));
    }
}
