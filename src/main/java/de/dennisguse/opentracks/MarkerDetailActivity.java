/*
 * Copyright 2009 Google Inc.
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

package de.dennisguse.opentracks;

import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;

import java.util.ArrayList;

import de.dennisguse.opentracks.content.ContentProviderUtils;
import de.dennisguse.opentracks.content.Waypoint;
import de.dennisguse.opentracks.fragments.DeleteMarkerDialogFragment.DeleteMarkerCaller;
import de.dennisguse.opentracks.fragments.MarkerDetailFragment;

/**
 * An activity to display marker detail info.
 *
 * @author Leif Hendrik Wilden
 */
public class MarkerDetailActivity extends AbstractActivity implements DeleteMarkerCaller {

    public static final String EXTRA_MARKER_ID = "marker_id";

    private static final String TAG = MarkerDetailActivity.class.getSimpleName();

    private ArrayList<Long> markerIds;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        long markerId = getIntent().getLongExtra(EXTRA_MARKER_ID, -1L);
        if (markerId == -1L) {
            Log.d(TAG, "invalid marker id");
            finish();
            return;
        }

        ContentProviderUtils contentProviderUtils = ContentProviderUtils.Factory.get(this);
        Waypoint waypoint = contentProviderUtils.getWaypoint(markerId);

        markerIds = new ArrayList<>();
        int markerIndex = -1;

        try (Cursor cursor = contentProviderUtils.getWaypointCursor(waypoint.getTrackId(), -1L, -1)) {
            if (cursor != null && cursor.moveToFirst()) {
                /*
                 * Yes, this will skip the first waypoint and that is intentional as the
                 * first waypoint holds the stats for the track.
                 */
                while (cursor.moveToNext()) {
                    Waypoint current = contentProviderUtils.createWaypoint(cursor);

                    markerIds.add(current.getId());
                    if (current.getId() == markerId) {
                        markerIndex = markerIds.size() - 1;
                    }
                }
            }
        }

        ViewPager viewPager = findViewById(R.id.maker_detail_activity_view_pager);
        viewPager.setAdapter(new MarkerDetailPagerAdapter(getSupportFragmentManager()));
        if (markerIndex != -1) {
            viewPager.setCurrentItem(markerIndex);
        }
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.marker_detail_activity;
    }

    @Override
    public void onDeleteMarkerDone() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                finish();
            }
        });
    }

    /**
     * Marker detail pager adapter.
     *
     * @author Jimmy Shih
     */
    private class MarkerDetailPagerAdapter extends FragmentStatePagerAdapter {

        MarkerDetailPagerAdapter(FragmentManager fragmentManager) {
            super(fragmentManager);
        }

        @Override
        public Fragment getItem(int position) {
            String title = getString(R.string.marker_title, position + 1, getCount());
            return MarkerDetailFragment.newInstance(markerIds.get(position), title);
        }

        @Override
        public int getCount() {
            return markerIds.size();
        }
    }
}