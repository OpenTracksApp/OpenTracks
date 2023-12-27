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
import java.time.Instant;
import java.util.NoSuchElementException;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.data.ContentProviderUtils;
import de.dennisguse.opentracks.data.models.Marker;
import de.dennisguse.opentracks.data.models.Track;
import de.dennisguse.opentracks.services.TrackRecordingServiceConnection;
import de.dennisguse.opentracks.util.FileUtils;

public class MarkerEditViewModel extends AndroidViewModel {

    private static final String TAG = MarkerEditViewModel.class.getSimpleName();

    private MutableLiveData<Marker> markerData;
    private boolean isNewMarker;
    private Uri photoOriginalUri;
    private final TrackRecordingServiceConnection trackRecordingServiceConnection = new TrackRecordingServiceConnection((service, connection) -> {});

    public MarkerEditViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<Marker> getMarkerData(@NonNull Track.Id trackId, @Nullable Marker.Id markerId) {
        if (markerData == null) {
            markerData = new MutableLiveData<>();
            trackRecordingServiceConnection.bind(getApplication());
            loadData(trackId, markerId);
        }
        return markerData;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        trackRecordingServiceConnection.unbind(getApplication());
    }

    private void loadData(Track.Id trackId, Marker.Id markerId) {
        Marker marker;
        isNewMarker = markerId == null;
        if (isNewMarker) {
            int nextMarkerNumber = trackId == null ? 0 : new ContentProviderUtils(getApplication()).getNextMarkerNumber(trackId);
            marker = new Marker(trackId, (Instant) null);
            marker.setName(getApplication().getString(R.string.marker_name_format, nextMarkerNumber));
        } else {
            marker = new ContentProviderUtils(getApplication()).getMarker(markerId);
            if (marker.hasPhoto()) {
                photoOriginalUri = marker.getPhotoURI();
            }
        }
        markerData.postValue(marker);
    }

    private @NonNull Marker getMarker() throws NoSuchElementException {
        Marker marker = markerData != null ? markerData.getValue() : null;
        if (marker == null) {
            Log.d(TAG, "Marker data shouldn't be null. Call getMarkerData before.");
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
            deletePhoto(marker.getPhotoURI());
        }
    }

    public void onPhotoDelete(String name, String category, String description) {
        Marker marker =  getMarker();
        if (marker.hasPhoto()) {
            if (!marker.getPhotoURI().equals(photoOriginalUri)) {
                deletePhoto(marker.getPhotoURI());
            }
            marker.setPhotoUrl(null);
            marker.setName(name);
            marker.setCategory(category);
            marker.setDescription(description);
            markerData.postValue(marker);
        }
    }

    public void onNewCameraPhoto(@NonNull Uri photoUri, String name, String category, String description) {
        Marker marker =  getMarker();
        marker.setPhotoUrl(photoUri.toString());
        marker.setName(name);
        marker.setCategory(category);
        marker.setDescription(description);
        markerData.postValue(marker);
    }

    public void onNewGalleryPhoto(@NonNull Uri srcUri, String name, String category, String description) {
        Marker marker =  getMarker();

        try (ParcelFileDescriptor parcelFd = getApplication().getContentResolver().openFileDescriptor(srcUri, "r")) {
            FileDescriptor srcFd = parcelFd.getFileDescriptor();
            File dstFile = new File(MarkerUtils.getImageUrl(getApplication(), marker.getTrackId()));
            FileUtils.copy(srcFd, dstFile);

            Uri photoUri = FileUtils.getUriForFile(getApplication(), dstFile);
            marker.setPhotoUrl(photoUri.toString());
            marker.setName(name);
            marker.setCategory(category);
            marker.setDescription(description);
            markerData.postValue(marker);
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
            Toast.makeText(getApplication(), R.string.marker_add_canceled, Toast.LENGTH_LONG).show();
        }
    }

    private void onAddDone(@NonNull Marker marker, String name, String category, String description) {
        trackRecordingServiceConnection.addMarker(getApplication(), name, category, description, marker.hasPhoto() ? marker.getPhotoURI().toString() : null);
    }

    private void onSaveDone(@NonNull Marker marker, String name, String category, String description) {
        marker.setName(name);
        marker.setCategory(category);
        marker.setDescription(description);
        new ContentProviderUtils(getApplication()).updateMarker(getApplication(), marker);

        if (photoOriginalUri != null && (!marker.hasPhoto() || !photoOriginalUri.equals(marker.getPhotoURI()))) {
            deletePhoto(photoOriginalUri);
        }
    }

    public void onDone(String name, String category, String description) {
        Marker marker = getMarker();
        if (isNewMarker) {
            onAddDone(marker, name, category, description);
        } else {
            onSaveDone(marker, name, category, description);
        }
    }

    public void onCancel() {
        Marker marker = getMarker();
        if (isNewMarker) {
            // it's new marker -> clean all photos.
            deletePhoto(marker);
            deletePhoto(photoOriginalUri);
        } else if (photoOriginalUri == null || (marker.hasPhoto() && !marker.getPhotoURI().equals(photoOriginalUri))) {
            // it's an edit marker -> delete photo if it was empty or it was changed (leaving the original in that case).
            deletePhoto(marker);
        }
    }
}
