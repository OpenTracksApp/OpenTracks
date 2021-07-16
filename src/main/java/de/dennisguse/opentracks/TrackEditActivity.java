/*
 * Copyright 2008 Google Inc.
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

package de.dennisguse.opentracks;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;

import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.content.provider.ContentProviderUtils;
import de.dennisguse.opentracks.databinding.TrackEditBinding;
import de.dennisguse.opentracks.fragments.ChooseActivityTypeDialogFragment;
import de.dennisguse.opentracks.services.TrackRecordingServiceConnection;
import de.dennisguse.opentracks.util.TrackIconUtils;
import de.dennisguse.opentracks.util.TrackUtils;

/**
 * An activity that let's the user see and edit the user editable track meta data such as track name, activity type, and track description.
 *
 * @author Leif Hendrik Wilden
 */
public class TrackEditActivity extends AbstractActivity implements ChooseActivityTypeDialogFragment.ChooseActivityTypeCaller {

    public static final String EXTRA_TRACK_ID = "track_id";

    private static final String TAG = TrackEditActivity.class.getSimpleName();

    private static final String ICON_VALUE_KEY = "icon_value_key";

    private TrackRecordingServiceConnection trackRecordingServiceConnection;
    private ContentProviderUtils contentProviderUtils;
    private Track track;
    private String iconValue;

    private TrackEditBinding viewBinding;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        trackRecordingServiceConnection = new TrackRecordingServiceConnection();
        Track.Id trackId = getIntent().getParcelableExtra(EXTRA_TRACK_ID);
        if (trackId == null) {
            Log.e(TAG, "invalid trackId");
            finish();
            return;
        }

        contentProviderUtils = new ContentProviderUtils(this);
        track = contentProviderUtils.getTrack(trackId);
        if (track == null) {
            Log.e(TAG, "No track for " + trackId.getId());
            finish();
            return;
        }

        viewBinding.trackEditName.setText(track.getName());

        viewBinding.trackEditActivityType.setText(track.getCategory());

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.activity_types, android.R.layout.simple_dropdown_item_1line);
        viewBinding.trackEditActivityType.setAdapter(adapter);
        viewBinding.trackEditActivityType.setOnItemClickListener((parent, view, position, id) -> setActivityTypeIcon(TrackIconUtils.getIconValue(this, (String) viewBinding.trackEditActivityType.getAdapter().getItem(position))));
        viewBinding.trackEditActivityType.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                setActivityTypeIcon(TrackIconUtils.getIconValue(
                        TrackEditActivity.this, viewBinding.trackEditActivityType.getText().toString()));
            }
        });

        iconValue = null;
        if (bundle != null) {
            iconValue = bundle.getString(ICON_VALUE_KEY);
        }
        if (iconValue == null) {
            iconValue = track.getIcon();
        }

        setActivityTypeIcon(iconValue);
        viewBinding.trackEditActivityTypeIcon.setOnClickListener(v -> ChooseActivityTypeDialogFragment.showDialog(getSupportFragmentManager(), viewBinding.trackEditActivityType.getText().toString()));

        viewBinding.trackEditDescription.setText(track.getDescription());

        viewBinding.trackEditSave.setOnClickListener(v -> {
            TrackUtils.updateTrack(TrackEditActivity.this, track, viewBinding.trackEditName.getText().toString(),
                    viewBinding.trackEditActivityType.getText().toString(), viewBinding.trackEditDescription.getText().toString(),
                    contentProviderUtils);
            finish();
        });

        setTitle(R.string.menu_edit);
        viewBinding.trackEditCancel.setOnClickListener(v -> finish());
        viewBinding.trackEditCancel.setVisibility(View.VISIBLE);
    }


    @Override
    protected void onStart() {
        super.onStart();
        trackRecordingServiceConnection.startConnection(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        trackRecordingServiceConnection.unbind(this);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(ICON_VALUE_KEY, iconValue);
    }

    @Override
    protected View getRootView() {
        viewBinding = TrackEditBinding.inflate(getLayoutInflater());
        return viewBinding.getRoot();
    }

    private void setActivityTypeIcon(String iconValue) {
        this.iconValue = iconValue;
        viewBinding.trackEditActivityTypeIcon.setImageResource(TrackIconUtils.getIconDrawable(iconValue));
    }

    @Override
    public void onChooseActivityTypeDone(String value) {
        setActivityTypeIcon(value);
        viewBinding.trackEditActivityType.setText(getString(TrackIconUtils.getIconActivityType(value)));
    }
}
