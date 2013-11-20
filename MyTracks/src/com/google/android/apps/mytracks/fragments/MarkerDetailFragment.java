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
import com.google.android.apps.mytracks.util.PhotoUtils;
import com.google.android.apps.mytracks.util.StatsUtils;
import com.google.android.apps.mytracks.util.StringUtils;
import com.google.android.maps.mytracks.R;

import android.content.Intent;
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

  public static MarkerDetailFragment newInstance(long markerId) {
    Bundle bundle = new Bundle();
    bundle.putLong(KEY_MARKER_ID, markerId);

    MarkerDetailFragment fragment = new MarkerDetailFragment();
    fragment.setArguments(bundle);
    return fragment;
  }

  private static final String TAG = MarkerDetailFragment.class.getSimpleName();
  private static final String KEY_MARKER_ID = "markerId";
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
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    setHasOptionsMenu(true);
    View view = inflater.inflate(R.layout.marker_detail_fragment, container, false);

    photo = (ImageView) view.findViewById(R.id.marker_detail_waypoint_photo);
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
    textGradient = (ImageView) view.findViewById(R.id.marker_detail_waypoint_text_gradient);
    waypointInfo = (LinearLayout) view.findViewById(R.id.marker_detail_waypoint_info);

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

    if (waypoint.getType() == WaypointType.WAYPOINT) {
      waypointView.setVisibility(View.VISIBLE);
      statisticsView.setVisibility(View.GONE);

      String photoUrl = waypoint.getPhotoUrl();
      if (photoUrl == null || photoUrl.equals("")) {
        photo.setVisibility(View.GONE);
        textGradient.setVisibility(View.GONE);
        waypointInfo.setVisibility(View.VISIBLE);
      } else {
        handler.removeCallbacks(hideText);
        photo.setVisibility(View.VISIBLE);
        textGradient.setVisibility(View.VISIBLE);
        waypointInfo.setVisibility(View.VISIBLE);

        Display defaultDisplay = getActivity().getWindowManager().getDefaultDisplay();
        PhotoUtils.setImageVew(photo, Uri.parse(photoUrl), defaultDisplay.getWidth(),
            defaultDisplay.getHeight(), true);
        handler.postDelayed(hideText, HIDE_TEXT_DELAY);
      }

      TextView name = (TextView) getView().findViewById(R.id.marker_detail_waypoint_name);
      setTextView(name, waypoint.getName());

      TextView category = (TextView) getView().findViewById(R.id.marker_detail_waypoint_category);
      setTextView(category, StringUtils.getCategory(waypoint.getCategory()));

      TextView description = (TextView) getView()
          .findViewById(R.id.marker_detail_waypoint_description);
      setTextView(description, waypoint.getDescription());
    } else {
      waypointView.setVisibility(View.GONE);
      statisticsView.setVisibility(View.VISIBLE);

      TextView name = (TextView) getView().findViewById(R.id.marker_detail_statistics_name);
      setTextView(name, waypoint.getName());

      Track track = myTracksProviderUtils.getTrack(waypoint.getTrackId());
      ActivityType activityType = track != null ? CalorieUtils.getActivityType(
          getActivity(), track.getCategory())
          : ActivityType.INVALID;
      StatsUtils.setTripStatisticsValues(
          getActivity(), null, getView(), waypoint.getTripStatistics(), activityType, null);
      StatsUtils.setLocationValues(getActivity(), null, getView(), waypoint.getLocation(), false);
    }
  }

  /**
   * Sets a text view.
   * 
   * @param textView the text view
   * @param value the value for the text view
   */
  private void setTextView(TextView textView, String value) {
    if (value == null || value.length() == 0) {
      textView.setVisibility(View.GONE);
    } else {
      textView.setVisibility(View.VISIBLE);
      textView.setText(value);
    }
  }
}
