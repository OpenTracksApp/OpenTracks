/*
 * Copyright 2012 Google Inc.
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
import com.google.android.apps.mytracks.content.WaypointCreationRequest;
import com.google.android.apps.mytracks.services.TrackRecordingServiceConnection;
import com.google.android.apps.mytracks.util.IntentUtils;
import com.google.android.apps.mytracks.util.PreferencesUtils;
import com.google.android.apps.mytracks.util.TrackRecordingServiceConnectionUtils;
import com.google.android.maps.mytracks.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;

/**
 * A DialogFragment to add a marker.
 *
 * @author Jimmy Shih
 */
public class MarkerAddDialogFragment extends DialogFragment {

  public static final String MARKER_ADD_DIALOG_TAG = "markerAddDialog";

  private static final String KEY_TRACK_ID = "trackId";
  private TrackRecordingServiceConnection trackRecordingServiceConnection;
  private RadioGroup radioGroup;
  private Button moreOptions;

  /**
   * Creates a new instance of {@link MarkerAddDialogFragment}.
   *
   * @param trackId the track id
   */
  public static MarkerAddDialogFragment newInstance(long trackId) {
    Bundle bundle = new Bundle();
    bundle.putLong(KEY_TRACK_ID, trackId);

    MarkerAddDialogFragment markerAddDialogFragment = new MarkerAddDialogFragment();
    markerAddDialogFragment.setArguments(bundle);
    return markerAddDialogFragment;
  }

  @Override
  public void onCreate(Bundle arg0) {
    super.onCreate(arg0);
    trackRecordingServiceConnection = new TrackRecordingServiceConnection(getActivity(), null);
  }

  @Override
  public void onResume() {
    super.onResume();
    TrackRecordingServiceConnectionUtils.resume(getActivity(), trackRecordingServiceConnection);
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    trackRecordingServiceConnection.unbind();
  }

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    View view = getActivity().getLayoutInflater().inflate(R.layout.marker_add, null);
    radioGroup = (RadioGroup) view.findViewById(R.id.marker_add_type);
    moreOptions = (Button) view.findViewById(R.id.marker_add_more_options);
    moreOptions.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        boolean statisticsMarker = radioGroup.getCheckedRadioButtonId()
            == R.id.marker_add_statistics;
        Intent intent = IntentUtils.newIntent(getActivity(), MarkerEditActivity.class);
        intent.putExtra(MarkerEditActivity.EXTRA_TRACK_ID, getArguments().getLong(KEY_TRACK_ID));
        intent.putExtra(MarkerEditActivity.EXTRA_STATISTICS_MARKER, statisticsMarker);
        startActivity(intent);
        dismiss();
      }
    });
    boolean pickStatisticsMarker = PreferencesUtils.getBoolean(
        getActivity(), R.string.pick_statistics_marker_key, true);
    radioGroup.check(pickStatisticsMarker ? R.id.marker_add_statistics : R.id.marker_add_waypoint);
    return new AlertDialog.Builder(getActivity())
        .setNegativeButton(R.string.generic_cancel, null)
        .setPositiveButton(R.string.generic_add, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            boolean statisticsMarker = radioGroup.getCheckedRadioButtonId()
                == R.id.marker_add_statistics;
            PreferencesUtils.setBoolean(
                getActivity(), R.string.pick_statistics_marker_key, statisticsMarker);
            TrackRecordingServiceConnectionUtils.addMarker(getActivity(),
                trackRecordingServiceConnection,
                statisticsMarker ? WaypointCreationRequest.DEFAULT_STATISTICS
                    : WaypointCreationRequest.DEFAULT_WAYPOINT);
          }
        })
        .setTitle(R.string.menu_insert_marker)
        .setView(view)
        .create();
  }
}