package de.dennisguse.opentracks;

import android.Manifest;
import android.os.Build;

import androidx.test.rule.GrantPermissionRule;

public class TestUtil {

    public static GrantPermissionRule createGrantPermissionRule() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return GrantPermissionRule.grant(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.POST_NOTIFICATIONS);
        }
        return GrantPermissionRule.grant(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.BLUETOOTH_CONNECT);
    }
}
