package de.dennisguse.opentracks.ui.markers;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.util.Log;
import android.util.LruCache;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;
import java.io.InputStream;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.data.ContentProviderUtils;
import de.dennisguse.opentracks.data.models.Marker;
import de.dennisguse.opentracks.databinding.MarkerListItemBinding;
import de.dennisguse.opentracks.ui.util.ActivityUtils;
import de.dennisguse.opentracks.ui.util.ExecutorListViewService;
import de.dennisguse.opentracks.ui.util.ListItemUtils;
import de.dennisguse.opentracks.ui.util.ThemeUtils;
import de.dennisguse.opentracks.util.IntentUtils;
import de.dennisguse.opentracks.util.StringUtils;

public class MarkerListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements ActionMode.Callback {

    private static final String TAG = MarkerListAdapter.class.getSimpleName();

    private final AppCompatActivity context;
    private final RecyclerView recyclerView;
    private List<Marker> markers;
    private final SparseBooleanArray selection;
    private boolean selectionMode;
    private ActivityUtils.ContextualActionModeCallback actionModeCallback;
    private ActionMode actionMode;

    private final ExecutorListViewService<Marker.Id> executorService = new ExecutorListViewService<>(128);

    private final LruCache<Marker.Id, Bitmap> memoryCache;

    public MarkerListAdapter(AppCompatActivity context, RecyclerView recyclerView, List<Marker> markers) {
        this.context = context;
        this.recyclerView = recyclerView;
        this.markers = markers;

        selection = new SparseBooleanArray();
        selectionMode = false;

        // Get max available VM memory, exceeding this amount will throw an OutOfMemory exception.
        final long usedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        final int cacheSize = (int) (Runtime.getRuntime().maxMemory() - usedMemory) / 8;

        memoryCache = new LruCache<>(cacheSize) {
            @Override
            protected int sizeOf(Marker.Id key, Bitmap value) {
                return value.getByteCount();
            }
        };
    }

    public void setActionModeCallback(ActivityUtils.ContextualActionModeCallback actionModeCallback) {
        this.actionModeCallback = actionModeCallback;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.marker_list_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ViewHolder viewHolder = (ViewHolder) holder;

        viewHolder.bind(markers.get(position));
    }

    @Override
    public int getItemCount() {
        if (markers == null) {
            return 0;
        }
        return markers.size();
    }

    public void swapData(List<Marker> markers) {
        this.markers = markers;
        this.notifyDataSetChanged();
    }

    @Override
    public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
        actionMode.getMenuInflater().inflate(R.menu.list_context_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
        selectionMode = true;
        actionModeCallback.onPrepare(menu, null, getCheckedIds(), true);
        return true;
    }

    @Override
    public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
        if (actionModeCallback.onClick(menuItem.getItemId(), null, getCheckedIds())) {
            actionMode.finish();
        }
        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode actionMode) {
        selectionMode = false;

        setAllSelected(false);

        actionModeCallback.onDestroy();
    }

    public void setAllSelected(boolean isSelected) {
        if (isSelected) {
            for (Marker marker : markers) {
                selection.put((int) marker.getId().id(), true);
            }
        } else {
            selection.clear();
        }

        for (int i = 0; i < recyclerView.getChildCount(); i++) {
            ViewHolder holder = (ViewHolder) recyclerView.getChildViewHolder(recyclerView.getChildAt(i));
            holder.setSelected(isSelected);
        }
    }

    private long[] getCheckedIds() {
        List<Long> ids = new ArrayList<>();

        for (int i = 0; i < selection.size(); i++) {
            if (selection.valueAt(i)) {
                ids.add((long) selection.keyAt(i));
            }
        }

        return ids.stream().mapToLong(i -> i).toArray();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {

        private final MarkerListItemBinding viewBinding;

        private Marker marker;

        public ViewHolder(@NonNull View view) {
            super(view);

            viewBinding = MarkerListItemBinding.bind(view);

            viewBinding.getRoot().setOnClickListener(this);
            viewBinding.getRoot().setOnLongClickListener(this);
        }

        public void bind(Marker marker) {
            this.marker = marker;

            viewBinding.markerListItemPhoto.setVisibility(marker.hasPhoto() ? View.VISIBLE : View.GONE);
            if (marker.hasPhoto()) {
                int height = ThemeUtils.getPhotoHeight(context);
                ViewGroup.LayoutParams params = viewBinding.markerListItemPhoto.getLayoutParams();
                params.height = height;
                viewBinding.markerListItemPhoto.setLayoutParams(params);

                asyncLoadPhoto(viewBinding.markerListItemPhoto, marker.getPhotoUrl(), marker.getId());
            }

            viewBinding.markerListItemName.setText(marker.getName());

            {
                ZoneOffset timeZone = new ContentProviderUtils(context).getTrack(marker.getTrackId()).getZoneOffset();
                ListItemUtils.setDateAndTime(context, viewBinding.markerListItemDate, viewBinding.markerListItemTime, marker.getTime(), timeZone);
            }

            String categoryDescription = StringUtils.getCategoryDescription(marker.getCategory(), marker.getDescription());
            viewBinding.markerListItemTimeDistance.setText(categoryDescription);

            setSelected(selection.get((int) getId()));
        }

        public void setSelected(boolean isSelected) {
            selection.put((int) getId(), isSelected);
            viewBinding.getRoot().setActivated(isSelected);
        }

        public long getId() {
            return marker.getId().id();
        }

        @Override
        public void onClick(View view) {
            if (selectionMode) {
                setSelected(!view.isActivated());
                actionMode.invalidate();
            } else {
                Intent intent = IntentUtils.newIntent(context, MarkerDetailActivity.class)
                        .putExtra(MarkerDetailActivity.EXTRA_MARKER_ID, marker.getId());
                context.startActivity(intent);
            }
        }

        @Override
        public boolean onLongClick(View view) {
            setSelected(!view.isActivated());
            if (!selectionMode) {
                actionMode = context.startSupportActionMode(MarkerListAdapter.this);
            } else {
                actionMode.invalidate();
            }
            return true;
        }
    }

    /**
     * It loads the photoUrl in the imageView from view.
     * It takes the photo from cache or from storage if isn't in the cache.
     *
     * @param imageView view object where photo will be loaded.
     * @param photoUri  photo's url.
     * @param id        marker's id where photo belong.
     */
    private void asyncLoadPhoto(ImageView imageView, Uri photoUri, Marker.Id id) {

        Bitmap bm = memoryCache.get(id);
        imageView.setImageBitmap(bm);
        if (bm != null) {
            return;
        }

        executorService.execute(id, () -> {
            try (InputStream inputStream = context.getContentResolver().openInputStream(photoUri)) {
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                int height = ThemeUtils.getPhotoHeight(context);
                Log.d(TAG, "Width : " + (bitmap.getWidth() / (bitmap.getHeight() / height)) + " | Height: " + height);
                final Bitmap finalPhoto = ThumbnailUtils.extractThumbnail(bitmap, bitmap.getWidth() / (bitmap.getHeight() / height), height);
                context.runOnUiThread(() -> imageView.setImageBitmap(finalPhoto));
                memoryCache.put(id, finalPhoto);
            } catch (IOException e) {
                Log.e(TAG, "Failed to image " + e);
            }
        });
    }
}
