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

package de.dennisguse.opentracks;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;

import java.io.FileDescriptor;
import java.io.IOException;

import de.dennisguse.opentracks.content.data.Marker;
import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.databinding.MarkerEditBinding;
import de.dennisguse.opentracks.util.IntentUtils;
import de.dennisguse.opentracks.viewmodels.MarkerEditViewModel;

/**
 * An activity to add/edit a marker.
 *
 * @author Jimmy Shih
 */
public class MarkerEditActivity extends AbstractActivity {

    public static final String EXTRA_TRACK_ID = "track_id";
    public static final String EXTRA_MARKER_ID = "marker_id";

    private static final int CAMERA_REQUEST_CODE = 5;
    private static final int GALLERY_IMG_REQUEST_CODE = 7;

    private static final String TAG = MarkerEditActivity.class.getSimpleName();
    private Track.Id trackId;
    private Marker marker;

    private MenuItem insertPhotoMenuItem;
    private MenuItem insertGalleryImgMenuItem;

    private boolean hasCamera;
    private Uri cameraPhotoUri;

    private MarkerEditViewModel viewModel;

    // UI elements
    private MarkerEditBinding viewBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        trackId = getIntent().getParcelableExtra(EXTRA_TRACK_ID);
        Marker.Id markerId = getIntent().getParcelableExtra(EXTRA_MARKER_ID);

        hasCamera = getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY);

        // Setup UI elements
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.marker_types, android.R.layout.simple_dropdown_item_1line);
        viewBinding.markerEditMarkerType.setAdapter(adapter);
        viewBinding.markerEditPhotoDelete.setOnClickListener(v -> viewModel.onPhotoDelete(viewBinding.markerEditName.getText().toString(),
                viewBinding.markerEditMarkerType.getText().toString(),
                viewBinding.markerEditDescription.getText().toString()));

        viewBinding.markerEditCancel.setOnClickListener(v -> {
            viewModel.onCancel();
            finish();
        });

        boolean isNewMarker = markerId == null;
        setTitle(isNewMarker ? R.string.menu_insert_marker : R.string.menu_edit);
        viewBinding.markerEditDone.setText(isNewMarker ? R.string.generic_add : R.string.generic_save);
        viewBinding.markerEditDone.setOnClickListener(v -> {
            viewModel.onDone(viewBinding.markerEditName.getText().toString(),
                        viewBinding.markerEditMarkerType.getText().toString(),
                        viewBinding.markerEditDescription.getText().toString());
            finish();
        });

        viewModel = new ViewModelProvider(this).get(MarkerEditViewModel.class);
        viewModel.getMarkerData(trackId, markerId).observe(this, data -> {
            marker = data;
            viewBinding.markerEditName.setText(marker.getName());
            viewBinding.markerEditMarkerType.setText(marker.getCategory());
            viewBinding.markerEditDescription.setText(marker.getDescription());
            if (marker.hasPhoto()) {
                setMarkerImageView(marker.getPhotoURI());
            } else {
                viewBinding.markerEditPhoto.setImageDrawable(null);
            }

            hideAndShowOptions();
        });
    }

    @Override
    protected View getRootView() {
        viewBinding = MarkerEditBinding.inflate(getLayoutInflater());
        return viewBinding.getRoot();
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CAMERA_REQUEST_CODE) {
            if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, R.string.marker_add_photo_canceled, Toast.LENGTH_LONG).show();
                return;
            } else if (resultCode == RESULT_OK) {
                viewModel.onNewCameraPhoto(cameraPhotoUri,
                        viewBinding.markerEditName.getText().toString(),
                        viewBinding.markerEditMarkerType.getText().toString(),
                        viewBinding.markerEditDescription.getText().toString());
            }
        } else if (requestCode == GALLERY_IMG_REQUEST_CODE) {
            if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, R.string.marker_add_photo_canceled, Toast.LENGTH_LONG).show();
                return;
            } else if (resultCode == RESULT_OK) {
                viewModel.onNewGalleryPhoto(data.getData(),
                        viewBinding.markerEditName.getText().toString(),
                        viewBinding.markerEditMarkerType.getText().toString(),
                        viewBinding.markerEditDescription.getText().toString());
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
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
        Pair<Intent, Uri> intentAndPhotoUri = IntentUtils.createTakePictureIntent(this, getTrackId());
        cameraPhotoUri = intentAndPhotoUri.second;
        startActivityForResult(intentAndPhotoUri.first, CAMERA_REQUEST_CODE);
    }

    private void createMarkerWithGalleryImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, GALLERY_IMG_REQUEST_CODE);
    }
}
