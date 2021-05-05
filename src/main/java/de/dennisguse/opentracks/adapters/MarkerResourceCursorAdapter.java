package de.dennisguse.opentracks.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.collection.LruCache;
import androidx.cursoradapter.widget.ResourceCursorAdapter;

import java.io.IOException;
import java.io.InputStream;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.content.data.MarkerColumns;
import de.dennisguse.opentracks.util.ListItemUtils;
import de.dennisguse.opentracks.util.MarkerUtils;

public class MarkerResourceCursorAdapter extends ResourceCursorAdapter implements ScrollVisibleViews.VisibleViewsListener {

    private static final String TAG = MarkerResourceCursorAdapter.class.getSimpleName();

    private static final int LIST_PREFERRED_ITEM_HEIGHT_DEFAULT = 128;

    private final Activity activity;

    //TODO Should be Marker.Id
    private final ExecutorListViewService<Long> executorService = new ExecutorListViewService<>(LIST_PREFERRED_ITEM_HEIGHT_DEFAULT);

    private boolean scroll = false;

    // Cache size is in bytes.
    private final LruCache<String, Bitmap> memoryCache;

    public MarkerResourceCursorAdapter(Activity activity, int layout) {
        super(activity, layout, null, 0);

        this.activity = activity;

        // Get max available VM memory, exceeding this amount will throw an OutOfMemory exception.
        final long usedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        final int cacheSize = (int) (Runtime.getRuntime().maxMemory() - usedMemory) / 8;

        memoryCache = new LruCache<>(cacheSize) {
            @Override
            protected int sizeOf(@NonNull String key, @NonNull Bitmap bitmap) {
                return bitmap.getByteCount();
            }
        };
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        int idIndex = cursor.getColumnIndex(MarkerColumns._ID);
        int nameIndex = cursor.getColumnIndex(MarkerColumns.NAME);
        int timeIndex = cursor.getColumnIndexOrThrow(MarkerColumns.TIME);
        int categoryIndex = cursor.getColumnIndex(MarkerColumns.CATEGORY);
        int descriptionIndex = cursor.getColumnIndex(MarkerColumns.DESCRIPTION);
        int photoUrlIndex = cursor.getColumnIndex(MarkerColumns.PHOTOURL);

        long id = cursor.getLong(idIndex);
        int iconId = MarkerUtils.ICON_ID;
        String name = cursor.getString(nameIndex);
        long time = cursor.getLong(timeIndex);
        String category = cursor.getString(categoryIndex);
        String description = cursor.getString(descriptionIndex);
        String photoUrl = cursor.getString(photoUrlIndex);

        view.setTag(String.valueOf(id));

        boolean hasPhoto = photoUrl != null && !photoUrl.equals("");

        ImageView imageView = view.findViewById(R.id.list_item_photo);
        ImageView textGradient = view.findViewById(R.id.list_item_text_gradient);
        imageView.setVisibility(hasPhoto ? View.VISIBLE : View.GONE);
        textGradient.setVisibility(hasPhoto ? View.VISIBLE : View.GONE);
        imageView.setImageBitmap(null);

        if (hasPhoto) {
            int height = getPhotoHeight(activity);
            ViewGroup.LayoutParams params = imageView.getLayoutParams();
            params.height = height;
            imageView.setLayoutParams(params);

            if (getBitmapFromMemCache(String.valueOf(id)) != null || !scroll) {
                asyncLoadPhoto(view, imageView, photoUrl, id);
            }
        }

        ListItemUtils.setListItem(activity, view, false, true, iconId, R.string.image_marker, name, null, null, 0, time, false, category, description, hasPhoto);
    }

    public void clear() {
        executorService.shutdown();
    }

    @Override
    public void onViewVisible(View view, int position) {
        scroll = true;

        Cursor cursor = getCursor();
        if (!cursor.moveToPosition(position)) {
            return;
        }

        long id = cursor.getLong(cursor.getColumnIndexOrThrow(MarkerColumns._ID));
        String photoUrl = cursor.getString(cursor.getColumnIndexOrThrow(MarkerColumns.PHOTOURL));

        boolean hasPhoto = photoUrl != null && !photoUrl.equals("");
        if (hasPhoto) {
            ImageView imageView = view.findViewById(R.id.list_item_photo);
            asyncLoadPhoto(view, imageView, photoUrl, id);
        }
    }

    public void markerInvalid(long id) {
        memoryCache.remove(String.valueOf(id));
        scroll = false;
    }

    private void addBitmapToMemoryCache(String key, Bitmap bitmap) {
        synchronized (memoryCache) {
            if (getBitmapFromMemCache(key) == null) {
                memoryCache.put(key, bitmap);
            }
        }
    }

    private Bitmap getBitmapFromMemCache(String key) {
        return memoryCache.get(key);
    }

    /**
     * It loads the photoUrl in the imageView from view.
     * It takes the photo from cache or from storage if isn't in the cache.
     *
     * @param view      item's view.
     * @param imageView view object where photo will be loaded.
     * @param photoUrl  photo's url.
     * @param id        marker's id where photo belong.
     */
    private void asyncLoadPhoto(View view, ImageView imageView, String photoUrl, long id) {
        Bitmap photo = getBitmapFromMemCache(String.valueOf(id));
        imageView.setImageBitmap(photo);

        if (photo == null) {
            executorService.execute(id, () -> {
                try (InputStream inputStream = activity.getContentResolver().openInputStream(Uri.parse(photoUrl))) {
                    Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                    int height = getPhotoHeight(activity);
                    Log.d(TAG, "Width : " + (bitmap.getWidth() / (bitmap.getHeight() / height)) + " | Height: " + height);
                    final Bitmap finalPhoto = ThumbnailUtils.extractThumbnail(bitmap, bitmap.getWidth() / (bitmap.getHeight() / height), height);
                    addBitmapToMemoryCache(String.valueOf(id), finalPhoto);
                    if (view.getTag().equals(String.valueOf(id))) {
                        activity.runOnUiThread(() -> imageView.setImageBitmap(finalPhoto));
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Failed to image " + e);
                }
            });
        }
    }

    /**
     * Gets the photo height.
     *
     * @param context the context
     */
    private static int getPhotoHeight(Context context) {
        int[] attrs = new int[]{android.R.attr.listPreferredItemHeight};
        TypedArray typeArray = context.obtainStyledAttributes(attrs);
        int height = typeArray.getDimensionPixelSize(0, LIST_PREFERRED_ITEM_HEIGHT_DEFAULT);
        typeArray.recycle();
        return 2 * height;
    }
}
