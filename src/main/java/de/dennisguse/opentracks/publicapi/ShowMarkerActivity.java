package de.dennisguse.opentracks.publicapi;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;

import java.util.ArrayList;
import java.util.List;

import de.dennisguse.opentracks.AbstractActivity;
import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.data.ContentProviderUtils;
import de.dennisguse.opentracks.data.models.Marker;
import de.dennisguse.opentracks.databinding.MarkerDetailActivityBinding;
import de.dennisguse.opentracks.ui.markers.DeleteMarkerDialogFragment.DeleteMarkerCaller;
import de.dennisguse.opentracks.ui.markers.MarkerDetailActivity;
import de.dennisguse.opentracks.ui.markers.MarkerDetailFragment;
import de.dennisguse.opentracks.util.IntentUtils;

/**
 * Public api to show an existing marker
 */
public class ShowMarkerActivity extends AppCompatActivity {

    public static final String EXTRA_MARKER_ID = "marker_id";

    private static final String TAG = ShowMarkerActivity.class.getSimpleName();

    private MarkerDetailActivityBinding viewBinding;

    private List<Marker.Id> markerIds;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        Marker.Id markerId = null;
        if (getIntent().hasExtra(EXTRA_MARKER_ID)) {
            markerId = new Marker.Id(getIntent().getLongExtra(EXTRA_MARKER_ID, 0));
        }
        if (markerId == null) {
            Log.d(TAG, "invalid marker id");
        } else {
            Intent intent = IntentUtils.newIntent(this, MarkerDetailActivity.class)
                    .putExtra(MarkerDetailActivity.EXTRA_MARKER_ID, markerId);
            startActivity(intent);
        }
        finish();
    }

}