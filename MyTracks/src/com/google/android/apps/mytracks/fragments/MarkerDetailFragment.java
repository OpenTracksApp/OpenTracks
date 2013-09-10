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
import com.google.android.apps.mytracks.util.IntentUtils;
import com.google.android.apps.mytracks.util.PhotoUtils;
import com.google.android.apps.mytracks.util.PreferencesUtils;
import com.google.android.apps.mytracks.util.StatsUtils;
import com.google.android.maps.mytracks.R;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
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
import android.widget.ImageView;
import android.widget.TextView;

/**
 * A fragment to show marker details.
 * 
 * @author Jimmy Shih
 */
public class MarkerDetailFragment extends Fragment {

  private static final String TAG = MarkerDetailFragment.class.getSimpleName();
  private static final String KEY_MARKER_ID = "markerId";

  public static MarkerDetailFragment newInstance(long markerId) {
    MarkerDetailFragment fragment = new MarkerDetailFragment();

    Bundle bundle = new Bundle();
    bundle.putLong(KEY_MARKER_ID, markerId);
    fragment.setArguments(bundle);

    return fragment;
  }

  private MyTracksProviderUtils myTracksProviderUtils;
  private Waypoint waypoint;

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
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    setHasOptionsMenu(true);
    return inflater.inflate(R.layout.marker_detail_fragment, container, false);
  };

  @Override
  public void onResume() {
    super.onResume();

    // Need to update the waypoint in case returning after an edit
    updateWaypoint(true);
    update();
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    inflater.inflate(R.menu.marker_detail, menu);

    updateWaypoint(false);
    
    Track track = myTracksProviderUtils.getTrack(waypoint.getTrackId());
    boolean isSharedWithMe = track != null ? track.isSharedWithMe() : true;

    menu.findItem(R.id.marker_detail_edit).setVisible(!isSharedWithMe);
    menu.findItem(R.id.marker_detail_delete).setVisible(!isSharedWithMe);
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
  private void update() {
    View waypointSection = getView().findViewById(R.id.marker_detail_waypoint_section);
    View statisticsSection = getView().findViewById(R.id.marker_detail_statistics_section);

    if (waypoint.getType() == WaypointType.WAYPOINT) {
      waypointSection.setVisibility(View.VISIBLE);
      statisticsSection.setVisibility(View.GONE);

      ImageView imageView = (ImageView) getView().findViewById(R.id.marker_detail_waypoint_photo);
      String photoUrl = waypoint.getPhotoUrl();
      if (photoUrl == null || photoUrl.equals("")) {
        imageView.setVisibility(View.GONE);
      } else {
        imageView.setVisibility(View.VISIBLE);
        Display defaultDisplay = getActivity().getWindowManager().getDefaultDisplay();
        @SuppressWarnings("deprecation")
        int displayWidth = defaultDisplay.getWidth();
        @SuppressWarnings("deprecation")
        int displayHeight = defaultDisplay.getHeight();
        PhotoUtils.setImageVew(imageView, Uri.parse(photoUrl), displayWidth, displayHeight);
      }

      TextView name = (TextView) getView().findViewById(R.id.marker_detail_waypoint_name);
      name.setText(getString(R.string.generic_name_line, waypoint.getName()));

      TextView markerType = (TextView) getView()
          .findViewById(R.id.marker_detail_waypoint_marker_type);
      markerType.setText(
          getString(R.string.marker_detail_waypoint_marker_type, waypoint.getCategory()));

      TextView description = (TextView) getView()
          .findViewById(R.id.marker_detail_waypoint_description);
      description.setText(getString(R.string.generic_description_line, waypoint.getDescription()));
    } else {
      waypointSection.setVisibility(View.GONE);
      statisticsSection.setVisibility(View.VISIBLE);

      TextView name = (TextView) getView().findViewById(R.id.marker_detail_statistics_name);
      name.setText(getString(R.string.generic_name_line, waypoint.getName()));

      StatsUtils.setTripStatisticsValues(getActivity(), waypoint.getTripStatistics(),
          PreferencesUtils.RECORDING_TRACK_ID_DEFAULT);
      StatsUtils.setLocationValues(getActivity(), waypoint.getLocation(), false);
    }
  }
}
