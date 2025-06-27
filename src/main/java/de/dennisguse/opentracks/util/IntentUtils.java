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

package de.dennisguse.opentracks.util;

import android.content.Context;
import android.content.Intent;
import android.content.UriPermission;
import android.net.Uri;
import android.util.Log;

import androidx.documentfile.provider.DocumentFile;

import de.dennisguse.opentracks.data.models.Marker;

/**
 * Utilities for creating intents.
 *
 * @author Jimmy Shih
 */
public class IntentUtils {

    private static final String TAG = IntentUtils.class.getSimpleName();

    private static final String JPEG_EXTENSION = "jpeg";

    private IntentUtils() {
    }

    /**
     * Creates an intent with {@link Intent#FLAG_ACTIVITY_CLEAR_TOP} and {@link Intent#FLAG_ACTIVITY_NEW_TASK}.
     *
     * @param context the context
     * @param cls     the class
     */
    public static Intent newIntent(Context context, Class<?> cls) {
        return new Intent(context, cls).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
    }

    /**
     * Send intent to show coordinates on a map (needs an another app).
     */
    public static void showCoordinateOnMap(Context context, Marker marker) {
        //SEE https://developer.android.com/guide/components/intents-common.html#Maps
        String uri = "geo:0,0?q=" + marker.getPosition().latitude() + "," + marker.getPosition().longitude();
        if (marker.getName() != null && !marker.getName().isEmpty()) {
            uri += "(" + marker.getName() + ")";
        }
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(uri));


        context.startActivity(Intent.createChooser(intent, null));
    }

    public static void persistDirectoryAccessPermission(Context context, Uri directoryUri, int existingFlags) {
        int newFlags = existingFlags & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        context.getApplicationContext().getContentResolver().takePersistableUriPermission(directoryUri, newFlags);
    }

    public static void releaseDirectoryAccessPermission(Context context, final Uri documentUri) {
        if (documentUri == null) {
            return;
        }

        context.getApplicationContext().getContentResolver().getPersistedUriPermissions().stream()
                .map(UriPermission::getUri)
                .filter(documentUri::equals)
                .forEach(u -> context.getContentResolver().releasePersistableUriPermission(u, 0));
    }

    public static DocumentFile toDocumentFile(Context context, Uri directoryUri) {
        if (directoryUri == null) {
            return null;
        }
        try {
            return DocumentFile.fromTreeUri(context.getApplicationContext(), directoryUri);
        } catch (Exception e) {
            Log.w(TAG, "Could not decode directory: " + e.getMessage());
        }
        return null;
    }

}
