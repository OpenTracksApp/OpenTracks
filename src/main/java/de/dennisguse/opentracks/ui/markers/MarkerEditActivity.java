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

package de.dennisguse.opentracks.ui.markers;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;

import java.io.FileDescriptor;
import java.io.IOException;
import java.time.Instant;

import de.dennisguse.opentracks.AbstractActivity;
import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.data.models.Marker;
import de.dennisguse.opentracks.data.models.Position;
import de.dennisguse.opentracks.data.models.Track;
import de.dennisguse.opentracks.data.models.TrackPoint;
import de.dennisguse.opentracks.databinding.MarkerEditBinding;

/**
 * An activity to add/edit a marker.
 *
 * @author Jimmy Shih
 */
public class MarkerEditActivity extends AbstractActivity {

    public static final String EXTRA_TRACK_ID = "track_id";
    public static final String EXTRA_MARKER_ID = "marker_id";
    public static final String EXTRA_LOCATION = "location";

    private static final String CAMERA_PHOTO_URI_KEY = "camera_photo_uri_key";

    private static final String TAG = MarkerEditActivity.class.getSimpleName();
    private Track.Id trackId;
    private Location location;
    private Marker.Id markerId;
    private Marker marker;

    private MenuItem insertPhotoMenuItem;
    private MenuItem insertGalleryImgMenuItem;

    private boolean hasCamera;
    private Uri cameraPhotoUri;

    private ActivityResultLauncher<Intent> takePictureFromCamera;
    private ActivityResultLauncher<PickVisualMediaRequest> takePictureFromGallery;

    private MarkerEditViewModel viewModel;

    // UI elements
    private MarkerEditBinding viewBinding;

    @NonNull
    @Override
    protected View createRootView() {
        viewBinding = MarkerEditBinding.inflate(getLayoutInflater());
        return viewBinding.getRoot();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        trackId = getIntent().getParcelableExtra(EXTRA_TRACK_ID);
        location = getIntent().getParcelableExtra(EXTRA_LOCATION);
        markerId = getIntent().getParcelableExtra(EXTRA_MARKER_ID);
        if ((trackId == null || location == null) && markerId == null) {
            throw new IllegalStateException("TrackId and Location must be provided or an existing markerId");
        }

        if (savedInstanceState != null) {
            cameraPhotoUri = Uri.parse(savedInstanceState.getString(CAMERA_PHOTO_URI_KEY, ""));
        }
        boolean isNewMarker = markerId == null;

        hasCamera = getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY);

        // Setup UI elements
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.marker_types, android.R.layout.simple_dropdown_item_1line);
        viewBinding.markerEditMarkerType.setAdapter(adapter);
        viewBinding.markerEditPhotoDelete.setOnClickListener(v -> viewModel.onPhotoDelete(viewBinding.markerEditName.getText().toString(),
                viewBinding.markerEditMarkerType.getText().toString(),
                viewBinding.markerEditDescription.getText().toString()));

        viewBinding.markerEditCancel.setOnClickListener(v -> {
            viewModel.onCancel(isNewMarker);
            finish();
        });

        if (!isNewMarker) {
            viewBinding.markerEditToolbar.setTitle(R.string.menu_edit);
        }
        viewBinding.markerEditDone.setText(isNewMarker ? R.string.generic_add : R.string.generic_save);
        viewBinding.markerEditDone.setOnClickListener(v -> {
            viewModel.onDone(viewBinding.markerEditName.getText().toString(),
                    viewBinding.markerEditMarkerType.getText().toString(),
                    viewBinding.markerEditDescription.getText().toString());
            finish();
        });

        viewModel = new ViewModelProvider(this).get(MarkerEditViewModel.class);

        takePictureFromCamera = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    switch (result.getResultCode()) {
                        case RESULT_CANCELED ->
                                Toast.makeText(this, R.string.marker_add_photo_canceled, Toast.LENGTH_LONG).show();
                        case RESULT_OK -> viewModel.onNewCameraPhoto(cameraPhotoUri,
                                viewBinding.markerEditName.getText().toString(),
                                viewBinding.markerEditMarkerType.getText().toString(),
                                viewBinding.markerEditDescription.getText().toString());
                    }
                });

        takePictureFromGallery = registerForActivityResult(
                new ActivityResultContracts.PickVisualMedia(),
                uri -> {
                    if (uri == null) {
                        Toast.makeText(this, R.string.marker_add_photo_canceled, Toast.LENGTH_LONG).show();
                    } else {
                        viewModel.onNewGalleryPhoto(uri,
                                viewBinding.markerEditName.getText().toString(),
                                viewBinding.markerEditMarkerType.getText().toString(),
                                viewBinding.markerEditDescription.getText().toString());
                    }
                });

        if (isNewMarker) {
            createNewMarker().observe(this, this::loadMarkerData);
        } else {
            loadMarkerData(markerId);
        }

        setSupportActionBar(viewBinding.bottomAppBarLayout.bottomAppBar);
    }

    private LiveData<Marker> createNewMarker() {
        TrackPoint trackPoint = new TrackPoint(TrackPoint.Type.TRACKPOINT, Position.of(location, Instant.now()));
        return viewModel.createNewMarker(trackId, trackPoint);
    }

    private void loadMarkerData(Marker.Id markerId) {
        viewModel.getMarkerData(markerId).observe(this, this::loadMarkerData);
    }

    private void loadMarkerData(Marker data) {
        marker = data;
        viewBinding.markerEditName.setText(marker.getName());
        viewBinding.markerEditMarkerType.setText(marker.getCategory());
        viewBinding.markerEditDescription.setText(marker.getDescription());
        if (marker.hasPhoto()) {
            setMarkerImageView(marker.getPhotoUrl());
        } else {
            viewBinding.markerEditPhoto.setImageDrawable(null);
        }

        hideAndShowOptions();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        trackId = null;
        location = null;
        markerId = null;
        viewBinding = null;
        viewModel = null;
        takePictureFromGallery = null;
        takePictureFromCamera = null;
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (cameraPhotoUri != null) {
            outState.putString(CAMERA_PHOTO_URI_KEY, cameraPhotoUri.toString());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.marker_edit, menu);

        insertPhotoMenuItem = menu.findItem(R.id.marker_edit_insert_photo);
        insertPhotoMenuItem.setVisible(hasCamera);
        insertGalleryImgMenuItem = menu.findItem(R.id.marker_edit_insert_gallery_img);
        hideAndShowOptions();

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.marker_edit_insert_photo) {
            createMarkerWithPicture();
            return true;
        }

        if (item.getItemId() == R.id.marker_edit_insert_gallery_img) {
            createMarkerWithGalleryImage();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Checks and hide/shows all buttons/options about marker photo options.
     * <p>
     * If a photo is set then one's options are shown, otherwise another ones are shown.
     */
    private void hideAndShowOptions() {
        boolean isPhotoSet = (marker != null && marker.hasPhoto());
        if (insertPhotoMenuItem != null && insertGalleryImgMenuItem != null) {
            insertPhotoMenuItem.setVisible(!isPhotoSet);
            insertGalleryImgMenuItem.setVisible(!isPhotoSet);
        }
        viewBinding.markerEditPhotoDelete.setVisibility(isPhotoSet ? View.VISIBLE : View.GONE);
    }

    /**
     * Returns the trackId; either from track or marker.
     */
    private Track.Id getTrackId() {
        return trackId == null ? marker.getTrackId() : trackId;
    }

    private void setMarkerImageView(@NonNull Uri uri) {
        try (ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(uri, "r")) {
            FileDescriptor fd = pfd.getFileDescriptor();
            Bitmap bitmap = BitmapFactory.decodeFileDescriptor(fd);
            viewBinding.markerEditPhoto.setImageBitmap(bitmap);
            hideAndShowOptions();
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
            Toast.makeText(this, R.string.marker_add_photo_canceled, Toast.LENGTH_LONG).show();
        }
    }

    private void createMarkerWithPicture() {
        Pair<Intent, Uri> intentAndPhotoUri = MarkerUtils.createTakePictureIntent(this, getTrackId());
        cameraPhotoUri = intentAndPhotoUri.second;

        try {
            takePictureFromCamera.launch(intentAndPhotoUri.first);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.no_compatible_camera_installed, Toast.LENGTH_LONG).show();
        }
    }

    private void createMarkerWithGalleryImage() {
        PickVisualMediaRequest request = new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build();
        try {
            takePictureFromGallery.launch(request);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.no_compatible_gallery_installed, Toast.LENGTH_LONG).show();
        }
    }
}
