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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;

import java.util.ArrayList;
import java.util.List;

import de.dennisguse.opentracks.content.ContentProviderUtils;
import de.dennisguse.opentracks.content.Waypoint;
import de.dennisguse.opentracks.fragments.DeleteMarkerDialogFragment.DeleteMarkerCaller;
import de.dennisguse.opentracks.fragments.MarkerDetailFragment;

/**
 * An activity to display marker detail info.
 * <p>
 * Allows to swipe to the next and previous marker.
 *
 * @author Leif Hendrik Wilden
 */
public class MarkerDetailActivity extends AbstractActivity implements DeleteMarkerCaller {

    public static final String EXTRA_MARKER_ID = "marker_id";

    private static final String TAG = MarkerDetailActivity.class.getSimpleName();

    private List<Long> markerIds;

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
                for (int i = 0; i < cursor.getCount(); i++) {
                    Waypoint currentMarker = contentProviderUtils.createWaypoint(cursor);

                    if (!currentMarker.isTripStatistics()) {
                        markerIds.add(currentMarker.getId());
                        if (currentMarker.getId() == markerId) {
                            markerIndex = markerIds.size() - 1;
                        }
                    }

                    cursor.moveToNext();
                }
            }
        }

        final ViewPager viewPager = findViewById(R.id.maker_detail_activity_view_pager);
        final MarkerDetailPagerAdapter markerAdapter = new MarkerDetailPagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(markerAdapter);
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                setTitle(markerAdapter.getPageTitle(position));
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
        viewPager.setCurrentItem(markerIndex == -1 ? 0 : markerIndex);
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
        @NonNull
        public Fragment getItem(int position) {
            return MarkerDetailFragment.newInstance(markerIds.get(position));
        }

        @Nullable
        @Override
        public CharSequence getPageTitle(int position) {
            return getString(R.string.marker_title, position + 1, getCount());
        }

        @Override
        public int getCount() {
            return markerIds.size();
        }
    }
}