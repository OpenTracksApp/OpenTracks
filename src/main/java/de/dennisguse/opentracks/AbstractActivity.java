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

package de.dennisguse.opentracks;

import android.content.res.TypedArray;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.ViewCompat;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import com.google.android.material.appbar.AppBarLayout;
import de.dennisguse.opentracks.services.announcement.TTSManager;
import de.dennisguse.opentracks.settings.PreferencesUtils;

/**
 * @author Jimmy Shih
 */
public abstract class AbstractActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (PreferencesUtils.shouldApplyOledTheme()) {
            setTheme(R.style.OpenTracksThemeOled);
        }

        // Enable edge-to-edge rendering
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);

        // Set volume control stream for text to speech
        setVolumeControlStream(TTSManager.AUDIO_STREAM);

        View rootView = getRootView();
        setContentView(rootView);

//        apply_insets((ViewGroup) rootView);
        apply_insets((ViewGroup) ((ViewGroup) rootView).getChildAt(0));
    }

    private void apply_insets(ViewGroup rootView) {
        if (rootView == null) {
            return;
        }
        // FIXME: function is called twice, even though it is actually only called once (???????)
        System.out.printf("apply_insets(%s)\n", rootView.getClass());

        // Apply navbar insets to the whole content (for landscape mode)
        // FIXME: is not called
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (view, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            // Apply the insets as a padding to the view.
            // Don't need to set the top inset for some reason (?)
            view.setPadding(insets.left, 0, insets.right, 0);
            // Return the insets to apply to the next view
            System.out.println("SET INSET FOR ROOT VIEW");
            return windowInsets;
        });

        // Apply bottom inset to root's children
        for (int i = 0; i < rootView.getChildCount(); i++) {
            View child = rootView.getChildAt(i);
            // Don't apply bottom inset to Top AppBar
            // FIXME: Does not go to next iteration even if child is AppBarLayout (???)
            if (child instanceof AppBarLayout) {
                continue;
            }

            // FIXME: does not get called for any other children
            ViewCompat.setOnApplyWindowInsetsListener(rootView, (view, windowInsets) -> {
                Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
                // Apply the insets as a padding to the view.
                view.setPadding(0, 0, 0, insets.bottom);
                // Return the insets to apply to the next view
                System.out.printf("SET INSET FOR CHILD %s\n", view.getClass());

                // Modify BottomAppBar height to compensate for the inset
                if (view.getId() == R.id.bottom_app_bar) {
                    System.out.println("View is BottomAppBar");
                    LayoutParams layout = view.getLayoutParams();
                    // Get actionBar height attribute
                    final TypedArray arr = obtainStyledAttributes(new int[]{com.google.android.material.R.attr.actionBarSize});
                    int actionBarHeight = (int) arr.getDimension(0, 0f);
                    arr.recycle();
                    // Apply new height
                    layout.height = insets.bottom + actionBarHeight;
                    view.setLayoutParams(layout);
                }

                return windowInsets;
            });
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        getOnBackPressedDispatcher().onBackPressed();
        return true;
    }

    protected abstract View getRootView();
}
