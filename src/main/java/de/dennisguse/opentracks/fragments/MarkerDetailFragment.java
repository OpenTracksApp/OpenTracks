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

package de.dennisguse.opentracks.fragments;

import android.content.Intent;
import android.location.Location;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import de.dennisguse.opentracks.MarkerEditActivity;
import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.content.data.Marker;
import de.dennisguse.opentracks.content.provider.ContentProviderUtils;
import de.dennisguse.opentracks.util.IntentUtils;
import de.dennisguse.opentracks.util.ListItemUtils;
import de.dennisguse.opentracks.util.MarkerUtils;
import de.dennisguse.opentracks.util.StringUtils;
import de.dennisguse.opentracks.util.UnitConversions;

/**
 * A fragment to show marker details.
 *
 * @author Jimmy Shih
 */
public class MarkerDetailFragment extends Fragment {

    private static final String TAG = MarkerDetailFragment.class.getSimpleName();
    private static final String KEY_MARKER_ID = "markerId";

    private static final long HIDE_TEXT_DELAY = 4 * UnitConversions.ONE_SECOND_MS;

    private MenuItem shareMarkerImageMenuItem;

    private ContentProviderUtils contentProviderUtils;
    private Handler handler;
    private ImageView photoView;
    private ImageView textGradient;
    private LinearLayout markerInfo;
    private Marker marker;
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
                    textGradient.setVisibility(View.GONE);
                    markerInfo.setVisibility(View.GONE);
                }
            });
            textGradient.startAnimation(animation);
            markerInfo.startAnimation(animation);
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

        Marker.Id markerId = getArguments().getParcelable(KEY_MARKER_ID);
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
        View view = inflater.inflate(R.layout.marker_detail_fragment, container, false);

        photoView = view.findViewById(R.id.marker_detail_marker_photo);
        textGradient = view.findViewById(R.id.marker_detail_marker_text_gradient);
        markerInfo = view.findViewById(R.id.marker_detail_marker_info);

        photoView.setOnClickListener(v -> {
            handler.removeCallbacks(hideText);
            int visibility = markerInfo.getVisibility() == View.GONE ? View.VISIBLE : View.GONE;
            textGradient.setVisibility(visibility);
            markerInfo.setVisibility(visibility);
            if (visibility == View.VISIBLE) {
                handler.postDelayed(hideText, HIDE_TEXT_DELAY);
            }
        });
        return view;
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
        Marker.Id markerId = getArguments().getParcelable(KEY_MARKER_ID); //TODO Should only happen in onCreate?
        FragmentActivity fragmentActivity = getActivity();
        Intent intent;
        switch (item.getItemId()) {
            case R.id.marker_detail_show_on_map:
                IntentUtils.showCoordinateOnMap(getContext(), marker);
                return true;
            case R.id.marker_detail_edit:
                intent = IntentUtils.newIntent(fragmentActivity, MarkerEditActivity.class)
                        .putExtra(MarkerEditActivity.EXTRA_MARKER_ID, markerId);
                startActivity(intent);
                return true;
            case R.id.marker_detail_share:
                if (marker.hasPhoto()) {
                    intent = IntentUtils.newShareImageIntent(getContext(), marker.getPhotoURI());
                    intent = Intent.createChooser(intent, null);
                    startActivity(intent);
                }
                return true;
            case R.id.marker_detail_delete:
                DeleteMarkerDialogFragment.showDialog(getChildFragmentManager(), markerId);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void updateMarker(boolean refresh) {
        if (refresh || marker == null) {
            marker = contentProviderUtils.getMarker(getArguments().getParcelable(KEY_MARKER_ID)); //TODO Should only happen in onCreate?
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
            photoView.setImageURI(marker.getPhotoURI());
            handler.postDelayed(hideText, HIDE_TEXT_DELAY);
        } else {
            photoView.setImageResource(MarkerUtils.ICON_ID);
        }

        setName(hasPhoto);

        TextView category = getView().findViewById(R.id.marker_detail_marker_category);
        ListItemUtils.setTextView(getActivity(), category, StringUtils.getCategory(marker.getCategory()), hasPhoto);

        TextView description = getView().findViewById(R.id.marker_detail_marker_description);
        ListItemUtils.setTextView(getActivity(), description, marker.getDescription(), hasPhoto);

        setLocation(hasPhoto);
    }

    private void setName(boolean addShadow) {
        TextView textView = getView().findViewById(R.id.marker_detail_marker_name);
        ListItemUtils.setTextView(getActivity(), textView, marker.getName(), addShadow);
    }

    private void setLocation(boolean addShadow) {
        TextView textView = getView().findViewById(R.id.marker_detail_marker_location);
        Location location = marker.getLocation();
        String value;
        if (location == null) {
            value = null;
        } else {
            value = "[" + getString(R.string.stats_latitude) + " "
                    + StringUtils.formatCoordinate(location.getLatitude()) + ", "
                    + getString(R.string.stats_longitude) + " "
                    + StringUtils.formatCoordinate(location.getLongitude()) + "]";
        }
        ListItemUtils.setTextView(getActivity(), textView, value, addShadow);
    }
}
