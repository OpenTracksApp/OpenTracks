package de.dennisguse.opentracks.ui.markers;

import android.app.Application;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.util.NoSuchElementException;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.data.ContentProviderUtils;
import de.dennisguse.opentracks.data.models.Marker;
import de.dennisguse.opentracks.data.models.Track;
import de.dennisguse.opentracks.data.models.TrackPoint;
import de.dennisguse.opentracks.util.FileUtils;

public class MarkerEditViewModel extends AndroidViewModel {

    private static final String TAG = MarkerEditViewModel.class.getSimpleName();

    private MutableLiveData<Marker> markerData;
    private Uri photoOriginalUri;

    public MarkerEditViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<Marker> getMarkerData(@NonNull Marker.Id markerId) {
        if (markerData == null) {
            markerData = new MutableLiveData<>();

            Marker marker = new ContentProviderUtils(getApplication()).getMarker(markerId);
            if (marker.hasPhoto()) {
                photoOriginalUri = marker.getPhotoUrl();
            }

            markerData.postValue(marker);
        }
        return markerData;
    }

    private @NonNull Marker getMarker() throws NoSuchElementException {
        Marker marker = markerData != null ? markerData.getValue() : null;
        if (marker == null) {
            throw new NoSuchElementException("Marker data shouldn't be null. Call getMarkerData before.");
        }

        return marker;
    }

    private void deletePhoto(@Nullable Uri photoUri) {
        if (photoUri == null) {
            return;
        }

        File photoFile = MarkerUtils.getPhotoFileIfExists(getApplication(), markerData.getValue().getTrackId(), photoUri);
        if (photoFile != null) {
            FileUtils.deleteDirectoryRecurse(photoFile);
        }
    }

    private void deletePhoto(Marker marker) {
        if (marker.hasPhoto()) {
            deletePhoto(marker.getPhotoUrl());
        }
    }

    public void onPhotoDelete(String name, String category, String description) {
        Marker marker = getMarker();
        if (marker.hasPhoto()) {
            if (!marker.getPhotoUrl().equals(photoOriginalUri)) {
                deletePhoto(marker.getPhotoUrl());
            }
            marker.setPhotoUrl(null);
            marker.setName(name);
            marker.setCategory(category);
            marker.setDescription(description);
            markerData.postValue(marker);
        }
    }

    public void onNewCameraPhoto(@NonNull Uri photoUri, String name, String category, String description) {
        Marker marker = getMarker();
        marker.setPhotoUrl(photoUri);
        marker.setName(name);
        marker.setCategory(category);
        marker.setDescription(description);
        markerData.postValue(marker);
    }

    public void onNewGalleryPhoto(@NonNull Uri srcUri, String name, String category, String description) {
        Marker marker = getMarker();

        try (ParcelFileDescriptor parcelFd = getApplication().getContentResolver().openFileDescriptor(srcUri, "r")) {
            FileDescriptor srcFd = parcelFd.getFileDescriptor();
            File dstFile = new File(MarkerUtils.getImageUrl(getApplication(), marker.getTrackId()));
            FileUtils.copy(srcFd, dstFile);

            Uri photoUri = FileUtils.getUriForFile(getApplication(), dstFile);
            marker.setPhotoUrl(photoUri);
            marker.setName(name);
            marker.setCategory(category);
            marker.setDescription(description);
            markerData.postValue(marker);
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
            Toast.makeText(getApplication(), R.string.marker_add_canceled, Toast.LENGTH_LONG).show();
        }
    }

    public void onDone(String name, String category, String description) {
        Marker marker = getMarker();

        marker.setName(name);
        marker.setCategory(category);
        marker.setDescription(description);
        if (marker.getId() == null) {
            new ContentProviderUtils(getApplication()).insertMarker(marker);
        } else {
            new ContentProviderUtils(getApplication()).updateMarker(getApplication(), marker);
        }

        if (photoOriginalUri != null && (!marker.hasPhoto() || !photoOriginalUri.equals(marker.getPhotoUrl()))) {
            deletePhoto(photoOriginalUri);
        }
    }

    public LiveData<Marker> createNewMarker(Track.Id trackId, TrackPoint trackPoint) {
        Integer nextMarkerNumber = new ContentProviderUtils(getApplication()).getNextMarkerNumber(trackId);
        if (nextMarkerNumber == null) {
            nextMarkerNumber = 1;
        }
        String name = getApplication().getString(R.string.marker_name_format, nextMarkerNumber + 1);
        String icon = getApplication().getString(R.string.marker_icon_url);

        Marker marker = new Marker(trackId, trackPoint, name, "", "", icon, null);

        if (markerData == null) {
            markerData = new MutableLiveData<>();
        }
        markerData.postValue(marker);
        return markerData;
    }

    public void onCancel(boolean isNewMarker) {
        Marker marker = getMarker();
        if (isNewMarker) {
            // it's new marker -> clean all photos.
            deletePhoto(marker);
            deletePhoto(photoOriginalUri);
        } else if (photoOriginalUri == null || (marker.hasPhoto() && !marker.getPhotoUrl().equals(photoOriginalUri))) {
            // it's an edit marker -> delete photo if it was empty or it was changed (leaving the original in that case).
            deletePhoto(marker);
        }
    }
}
