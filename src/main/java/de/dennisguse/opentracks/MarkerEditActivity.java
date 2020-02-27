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
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;

import de.dennisguse.opentracks.content.data.Waypoint;
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

    private static final int CAMERA_REQUEST_CODE = 5;
    private static final int GALLERY_IMG_REQUEST_CODE = 7;

    private static final String TAG = MarkerEditActivity.class.getSimpleName();
    private long trackId;
    private long markerId;
    private TrackRecordingServiceConnection trackRecordingServiceConnection;
    private Waypoint waypoint;

    private MenuItem insertPhotoMenuItem;
    private MenuItem insertGalleryImgMenuItem;

    private Uri photoUri;
    private boolean hasCamera;

    // UI elements
    private EditText waypointName;
    private AutoCompleteTextView waypointMarkerType;
    private EditText waypointDescription;
    private ImageView waypointPhoto;
    private ImageView waypointDeletePhotoBtn;
    private Button done;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        trackId = getIntent().getLongExtra(EXTRA_TRACK_ID, -1L);
        markerId = getIntent().getLongExtra(EXTRA_MARKER_ID, -1L);
        trackRecordingServiceConnection = new TrackRecordingServiceConnection(this, null);

        hasCamera = getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY);

        // Setup UI elements
        waypointName = findViewById(R.id.marker_edit_waypoint_name);
        waypointMarkerType = findViewById(R.id.marker_edit_waypoint_marker_type);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.waypoint_types, android.R.layout.simple_dropdown_item_1line);
        waypointMarkerType.setAdapter(adapter);
        waypointDescription = findViewById(R.id.marker_edit_waypoint_description);
        waypointPhoto = findViewById(R.id.marker_edit_waypoint_photo);

        waypointDeletePhotoBtn = findViewById(R.id.marker_edit_waypoint_photo_delete);
        waypointDeletePhotoBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (waypoint != null && waypoint.hasPhoto())
                    waypoint.setPhotoUrl(null);
                waypointPhoto.setImageBitmap(null);
                photoUri = null;
                hideAndShowOptions();
            }
        });

        Button cancel = findViewById(R.id.marker_edit_cancel);
        cancel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        done = findViewById(R.id.marker_edit_done);
        updateUiByMarkerId();
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
                createWaypointWithPicture();
                return true;
            case R.id.marker_edit_insert_gallery_img:
                createWaypointWithGalleryImage();
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
                setWaypointImageView(photoUri);
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
                    setWaypointImageView(photoUri);
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
     * Updates the UI based on the marker id.
     */
    private void updateUiByMarkerId() {
        final boolean newMarker = markerId == -1L;

        setTitle(newMarker ? R.string.menu_insert_marker : R.string.menu_edit);
        done.setText(newMarker ? R.string.generic_add : R.string.generic_save);
        done.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (newMarker) {
                    addMarker();
                } else {
                    saveMarker();
                }
                finish();
            }
        });

        if (newMarker) {
            int nextWaypointNumber = trackId == -1L ? -1 : new ContentProviderUtils(this).getNextWaypointNumber(trackId);
            if (nextWaypointNumber == -1) {
                nextWaypointNumber = 0;
            }
            waypointName.setText(getString(R.string.marker_name_format, nextWaypointNumber));
            waypointName.selectAll();
            waypointMarkerType.setText("");
            waypointDescription.setText("");
        } else {
            waypoint = new ContentProviderUtils(this).getWaypoint(markerId);
            if (waypoint == null) {
                Log.d(TAG, "waypoint is null");
                finish();
                return;
            }
            waypointName.setText(waypoint.getName());
            waypointMarkerType.setText(waypoint.getCategory());
            waypointDescription.setText(waypoint.getDescription());
            if (waypoint.hasPhoto()) {
                photoUri = waypoint.getPhotoURI();
                setWaypointImageView(photoUri);
            }
        }

        hideAndShowOptions();
    }

    /**
     * Checks and hide/shows all buttons/options about marker photo options.
     *
     * If a photo is set then one's options are shown, otherwise another ones are shown.
     */
    private void hideAndShowOptions() {
        boolean isPhotoSet = (waypoint != null && waypoint.hasPhoto()) || photoUri != null;
        if (insertPhotoMenuItem != null && insertGalleryImgMenuItem != null) {
            insertPhotoMenuItem.setVisible(isPhotoSet ? false : true);
            insertGalleryImgMenuItem.setVisible(isPhotoSet ? false : true);
        }
        waypointDeletePhotoBtn.setVisibility(isPhotoSet ? View.VISIBLE : View.GONE);
    }

    /**
     * Returns the trackId. If trackId == -1 then get it from waypoint object.
     */
    private long getTrackId() {
        return trackId == -1 ? waypoint.getTrackId() : trackId;
    }

    /**
     * Sets the ImageView waypointPhoto with the uri photo.
     *
     * @param uri the uri photo.
     */
    private void setWaypointImageView(Uri uri) {
        try (ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(uri, "r")) {
            FileDescriptor fd = pfd.getFileDescriptor();
            Bitmap bitmap = BitmapFactory.decodeFileDescriptor(fd);
            waypointPhoto.setImageBitmap(bitmap);
            hideAndShowOptions();
        } catch(IOException e) {
            Log.e(TAG, e.getMessage());
            Toast.makeText(this, R.string.marker_add_photo_canceled, Toast.LENGTH_LONG).show();
        }
    }

    private void createWaypointWithPicture() {
        Pair<Intent, Uri> intentAndPhotoUri = IntentUtils.createTakePictureIntent(this, getTrackId());
        photoUri = intentAndPhotoUri.second;
        startActivityForResult(intentAndPhotoUri.first, CAMERA_REQUEST_CODE);
    }

    private void createWaypointWithGalleryImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, GALLERY_IMG_REQUEST_CODE);
    }

    /**
     * Adds a marker.
     */
    private void addMarker() {
        trackRecordingServiceConnection.addMarker(this,
                waypointName.getText().toString(),
                waypointMarkerType.getText().toString(),
                waypointDescription.getText().toString(),
                photoUri != null ? photoUri.toString() : null);
    }

    /**
     * Saves a marker.
     */
    private void saveMarker() {
        waypoint.setName(waypointName.getText().toString());
        waypoint.setCategory(waypointMarkerType.getText().toString());
        waypoint.setDescription(waypointDescription.getText().toString());
        waypoint.setPhotoUrl(photoUri != null ? photoUri.toString() : null);

        new ContentProviderUtils(this).updateWaypoint(waypoint);
    }
}
