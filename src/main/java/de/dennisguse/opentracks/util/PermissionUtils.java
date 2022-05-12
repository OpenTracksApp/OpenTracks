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

import java.util.Arrays;

public class PermissionUtils {

    private static final String[] GPS = new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
    private static final String[] BLUETOOTH;

    static {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            BLUETOOTH = new String[]{};
        } else {
            BLUETOOTH = new String[]{Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT};
        }
    }

    private PermissionUtils() {
    }

    public static boolean hasGPSPermission(Context context) {
        return hasPermissions(context, GPS);
    }

    public static void requestGPSPermission(ActivityResultCaller context, @Nullable Runnable onGranted, @Nullable Runnable onRejected) {
        requestPermission(context, GPS, onGranted, onRejected);
    }

    public static boolean shouldShowRequestPermissionRationaleBluetooth(Fragment context) {
        return Arrays.stream(BLUETOOTH).anyMatch(context::shouldShowRequestPermissionRationale);
    }

    public static boolean hasBluetoothPermissions(Context context) {
        return hasPermissions(context, BLUETOOTH);
    }

    public static void requestBluetoothPermission(ActivityResultCaller context, @Nullable Runnable onGranted, @Nullable Runnable onRejected) {
        requestPermission(context, BLUETOOTH, onGranted, onRejected);
    }

    private static void requestPermission(ActivityResultCaller context, final String[] permissions, @Nullable Runnable onGranted, @Nullable Runnable onRejected) {
        ActivityResultLauncher<String[]> locationPermissionRequest = context.registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                    boolean isGranted = Arrays.stream(permissions)
                            .allMatch(p -> result.getOrDefault(p, false));
                    if (isGranted && onGranted != null) {
                        onGranted.run();
                    }
                    if (!isGranted && onRejected != null) {
                        onRejected.run();
                    }
                }
        );

        locationPermissionRequest.launch(permissions);
    }

    private static boolean hasPermissions(Context context, String[] permissions) {
        return Arrays.stream(permissions)
                .map(p -> ContextCompat.checkSelfPermission(context, p))
                .allMatch(r -> r == PackageManager.PERMISSION_GRANTED);
    }
}
