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
import de.dennisguse.opentracks.content.ContentProviderUtils;
import de.dennisguse.opentracks.content.Waypoint;
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
    private static final long HIDE_TEXT_DELAY = 4 * UnitConversions.ONE_SECOND;
    private ContentProviderUtils contentProviderUtils;
    private Handler handler;
    private ImageView photoView;
    private ImageView textGradient;
    private LinearLayout waypointInfo;
    private Waypoint waypoint;
    private Runnable hideText = new Runnable() {
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
                    waypointInfo.setVisibility(View.GONE);
                }
            });
            textGradient.startAnimation(animation);
            waypointInfo.startAnimation(animation);
        }
    };

    public static MarkerDetailFragment newInstance(long markerId) {
        Bundle bundle = new Bundle();
        bundle.putLong(KEY_MARKER_ID, markerId);

        MarkerDetailFragment fragment = new MarkerDetailFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        long markerId = getArguments().getLong(KEY_MARKER_ID);
        if (markerId == -1L) {
            Log.d(TAG, "invalid marker id");
            getFragmentManager().popBackStack();
            return;
        }
        contentProviderUtils = new ContentProviderUtils(getActivity());
        handler = new Handler();
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.marker_detail_fragment, container, false);

        photoView = view.findViewById(R.id.marker_detail_waypoint_photo);
        textGradient = view.findViewById(R.id.marker_detail_waypoint_text_gradient);
        waypointInfo = view.findViewById(R.id.marker_detail_waypoint_info);

        photoView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handler.removeCallbacks(hideText);
                int visibility = waypointInfo.getVisibility() == View.GONE ? View.VISIBLE : View.GONE;
                textGradient.setVisibility(visibility);
                waypointInfo.setVisibility(visibility);
                if (visibility == View.VISIBLE) {
                    handler.postDelayed(hideText, HIDE_TEXT_DELAY);
                }
            }
        });
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        // Need to update the waypoint in case returning after an edit
        updateWaypoint(true);
        updateUi();
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

        updateWaypoint(false);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        long markerId = getArguments().getLong(KEY_MARKER_ID);
        FragmentActivity fragmentActivity = getActivity();
        Intent intent;
        switch (item.getItemId()) {
            case R.id.marker_detail_show_on_map:
                IntentUtils.showCoordinateOnMap(getContext(), waypoint);
                return true;
            case R.id.marker_detail_edit:
                intent = IntentUtils.newIntent(fragmentActivity, MarkerEditActivity.class)
                        .putExtra(MarkerEditActivity.EXTRA_MARKER_ID, markerId);
                startActivity(intent);
                return true;
            case R.id.marker_detail_delete:
                DeleteMarkerDialogFragment.showDialog(getChildFragmentManager(), new long[]{markerId});
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Updates the waypoint.
     *
     * @param refresh true to always update
     */
    private void updateWaypoint(boolean refresh) {
        if (refresh || waypoint == null) {
            waypoint = contentProviderUtils.getWaypoint(getArguments().getLong(KEY_MARKER_ID));
            if (waypoint == null) {
                Log.d(TAG, "waypoint is null");
                getFragmentManager().popBackStack();
            }
        }
    }

    private void updateUi() {
        boolean hasPhoto = waypoint.hasPhoto();
        if (hasPhoto) {
            handler.removeCallbacks(hideText);
            photoView.setImageURI(waypoint.getPhotoURI());
            handler.postDelayed(hideText, HIDE_TEXT_DELAY);
        } else {
            photoView.setImageResource(MarkerUtils.ICON_ID);
        }

        setName(R.id.marker_detail_waypoint_name, hasPhoto);

        TextView category = getView().findViewById(R.id.marker_detail_waypoint_category);
        ListItemUtils.setTextView(getActivity(), category, StringUtils.getCategory(waypoint.getCategory()), hasPhoto);

        TextView description = getView().findViewById(R.id.marker_detail_waypoint_description);
        ListItemUtils.setTextView(getActivity(), description, waypoint.getDescription(), hasPhoto);

        setLocation(R.id.marker_detail_waypoint_location, hasPhoto);
    }

    private void setName(int resId, boolean addShadow) {
        TextView textView = getView().findViewById(resId);
        ListItemUtils.setTextView(getActivity(), textView, waypoint.getName(), addShadow);
    }

    private void setLocation(int resId, boolean addShadow) {
        TextView textView = getView().findViewById(resId);
        Location location = waypoint.getLocation();
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
