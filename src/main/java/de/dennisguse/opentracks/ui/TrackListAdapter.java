package de.dennisguse.opentracks.ui;

import android.app.ActivityOptions;
import android.content.Intent;
import android.database.Cursor;
import android.util.Pair;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.recyclerview.widget.RecyclerView;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.TrackRecordedActivity;
import de.dennisguse.opentracks.TrackRecordingActivity;
import de.dennisguse.opentracks.data.models.ActivityType;
import de.dennisguse.opentracks.data.models.Distance;
import de.dennisguse.opentracks.data.models.Track;
import de.dennisguse.opentracks.data.tables.TracksColumns;
import de.dennisguse.opentracks.databinding.TrackListItemBinding;
import de.dennisguse.opentracks.services.RecordingStatus;
import de.dennisguse.opentracks.settings.UnitSystem;
import de.dennisguse.opentracks.ui.util.ActivityUtils;
import de.dennisguse.opentracks.ui.util.ListItemUtils;
import de.dennisguse.opentracks.util.IntentUtils;
import de.dennisguse.opentracks.util.StringUtils;

public class TrackListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements ActionMode.Callback {

    private static final String TAG = TrackListAdapter.class.getSimpleName();

    private final AppCompatActivity context;
    private final RecyclerView recyclerView;
    private final SparseBooleanArray selection = new SparseBooleanArray();
    private RecordingStatus recordingStatus;
    private UnitSystem unitSystem;
    private Cursor cursor;
    private boolean selectionMode = false;
    private ActivityUtils.ContextualActionModeCallback actionModeCallback;
    private ActionMode actionMode;

    public TrackListAdapter(AppCompatActivity context, RecyclerView recyclerView, RecordingStatus recordingStatus, UnitSystem unitSystem) {
        this.context = context;
        this.recyclerView = recyclerView;
        this.recordingStatus = recordingStatus;
        this.unitSystem = unitSystem;
    }

    public void setActionModeCallback(ActivityUtils.ContextualActionModeCallback actionModeCallback) {
        this.actionModeCallback = actionModeCallback;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.track_list_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ViewHolder viewHolder = (ViewHolder) holder;

        cursor.moveToPosition(position);
        viewHolder.bind(cursor);
    }

    @Override
    public int getItemCount() {
        if (cursor == null) {
            return 0;
        }
        return cursor.getCount();
    }

    public void swapData(Cursor cursor) {
        this.cursor = cursor;
        this.notifyDataSetChanged();
    }

    public void updateRecordingStatus(RecordingStatus recordingStatus) {
        this.recordingStatus = recordingStatus;
    }

    public void updateUnitSystem(UnitSystem unitSystem) {
        this.unitSystem = unitSystem;
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        mode.getMenuInflater().inflate(R.menu.list_context_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        selectionMode = true;

        actionModeCallback.onPrepare(menu, null, getCheckedIds(), true);
        return true;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        if (actionModeCallback.onClick(item.getItemId(), null, getCheckedIds())) {
            mode.finish();
        }
        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        selectionMode = false;

        setAllSelected(false);

        actionModeCallback.onDestroy();
    }

    public void setAllSelected(boolean isSelected) {
        if (isSelected) {
            final int idIndex = cursor.getColumnIndexOrThrow(TracksColumns._ID);

            cursor.moveToFirst();
            do {
                selection.put((int) cursor.getLong(idIndex), true);
            } while (cursor.moveToNext());
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

        private final TrackListItemBinding viewBinding;
        private final View view;

        private Track.Id trackId;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            viewBinding = TrackListItemBinding.bind(itemView);
            view = itemView;

            view.setOnClickListener(this);
            view.setOnLongClickListener(this);
        }


        public void bind(Cursor cursor) {
            final int idIndex = cursor.getColumnIndexOrThrow(TracksColumns._ID);
            final int nameIndex = cursor.getColumnIndexOrThrow(TracksColumns.NAME);
            final int descriptionIndex = cursor.getColumnIndexOrThrow(TracksColumns.DESCRIPTION);
            final int activityTypeIndex = cursor.getColumnIndexOrThrow(TracksColumns.ACTIVITY_TYPE);
            final int activityTypeLocalizedIndex = cursor.getColumnIndexOrThrow(TracksColumns.ACTIVITY_TYPE_LOCALIZED);
            final int startTimeIndex = cursor.getColumnIndexOrThrow(TracksColumns.STARTTIME);
            final int startTimeOffsetIndex = cursor.getColumnIndexOrThrow(TracksColumns.STARTTIME_OFFSET);
            final int totalDistanceIndex = cursor.getColumnIndexOrThrow(TracksColumns.TOTALDISTANCE);
            final int totalTimeIndex = cursor.getColumnIndexOrThrow(TracksColumns.TOTALTIME);
            final int markerCountIndex = cursor.getColumnIndexOrThrow(TracksColumns.MARKER_COUNT);

            ActivityType activityType = ActivityType.findBy(cursor.getString(activityTypeIndex));
            String name = cursor.getString(nameIndex);
            int markerCount = cursor.getInt(markerCountIndex);
            Duration totalTime = Duration.ofMillis(cursor.getLong(totalTimeIndex));
            Distance totalDistance = Distance.of(cursor.getFloat(totalDistanceIndex));
            Instant startTime = Instant.ofEpochMilli(cursor.getLong(startTimeIndex));
            ZoneOffset zoneOffset = ZoneOffset.ofTotalSeconds(cursor.getInt(startTimeOffsetIndex));
            String activityTypeLocalized = cursor.getString(activityTypeLocalizedIndex);
            String description = cursor.getString(descriptionIndex);
            trackId = new Track.Id(cursor.getLong(idIndex));

            int iconId = activityType.getIconDrawableId();
            int iconDesc = R.string.image_track;

            boolean isRecordingThisTrackRecording = trackId.equals(recordingStatus.trackId());
            if (isRecordingThisTrackRecording) {
                iconId = R.drawable.ic_track_recording;
                iconDesc = R.string.image_record;
            }

            viewBinding.trackListItemIcon.setImageResource(iconId);
            viewBinding.trackListItemIcon.setContentDescription(context.getString(iconDesc));

            viewBinding.trackListItemName.setText(name);

            String timeDistanceText = ListItemUtils.getTimeDistanceText(context, unitSystem, isRecordingThisTrackRecording, totalTime, totalDistance, markerCount);
            viewBinding.trackListItemTimeDistance.setText(timeDistanceText);

            viewBinding.trackListItemMarkerCountIcon.setVisibility(markerCount > 0 ? View.VISIBLE : View.GONE);
            viewBinding.trackListItemMarkerCount.setText(markerCount > 0 ? Integer.toString(markerCount) : null);

            if (!recordingStatus.isRecording()) {
                ListItemUtils.setDateAndTime(context, viewBinding.trackListItemDate, viewBinding.trackListItemTime, startTime, zoneOffset);
            } else {
                viewBinding.trackListItemDate.setText(null);
                viewBinding.trackListItemTime.setText(null);
            }

            //TODO Check if this is needed or a leftover from the MarkerList migration
            String category = activityType == null ? activityTypeLocalized : null;
            String categoryDescription = StringUtils.getCategoryDescription(category, description);
            viewBinding.trackListItemCategoryDescription.setText(categoryDescription);
            viewBinding.trackListItemCategoryDescription.setVisibility("".equals(categoryDescription) ? View.GONE : View.VISIBLE);

            setSelected(selection.get((int) getId()));
        }

        public void setSelected(boolean isSelected) {
            selection.put((int) getId(), isSelected);
            view.setActivated(isSelected);
        }

        public long getId() {
            return trackId.id();
        }


        @Override
        public void onClick(View v) {
            if (selectionMode) {
                setSelected(!view.isActivated());
                actionMode.invalidate();
                return;
            }

            if (recordingStatus.isRecording() && trackId.equals(recordingStatus.trackId())) {
                // Is recording -> open record activity.
                Intent newIntent = IntentUtils.newIntent(context, TrackRecordingActivity.class)
                        .putExtra(TrackRecordedActivity.EXTRA_TRACK_ID, trackId);
                context.startActivity(newIntent);
            } else {
                // Not recording -> open detail activity.
                Intent newIntent = IntentUtils.newIntent(context, TrackRecordedActivity.class)
                        .putExtra(TrackRecordedActivity.EXTRA_TRACK_ID, trackId);
                ActivityOptions activityOptions = ActivityOptions.makeSceneTransitionAnimation(
                        context,
                        new Pair<>(view.findViewById(R.id.track_list_item_icon), TrackRecordedActivity.VIEW_TRACK_ICON));
                context.startActivity(newIntent, activityOptions.toBundle());
            }
        }

        @Override
        public boolean onLongClick(View v) {
            setSelected(!view.isActivated());
            if (!selectionMode) {
                actionMode = context.startSupportActionMode(TrackListAdapter.this);
            } else {
                actionMode.invalidate();
            }
            return true;
        }
    }
}
