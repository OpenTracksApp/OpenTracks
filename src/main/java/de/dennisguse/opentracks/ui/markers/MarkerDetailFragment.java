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
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import java.time.Duration;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.data.ContentProviderUtils;
import de.dennisguse.opentracks.data.models.Marker;
import de.dennisguse.opentracks.databinding.MarkerDetailFragmentBinding;
import de.dennisguse.opentracks.share.ShareUtils;
import de.dennisguse.opentracks.ui.util.ListItemUtils;
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

    private static final Duration HIDE_TEXT_DELAY = Duration.ofSeconds(4);

    private MenuItem shareMarkerImageMenuItem;

    private ContentProviderUtils contentProviderUtils;
    private Handler handler;

    private Marker.Id markerId;
    private Marker marker;

    private MarkerDetailFragmentBinding viewBinding;

    private final Runnable hideText = new Runnable() {
        @Override
        public void run() {
            Animation animation = AnimationUtils.loadAnimation(getActivity(), R.anim.fadeout);
            animation.setAnimationListener(new AnimationListener() {

                @Override
                public void onAnimationStart(Animation anim) {
                }

                @Override
                public void onAnimationRepeat(Animation anim) {
                }

                @Override
                public void onAnimationEnd(Animation anim) {
                    viewBinding.markerDetailMarkerTextGradient.setVisibility(View.GONE);
                    viewBinding.markerDetailMarkerInfo.setVisibility(View.GONE);
                }
            });
            viewBinding.markerDetailMarkerTextGradient.startAnimation(animation);
            viewBinding.markerDetailMarkerInfo.startAnimation(animation);
        }
    };

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
        contentProviderUtils = new ContentProviderUtils(getActivity());
        handler = new Handler();
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        viewBinding = MarkerDetailFragmentBinding.inflate(inflater, container, false);

        viewBinding.markerDetailMarkerPhoto.setOnClickListener(v -> {
            handler.removeCallbacks(hideText);
            int visibility = viewBinding.markerDetailMarkerInfo.getVisibility() == View.GONE ? View.VISIBLE : View.GONE;
            viewBinding.markerDetailMarkerTextGradient.setVisibility(visibility);
            viewBinding.markerDetailMarkerInfo.setVisibility(visibility);
            if (visibility == View.VISIBLE) {
                handler.postDelayed(hideText, HIDE_TEXT_DELAY.toMillis());
            }
        });
        return viewBinding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();

        // Need to update the marker in case returning after an edit
        updateMarker(true);
        updateUi();
        updateMenuItems();
    }

    @Override
    public void onPause() {
        super.onPause();
        handler.removeCallbacks(hideText);
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
        if (isResumed()) {
            if (menuVisible) {
                updateUi();
            } else {
                handler.removeCallbacks(hideText);
            }
        }
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.marker_detail, menu);
        shareMarkerImageMenuItem = menu.findItem(R.id.marker_detail_share);
        updateMarker(false);
        updateMenuItems();
    }

    private void updateMenuItems() {
        if (shareMarkerImageMenuItem != null)
            shareMarkerImageMenuItem.setVisible(marker.hasPhoto());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
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

        return super.onOptionsItemSelected(item);
    }

    private void updateMarker(boolean refresh) {
        if (refresh || marker == null) {
            marker = contentProviderUtils.getMarker(markerId);
            if (marker == null) {
                Log.d(TAG, "marker is null");
                getParentFragmentManager().popBackStack();
            }
        }
    }

    private void updateUi() {
        boolean hasPhoto = marker.hasPhoto();
        if (hasPhoto) {
            handler.removeCallbacks(hideText);
            viewBinding.markerDetailMarkerPhoto.setImageURI(marker.getPhotoURI());
            handler.postDelayed(hideText, HIDE_TEXT_DELAY.toMillis());
        } else {
            viewBinding.markerDetailMarkerPhoto.setImageResource(MarkerUtils.ICON_ID);
        }

        ListItemUtils.setTextView(getActivity(), viewBinding.markerDetailMarkerName, marker.getName(), hasPhoto);


        ListItemUtils.setTextView(getActivity(), viewBinding.markerDetailMarkerCategory, StringUtils.getCategory(marker.getCategory()), hasPhoto);

        ListItemUtils.setTextView(getActivity(), viewBinding.markerDetailMarkerDescription, marker.getDescription(), hasPhoto);

        setLocation(hasPhoto);
    }

    private void setLocation(boolean addShadow) {
        String value = "[" + getString(R.string.stats_latitude) + " "
                + StringUtils.formatCoordinate(getContext(), marker.getLatitude()) + ", "
                + getString(R.string.stats_longitude) + " "
                + StringUtils.formatCoordinate(getContext(), marker.getLongitude()) + "]";

        ListItemUtils.setTextView(getActivity(), viewBinding.markerDetailMarkerLocation, value, addShadow);
    }
}
