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

package de.dennisguse.opentracks.ui.markers;

import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import de.dennisguse.opentracks.AbstractActivity;
import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.data.ContentProviderUtils;
import de.dennisguse.opentracks.data.models.Marker;
import de.dennisguse.opentracks.data.tables.MarkerColumns;
import de.dennisguse.opentracks.databinding.MarkerDetailActivityBinding;
import de.dennisguse.opentracks.ui.markers.DeleteMarkerDialogFragment.DeleteMarkerCaller;

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

    private MarkerDetailActivityBinding viewBinding;

    private Cursor cursor;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        Marker.Id markerId = getIntent().getParcelableExtra(EXTRA_MARKER_ID);
        if (markerId == null) {
            Log.d(TAG, "invalid marker id");
            finish();
            return;
        }

        ContentProviderUtils contentProviderUtils = new ContentProviderUtils(this);
        Marker marker = contentProviderUtils.getMarker(markerId);

        //TODO only load used data: ID + name
        cursor = contentProviderUtils.getMarkerCursor(marker.getTrackId(), null, -1);
        if (cursor == null) {
            finish();
        }

        int markerIndex = -1;
        if (cursor != null && cursor.moveToFirst()) {
            while (markerIndex == -1 && cursor.moveToNext()) {
                Marker.Id currentId = new Marker.Id(cursor.getLong(cursor.getColumnIndexOrThrow(MarkerColumns._ID)));
                if (markerId.equals(currentId)) {
                    markerIndex = cursor.getPosition();
                }
            }
        }


        final MarkerDetailPagerAdapter markerAdapter = new MarkerDetailPagerAdapter(this);
        viewBinding.makerDetailActivityViewPager.setAdapter(markerAdapter);
        viewBinding.makerDetailActivityViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                viewBinding.markerDetailToolbar.setTitle(markerAdapter.getPageTitle(position));
            }
        });
        viewBinding.makerDetailActivityViewPager.setCurrentItem(markerIndex == -1 ? 0 : markerIndex);

        setSupportActionBar(viewBinding.bottomAppBarLayout.bottomAppBar);
    }

    @NonNull
    @Override
    protected View createRootView() {
        viewBinding = MarkerDetailActivityBinding.inflate(getLayoutInflater());
        return viewBinding.getRoot();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        viewBinding = null;

        if (cursor != null) cursor.close();
        cursor = null;
    }

    @Override
    public void onMarkerDeleted() {
        runOnUiThread(this::finish);
    }

    private class MarkerDetailPagerAdapter extends FragmentStateAdapter {

        MarkerDetailPagerAdapter(@NonNull FragmentActivity fa) {
            super(fa);
        }

        @Override
        @NonNull
        public Fragment createFragment(int position) {
            cursor.moveToPosition(position);
            return MarkerDetailFragment.newInstance(new Marker.Id(cursor.getLong(cursor.getColumnIndexOrThrow(MarkerColumns._ID))));
        }

        @Override
        public int getItemCount() {
            return cursor.getCount();
        }

        @Nullable
        public CharSequence getPageTitle(int position) {
            cursor.moveToPosition(position);
            return getString(R.string.marker_detail_title, position + 1, getItemCount(), cursor.getString(cursor.getColumnIndexOrThrow(MarkerColumns.NAME)));
        }
    }
}