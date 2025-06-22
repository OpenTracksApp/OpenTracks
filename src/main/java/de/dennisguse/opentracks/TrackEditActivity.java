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

import de.dennisguse.opentracks.data.ContentProviderUtils;
import de.dennisguse.opentracks.data.models.ActivityType;
import de.dennisguse.opentracks.data.models.Track;
import de.dennisguse.opentracks.databinding.TrackEditBinding;
import de.dennisguse.opentracks.fragments.ChooseActivityTypeDialogFragment;
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

    private ContentProviderUtils contentProviderUtils;
    private Track track;
    private ActivityType activityType;

    private TrackEditBinding viewBinding;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        Track.Id trackId = getIntent().getParcelableExtra(EXTRA_TRACK_ID);
        if (trackId == null) {
            Log.e(TAG, "invalid trackId");
            finish();
            return;
        }

        contentProviderUtils = new ContentProviderUtils(this);
        track = contentProviderUtils.getTrack(trackId);
        if (track == null) {
            Log.e(TAG, "No track for " + trackId.id());
            finish();
            return;
        }

        viewBinding.trackEditName.setText(track.getName());

        viewBinding.trackEditActivityType.setText(track.getActivityTypeLocalized());

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, ActivityType.getLocalizedStrings(this));
        viewBinding.trackEditActivityType.setAdapter(adapter);
        viewBinding.trackEditActivityType.setOnItemClickListener((parent, view, position, id) -> {
            String localizedActivityType = (String) viewBinding.trackEditActivityType.getAdapter().getItem(position);
            setActivityTypeIcon(ActivityType.findByLocalizedString(this, localizedActivityType));
        });
        viewBinding.trackEditActivityType.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String localizedActivityType = viewBinding.trackEditActivityType.getText().toString();
                setActivityTypeIcon(ActivityType.findByLocalizedString(this, localizedActivityType));
            }
        });

        activityType = null;
        if (bundle != null) {
            activityType = (ActivityType) bundle.getSerializable(ICON_VALUE_KEY);
        }
        if (activityType == null) {
            activityType = track.getActivityType();
        }

        setActivityTypeIcon(activityType);
        viewBinding.trackEditActivityTypeIcon.setOnClickListener(v -> ChooseActivityTypeDialogFragment.showDialog(getSupportFragmentManager(), this, viewBinding.trackEditActivityType.getText().toString()));

        viewBinding.trackEditDescription.setText(track.getDescription());

        viewBinding.trackEditSave.setOnClickListener(v -> {
            TrackUtils.updateTrack(TrackEditActivity.this, track, viewBinding.trackEditName.getText().toString(),
                    viewBinding.trackEditActivityType.getText().toString(), viewBinding.trackEditDescription.getText().toString(),
                    contentProviderUtils);
            finish();
        });

        viewBinding.trackEditCancel.setOnClickListener(v -> finish());
        viewBinding.trackEditCancel.setVisibility(View.VISIBLE);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(ICON_VALUE_KEY, activityType);
    }

    @NonNull
    @Override
    protected View createRootView() {
        viewBinding = TrackEditBinding.inflate(getLayoutInflater());
        return viewBinding.getRoot();
    }

    private void setActivityTypeIcon(ActivityType activityType) {
        this.activityType = activityType;
        viewBinding.trackEditActivityTypeIcon.setImageResource(activityType.getIconDrawableId());
    }

    @Override
    public void onChooseActivityTypeDone(ActivityType activityType) {
        setActivityTypeIcon(activityType);
        viewBinding.trackEditActivityType.setText(getString(activityType.getLocalizedStringId()));
    }
}
