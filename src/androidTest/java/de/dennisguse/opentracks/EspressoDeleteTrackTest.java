package de.dennisguse.opentracks;


import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.longClick;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anything;
import static de.dennisguse.opentracks.util.EspressoUtils.waitFor;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.rule.GrantPermissionRule;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class EspressoDeleteTrackTest {

    @Rule
    public ActivityScenarioRule<TrackListActivity> mActivityTestRule = new ActivityScenarioRule<>(TrackListActivity.class);

    @Rule
    public GrantPermissionRule mGrantPermissionRule = GrantPermissionRule.grant(android.Manifest.permission.ACCESS_FINE_LOCATION);

    @Test
    public void espressoDeleteTrackTest() {
        // TrackListActivity: start recording
        onView(withId(R.id.track_list_fab_action)).perform(click());

        // TrackRecordingActivity
        onView(withId(R.id.track_recording_fab_action))
                // wait; stay recording
                .perform(waitFor(5000))
                // stop;
                .perform(longClick());

        // TrackStoppedActivity
        onView(withId(R.id.finish_button)).perform(click());

        // select track
        onData(anything()).inAdapterView(withId(R.id.track_list)).atPosition(0).perform(longClick());

        // open menu and delete selected track
        onView(allOf(withContentDescription("More options"), isDisplayed()))
                .perform(click());

        onView(withText("Delete")).perform(click());

        onView(withText("OK")).perform(click());

        // tracklist is empty now
        onView(allOf(withText("Start recording your next adventure here"), isDisplayed()));
    }
}
