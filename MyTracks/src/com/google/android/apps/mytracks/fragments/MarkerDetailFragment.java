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

package com.google.android.apps.mytracks.fragments;

import com.google.android.apps.mytracks.MarkerEditActivity;
import com.google.android.apps.mytracks.TrackDetailActivity;
import com.google.android.apps.mytracks.content.MyTracksProviderUtils;
import com.google.android.apps.mytracks.content.Track;
import com.google.android.apps.mytracks.content.Waypoint;
import com.google.android.apps.mytracks.content.Waypoint.WaypointType;
import com.google.android.apps.mytracks.util.CalorieUtils;
import com.google.android.apps.mytracks.util.CalorieUtils.ActivityType;
import com.google.android.apps.mytracks.util.IntentUtils;
import com.google.android.apps.mytracks.util.ListItemUtils;
import com.google.android.apps.mytracks.util.PhotoUtils;
import com.google.android.apps.mytracks.util.StatsUtils;
import com.google.android.apps.mytracks.util.StringUtils;
import com.google.android.apps.mytracks.util.TrackIconUtils;
import com.google.android.maps.mytracks.R;

import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Display;
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

/**
 * A fragment to show marker details.
 * 
 * @author Jimmy Shih
 */
public class MarkerDetailFragment extends Fragment {

  public static MarkerDetailFragment newInstance(long markerId, String title) {
    Bundle bundle = new Bundle();
    bundle.putLong(KEY_MARKER_ID, markerId);
    bundle.putString(KEY_TITLE, title);

    MarkerDetailFragment fragment = new MarkerDetailFragment();
    fragment.setArguments(bundle);
    return fragment;
  }

  private static final String TAG = MarkerDetailFragment.class.getSimpleName();
  private static final String KEY_MARKER_ID = "markerId";
  private static final String KEY_TITLE = "title";
  private static final long HIDE_TEXT_DELAY = 4000L; // 4 seconds

  private MyTracksProviderUtils myTracksProviderUtils;
  private Handler handler;
  private ImageView photo;
  private ImageView textGradient;
  private LinearLayout waypointInfo;
  private Waypoint waypoint;

  private Runnable hideText = new Runnable() {
      @Override
    public void run() {
      Animation animation = AnimationUtils.loadAnimation(getActivity(), R.anim.fadeout);
      animation.setAnimationListener(new AnimationListener() {

          @Override
        public void onAnimationStart(Animation anim) {}

          @Override
        public void onAnimationRepeat(Animation anim) {}

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

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    long markerId = getArguments().getLong(KEY_MARKER_ID);
    if (markerId == -1L) {
      Log.d(TAG, "invalid marker id");
      getFragmentManager().popBackStack();
      return;
    }
    myTracksProviderUtils = MyTracksProviderUtils.Factory.get(getActivity());
    handler = new Handler();
    setHasOptionsMenu(true);
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.marker_detail_fragment, container, false);

    photo = (ImageView) view.findViewById(R.id.marker_detail_waypoint_photo);
    textGradient = (ImageView) view.findViewById(R.id.marker_detail_waypoint_text_gradient);
    waypointInfo = (LinearLayout) view.findViewById(R.id.marker_detail_waypoint_info);
    
    photo.setOnClickListener(new View.OnClickListener() {
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
  };

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
  public void setUserVisibleHint(boolean isVisibleToUser) {
    super.setUserVisibleHint(isVisibleToUser);
    if (isVisibleToUser) {
      getActivity().setTitle(getArguments().getString(KEY_TITLE));
    }
  }

  @Override
  public void setMenuVisibility(boolean menuVisible) {
    super.setMenuVisibility(menuVisible);
    /*
     * View pager caches the neighboring fragments in the resumed state. If
     * becoming visible from the resumed state, update the UI to display the
     * text above the image.
     */
    if (isResumed()) {
      if (menuVisible) {
        updateUi();
      } else {
        handler.removeCallbacks(hideText);
      }
    }
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    inflater.inflate(R.menu.marker_detail, menu);

    updateWaypoint(false);

    Track track = myTracksProviderUtils.getTrack(waypoint.getTrackId());
    boolean isSharedWithMe = track != null ? track.isSharedWithMe() : true;

    menu.findItem(R.id.marker_detail_edit).setVisible(!isSharedWithMe);
    menu.findItem(R.id.marker_detail_delete).setVisible(!isSharedWithMe);

    String photoUrl = waypoint.getPhotoUrl();
    boolean hasPhoto = photoUrl != null && !photoUrl.equals("");
    menu.findItem(R.id.marker_detail_view_photo).setVisible(hasPhoto);

    TrackIconUtils.setMenuIconColor(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    long markerId = getArguments().getLong(KEY_MARKER_ID);
    FragmentActivity fragmentActivity = getActivity();
    Intent intent;

    switch (item.getItemId()) {
      case R.id.marker_detail_show_on_map:
        intent = IntentUtils.newIntent(fragmentActivity, TrackDetailActivity.class)
            .putExtra(TrackDetailActivity.EXTRA_MARKER_ID, markerId);
        startActivity(intent);
        return true;
      case R.id.marker_detail_edit:
        intent = IntentUtils.newIntent(fragmentActivity, MarkerEditActivity.class)
            .putExtra(MarkerEditActivity.EXTRA_MARKER_ID, markerId);
        startActivity(intent);
        return true;
      case R.id.marker_detail_delete:
        DeleteMarkerDialogFragment.newInstance(new long[] { markerId })
            .show(getChildFragmentManager(), DeleteMarkerDialogFragment.DELETE_MARKER_DIALOG_TAG);
        return true;
      case R.id.marker_detail_view_photo:
        intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.parse(waypoint.getPhotoUrl()), "image/*");
        startActivity(intent);
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
      waypoint = myTracksProviderUtils.getWaypoint(getArguments().getLong(KEY_MARKER_ID));
      if (waypoint == null) {
        Log.d(TAG, "waypoint is null");
        getFragmentManager().popBackStack();
      }
    }
  }

  /**
   * Updates the UI.
   */
  @SuppressWarnings("deprecation")
  private void updateUi() {
    View waypointView = getView().findViewById(R.id.marker_detail_waypoint);
    View statisticsView = getView().findViewById(R.id.marker_detail_statistics);

    boolean isWaypoint = waypoint.getType() == WaypointType.WAYPOINT;
    waypointView.setVisibility(isWaypoint ? View.VISIBLE : View.GONE);
    statisticsView.setVisibility(isWaypoint ? View.GONE : View.VISIBLE);

    if (isWaypoint) {
      String photoUrl = waypoint.getPhotoUrl();
      boolean hasPhoto = photoUrl != null && !photoUrl.equals("");
      photo.setVisibility(hasPhoto ? View.VISIBLE : View.GONE);
      textGradient.setVisibility(hasPhoto ? View.VISIBLE : View.GONE);
      waypointInfo.setVisibility(View.VISIBLE);

      if (hasPhoto) {
        handler.removeCallbacks(hideText);
        Display defaultDisplay = getActivity().getWindowManager().getDefaultDisplay();
        PhotoUtils.setImageVew(photo, Uri.parse(photoUrl), defaultDisplay.getWidth(),
            defaultDisplay.getHeight(), true);
        handler.postDelayed(hideText, HIDE_TEXT_DELAY);
      }

      setName(R.id.marker_detail_waypoint_name, hasPhoto);

      TextView category = (TextView) getView().findViewById(R.id.marker_detail_waypoint_category);
      ListItemUtils.setTextView(
          getActivity(), category, StringUtils.getCategory(waypoint.getCategory()), hasPhoto);

      TextView description = (TextView) getView()
          .findViewById(R.id.marker_detail_waypoint_description);
      ListItemUtils.setTextView(getActivity(), description, waypoint.getDescription(), hasPhoto);

      setLocation(R.id.marker_detail_waypoint_location, hasPhoto);
    } else {
      setName(R.id.marker_detail_statistics_name, false);

      setLocation(R.id.marker_detail_statistics_location, false);

      Track track = myTracksProviderUtils.getTrack(waypoint.getTrackId());
      ActivityType activityType = track != null ? CalorieUtils.getActivityType(
          getActivity(), track.getCategory())
          : ActivityType.INVALID;
      StatsUtils.setTripStatisticsValues(
          getActivity(), null, getView(), waypoint.getTripStatistics(), activityType, null);
      StatsUtils.setLocationValues(getActivity(), null, getView(), waypoint.getLocation(), false);
    }
  }

  private void setName(int resId, boolean addShadow) {
    TextView textView = (TextView) getView().findViewById(resId);
    ListItemUtils.setTextView(getActivity(), textView, waypoint.getName(), addShadow);
  }

  private void setLocation(int resId, boolean addShadow) {
    TextView textView = (TextView) getView().findViewById(resId);
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
