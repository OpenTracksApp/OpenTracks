package de.dennisguse.opentracks.android;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;

/**
 * Defines the interface actually shared by {@link android.content.ContentProvider} and {@link android.content.ContentResolver}.
 * So, both can be used interchangeably.
 */
public interface IContentResolver {
    @Nullable
    Uri insert(@RequiresPermission.Write @NonNull Uri url, @Nullable ContentValues values);

    int bulkInsert(@RequiresPermission.Write @NonNull Uri url, @NonNull ContentValues[] values);

    @Nullable
    Cursor query(@RequiresPermission.Read @NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder);

    int update(@RequiresPermission.Write @NonNull Uri uri, @Nullable ContentValues values, @Nullable String where, @Nullable String[] selectionArgs);

    int delete(@RequiresPermission.Write @NonNull Uri url, @Nullable String where, @Nullable String[] selectionArgs);
}