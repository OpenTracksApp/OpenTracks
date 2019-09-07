package de.dennisguse.opentracks.android;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Wraps a {@link ContentResolver} and provides access as {@link IContentResolver}.
 */
public class ContentResolverWrapper implements IContentResolver {

    private ContentResolver contentResolver;

    public ContentResolverWrapper(ContentResolver contentResolver) {
        this.contentResolver = contentResolver;
    }

    @Nullable
    public Uri insert(@NonNull Uri url, @Nullable ContentValues values) {
        return contentResolver.insert(url, values);
    }

    public int bulkInsert(@NonNull Uri url, @NonNull ContentValues[] values) {
        return contentResolver.bulkInsert(url, values);
    }

    @Nullable
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        return contentResolver.query(uri, projection, selection, selectionArgs, sortOrder);
    }

    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String where, @Nullable String[] selectionArgs) {
        return contentResolver.update(uri, values, where, selectionArgs);
    }

    public int delete(@NonNull Uri url, @Nullable String where, @Nullable String[] selectionArgs) {
        return contentResolver.delete(url, where, selectionArgs);
    }
}
