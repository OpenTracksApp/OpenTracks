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
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;

import de.dennisguse.opentracks.content.data.Marker;
import de.dennisguse.opentracks.content.data.Track;
import de.dennisguse.opentracks.content.provider.ContentProviderUtils;
import de.dennisguse.opentracks.services.TrackRecordingServiceConnection;
import de.dennisguse.opentracks.util.FileUtils;
import de.dennisguse.opentracks.util.IntentUtils;

/**
 * An activity to add/edit a marker.
 *
 * @author Jimmy Shih
 */
public class MarkerEditActivity extends AbstractActivity {

    public static final String EXTRA_TRACK_ID = "track_id";
    public static final String EXTRA_MARKER_ID = "marker_id";

    private static final String BUNDLE_PHOTO_URI = "photo_uri";

    private static final int CAMERA_REQUEST_CODE = 5;
    private static final int GALLERY_IMG_REQUEST_CODE = 7;

    private static final String TAG = MarkerEditActivity.class.getSimpleName();
    private Track.Id trackId;
    private TrackRecordingServiceConnection trackRecordingServiceConnection = new TrackRecordingServiceConnection();
    private Marker marker;

    private MenuItem insertPhotoMenuItem;
    private MenuItem insertGalleryImgMenuItem;

    private Uri photoUri;
    private boolean hasCamera;

    // UI elements
    private TextInputEditText markerName;
    private AutoCompleteTextView markerType;
    private TextInputEditText markerDescription;
    private ImageView markerPhoto;
    private ImageView markerDeletePhotoBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        trackId = getIntent().getParcelableExtra(EXTRA_TRACK_ID);
        Marker.Id markerId = getIntent().getParcelableExtra(EXTRA_MARKER_ID);

        hasCamera = getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY);

        // Setup UI elements
        markerName = findViewById(R.id.marker_edit_name);
        markerType = findViewById(R.id.marker_edit_marker_type);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.marker_types, android.R.layout.simple_dropdown_item_1line);
        markerType.setAdapter(adapter);
        markerDescription = findViewById(R.id.marker_edit_description);
        markerPhoto = findViewById(R.id.marker_edit_photo);

        markerDeletePhotoBtn = findViewById(R.id.marker_edit_photo_delete);
        markerDeletePhotoBtn.setOnClickListener(v -> {
            if (marker != null && marker.hasPhoto()) {
                marker.setPhotoUrl(null);
            }
            markerPhoto.setImageBitmap(null);
            photoUri = null;
            hideAndShowOptions();
        });

        Button cancel = findViewById(R.id.marker_edit_cancel);
        cancel.setOnClickListener(v -> finish());

        final boolean isNewMarker = markerId == null;

        setTitle(isNewMarker ? R.string.menu_insert_marker : R.string.menu_edit);
        Button done = findViewById(R.id.marker_edit_done);
        done.setText(isNewMarker ? R.string.generic_add : R.string.generic_save);
        done.setOnClickListener(v -> {
            if (isNewMarker) {
                addMarker();
            } else {
                saveMarker();
            }
            finish();
        });

        if (isNewMarker) {
            int nextMarkerNumber = trackId == null ? -1 : new ContentProviderUtils(this).getNextMarkerNumber(trackId);
            if (nextMarkerNumber == -1) {
                nextMarkerNumber = 0;
            }
            markerName.setText(getString(R.string.marker_name_format, nextMarkerNumber));
            markerName.selectAll();
            markerType.setText("");
            markerDescription.setText("");
        } else {
            marker = new ContentProviderUtils(this).getMarker(markerId);
            if (marker == null) {
                Log.d(TAG, "marker is null");
                finish();
                return;
            }
            markerName.setText(marker.getName());
            markerType.setText(marker.getCategory());
            markerDescription.setText(marker.getDescription());
            if (marker.hasPhoto()) {
                photoUri = marker.getPhotoURI();
            }
        }

        if (savedInstanceState != null) {
            photoUri = savedInstanceState.getParcelable(BUNDLE_PHOTO_URI);
            if (marker != null) {
                marker.setPhotoUrl(photoUri != null ? photoUri.toString() : null);
            }
        }
        if (photoUri != null) {
            setMarkerImageView(photoUri);
        }

        hideAndShowOptions();
    }

    @Override
    protected void onStart() {
        super.onStart();
        trackRecordingServiceConnection.startConnection(this);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(BUNDLE_PHOTO_URI, photoUri);
    }

    @Override
    protected void onStop() {
        super.onStop();
        trackRecordingServiceConnection.unbind(this);
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.marker_edit;
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
        switch (item.getItemId()) {
            case R.id.marker_edit_insert_photo:
                createMarkerWithPicture();
                return true;
            case R.id.marker_edit_insert_gallery_img:
                createMarkerWithGalleryImage();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CAMERA_REQUEST_CODE) {
            if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, R.string.marker_add_photo_canceled, Toast.LENGTH_LONG).show();
                return;
            } else if (resultCode == RESULT_OK) {
                setMarkerImageView(photoUri);
            }
        } else if (requestCode == GALLERY_IMG_REQUEST_CODE) {
            if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, R.string.marker_add_photo_canceled, Toast.LENGTH_LONG).show();
                return;
            } else if (resultCode == RESULT_OK)  {
                Uri srcUri = data.getData();
                try (ParcelFileDescriptor parcelFd = getContentResolver().openFileDescriptor(srcUri, "r")) {
                    FileDescriptor srcFd = parcelFd.getFileDescriptor();
                    File dstFile = new File(FileUtils.getImageUrl(this, getTrackId()));
                    FileUtils.copy(srcFd, dstFile);

                    photoUri = FileUtils.getUriForFile(this, dstFile);
                    setMarkerImageView(photoUri);
                } catch(Exception e) {
                    Log.e(TAG, e.getMessage());
                    Toast.makeText(this, R.string.marker_add_canceled, Toast.LENGTH_LONG).show();
                    return;
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * Checks and hide/shows all buttons/options about marker photo options.
     *
     * If a photo is set then one's options are shown, otherwise another ones are shown.
     */
    private void hideAndShowOptions() {
        boolean isPhotoSet = (marker != null && marker.hasPhoto()) || photoUri != null;
        if (insertPhotoMenuItem != null && insertGalleryImgMenuItem != null) {
            insertPhotoMenuItem.setVisible(!isPhotoSet);
            insertGalleryImgMenuItem.setVisible(!isPhotoSet);
        }
        markerDeletePhotoBtn.setVisibility(isPhotoSet ? View.VISIBLE : View.GONE);
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
            markerPhoto.setImageBitmap(bitmap);
            hideAndShowOptions();
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
            Toast.makeText(this, R.string.marker_add_photo_canceled, Toast.LENGTH_LONG).show();
        }
    }

    private void createMarkerWithPicture() {
        Pair<Intent, Uri> intentAndPhotoUri = IntentUtils.createTakePictureIntent(this, getTrackId());
        photoUri = intentAndPhotoUri.second;
        startActivityForResult(intentAndPhotoUri.first, CAMERA_REQUEST_CODE);
    }

    private void createMarkerWithGalleryImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, GALLERY_IMG_REQUEST_CODE);
    }

    private void addMarker() {
        trackRecordingServiceConnection.addMarker(this,
                markerName.getText().toString(),
                markerType.getText().toString(),
                markerDescription.getText().toString(),
                photoUri != null ? photoUri.toString() : null);
    }

    private void saveMarker() {
        marker.setName(markerName.getText().toString());
        marker.setCategory(markerType.getText().toString());
        marker.setDescription(markerDescription.getText().toString());
        marker.setPhotoUrl(photoUri != null ? photoUri.toString() : null);

        new ContentProviderUtils(this).updateMarker(this, marker);
    }
}
