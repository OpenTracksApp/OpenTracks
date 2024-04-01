package de.dennisguse.opentracks.util;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.activity.result.ActivityResultCaller;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PermissionRequester {

    private final List<String> permissions;

    public PermissionRequester(List<String> permissions) {
        this.permissions = permissions;
    }

    public boolean hasPermission(Context context) {
        return permissions.stream()
                .map(p -> ContextCompat.checkSelfPermission(context, p))
                .allMatch(r -> r == PackageManager.PERMISSION_GRANTED);
    }

    public void requestPermissionsIfNeeded(Context context, ActivityResultCaller caller, @Nullable Runnable onGranted, @Nullable RejectedCallback onRejected) {
        if (!hasPermission(context)) {
            requestPermission(caller, onGranted, onRejected);
        }
    }

    public boolean shouldShowRequestPermissionRationale(Fragment context) {
        return permissions.stream()
                .anyMatch(context::shouldShowRequestPermissionRationale);
    }

    private void requestPermission(ActivityResultCaller context, @Nullable Runnable onGranted, @Nullable RejectedCallback onRejected) {
        ActivityResultLauncher<String[]> locationPermissionRequest = context.registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                    boolean isGranted = permissions.stream()
                            .allMatch(p -> result.getOrDefault(p, false));
                    if (isGranted && onGranted != null) {
                        onGranted.run();
                    }
                    if (!isGranted && onRejected != null) {
                        onRejected.rejected(this);
                    }
                }
        );

        locationPermissionRequest.launch(permissions.toArray(new String[0]));
    }

    private static final List<String> GPS_PERMISSION;

    static {
        //TODO ACCESS_BACKGROUND_LOCATION is required for API, but the permission is not properly granted. See #1653.
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//            GPS_PERMISSION = List.of(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION);
//        } else {
            GPS_PERMISSION = List.of(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION);
//        }
    }

    private static final List<String> BLUETOOTH_PERMISSIONS;

    static {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            BLUETOOTH_PERMISSIONS = List.of(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT);
        } else {
            BLUETOOTH_PERMISSIONS = Collections.emptyList();
        }
    }

    private static final List<String> NOTIFICATION_PERMISSIONS;

    static {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            NOTIFICATION_PERMISSIONS = List.of(Manifest.permission.POST_NOTIFICATIONS);
        } else {
            NOTIFICATION_PERMISSIONS = Collections.emptyList();
        }
    }

    private static final List<String> ALL_PERMISSIONS;
    private static final List<String> RECORDING_PERMISSIONS;

    static {
        ArrayList<String> recording = new ArrayList<>(GPS_PERMISSION);
        recording.addAll(BLUETOOTH_PERMISSIONS);

        RECORDING_PERMISSIONS = Collections.unmodifiableList(new ArrayList<>(recording));

        recording.addAll(NOTIFICATION_PERMISSIONS);

        ALL_PERMISSIONS = Collections.unmodifiableList(recording);
    }

    public final static PermissionRequester GPS = new PermissionRequester(GPS_PERMISSION);
    public final static PermissionRequester BLUETOOTH = new PermissionRequester(BLUETOOTH_PERMISSIONS);
    public final static PermissionRequester NOTIFICATION = new PermissionRequester(NOTIFICATION_PERMISSIONS);

    public final static PermissionRequester ALL = new PermissionRequester(ALL_PERMISSIONS);
    public final static PermissionRequester RECORDING = new PermissionRequester(RECORDING_PERMISSIONS);

    public interface RejectedCallback {
        void rejected(PermissionRequester permissionRequester);
    }
}
