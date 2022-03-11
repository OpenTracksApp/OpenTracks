package de.dennisguse.opentracks.ui.customRecordingLayout;


import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.assertThat;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.CoreMatchers.is;
import static de.dennisguse.opentracks.util.EspressoUtils.veryLongTouch;

import android.view.View;

import androidx.recyclerview.widget.RecyclerView;
import androidx.test.espresso.NoMatchingViewException;
import androidx.test.espresso.ViewAssertion;
import androidx.test.espresso.ViewInteraction;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.rule.GrantPermissionRule;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import de.dennisguse.opentracks.R;
import de.dennisguse.opentracks.TrackListActivity;
import de.dennisguse.opentracks.settings.PreferencesUtils;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class EspressoCustomLayoutTest {

    @Rule
    public ActivityScenarioRule<TrackListActivity> mActivityTestRule = new ActivityScenarioRule<>(TrackListActivity.class);

    @Rule
    public GrantPermissionRule mGrantPermissionRule = GrantPermissionRule.grant(android.Manifest.permission.ACCESS_FINE_LOCATION);

    @Test
    public void customLayoutTest() {
        // TrackListActivity: start recording
        ViewInteraction trackControllerRecordButton = onView(ViewMatchers.withId(R.id.track_list_fab_action));
        trackControllerRecordButton.perform(click());

        // Get custom layout preferences and check all data fields are showed.
        Layout layout = PreferencesUtils.getCustomLayout();

        onView(withId(R.id.stats_recycler_view)).check(new RecyclerViewItemCountAssertion((int) layout.getFields().stream().filter(DataField::isVisible).count()));

        // stop recording
        ViewInteraction trackControllerStopButton = onView(withId(R.id.track_recording_fab_action));
        trackControllerStopButton.perform(veryLongTouch(1600));
    }

    private static class RecyclerViewItemCountAssertion implements ViewAssertion {
        private final int expectedCount;

        public RecyclerViewItemCountAssertion(int expectedCount) {
            this.expectedCount = expectedCount;
        }

        @Override
        public void check(View view, NoMatchingViewException noViewFoundException) {
            if (noViewFoundException != null) {
                throw noViewFoundException;
            }

            RecyclerView recyclerView = (RecyclerView) view;
            RecyclerView.Adapter<?> adapter = recyclerView.getAdapter();
            assertThat(adapter.getItemCount(), is(expectedCount));
        }
    }

}
